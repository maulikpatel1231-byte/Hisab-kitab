package com.example.data.repository

import com.example.data.local.EmailGroupDao
import com.example.data.local.PersonDao
import com.example.data.local.ProjectDao
import com.example.data.local.TransactionDao
import com.example.data.model.EmailGroup
import com.example.data.model.Person
import com.example.data.model.PersonWithBalance
import com.example.data.model.Project
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HisabRepository(
    private val projectDao: ProjectDao,
    private val personDao: PersonDao,
    private val transactionDao: TransactionDao,
    private val emailGroupDao: EmailGroupDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allPersons: Flow<List<Person>> = personDao.getAllPersons()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allEmailGroups: Flow<List<EmailGroup>> = emailGroupDao.getAllGroups()

    suspend fun ensureDefaultProject(ownerEmail: String = "Maulikpatel1231@gmail.com"): Project {
        // Automatically clean up (Mera Hisab) from any previously saved project in existing database
        val existingProjects = projectDao.getAllProjectsDirect()
        for (proj in existingProjects) {
            if (proj.name.contains("Mera Hisab", ignoreCase = true) || proj.name.contains("(Main)") || proj.name == "Daily Book") {
                val cleanedName = if (proj.name == "Daily Book") "Hisab Kitab" else {
                    proj.name
                        .replace("(Mera Hisab)", "", ignoreCase = true)
                        .replace("(mera hisab)", "", ignoreCase = true)
                        .replace("(Main)", "")
                        .trim()
                        .ifEmpty { "Hisab Kitab" }
                }
                projectDao.updateProject(proj.copy(name = cleanedName))
            }
        }

        val count = projectDao.getProjectsCount()
        if (count == 0) {
            val defaultProject = Project(
                name = "Hisab Kitab",
                description = "Primary daily personal / business book",
                ownerEmail = ownerEmail,
                sharedEmails = "",
                colorHex = "#0F766E"
            )
            val newId = projectDao.insertProject(defaultProject)
            return defaultProject.copy(id = newId)
        }
        val defaultP = projectDao.getProjectByIdDirect(1L) ?: Project(id = 1L, name = "Hisab Kitab")
        return defaultP.copy(name = defaultP.displayName)
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: Project) {
        transactionDao.deleteTransactionsByProject(project.id)
        personDao.deletePersonsByProject(project.id)
        projectDao.deleteProject(project)
    }

    suspend fun addSharedEmailToProject(projectId: Long, newEmail: String) {
        val currentProject = projectDao.getProjectByIdDirect(projectId) ?: return
        val currentList = currentProject.getSharedEmailList().toMutableList()
        val trimmed = newEmail.trim().lowercase()
        if (trimmed.isNotEmpty() && !currentList.contains(trimmed)) {
            currentList.add(trimmed)
            val updated = currentProject.copy(sharedEmails = currentList.joinToString(","))
            projectDao.updateProject(updated)
        }
    }

    suspend fun removeSharedEmailFromProject(projectId: Long, emailToRemove: String) {
        val currentProject = projectDao.getProjectByIdDirect(projectId) ?: return
        val currentList = currentProject.getSharedEmailList().toMutableList()
        currentList.remove(emailToRemove.trim().lowercase())
        val updated = currentProject.copy(sharedEmails = currentList.joinToString(","))
        projectDao.updateProject(updated)
    }

    // Email Groups methods
    suspend fun insertEmailGroup(name: String, memberEmails: String): Long {
        val group = EmailGroup(name = name.trim(), memberEmails = memberEmails.trim())
        return emailGroupDao.insertGroup(group)
    }

    suspend fun updateEmailGroup(group: EmailGroup) {
        emailGroupDao.updateGroup(group)
    }

    suspend fun deleteEmailGroup(group: EmailGroup) {
        emailGroupDao.deleteGroup(group)
    }

    suspend fun deleteEmailGroupById(id: Long) {
        emailGroupDao.deleteGroupById(id)
    }

    fun getPersonsForProject(projectId: Long): Flow<List<Person>> {
        return personDao.getPersonsForProject(projectId)
    }

    fun getTransactionsForProject(projectId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForProject(projectId)
    }

    fun getPersonsWithBalancesForProject(projectId: Long): Flow<List<PersonWithBalance>> {
        return combine(
            personDao.getPersonsForProject(projectId),
            transactionDao.getTransactionsForProject(projectId)
        ) { persons, transactions ->
            persons.map { person ->
                val personTxs = transactions.filter { it.personId == person.id }
                var totalGiven = 0.0
                var totalGot = 0.0
                var lastTimestamp: Long? = null

                for (tx in personTxs) {
                    if (tx.type == TransactionType.OUTGOING.name) {
                        totalGiven += tx.amount
                    } else if (tx.type == TransactionType.INCOMING.name) {
                        totalGot += tx.amount
                    }
                    if (lastTimestamp == null || tx.timestamp > lastTimestamp) {
                        lastTimestamp = tx.timestamp
                    }
                }

                val net = totalGiven - totalGot
                PersonWithBalance(
                    person = person,
                    totalGiven = totalGiven,
                    totalGot = totalGot,
                    netBalance = net,
                    lastTransactionTimestamp = lastTimestamp,
                    transactionCount = personTxs.size
                )
            }
        }
    }

    fun getTransactionsForPerson(personId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByPerson(personId)
    }

    fun getPersonById(personId: Long): Flow<Person?> {
        return personDao.getPersonById(personId)
    }

    suspend fun insertPerson(
        person: Person,
        openingBalance: Double = 0.0,
        isReceivable: Boolean = true
    ): Long {
        val newPersonId = personDao.insertPerson(person)
        if (openingBalance > 0.009) {
            val txType = if (isReceivable) TransactionType.OUTGOING.name else TransactionType.INCOMING.name
            val note = if (isReceivable) "Opening Balance (Lena Hai)" else "Opening Balance (Dena Hai)"
            transactionDao.insertTransaction(
                Transaction(
                    projectId = person.projectId,
                    personId = newPersonId,
                    personName = person.name,
                    type = txType,
                    amount = openingBalance,
                    category = "Opening Balance",
                    paymentMode = "Cash",
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return newPersonId
    }

    suspend fun updatePerson(person: Person) {
        personDao.updatePerson(person)
    }

    suspend fun deletePerson(person: Person) {
        transactionDao.deleteTransactionsByPerson(person.id)
        personDao.deletePerson(person)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
}

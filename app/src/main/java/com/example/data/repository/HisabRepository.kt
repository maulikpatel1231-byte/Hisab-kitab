package com.example.data.repository

import com.example.data.local.PersonDao
import com.example.data.local.TransactionDao
import com.example.data.model.Person
import com.example.data.model.PersonWithBalance
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HisabRepository(
    private val personDao: PersonDao,
    private val transactionDao: TransactionDao
) {
    val allPersons: Flow<List<Person>> = personDao.getAllPersons()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Reactive computation of persons with their balances
    val personsWithBalances: Flow<List<PersonWithBalance>> =
        combine(personDao.getAllPersons(), transactionDao.getAllTransactions()) { persons, transactions ->
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

    fun getTransactionsForPerson(personId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByPerson(personId)
    }

    fun getPersonById(personId: Long): Flow<Person?> {
        return personDao.getPersonById(personId)
    }

    suspend fun insertPerson(
        person: Person,
        openingBalance: Double = 0.0,
        isReceivable: Boolean = true // true = Lena Hai (User gave), false = Dena Hai (User received)
    ): Long {
        val newPersonId = personDao.insertPerson(person)
        if (openingBalance > 0.009) {
            val txType = if (isReceivable) TransactionType.OUTGOING.name else TransactionType.INCOMING.name
            val note = if (isReceivable) "Opening Balance (Lena Hai)" else "Opening Balance (Dena Hai)"
            transactionDao.insertTransaction(
                Transaction(
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

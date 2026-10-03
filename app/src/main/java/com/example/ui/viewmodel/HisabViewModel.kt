package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.EmailGroup
import com.example.data.model.Person
import com.example.data.model.PersonType
import com.example.data.model.PersonWithBalance
import com.example.data.model.Project
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UserSession
import com.example.data.repository.HisabRepository
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardSummary(
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val netCashBalance: Double = 0.0,
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val totalPersonsCount: Int = 0,
    val totalTransactionsCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class HisabViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("hisab_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val repository = HisabRepository(
        database.projectDao(),
        database.personDao(),
        database.transactionDao(),
        database.emailGroupDao()
    )

    // Language & Onboarding State
    private val _currentLanguage = MutableStateFlow(loadLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // User Gmail Authentication Session
    private val _userSession = MutableStateFlow(loadUserSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    // Projects
    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProjectId = MutableStateFlow(prefs.getLong("active_project_id", 1L))
    val activeProjectId: StateFlow<Long> = _activeProjectId.asStateFlow()

    val activeProject: StateFlow<Project?> = combine(allProjects, _activeProjectId) { projects, currentId ->
        projects.find { it.id == currentId } ?: projects.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Email Groups
    val allEmailGroups: StateFlow<List<EmailGroup>> = repository.allEmailGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Project-scoped data
    val allPersons: StateFlow<List<Person>> = _activeProjectId
        .flatMapLatest { pid -> repository.getPersonsForProject(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = _activeProjectId
        .flatMapLatest { pid -> repository.getTransactionsForProject(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personsWithBalances: StateFlow<List<PersonWithBalance>> = _activeProjectId
        .flatMapLatest { pid -> repository.getPersonsWithBalancesForProject(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Navigation State
    private val _selectedTab = MutableStateFlow(0) // 0: Dashboard, 1: Parties, 2: Cashbook, 3: Reports
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<Long?>(null)
    val selectedPersonId: StateFlow<Long?> = _selectedPersonId.asStateFlow()

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _personFilter = MutableStateFlow("ALL")
    val personFilter: StateFlow<String> = _personFilter.asStateFlow()

    private val _cashbookTypeFilter = MutableStateFlow("ALL")
    val cashbookTypeFilter: StateFlow<String> = _cashbookTypeFilter.asStateFlow()

    private val _cashbookDateFilter = MutableStateFlow("ALL")
    val cashbookDateFilter: StateFlow<String> = _cashbookDateFilter.asStateFlow()

    // Dialog & Action States
    private val _isAddPersonDialogOpen = MutableStateFlow(false)
    val isAddPersonDialogOpen: StateFlow<Boolean> = _isAddPersonDialogOpen.asStateFlow()

    private val _isAddTransactionDialogOpen = MutableStateFlow(false)
    val isAddTransactionDialogOpen: StateFlow<Boolean> = _isAddTransactionDialogOpen.asStateFlow()

    private val _isAddProjectDialogOpen = MutableStateFlow(false)
    val isAddProjectDialogOpen: StateFlow<Boolean> = _isAddProjectDialogOpen.asStateFlow()

    private val _isShareProjectDialogOpen = MutableStateFlow(false)
    val isShareProjectDialogOpen: StateFlow<Boolean> = _isShareProjectDialogOpen.asStateFlow()

    private val _isGmailLoginDialogOpen = MutableStateFlow(false)
    val isGmailLoginDialogOpen: StateFlow<Boolean> = _isGmailLoginDialogOpen.asStateFlow()

    private val _isProjectSwitcherOpen = MutableStateFlow(false)
    val isProjectSwitcherOpen: StateFlow<Boolean> = _isProjectSwitcherOpen.asStateFlow()

    private val _isEmailGroupDialogOpen = MutableStateFlow(false)
    val isEmailGroupDialogOpen: StateFlow<Boolean> = _isEmailGroupDialogOpen.asStateFlow()

    private val _isLanguageDialogOpen = MutableStateFlow(false)
    val isLanguageDialogOpen: StateFlow<Boolean> = _isLanguageDialogOpen.asStateFlow()

    private val _transactionDialogPreselectedPerson = MutableStateFlow<Person?>(null)
    val transactionDialogPreselectedPerson: StateFlow<Person?> = _transactionDialogPreselectedPerson.asStateFlow()

    private val _transactionDialogPreselectedType = MutableStateFlow<TransactionType?>(null)
    val transactionDialogPreselectedType: StateFlow<TransactionType?> = _transactionDialogPreselectedType.asStateFlow()

    // Dashboard Summary reactive state
    val dashboardSummary: StateFlow<DashboardSummary> =
        combine(allTransactions, personsWithBalances) { txs, persons ->
            var cashIn = 0.0
            var cashOut = 0.0

            for (tx in txs) {
                if (tx.type == TransactionType.INCOMING.name) {
                    cashIn += tx.amount
                } else if (tx.type == TransactionType.OUTGOING.name) {
                    cashOut += tx.amount
                }
            }

            var totalRec = 0.0
            var totalPay = 0.0
            for (p in persons) {
                if (p.netBalance > 0.009) {
                    totalRec += p.netBalance
                } else if (p.netBalance < -0.009) {
                    totalPay += kotlin.math.abs(p.netBalance)
                }
            }

            DashboardSummary(
                totalCashIn = cashIn,
                totalCashOut = cashOut,
                netCashBalance = cashIn - cashOut,
                totalReceivable = totalRec,
                totalPayable = totalPay,
                totalPersonsCount = persons.size,
                totalTransactionsCount = txs.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    init {
        viewModelScope.launch {
            val user = _userSession.value
            repository.ensureDefaultProject(user.email.ifEmpty { "Maulikpatel1231@gmail.com" })
        }
    }

    private fun loadLanguage(): AppLanguage {
        val code = prefs.getString("selected_language", AppLanguage.HINDI.code) ?: AppLanguage.HINDI.code
        return AppLanguage.values().find { it.code == code } ?: AppLanguage.HINDI
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("selected_language", language.code).apply()
        _currentLanguage.value = language
        _isLanguageDialogOpen.value = false
    }

    fun completeOnboarding(language: AppLanguage, email: String, name: String) {
        setLanguage(language)
        if (email.isNotBlank()) {
            loginWithGmail(email, name)
        }
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
    }

    private fun loadUserSession(): UserSession {
        val isLoggedIn = prefs.getBoolean("user_logged_in", true)
        val email = prefs.getString("user_email", "Maulikpatel1231@gmail.com") ?: "Maulikpatel1231@gmail.com"
        val name = prefs.getString("user_name", "Maulik Patel") ?: "Maulik Patel"
        return UserSession(email = email, displayName = name, isLoggedIn = isLoggedIn)
    }

    fun loginWithGmail(email: String, name: String) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@") }
        prefs.edit()
            .putBoolean("user_logged_in", true)
            .putString("user_email", cleanEmail)
            .putString("user_name", cleanName)
            .apply()
        _userSession.value = UserSession(email = cleanEmail, displayName = cleanName, isLoggedIn = true)
        _isGmailLoginDialogOpen.value = false
    }

    fun logoutGmail() {
        prefs.edit().putBoolean("user_logged_in", false).apply()
        _userSession.value = _userSession.value.copy(isLoggedIn = false)
        _isGmailLoginDialogOpen.value = false
    }

    fun selectProject(projectId: Long) {
        _activeProjectId.value = projectId
        prefs.edit().putLong("active_project_id", projectId).apply()
        _selectedPersonId.value = null
        _isProjectSwitcherOpen.value = false
    }

    fun createProject(
        name: String,
        description: String,
        colorHex: String,
        initialSharedEmail: String = ""
    ) {
        viewModelScope.launch {
            val ownerEmail = _userSession.value.email.ifEmpty { "Maulikpatel1231@gmail.com" }
            val sharedList = if (initialSharedEmail.isNotBlank()) initialSharedEmail.trim().lowercase() else ""
            val newProject = Project(
                name = name.trim(),
                description = description.trim(),
                ownerEmail = ownerEmail,
                sharedEmails = sharedList,
                colorHex = colorHex.ifEmpty { "#0F766E" },
                createdAt = System.currentTimeMillis()
            )
            val newId = repository.insertProject(newProject)
            selectProject(newId)
            closeAddProjectDialog()
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            val remaining = allProjects.value.filter { it.id != project.id }
            if (remaining.isNotEmpty()) {
                selectProject(remaining.first().id)
            } else {
                val def = repository.ensureDefaultProject(_userSession.value.email)
                selectProject(def.id)
            }
        }
    }

    fun addSharedEmailToActiveProject(email: String) {
        viewModelScope.launch {
            val pid = _activeProjectId.value
            repository.addSharedEmailToProject(pid, email)
        }
    }

    fun removeSharedEmailFromActiveProject(email: String) {
        viewModelScope.launch {
            val pid = _activeProjectId.value
            repository.removeSharedEmailFromProject(pid, email)
        }
    }

    // Email Groups Management
    fun createEmailGroup(name: String, emails: String) {
        viewModelScope.launch {
            repository.insertEmailGroup(name, emails)
        }
    }

    fun deleteEmailGroup(group: EmailGroup) {
        viewModelScope.launch {
            repository.deleteEmailGroup(group)
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        if (_selectedPersonId.value != null) {
            _selectedPersonId.value = null
        }
    }

    fun openPersonDetail(personId: Long) {
        _selectedPersonId.value = personId
    }

    fun closePersonDetail() {
        _selectedPersonId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPersonFilter(filter: String) {
        _personFilter.value = filter
    }

    fun setCashbookTypeFilter(filter: String) {
        _cashbookTypeFilter.value = filter
    }

    fun setCashbookDateFilter(filter: String) {
        _cashbookDateFilter.value = filter
    }

    fun openAddPersonDialog() {
        _isAddPersonDialogOpen.value = true
    }

    fun closeAddPersonDialog() {
        _isAddPersonDialogOpen.value = false
    }

    fun openAddProjectDialog() {
        _isAddProjectDialogOpen.value = true
    }

    fun closeAddProjectDialog() {
        _isAddProjectDialogOpen.value = false
    }

    fun openShareProjectDialog() {
        _isShareProjectDialogOpen.value = true
    }

    fun closeShareProjectDialog() {
        _isShareProjectDialogOpen.value = false
    }

    fun openGmailLoginDialog() {
        _isGmailLoginDialogOpen.value = true
    }

    fun closeGmailLoginDialog() {
        _isGmailLoginDialogOpen.value = false
    }

    fun openProjectSwitcher() {
        _isProjectSwitcherOpen.value = true
    }

    fun closeProjectSwitcher() {
        _isProjectSwitcherOpen.value = false
    }

    fun openEmailGroupDialog() {
        _isEmailGroupDialogOpen.value = true
    }

    fun closeEmailGroupDialog() {
        _isEmailGroupDialogOpen.value = false
    }

    fun openLanguageDialog() {
        _isLanguageDialogOpen.value = true
    }

    fun closeLanguageDialog() {
        _isLanguageDialogOpen.value = false
    }

    fun openAddTransactionDialog(
        person: Person? = null,
        type: TransactionType? = null
    ) {
        _transactionDialogPreselectedPerson.value = person
        _transactionDialogPreselectedType.value = type
        _isAddTransactionDialogOpen.value = true
    }

    fun closeAddTransactionDialog() {
        _isAddTransactionDialogOpen.value = false
        _transactionDialogPreselectedPerson.value = null
        _transactionDialogPreselectedType.value = null
    }

    fun addPerson(
        name: String,
        phone: String,
        type: String,
        notes: String,
        openingBalance: Double,
        isReceivable: Boolean
    ) {
        viewModelScope.launch {
            val person = Person(
                projectId = _activeProjectId.value,
                name = name.trim(),
                phone = phone.trim(),
                type = type,
                notes = notes.trim(),
                createdAt = System.currentTimeMillis()
            )
            repository.insertPerson(person, openingBalance, isReceivable)
            closeAddPersonDialog()
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            repository.deletePerson(person)
            if (_selectedPersonId.value == person.id) {
                _selectedPersonId.value = null
            }
        }
    }

    fun addTransaction(
        personId: Long?,
        personName: String?,
        type: TransactionType,
        amount: Double,
        category: String,
        paymentMode: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                projectId = _activeProjectId.value,
                personId = personId,
                personName = personName,
                type = type.name,
                amount = amount,
                category = category.trim().ifEmpty { "General" },
                paymentMode = paymentMode,
                note = note.trim(),
                timestamp = timestamp
            )
            repository.insertTransaction(tx)
            closeAddTransactionDialog()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            if (allPersons.value.isNotEmpty()) return@launch

            val currentProjectId = _activeProjectId.value
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            // 1. Ramesh Kumar (Customer)
            val p1Id = repository.insertPerson(
                Person(
                    projectId = currentProjectId,
                    name = "Ramesh Kumar",
                    phone = "+91 98765 43210",
                    type = PersonType.CUSTOMER.name,
                    notes = "Regular grocery buyer"
                ),
                openingBalance = 1500.0,
                isReceivable = true
            )
            repository.insertTransaction(
                Transaction(
                    projectId = currentProjectId,
                    personId = p1Id,
                    personName = "Ramesh Kumar",
                    type = TransactionType.INCOMING.name,
                    amount = 500.0,
                    category = "Customer Payment",
                    paymentMode = "UPI",
                    note = "GPay received for bill #104",
                    timestamp = now - (dayMillis / 2)
                )
            )

            // 2. Sharma Ji Wholesale (Supplier)
            val p2Id = repository.insertPerson(
                Person(
                    projectId = currentProjectId,
                    name = "Sharma Ji Wholesale",
                    phone = "+91 91234 56789",
                    type = PersonType.SUPPLIER.name,
                    notes = "Goods vendor"
                ),
                openingBalance = 3200.0,
                isReceivable = false
            )
            repository.insertTransaction(
                Transaction(
                    projectId = currentProjectId,
                    personId = p2Id,
                    personName = "Sharma Ji Wholesale",
                    type = TransactionType.OUTGOING.name,
                    amount = 1200.0,
                    category = "Vendor Payment",
                    paymentMode = "Bank Transfer",
                    note = "NEFT part payment done",
                    timestamp = now - (dayMillis * 2)
                )
            )

            // 3. Amit Verma (Friend)
            repository.insertPerson(
                Person(
                    projectId = currentProjectId,
                    name = "Amit Verma",
                    phone = "+91 99887 76655",
                    type = PersonType.FRIEND.name,
                    notes = "College friend"
                ),
                openingBalance = 2000.0,
                isReceivable = true
            )

            // General cash flow entries
            repository.insertTransaction(
                Transaction(
                    projectId = currentProjectId,
                    personId = null,
                    personName = null,
                    type = TransactionType.INCOMING.name,
                    amount = 4500.0,
                    category = "Daily Sales",
                    paymentMode = "Cash",
                    note = "Counter cash collection",
                    timestamp = now - (dayMillis * 3)
                )
            )
            repository.insertTransaction(
                Transaction(
                    projectId = currentProjectId,
                    personId = null,
                    personName = null,
                    type = TransactionType.OUTGOING.name,
                    amount = 800.0,
                    category = "Shop Electricity",
                    paymentMode = "UPI",
                    note = "Monthly power bill paid",
                    timestamp = now - (dayMillis * 4)
                )
            )
        }
    }
}

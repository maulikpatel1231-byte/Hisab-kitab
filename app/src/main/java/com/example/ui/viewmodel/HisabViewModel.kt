package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Person
import com.example.data.model.PersonType
import com.example.data.model.PersonWithBalance
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.HisabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardSummary(
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val netCashBalance: Double = 0.0,
    val totalReceivable: Double = 0.0, // Lena hai (market udhaar)
    val totalPayable: Double = 0.0,    // Dena hai (market deydari)
    val totalPersonsCount: Int = 0,
    val totalTransactionsCount: Int = 0
)

class HisabViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HisabRepository(database.personDao(), database.transactionDao())

    val allPersons: StateFlow<List<Person>> = repository.allPersons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personsWithBalances: StateFlow<List<PersonWithBalance>> = repository.personsWithBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Navigation State
    private val _selectedTab = MutableStateFlow(0) // 0: Dashboard, 1: Parties, 2: Cashbook, 3: Reports
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<Long?>(null)
    val selectedPersonId: StateFlow<Long?> = _selectedPersonId.asStateFlow()

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _personFilter = MutableStateFlow("ALL") // ALL, RECEIVABLE, PAYABLE, SETTLED
    val personFilter: StateFlow<String> = _personFilter.asStateFlow()

    private val _cashbookTypeFilter = MutableStateFlow("ALL") // ALL, INCOMING, OUTGOING
    val cashbookTypeFilter: StateFlow<String> = _cashbookTypeFilter.asStateFlow()

    private val _cashbookDateFilter = MutableStateFlow("ALL") // ALL, TODAY, THIS_WEEK, THIS_MONTH
    val cashbookDateFilter: StateFlow<String> = _cashbookDateFilter.asStateFlow()

    // Dialog & Action States
    private val _isAddPersonDialogOpen = MutableStateFlow(false)
    val isAddPersonDialogOpen: StateFlow<Boolean> = _isAddPersonDialogOpen.asStateFlow()

    private val _isAddTransactionDialogOpen = MutableStateFlow(false)
    val isAddTransactionDialogOpen: StateFlow<Boolean> = _isAddTransactionDialogOpen.asStateFlow()

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
            // Check if already seeded to avoid double seeding
            if (allPersons.value.isNotEmpty()) return@launch

            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            // 1. Ramesh Kumar (Grahak / Customer - owes money)
            val p1Id = repository.insertPerson(
                Person(name = "Ramesh Kumar", phone = "+91 98765 43210", type = PersonType.CUSTOMER.name, notes = "Regular grocery buyer"),
                openingBalance = 1500.0,
                isReceivable = true
            )
            // Add a repayment from Ramesh
            repository.insertTransaction(
                Transaction(
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

            // 2. Sharma Ji Wholesale (Supplier - we owe him)
            val p2Id = repository.insertPerson(
                Person(name = "Sharma Ji Wholesale", phone = "+91 91234 56789", type = PersonType.SUPPLIER.name, notes = "Goods vendor"),
                openingBalance = 3200.0,
                isReceivable = false // We owe him
            )
            repository.insertTransaction(
                Transaction(
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
            val p3Id = repository.insertPerson(
                Person(name = "Amit Verma", phone = "+91 99887 76655", type = PersonType.FRIEND.name, notes = "College friend"),
                openingBalance = 2000.0,
                isReceivable = true
            )

            // General cash in & out (sales & expenses)
            repository.insertTransaction(
                Transaction(
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
            repository.insertTransaction(
                Transaction(
                    personId = null,
                    personName = null,
                    type = TransactionType.OUTGOING.name,
                    amount = 350.0,
                    category = "Tea & Snacks",
                    paymentMode = "Cash",
                    note = "Office tea & refreshments",
                    timestamp = now - (dayMillis * 1)
                )
            )
        }
    }
}

package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.components.AddPersonDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.screens.CashbookScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PersonDetailScreen
import com.example.ui.screens.PersonsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.viewmodel.HisabViewModel

sealed class NavItem(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Parties : NavItem("parties", "Parties", Icons.Filled.Group, Icons.Outlined.Group)
    object Cashbook : NavItem("cashbook", "Cashbook", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object Reports : NavItem("reports", "Reports", Icons.Filled.Analytics, Icons.Outlined.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedPersonId by viewModel.selectedPersonId.collectAsStateWithLifecycle()

    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val allPersons by viewModel.allPersons.collectAsStateWithLifecycle()
    val personsWithBalances by viewModel.personsWithBalances.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val personFilter by viewModel.personFilter.collectAsStateWithLifecycle()
    val cashbookTypeFilter by viewModel.cashbookTypeFilter.collectAsStateWithLifecycle()
    val cashbookDateFilter by viewModel.cashbookDateFilter.collectAsStateWithLifecycle()

    val isAddPersonDialogOpen by viewModel.isAddPersonDialogOpen.collectAsStateWithLifecycle()
    val isAddTransactionDialogOpen by viewModel.isAddTransactionDialogOpen.collectAsStateWithLifecycle()
    val preselectedPersonForTx by viewModel.transactionDialogPreselectedPerson.collectAsStateWithLifecycle()
    val preselectedTypeForTx by viewModel.transactionDialogPreselectedType.collectAsStateWithLifecycle()

    val navItems = listOf(
        NavItem.Dashboard,
        NavItem.Parties,
        NavItem.Cashbook,
        NavItem.Reports
    )

    // Check if viewing a specific person
    val activePersonWithBalance = selectedPersonId?.let { id ->
        personsWithBalances.find { it.person.id == id }
    }

    if (activePersonWithBalance != null) {
        val personTransactions = allTransactions.filter { it.personId == activePersonWithBalance.person.id }
        PersonDetailScreen(
            personWithBalance = activePersonWithBalance,
            transactions = personTransactions,
            onBack = { viewModel.closePersonDetail() },
            onAddEntry = { type ->
                viewModel.openAddTransactionDialog(
                    person = activePersonWithBalance.person,
                    type = type
                )
            },
            onDeletePerson = { person -> viewModel.deletePerson(person) },
            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Hisab Kitab (खाता & रोकड़)"
                                1 -> "Parties & Log (व्यक्ति खाते)"
                                2 -> "Cashbook (रोकड़ बही)"
                                else -> "Reports & Analytics (विवरण)"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    navItems.forEachIndexed { index, item ->
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.setSelectedTab(index)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        summary = dashboardSummary,
                        persons = personsWithBalances,
                        recentTransactions = allTransactions,
                        onAddCashIn = {
                            viewModel.openAddTransactionDialog(type = TransactionType.INCOMING)
                        },
                        onAddCashOut = {
                            viewModel.openAddTransactionDialog(type = TransactionType.OUTGOING)
                        },
                        onAddPerson = { viewModel.openAddPersonDialog() },
                        onPersonClick = { personId -> viewModel.openPersonDetail(personId) },
                        onViewAllPersons = { viewModel.setSelectedTab(1) },
                        onViewAllCashbook = { viewModel.setSelectedTab(2) },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onSeedSampleData = { viewModel.seedSampleData() }
                    )

                    1 -> PersonsScreen(
                        persons = personsWithBalances,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        activeFilter = personFilter,
                        onFilterChange = { f -> viewModel.setPersonFilter(f) },
                        onPersonClick = { personId -> viewModel.openPersonDetail(personId) },
                        onAddPerson = { viewModel.openAddPersonDialog() }
                    )

                    2 -> CashbookScreen(
                        transactions = allTransactions,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        typeFilter = cashbookTypeFilter,
                        onTypeFilterChange = { f -> viewModel.setCashbookTypeFilter(f) },
                        dateFilter = cashbookDateFilter,
                        onDateFilterChange = { f -> viewModel.setCashbookDateFilter(f) },
                        onAddTransaction = { viewModel.openAddTransactionDialog() },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onPersonClick = { personId -> viewModel.openPersonDetail(personId) }
                    )

                    3 -> ReportsScreen(
                        summary = dashboardSummary,
                        persons = personsWithBalances,
                        transactions = allTransactions
                    )
                }
            }
        }
    }

    // Dialogs
    if (isAddPersonDialogOpen) {
        AddPersonDialog(
            onDismiss = { viewModel.closeAddPersonDialog() },
            onSave = { name, phone, type, notes, openingBalance, isReceivable ->
                viewModel.addPerson(name, phone, type, notes, openingBalance, isReceivable)
            }
        )
    }

    if (isAddTransactionDialogOpen) {
        AddTransactionDialog(
            persons = allPersons,
            preselectedPerson = preselectedPersonForTx,
            preselectedType = preselectedTypeForTx,
            onDismiss = { viewModel.closeAddTransactionDialog() },
            onSave = { personId, personName, type, amount, category, paymentMode, note ->
                viewModel.addTransaction(
                    personId = personId,
                    personName = personName,
                    type = type,
                    amount = amount,
                    category = category,
                    paymentMode = paymentMode,
                    note = note
                )
            }
        )
    }
}

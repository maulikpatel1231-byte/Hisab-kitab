package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.components.AddPersonDialog
import com.example.ui.components.AddProjectDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.GmailLoginDialog
import com.example.ui.components.ProjectSwitcherSheet
import com.example.ui.components.ShareProjectDialog
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

    val userSession by viewModel.userSession.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val activeProjectId by viewModel.activeProjectId.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()

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
    val isAddProjectDialogOpen by viewModel.isAddProjectDialogOpen.collectAsStateWithLifecycle()
    val isShareProjectDialogOpen by viewModel.isShareProjectDialogOpen.collectAsStateWithLifecycle()
    val isGmailLoginDialogOpen by viewModel.isGmailLoginDialogOpen.collectAsStateWithLifecycle()
    val isProjectSwitcherOpen by viewModel.isProjectSwitcherOpen.collectAsStateWithLifecycle()

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
                        // Project Selector Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.openProjectSwitcher() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("topbar_project_selector")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Projects",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeProject?.name ?: "Hisab Kitab",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = if (userSession.isLoggedIn) userSession.email else "Guest Mode",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // Share with Gmail Button
                        IconButton(
                            onClick = { viewModel.openShareProjectDialog() },
                            modifier = Modifier.testTag("topbar_share_gmail_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Share via Gmail",
                                    tint = Color(0xFFEA4335),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Add Project Button
                        IconButton(
                            onClick = { viewModel.openAddProjectDialog() },
                            modifier = Modifier.testTag("topbar_add_project_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Project",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // User Gmail Avatar / Account Switcher
                        IconButton(
                            onClick = { viewModel.openGmailLoginDialog() },
                            modifier = Modifier.testTag("topbar_account_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (userSession.isLoggedIn) MaterialTheme.colorScheme.primary else Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userSession.isLoggedIn) {
                                    Text(
                                        text = userSession.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Sign in",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
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
                        activeProject = activeProject,
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
                        onSeedSampleData = { viewModel.seedSampleData() },
                        onSwitchProject = { viewModel.openProjectSwitcher() },
                        onShareProject = { viewModel.openShareProjectDialog() },
                        onAddNewProject = { viewModel.openAddProjectDialog() }
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

    if (isAddProjectDialogOpen) {
        AddProjectDialog(
            onDismiss = { viewModel.closeAddProjectDialog() },
            onSave = { name, description, colorHex, initialSharedEmail ->
                viewModel.createProject(name, description, colorHex, initialSharedEmail)
            }
        )
    }

    if (isShareProjectDialogOpen && activeProject != null) {
        ShareProjectDialog(
            project = activeProject!!,
            persons = personsWithBalances,
            transactions = allTransactions,
            onDismiss = { viewModel.closeShareProjectDialog() },
            onAddSharedEmail = { email -> viewModel.addSharedEmailToActiveProject(email) },
            onRemoveSharedEmail = { email -> viewModel.removeSharedEmailFromActiveProject(email) }
        )
    }

    if (isGmailLoginDialogOpen) {
        GmailLoginDialog(
            currentSession = userSession,
            onDismiss = { viewModel.closeGmailLoginDialog() },
            onLogin = { email, name -> viewModel.loginWithGmail(email, name) },
            onLogout = { viewModel.logoutGmail() }
        )
    }

    if (isProjectSwitcherOpen) {
        ProjectSwitcherSheet(
            projects = allProjects,
            activeProjectId = activeProjectId,
            onSelectProject = { pid -> viewModel.selectProject(pid) },
            onAddNewProject = { viewModel.openAddProjectDialog() },
            onDeleteProject = { project -> viewModel.deleteProject(project) },
            onDismiss = { viewModel.closeProjectSwitcher() }
        )
    }
}

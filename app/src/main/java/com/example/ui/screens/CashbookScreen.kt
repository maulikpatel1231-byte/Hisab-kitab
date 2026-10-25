package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyGreenLight
import com.example.ui.theme.MoneyRed
import com.example.ui.theme.MoneyRedLight
import com.example.ui.util.AppStrings
import com.example.ui.util.FormatUtils
import java.util.Calendar

@Composable
fun CashbookScreen(
    transactions: List<Transaction>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    typeFilter: String, // ALL, INCOMING, OUTGOING
    onTypeFilterChange: (String) -> Unit,
    dateFilter: String, // ALL, TODAY, THIS_WEEK, THIS_MONTH
    onDateFilterChange: (String) -> Unit,
    strings: AppStrings,
    onAddCashIn: () -> Unit,
    onAddCashOut: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onPersonClick: (Long) -> Unit,
    onSharePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = Calendar.getInstance()

    val filteredTransactions = transactions.filter { tx ->
        // Search query
        val matchesQuery = (tx.personName?.contains(searchQuery, ignoreCase = true) == true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true)

        // Type filter
        val matchesType = when (typeFilter) {
            "INCOMING" -> tx.type == TransactionType.INCOMING.name
            "OUTGOING" -> tx.type == TransactionType.OUTGOING.name
            else -> true
        }

        // Date filter
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        val matchesDate = when (dateFilter) {
            "TODAY" -> now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
            "THIS_WEEK" -> now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                    now.get(Calendar.WEEK_OF_YEAR) == txCal.get(Calendar.WEEK_OF_YEAR)
            "THIS_MONTH" -> now.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                    now.get(Calendar.MONTH) == txCal.get(Calendar.MONTH)
            else -> true
        }

        matchesQuery && matchesType && matchesDate
    }

    val totalIn = filteredTransactions
        .filter { it.type == TransactionType.INCOMING.name }
        .sumOf { it.amount }

    val totalOut = filteredTransactions
        .filter { it.type == TransactionType.OUTGOING.name }
        .sumOf { it.amount }

    val netTotal = totalIn - totalOut

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cashbook_screen"),
            contentPadding = PaddingValues(bottom = 110.dp, top = 12.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input & PDF Button Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_tx_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Quick PDF Share Icon Button
                    Surface(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSharePdf() }
                            .testTag("btn_cashbook_share_pdf"),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Share PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Type Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = typeFilter == "ALL",
                        onClick = { onTypeFilterChange("ALL") },
                        label = { Text("${strings.filterAll} (${transactions.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = typeFilter == "INCOMING",
                        onClick = { onTypeFilterChange("INCOMING") },
                        label = { Text(strings.filterCashIn, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MoneyGreenLight,
                            selectedLabelColor = MoneyGreen
                        )
                    )
                    FilterChip(
                        selected = typeFilter == "OUTGOING",
                        onClick = { onTypeFilterChange("OUTGOING") },
                        label = { Text(strings.filterCashOut, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MoneyRedLight,
                            selectedLabelColor = MoneyRed
                        )
                    )
                }
            }

            // Date Period Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dateOptions = listOf("ALL" to "All Time", "TODAY" to "Today", "THIS_WEEK" to "This Week", "THIS_MONTH" to "This Month")
                    dateOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = dateFilter == key,
                            onClick = { onDateFilterChange(key) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Summary Bar for filtered transactions
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.cashIn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalIn),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MoneyGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.cashOut,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalOut),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MoneyRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.netBalance,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatCurrency(netTotal),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (netTotal >= 0) MaterialTheme.colorScheme.primary else MoneyRed
                            )
                        }
                    }
                }
            }

            // Transaction items
            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No entries found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '${strings.btnCashIn}' or '${strings.btnCashOut}' below to add a transaction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onDelete = onDeleteTransaction,
                        onPersonClick = { personId -> onPersonClick(personId) }
                    )
                }
            }
        }

        // Dual Action Bottom FAB Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // + Cash In (Incoming Entry)
                Button(
                    onClick = onAddCashIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cashbook_fab_cash_in"),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.btnCashIn, fontWeight = FontWeight.Bold)
                }

                // - Cash Out (Outgoing Entry)
                Button(
                    onClick = onAddCashOut,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cashbook_fab_cash_out"),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneyRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.btnCashOut, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

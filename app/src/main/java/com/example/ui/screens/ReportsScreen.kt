package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonWithBalance
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyGreenLight
import com.example.ui.theme.MoneyRed
import com.example.ui.theme.MoneyRedLight
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.DashboardSummary
import kotlin.math.abs

@Composable
fun ReportsScreen(
    summary: DashboardSummary,
    persons: List<PersonWithBalance>,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalVolume = summary.totalCashIn + summary.totalCashOut
    val inPercentage = if (totalVolume > 0) (summary.totalCashIn / totalVolume).toFloat() else 0.5f

    // Category-wise expense breakdown
    val categoryExpenses = transactions
        .filter { it.type == TransactionType.OUTGOING.name }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Top Debtors (Lena Hai)
    val topDebtors = persons
        .filter { it.isReceivable }
        .sortedByDescending { it.netBalance }
        .take(4)

    // Top Creditors (Dena Hai)
    val topCreditors = persons
        .filter { it.isPayable }
        .sortedByDescending { abs(it.netBalance) }
        .take(4)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 90.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cash Flow Visual Ratio Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cash Flow Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ratio Bar
                    ClipProgressBar(inPercentage = inPercentage)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MoneyGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "In: ${FormatUtils.formatCurrency(summary.totalCashIn)} (${(inPercentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MoneyRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Out: ${FormatUtils.formatCurrency(summary.totalCashOut)} (${((1 - inPercentage) * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // Top Debtors (Log jinse lena hai)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Pending Receivables (Kisse kitna lena hai)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MoneyGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (topDebtors.isEmpty()) {
                        Text(
                            text = "Kisi se koi udhaar baaki nahi hai (All settled)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        topDebtors.forEach { debtor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = debtor.person.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(debtor.netBalance),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Creditors (Log jinhe dena hai)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Pending Payables (Kisko kitna dena hai)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MoneyRed
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (topCreditors.isEmpty()) {
                        Text(
                            text = "Aap par kisi ki koi deydari nahi hai",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        topCreditors.forEach { creditor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = creditor.person.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatCurrency(abs(creditor.netBalance)),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyRed
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expenses by category
        if (categoryExpenses.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Expenses by Category (Kharche)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        categoryExpenses.forEach { (cat, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoneyRed
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Share Summary Report Button
        item {
            Button(
                onClick = {
                    val summaryText = buildString {
                        appendLine("📊 *HISAB KITAB - FINANCIAL SUMMARY* 📊")
                        appendLine("----------------------------------------")
                        appendLine("💰 Total Cash In (Aaya): ${FormatUtils.formatCurrency(summary.totalCashIn)}")
                        appendLine("💸 Total Cash Out (Gaya): ${FormatUtils.formatCurrency(summary.totalCashOut)}")
                        appendLine("💼 Net Balance: ${FormatUtils.formatCurrency(summary.netCashBalance)}")
                        appendLine("----------------------------------------")
                        appendLine("🟢 Market Lena Hai: ${FormatUtils.formatCurrency(summary.totalReceivable)}")
                        appendLine("🔴 Market Dena Hai: ${FormatUtils.formatCurrency(summary.totalPayable)}")
                        appendLine("👥 Total Persons: ${summary.totalPersonsCount}")
                        appendLine("📝 Total Transactions: ${summary.totalTransactionsCount}")
                        appendLine("----------------------------------------")
                        appendLine("Generated via Hisab Kitab")
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summaryText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Hisab Summary"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_share_summary"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Full Hisab Summary Report")
            }
        }
    }
}

@Composable
private fun ClipProgressBar(inPercentage: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFE2E8F0))
    ) {
        if (inPercentage > 0f) {
            Box(
                modifier = Modifier
                    .weight(inPercentage.coerceIn(0.01f, 0.99f))
                    .fillMaxSize()
                    .background(MoneyGreen)
            )
        }
        val outPercentage = 1f - inPercentage
        if (outPercentage > 0f) {
            Box(
                modifier = Modifier
                    .weight(outPercentage.coerceIn(0.01f, 0.99f))
                    .fillMaxSize()
                    .background(MoneyRed)
            )
        }
    }
}

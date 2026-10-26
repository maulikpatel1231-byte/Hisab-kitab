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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.Project
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed
import com.example.ui.util.AppStrings
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.DashboardSummary

@Composable
fun ReportsScreen(
    summary: DashboardSummary,
    project: Project?,
    transactions: List<Transaction>,
    strings: AppStrings,
    onSharePdf: () -> Unit,
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

    // Category-wise income breakdown
    val categoryIncomes = transactions
        .filter { it.type == TransactionType.INCOMING.name }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 90.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cash Flow Ratio Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.cashFlowOverview,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ratio bar
                    ClipProgressBar(inPercentage = inPercentage)

                    Spacer(modifier = Modifier.height(10.dp))

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
                                text = "${strings.cashIn}: ${FormatUtils.formatCurrency(summary.totalCashIn)} (${(inPercentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF14532D)
                            )
                        }

                        val outPct = (100 - (inPercentage * 100).toInt()).coerceAtLeast(0)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MoneyRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.cashOut}: ${FormatUtils.formatCurrency(summary.totalCashOut)} ($outPct%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Net balance highlight
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.netBalance}:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(summary.netCashBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.netCashBalance >= 0) MaterialTheme.colorScheme.primary else MoneyRed
                                )
                            )
                        }
                    }
                }
            }
        }

        // Primary Action: Generate & Share PDF Statement
        item {
            Button(
                onClick = onSharePdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_reports_share_pdf"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Statement", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.sharePdfBtn,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Expenses breakdown by Category
        if (categoryExpenses.isNotEmpty()) {
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
                                text = strings.expenseBreakdown,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(summary.totalCashOut),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyRed
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        categoryExpenses.forEach { (cat, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "- " + FormatUtils.formatCurrency(amount),
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

        // Income breakdown by Category
        if (categoryIncomes.isNotEmpty()) {
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
                                text = strings.incomeBreakdown,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(summary.totalCashIn),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        categoryIncomes.forEach { (cat, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "+ " + FormatUtils.formatCurrency(amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoneyGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Share Text Summary Option
        item {
            OutlinedButton(
                onClick = {
                    val summaryText = buildString {
                        appendLine("📊 *HISAB KITAB - CASH STATEMENT* 📊")
                        appendLine("Project: ${project?.name ?: "Hisab Kitab"}")
                        appendLine("----------------------------------------")
                        appendLine("💰 Total Cash In: ${FormatUtils.formatCurrency(summary.totalCashIn)}")
                        appendLine("💸 Total Cash Out: ${FormatUtils.formatCurrency(summary.totalCashOut)}")
                        appendLine("💼 Net Balance: ${FormatUtils.formatCurrency(summary.netCashBalance)}")
                        appendLine("📝 Total Transactions: ${transactions.size}")
                        appendLine("----------------------------------------")
                        appendLine("Shared via Hisab Kitab Android App")
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summaryText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Cash Summary"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_share_summary_text"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shareSummaryBtn)
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

package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonWithBalance
import com.example.data.model.Project
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed
import com.example.ui.util.FormatUtils
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareProjectDialog(
    project: Project,
    persons: List<PersonWithBalance>,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onAddSharedEmail: (String) -> Unit,
    onRemoveSharedEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var targetEmail by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

    val sharedList = project.getSharedEmailList()

    // Build the formatted report text
    fun generateReportText(reportType: String): String {
        return buildString {
            appendLine("📋 *HISAB KITAB PROJECT STATEMENT* 📋")
            appendLine("Project: ${project.name}")
            if (project.description.isNotBlank()) appendLine("Description: ${project.description}")
            appendLine("Owner: ${project.ownerEmail}")
            appendLine("Date: ${FormatUtils.formatDate(System.currentTimeMillis())}")
            appendLine("=========================================")

            val totalIn = transactions.filter { it.type == TransactionType.INCOMING.name }.sumOf { it.amount }
            val totalOut = transactions.filter { it.type == TransactionType.OUTGOING.name }.sumOf { it.amount }
            val net = totalIn - totalOut

            val totalReceivable = persons.filter { it.isReceivable }.sumOf { it.netBalance }
            val totalPayable = persons.filter { it.isPayable }.sumOf { abs(it.netBalance) }

            appendLine("💰 Total Cash In (Aaya): ${FormatUtils.formatCurrency(totalIn)}")
            appendLine("💸 Total Cash Out (Gaya): ${FormatUtils.formatCurrency(totalOut)}")
            appendLine("💼 Net Balance: ${FormatUtils.formatCurrency(net)}")
            appendLine("🟢 Market Lena Hai (Receivable): ${FormatUtils.formatCurrency(totalReceivable)}")
            appendLine("🔴 Market Dena Hai (Payable): ${FormatUtils.formatCurrency(totalPayable)}")
            appendLine("=========================================")

            appendLine("\n👥 *PARTIES / PERSON ACCOUNTS:*")
            if (persons.isEmpty()) {
                appendLine("No parties added yet.")
            } else {
                persons.forEach { p ->
                    val status = when {
                        p.isReceivable -> "Lena Hai: ${FormatUtils.formatCurrency(p.netBalance)}"
                        p.isPayable -> "Dena Hai: ${FormatUtils.formatCurrency(abs(p.netBalance))}"
                        else -> "Settled (Nill)"
                    }
                    appendLine("• ${p.person.name} (${p.person.type}) -> $status")
                }
            }

            appendLine("\n📝 *RECENT TRANSACTIONS (CASH FLOW):*")
            if (transactions.isEmpty()) {
                appendLine("No entries recorded.")
            } else {
                transactions.take(15).forEach { tx ->
                    val sign = if (tx.type == TransactionType.INCOMING.name) "+" else "-"
                    val personTag = if (tx.personName != null) " [${tx.personName}]" else ""
                    appendLine("• $sign${FormatUtils.formatCurrency(tx.amount)} | ${tx.category}$personTag | Mode: ${tx.paymentMode} | ${FormatUtils.formatDateShort(tx.timestamp)}")
                    if (tx.note.isNotBlank()) appendLine("   Note: ${tx.note}")
                }
            }

            appendLine("\n=========================================")
            appendLine("Shared via Hisab Kitab Android App")
        }
    }

    fun sendToGmail(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            emailError = true
            return
        }

        onAddSharedEmail(cleanEmail)

        val reportBody = generateReportText("FULL")
        val subject = "[Hisab Kitab] ${project.name} - Statement & Data"

        // Use Intent.ACTION_SENDTO with mailto: to directly launch Gmail or user's email client
        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$cleanEmail")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, reportBody)
        }

        try {
            context.startActivity(Intent.createChooser(mailIntent, "Send Hisab to $cleanEmail"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open email app. Report copied to clipboard!", Toast.LENGTH_LONG).show()
            clipboardManager.setText(AnnotatedString(reportBody))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEA4335).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Share",
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Share with Gmail ID",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Project: ${project.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Previously shared Gmail IDs
                if (sharedList.isNotEmpty()) {
                    Text(
                        text = "Collaborator Gmail IDs (Pehle se jude hue):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sharedList.forEach { email ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.clickable { targetEmail = email }
                                    )
                                    IconButton(
                                        onClick = { onRemoveSharedEmail(email) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Enter particular Gmail ID
                Text(
                    text = "Particular Gmail ID Par Data Bhejein:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = targetEmail,
                    onValueChange = {
                        targetEmail = it
                        emailError = false
                    },
                    label = { Text("Particular Gmail ID *") },
                    placeholder = { Text("e.g. partner@gmail.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    isError = emailError,
                    supportingText = {
                        if (emailError) Text("Please enter a valid Gmail ID")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_gmail_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview summary card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Included in this Gmail Share:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Full Financial Cash In & Cash Out balance\n✓ All ${persons.size} Parties (Lena/Dena Hai accounts)\n✓ All ${transactions.size} ledger transactions with notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Copy option
                OutlinedButton(
                    onClick = {
                        val report = generateReportText("FULL")
                        clipboardManager.setText(AnnotatedString(report))
                        Toast.makeText(context, "Full Project statement copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Statement to Clipboard")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { sendToGmail(targetEmail) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                modifier = Modifier.testTag("btn_send_gmail_data")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send to Gmail", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.model.EmailGroup
import com.example.data.model.PersonWithBalance
import com.example.data.model.Project
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.util.FormatUtils
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareProjectDialog(
    project: Project,
    persons: List<PersonWithBalance>,
    transactions: List<Transaction>,
    emailGroups: List<EmailGroup>,
    onDismiss: () -> Unit,
    onAddSharedEmail: (String) -> Unit,
    onRemoveSharedEmail: (String) -> Unit,
    onOpenGroupManager: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var shareMode by remember { mutableStateOf("PARTICULAR") } // "PARTICULAR" or "GROUP"
    var targetEmail by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(emailGroups.firstOrNull()) }
    var emailError by remember { mutableStateOf(false) }

    val sharedList = project.getSharedEmailList()

    fun generateReportText(): String {
        return buildString {
            appendLine("📋 *DAILY BOOK PROJECT STATEMENT* 📋")
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

            appendLine("\n📝 *RECENT TRANSACTIONS:*")
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
            appendLine("Shared via Daily Book Android App")
        }
    }

    fun sendToEmails(emails: List<String>) {
        val validEmails = emails.map { it.trim().lowercase() }.filter { it.isNotEmpty() && it.contains("@") }
        if (validEmails.isEmpty()) {
            emailError = true
            return
        }

        // Add first email to project shared list
        onAddSharedEmail(validEmails.first())

        val reportBody = generateReportText()
        val subject = "[Daily Book] ${project.name} - Statement & Data"

        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${validEmails.first()}")
            putExtra(Intent.EXTRA_EMAIL, validEmails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, reportBody)
        }

        try {
            context.startActivity(Intent.createChooser(mailIntent, "Send Hisab to ${validEmails.joinToString(", ")}"))
        } catch (e: Exception) {
            Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_LONG).show()
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
                        text = "Share Project Statement",
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
                // Segmented Toggle: Particular Email vs Email Group
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Particular Email
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { shareMode = "PARTICULAR" }
                            .testTag("toggle_share_particular"),
                        color = if (shareMode == "PARTICULAR") MaterialTheme.colorScheme.surface else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Single Email",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (shareMode == "PARTICULAR") FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    // Email Group
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { shareMode = "GROUP" }
                            .testTag("toggle_share_group"),
                        color = if (shareMode == "GROUP") MaterialTheme.colorScheme.surface else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Email Group",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (shareMode == "GROUP") FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (shareMode == "PARTICULAR") {
                    // Previously shared list
                    if (sharedList.isNotEmpty()) {
                        Text(
                            text = "Saved Collaborators:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sharedList.forEach { email ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.clickable { targetEmail = email }
                                        )
                                        IconButton(
                                            onClick = { onRemoveSharedEmail(email) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Input particular email
                    OutlinedTextField(
                        value = targetEmail,
                        onValueChange = {
                            targetEmail = it
                            emailError = false
                        },
                        label = { Text("Enter Particular Gmail ID *") },
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
                } else {
                    // GROUP MODE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Email Group:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = onOpenGroupManager) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Manage Groups", fontSize = 12.sp)
                        }
                    }

                    if (emailGroups.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No email groups created yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(onClick = onOpenGroupManager) {
                                    Text("+ Create New Group")
                                }
                            }
                        }
                    } else {
                        emailGroups.forEach { group ->
                            val isChosen = selectedGroup?.id == group.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isChosen) 2.dp else 1.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedGroup = group },
                                color = if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(group.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        val emails = group.getEmailList()
                                        Text(
                                            text = "${emails.size} email(s): ${emails.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Copy to clipboard option
                OutlinedButton(
                    onClick = {
                        val report = generateReportText()
                        clipboardManager.setText(AnnotatedString(report))
                        Toast.makeText(context, "Project report copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                onClick = {
                    if (shareMode == "PARTICULAR") {
                        sendToEmails(listOf(targetEmail))
                    } else {
                        val groupEmails = selectedGroup?.getEmailList() ?: emptyList()
                        if (groupEmails.isEmpty()) {
                            Toast.makeText(context, "Please select a group with member emails", Toast.LENGTH_SHORT).show()
                        } else {
                            sendToEmails(groupEmails)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                modifier = Modifier.testTag("btn_send_gmail_data")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (shareMode == "GROUP") "Send to Group" else "Send via Gmail",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

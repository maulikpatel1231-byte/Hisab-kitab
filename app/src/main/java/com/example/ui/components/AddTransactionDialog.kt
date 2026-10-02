package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMode
import com.example.data.model.Person
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyGreenLight
import com.example.ui.theme.MoneyRed
import com.example.ui.theme.MoneyRedLight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    persons: List<Person>,
    preselectedPerson: Person? = null,
    preselectedType: TransactionType? = null,
    onDismiss: () -> Unit,
    onSave: (
        personId: Long?,
        personName: String?,
        type: TransactionType,
        amount: Double,
        category: String,
        paymentMode: String,
        note: String
    ) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(preselectedType ?: TransactionType.INCOMING)
    }
    var amountStr by remember { mutableStateOf("") }
    var selectedPerson by remember { mutableStateOf(preselectedPerson) }
    var isPersonDropdownExpanded by remember { mutableStateOf(false) }

    val incomingCategories = listOf("Customer Payment", "Daily Sale", "Udhaar Wapsi", "Salary", "Advance", "General Income")
    val outgoingCategories = listOf("Supplier Payment", "Udhaar Diya", "Purchase", "Rent", "Electricity / Bill", "Tea / Food", "Salary Given", "General Expense")

    var selectedCategory by remember {
        mutableStateOf(if (selectedType == TransactionType.INCOMING) "Customer Payment" else "Supplier Payment")
    }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val isIncoming = selectedType == TransactionType.INCOMING

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Entry (Hisab Likhein)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Incoming vs Outgoing Segmented Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Cash In (Aaya / Mile)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = TransactionType.INCOMING
                                if (!incomingCategories.contains(selectedCategory)) {
                                    selectedCategory = incomingCategories.first()
                                }
                            }
                            .testTag("toggle_cash_in"),
                        color = if (isIncoming) MoneyGreen else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isIncoming) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Aaya (Cash In)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncoming) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Cash Out (Gaya / Diye)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = TransactionType.OUTGOING
                                if (!outgoingCategories.contains(selectedCategory)) {
                                    selectedCategory = outgoingCategories.first()
                                }
                            }
                            .testTag("toggle_cash_out"),
                        color = if (!isIncoming) MoneyRed else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (!isIncoming) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gaya (Cash Out)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isIncoming) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                            amountStr = input
                            amountError = false
                        }
                    },
                    label = { Text("Amount (Rashi) *") },
                    placeholder = { Text("0") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    isError = amountError,
                    supportingText = {
                        if (amountError) Text("Please enter a valid amount")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input"),
                    singleLine = true
                )

                // Quick amount chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(100, 500, 1000, 2000).forEach { quickVal ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    val current = amountStr.toDoubleOrNull() ?: 0.0
                                    amountStr = (current + quickVal).toInt().toString()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "+₹$quickVal",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Person Selection (Optional Particular Person)
                Text(
                    text = "Select Particular Person (Particular vyakti jodein):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedPerson?.name ?: "No Person (General Entry)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            if (selectedPerson != null && preselectedPerson == null) {
                                IconButton(onClick = { selectedPerson = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear person")
                                }
                            } else {
                                IconButton(onClick = { isPersonDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose Person")
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = preselectedPerson == null) {
                                isPersonDropdownExpanded = true
                            }
                            .testTag("select_person_field")
                    )

                    DropdownMenu(
                        expanded = isPersonDropdownExpanded,
                        onDismissRequest = { isPersonDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (General Income/Expense)") },
                            onClick = {
                                selectedPerson = null
                                isPersonDropdownExpanded = false
                            }
                        )
                        persons.forEach { person ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(person.name, fontWeight = FontWeight.SemiBold)
                                        if (person.phone.isNotBlank()) {
                                            Text(person.phone, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedPerson = person
                                    isPersonDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category chips
                Text(
                    text = "Category (Kategori):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val categories = if (isIncoming) incomingCategories else outgoingCategories
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isIncoming) MoneyGreenLight else MoneyRedLight,
                                selectedLabelColor = if (isIncoming) MoneyGreen else MoneyRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Mode
                Text(
                    text = "Payment Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMode.values().forEach { mode ->
                        FilterChip(
                            selected = selectedPaymentMode == mode,
                            onClick = { selectedPaymentMode = mode },
                            label = { Text(mode.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note / Remarks
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Bill No. / Description") },
                    placeholder = { Text("e.g. Bill #42, Paid via UPI") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_note_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        amountError = true
                    } else {
                        onSave(
                            selectedPerson?.id,
                            selectedPerson?.name,
                            selectedType,
                            amount,
                            selectedCategory,
                            selectedPaymentMode.label,
                            note
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncoming) MoneyGreen else MoneyRed
                ),
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text(if (isIncoming) "Save Cash In" else "Save Cash Out", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

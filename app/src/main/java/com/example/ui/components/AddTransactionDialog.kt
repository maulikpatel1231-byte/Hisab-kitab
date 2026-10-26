package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    recentPersonNames: List<String> = emptyList(),
    preselectedType: TransactionType? = null,
    isEnglish: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (
        personId: Long?,
        personName: String?,
        type: TransactionType,
        amount: Double,
        category: String,
        paymentMode: String,
        note: String,
        timestamp: Long
    ) -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember {
        mutableStateOf(preselectedType ?: TransactionType.INCOMING)
    }
    var amountStr by remember { mutableStateOf("") }
    var personNameInput by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val incomingCategories = if (isEnglish) {
        listOf("Sales", "Customer Payment", "Salary", "Investment", "Service Fee", "General Income")
    } else {
        listOf("बिक्री (Sales)", "ग्राहक से मिला", "वेतन (Salary)", "निवेश", "सर्विस चार्ज", "अन्य आय")
    }

    val outgoingCategories = if (isEnglish) {
        listOf("Purchases", "Vendor Payment", "Rent", "Electricity / Utilities", "Food & Meals", "Salary Paid", "Transport & Fuel", "General Expense")
    } else {
        listOf("खरीदारी (Purchase)", "सप्लायर पेमेंट", "दुकान किराया", "बिजली / बिल", "चाय व नाश्ता", "वेतन दिया", "किराया / पेट्रोल", "अन्य खर्चे")
    }

    var selectedCategory by remember {
        mutableStateOf(if (selectedType == TransactionType.INCOMING) incomingCategories.first() else outgoingCategories.first())
    }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val isIncoming = selectedType == TransactionType.INCOMING

    // Date calculations and picker
    val calendar = remember(selectedTimestamp) {
        Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    }
    val todayCal = remember { Calendar.getInstance() }
    val isToday = todayCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)

    val yesterdayCal = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) } }
    val isYesterday = yesterdayCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    fun openDatePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestamp = newCal.timeInMillis
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEnglish) {
                    if (isIncoming) "+ Cash In (Income Entry)" else "- Cash Out (Expense Entry)"
                } else {
                    if (isIncoming) "+ आया (Cash In)" else "- गया (Cash Out)"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isIncoming) MoneyGreen else MoneyRed
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
                    // Cash In (Incoming)
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
                                text = if (isEnglish) "Cash In" else "आया (Cash In)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncoming) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Cash Out (Outgoing)
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
                                text = if (isEnglish) "Cash Out" else "गया (Cash Out)",
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
                    label = { Text(if (isEnglish) "Amount *" else "रकम (Amount) *") },
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

                // PERSONAL NAME FIELD (DIRECT INPUT - NO ADD PERSON REQUIRED)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isIncoming) MoneyGreen else MoneyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) {
                            if (isIncoming) "Person Name (Received From)" else "Person Name (Paid To)"
                        } else {
                            if (isIncoming) "व्यक्ति का नाम (जिससे मिला)" else "व्यक्ति का नाम (जिसको दिया)"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = personNameInput,
                    onValueChange = { personNameInput = it },
                    label = {
                        Text(
                            text = if (isEnglish) {
                                if (isIncoming) "Name (e.g. Ramesh, Customer)" else "Name (e.g. Suresh, Vendor)"
                            } else {
                                if (isIncoming) "नाम (उदा. रमेश, ग्राहक)" else "नाम (उदा. सुरेश, वेंडर)"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (isEnglish) {
                                if (isIncoming) "Who gave the money?" else "Who was paid?"
                            } else {
                                if (isIncoming) "रुपये किससे मिले?" else "रुपये किसको दिए?"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isIncoming) MoneyGreen else MoneyRed
                        )
                    },
                    trailingIcon = {
                        if (personNameInput.isNotEmpty()) {
                            IconButton(onClick = { personNameInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear name")
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            text = if (isEnglish) {
                                "Direct personal name for incoming/outgoing (no separate person add needed)"
                            } else {
                                "सीधे व्यक्ति का नाम लिखें (अलग से व्यक्ति जोड़ने की आवश्यकता नहीं)"
                            },
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_party_input"),
                    singleLine = true
                )

                // Quick Recent Name Chips for Instant 1-Tap Entry
                if (recentPersonNames.isNotEmpty()) {
                    val filteredNames = recentPersonNames.filter {
                        personNameInput.isEmpty() || it.contains(personNameInput, ignoreCase = true)
                    }.take(6)

                    if (filteredNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Recent names:" else "हाल ही के नाम:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredNames.forEach { recentName ->
                                FilterChip(
                                    selected = personNameInput.equals(recentName, ignoreCase = true),
                                    onClick = { personNameInput = recentName },
                                    label = { Text(recentName, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DATE SELECTION FEATURE
                Text(
                    text = if (isEnglish) "Entry Date:" else "एंट्री की तारीख (Date):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Date Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isToday,
                        onClick = {
                            selectedTimestamp = System.currentTimeMillis()
                        },
                        label = { Text("Today", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = isYesterday,
                        onClick = {
                            val yesterdayTime = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -1)
                            }.timeInMillis
                            selectedTimestamp = yesterdayTime
                        },
                        label = { Text("Yesterday", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = !isToday && !isYesterday,
                        onClick = { openDatePicker() },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text("Pick Date", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Date Display Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openDatePicker() }
                        .testTag("tx_date_picker_button"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isToday -> "Today • ${dateFormat.format(Date(selectedTimestamp))}"
                                    isYesterday -> "Yesterday • ${dateFormat.format(Date(selectedTimestamp))}"
                                    else -> dateFormat.format(Date(selectedTimestamp))
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Change Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category chips
                Text(
                    text = if (isEnglish) "Category:" else "श्रेणी (Category):",
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
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Mode
                Text(
                    text = if (isEnglish) "Payment Mode:" else "भुगतान का माध्यम (Payment Mode):",
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
                    label = { Text(if (isEnglish) "Note / Bill No. / Description (Optional)" else "नोट / बिल नंबर / विवरण") },
                    placeholder = { Text(if (isEnglish) "e.g. Bill #42, Paid via UPI" else "उदा. बिल नंबर, UPI द्वारा") },
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
                            null,
                            personNameInput.trim().ifEmpty { null },
                            selectedType,
                            amount,
                            selectedCategory,
                            selectedPaymentMode.label,
                            note,
                            selectedTimestamp
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncoming) MoneyGreen else MoneyRed
                ),
                modifier = Modifier.testTag("save_tx_button")
            ) {
                Text(
                    text = if (isEnglish) {
                        if (isIncoming) "Save Cash In" else "Save Cash Out"
                    } else {
                        if (isIncoming) "जमा करें (Save In)" else "दर्ज करें (Save Out)"
                    },
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "रद्द करें")
            }
        }
    )
}

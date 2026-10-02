package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val indianCurrencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun formatCurrency(amount: Double): String {
        return try {
            val formatted = indianCurrencyFormat.format(amount)
            // Ensure ₹ symbol is clean
            if (!formatted.contains("₹")) "₹ ${String.format(Locale.getDefault(), "%,.0f", amount)}" else formatted
        } catch (e: Exception) {
            "₹ ${String.format(Locale.getDefault(), "%,.2f", amount)}"
        }
    }

    fun formatDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)

        val isYesterday = now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - txDate.get(Calendar.DAY_OF_YEAR) == 1

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        return when {
            isToday -> "Today, ${timeFormat.format(Date(timestamp))}"
            isYesterday -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
            else -> "${dateFormat.format(Date(timestamp))}, ${timeFormat.format(Date(timestamp))}"
        }
    }

    fun formatDateShort(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

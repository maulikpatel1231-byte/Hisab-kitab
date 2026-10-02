package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["personId"]), Index(value = ["timestamp"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personId: Long? = null,
    val personName: String? = null,
    val type: String, // TransactionType.INCOMING.name or TransactionType.OUTGOING.name
    val amount: Double,
    val category: String = "General",
    val paymentMode: String = PaymentMode.CASH.name,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

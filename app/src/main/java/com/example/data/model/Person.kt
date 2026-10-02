package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val type: String = PersonType.CUSTOMER.name,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

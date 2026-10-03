package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_groups")
data class EmailGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val memberEmails: String = "", // Comma-separated email addresses
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getEmailList(): List<String> {
        if (memberEmails.isBlank()) return emptyList()
        return memberEmails.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.contains("@") }
    }
}

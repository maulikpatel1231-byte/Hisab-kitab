package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val ownerEmail: String = "",
    val sharedEmails: String = "", // Comma-separated list of Gmail IDs
    val colorHex: String = "#0F766E",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getSharedEmailList(): List<String> {
        if (sharedEmails.isBlank()) return emptyList()
        return sharedEmails.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

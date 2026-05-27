package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val emailOrUserId: String,
    val passwordHash: String, // Stored safely for local session comparison
    val fullName: String,
    val isDarkMode: Boolean = false,
    val currentSessionToken: String? = null // Non-null if logged in
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val dateTimestamp: Long, // Epoch millis
    val isIncome: Boolean,   // true = Income, false = Expense
    val notes: String = ""
)

@Entity(tableName = "budgets", primaryKeys = ["monthYear", "userId"])
data class Budget(
    val monthYear: String, // Format: YYYY-MM
    val userId: String,
    val amount: Double
)

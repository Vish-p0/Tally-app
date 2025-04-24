package com.example.tally.models

import java.util.UUID

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val categoryId: String,
    val type: String, // "Income" or "Expense"
    val description: String = "",
    val date: Long? = null // Timestamp in milliseconds
)
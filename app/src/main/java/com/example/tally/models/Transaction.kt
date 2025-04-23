package com.example.tally.models

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String? = "",
    val amount: Double,
    val categoryId: String? = "",
    val date: Long = System.currentTimeMillis(),
    val type: String? = "Expense", // "Income" or "Expense"
    val description: String? = "" // Optional description field
)
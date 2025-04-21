package com.example.tally.models

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val type: String // "Income" or "Expense"
)
package com.example.tally.models

import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "Income" or "Expense"
    val emoji: String
) {
    companion object {
        val defaultCategories = listOf(
            Category(id = "food", name = "Food", type = "Expense", emoji = "🍕"),
            Category(id = "transport", name = "Transport", type = "Expense", emoji = "🚗"),
            Category(id = "rent", name = "Rent", type = "Expense", emoji = "📋"),
            Category(id = "entertainment", name = "Entertainment", type = "Expense", emoji = "🎉"),
            Category(id = "salary", name = "Salary", type = "Income", emoji = "💸")
        )
    }
}
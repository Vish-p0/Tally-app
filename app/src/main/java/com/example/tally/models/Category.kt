package com.example.tally.models

import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "Income" or "Expense"
    val emoji: String
) {
    companion object {
        // Empty list of default categories
        val defaultCategories = emptyList<Category>()
    }
}
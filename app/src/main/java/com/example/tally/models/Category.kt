package com.example.tally.models

data class Category(
    val id: String,
    val name: String,
    val type: String, // "Income" or "Expense"
    val color: Int? = null,
    val icon: String? = null
) 
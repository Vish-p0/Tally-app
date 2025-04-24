package com.example.tally.models

import java.io.Serializable

data class BudgetItem(
    val id: String = "",
    val categoryName: String = "",
    val type: String = "Expense", // "Income" or "Expense"
    var budgetAmount: Double = 0.0,
    var expenseAmount: Double = 0.0
) : Serializable {
    
    fun getRemainingBudget(): Double {
        return budgetAmount - expenseAmount
    }
    
    fun getPercentageSpent(): Int {
        if (budgetAmount <= 0) return 0
        val percentage = (expenseAmount / budgetAmount) * 100
        return percentage.toInt().coerceIn(0, 100)
    }
    
    fun isExpense(): Boolean {
        return type == "Expense"
    }
    
    fun isIncome(): Boolean {
        return type == "Income"
    }
    
    fun isOverBudget(): Boolean {
        return isExpense() && expenseAmount > budgetAmount
    }
    
    fun isNearLimit(): Boolean {
        return isExpense() && getPercentageSpent() >= 75 && !isOverBudget()
    }
} 
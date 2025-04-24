package com.example.tally.models

import java.io.Serializable
import java.util.Date

data class MonthlyBudget(
    val id: String = "",
    val month: Int = 0,
    val year: Int = 0,
    var budgetItems: MutableList<BudgetItem> = mutableListOf(),
    val createdAt: Date = Date()
) : Serializable {
    
    fun getTotalIncome(): Double {
        return budgetItems
            .filter { it.isIncome() }
            .sumOf { it.budgetAmount }
    }
    
    fun getTotalExpenses(): Double {
        return budgetItems
            .filter { it.isExpense() }
            .sumOf { it.expenseAmount }
    }
    
    fun getTotalBudgetedExpenses(): Double {
        return budgetItems
            .filter { it.isExpense() }
            .sumOf { it.budgetAmount }
    }
    
    fun getRemainingBudget(): Double {
        return getTotalIncome() - getTotalExpenses()
    }
    
    fun getPercentageSpent(): Int {
        val totalIncome = getTotalIncome()
        if (totalIncome <= 0) return 0
        val percentage = (getTotalExpenses() / totalIncome) * 100
        return percentage.toInt().coerceIn(0, 100)
    }
    
    fun getBudgetItemForCategory(categoryName: String): BudgetItem? {
        return budgetItems.find { it.categoryName == categoryName }
    }
    
    fun updateExpenseForCategory(categoryName: String, amount: Double) {
        val budgetItem = getBudgetItemForCategory(categoryName)
        if (budgetItem != null && budgetItem.isExpense()) {
            budgetItem.expenseAmount = amount
        }
    }
    
    fun addOrUpdateBudgetItem(item: BudgetItem) {
        val existingItem = budgetItems.find { it.categoryName == item.categoryName && it.type == item.type }
        if (existingItem != null) {
            existingItem.budgetAmount = item.budgetAmount
            existingItem.expenseAmount = item.expenseAmount
        } else {
            budgetItems.add(item)
        }
    }
    
    fun getIncomeItems(): List<BudgetItem> {
        return budgetItems.filter { it.isIncome() }
    }
    
    fun getExpenseItems(): List<BudgetItem> {
        return budgetItems.filter { it.isExpense() }
    }
} 
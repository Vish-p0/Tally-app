package com.example.tally.repositories

import android.content.Context
import com.example.tally.models.AppNotification
import com.example.tally.models.BudgetItem
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FinanceRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCategories(): List<Category> {
        val categoriesJson = prefs.getString("categories", null)
        return if (categoriesJson != null) {
            val type = object : TypeToken<List<Category>>() {}.type
            gson.fromJson(categoriesJson, type)
        } else {
            // Return empty list instead of defaultCategories
            emptyList()
        }
    }

    fun saveCategories(categories: List<Category>) {
        val categoriesJson = gson.toJson(categories)
        prefs.edit().putString("categories", categoriesJson).apply()
    }
    
    fun clearCategories() {
        prefs.edit().remove("categories").apply()
    }

    fun resetCategoriesToDefaults() {
        // Since defaultCategories is now empty, this essentially clears categories
        saveCategories(Category.defaultCategories)
    }

    fun getTransactions(): List<Transaction> {
        val transactionsJson = prefs.getString("transactions", null)
        return if (transactionsJson != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(transactionsJson, type)
        } else {
            emptyList()
        }
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val transactionsJson = gson.toJson(transactions)
        prefs.edit().putString("transactions", transactionsJson).apply()
    }
    
    fun clearTransactions() {
        prefs.edit().remove("transactions").apply()
    }

    fun getBudget(): Double {
        return prefs.getFloat("budget", 0f).toDouble()
    }

    fun saveBudget(budget: Double) {
        prefs.edit().putFloat("budget", budget.toFloat()).apply()
    }
    
    fun clearBudget() {
        prefs.edit().remove("budget").apply()
    }
    
    // Budget Items methods
    fun getAllBudgetItems(): List<BudgetItem> {
        val budgetItemsJson = prefs.getString("budget_items", null)
        return if (budgetItemsJson != null) {
            val type = object : TypeToken<List<BudgetItem>>() {}.type
            gson.fromJson(budgetItemsJson, type)
        } else {
            emptyList()
        }
    }
    
    fun saveBudgetItems(budgetItems: List<BudgetItem>) {
        val budgetItemsJson = gson.toJson(budgetItems)
        prefs.edit().putString("budget_items", budgetItemsJson).apply()
    }
    
    fun clearBudgetItems() {
        prefs.edit().remove("budget_items").apply()
    }
    
    // Notification methods
    fun getNotifications(): List<AppNotification> {
        val notificationsJson = prefs.getString("notifications", null)
        return if (notificationsJson != null) {
            val type = object : TypeToken<List<AppNotification>>() {}.type
            gson.fromJson(notificationsJson, type)
        } else {
            emptyList()
        }
    }
    
    fun saveNotifications(notifications: List<AppNotification>) {
        // Sort notifications: unread first, then by timestamp (newer first)
        val sortedNotifications = notifications.sortedByDescending { it.sortValue() }
        // Keep only the latest 50 notifications to avoid excessive storage use
        val trimmedNotifications = if (sortedNotifications.size > 50) {
            sortedNotifications.take(50)
        } else {
            sortedNotifications
        }
        val notificationsJson = gson.toJson(trimmedNotifications)
        prefs.edit().putString("notifications", notificationsJson).apply()
    }
    
    fun markNotificationAsRead(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            val notification = notifications[index]
            notifications[index] = notification.copy(isRead = true)
            saveNotifications(notifications)
        }
    }
    
    fun markAllNotificationsAsRead() {
        val notifications = getNotifications().toMutableList()
        val updatedNotifications = notifications.map { it.copy(isRead = true) }
        saveNotifications(updatedNotifications)
    }
    
    fun deleteNotification(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        notifications.removeAll { it.id == notificationId }
        saveNotifications(notifications)
    }
    
    fun clearAllNotifications() {
        prefs.edit().remove("notifications").apply()
    }
    
    // Clear all data in the app
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
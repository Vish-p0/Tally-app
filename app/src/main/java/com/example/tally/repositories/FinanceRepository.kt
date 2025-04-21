package com.example.tally.repositories

import android.content.Context
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.utils.FileUtils
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.UUID

class FinanceRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString("transactions", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<Transaction>>() {}.type)
    }

    fun saveTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun deleteTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.remove(transaction)
        saveTransactions(transactions)
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", json).apply()
    }
    
    fun getCategories(): List<Category> {
        val json = sharedPreferences.getString("categories", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<Category>>() {}.type)
    }
    
    fun saveCategory(category: Category) {
        val categories = getCategories().toMutableList()
        categories.add(category)
        saveCategories(categories)
    }
    
    fun updateCategory(category: Category) {
        val categories = getCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            saveCategories(categories)
        }
    }
    
    fun deleteCategory(category: Category) {
        val categories = getCategories().toMutableList()
        categories.remove(category)
        saveCategories(categories)
    }
    
    private fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        sharedPreferences.edit().putString("categories", json).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat("monthly_budget", 1000f).toDouble()
    }

    fun updateMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat("monthly_budget", budget.toFloat()).apply()
    }

    fun backupData(context: Context) {
        val data = gson.toJson(getTransactions())
        FileUtils.writeToFile(context, "backup.json", data)
    }

    fun restoreData(context: Context) {
        val data = FileUtils.readFromFile(context, "backup.json")
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions = gson.fromJson<List<Transaction>>(data, type)
        saveTransactions(transactions)
    }
}
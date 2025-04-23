package com.example.tally.repositories

import android.content.Context
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
            Category.defaultCategories.also { saveCategories(it) }
        }
    }

    fun saveCategories(categories: List<Category>) {
        val categoriesJson = gson.toJson(categories)
        prefs.edit().putString("categories", categoriesJson).apply()
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

    fun getBudget(): Double {
        return prefs.getFloat("budget", 0f).toDouble()
    }

    fun saveBudget(budget: Double) {
        prefs.edit().putFloat("budget", budget.toFloat()).apply()
    }
}
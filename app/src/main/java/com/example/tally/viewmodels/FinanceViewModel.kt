package com.example.tally.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.repositories.FinanceRepository
import kotlinx.coroutines.launch
import java.util.UUID

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository(application)
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> get() = _transactions
    
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    init {
        loadTransactions()
        loadCategories()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = repository.getTransactions()
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = repository.getCategories()
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.saveTransaction(transaction)
            loadTransactions()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            loadTransactions()
        }
    }
    
    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.saveCategory(category)
            loadCategories()
        }
    }
    
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            loadCategories()
        }
    }
    
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            loadCategories()
        }
    }

    fun getMonthlyBudget(): Double = repository.getMonthlyBudget()
    
    fun updateMonthlyBudget(budget: Double) {
        repository.updateMonthlyBudget(budget)
    }

    fun backupData(context: Context) = repository.backupData(context)
    fun restoreData(context: Context) = repository.restoreData(context)
    
    fun refreshData() {
        loadTransactions()
        loadCategories()
    }
}
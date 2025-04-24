package com.example.tally.viewmodels

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.repositories.FinanceRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(application)
    private val gson = Gson()

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> get() = _transactions

    private val _budget = MutableLiveData<Double>()
    val budget: LiveData<Double> get() = _budget

    init {
        // Load data
        loadCategories()
        loadTransactions()
        loadBudget()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = repository.getCategories()
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _transactions.value = repository.getTransactions()
        }
    }

    private fun loadBudget() {
        viewModelScope.launch {
            _budget.value = repository.getBudget()
        }
    }

    fun addCategory(category: Category) {
        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()
        currentCategories.add(category)
        repository.saveCategories(currentCategories)
        _categories.value = currentCategories
    }

    fun updateCategory(updatedCategory: Category) {
        val currentCategories = _categories.value?.toMutableList() ?: return
        val index = currentCategories.indexOfFirst { it.id == updatedCategory.id }
        if (index != -1) {
            currentCategories[index] = updatedCategory
            repository.saveCategories(currentCategories)
            _categories.value = currentCategories
        }
    }

    fun deleteCategory(categoryId: String) {
        val currentCategories = _categories.value?.toMutableList() ?: return
        currentCategories.removeAll { it.id == categoryId }
        repository.saveCategories(currentCategories)
        _categories.value = currentCategories
    }

    fun resetCategoriesToDefaults() {
        repository.resetCategoriesToDefaults()
        loadCategories() // Reload categories from repository
    }

    fun addTransaction(transaction: Transaction) {
        val currentTransactions = _transactions.value?.toMutableList() ?: mutableListOf()
        currentTransactions.add(transaction)
        repository.saveTransactions(currentTransactions)
        _transactions.value = currentTransactions
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        val currentTransactions = _transactions.value?.toMutableList() ?: return
        val index = currentTransactions.indexOfFirst { it.id == updatedTransaction.id }
        if (index != -1) {
            currentTransactions[index] = updatedTransaction
            repository.saveTransactions(currentTransactions)
            _transactions.value = currentTransactions
        }
    }

    fun deleteTransaction(transactionId: String) {
        val currentTransactions = _transactions.value?.toMutableList() ?: return
        currentTransactions.removeAll { it.id == transactionId }
        repository.saveTransactions(currentTransactions)
        _transactions.value = currentTransactions
    }

    fun setBudget(budget: Double) {
        repository.saveBudget(budget)
        _budget.value = budget
    }

    fun getMonthlyBudget(): Double {
        return _budget.value ?: 0.0
    }

    fun refreshData() {
        loadCategories()
        loadTransactions()
        loadBudget()
    }

    fun getCategoryById(categoryId: String?): Category? {
        if (categoryId == null || categoryId.isEmpty()) {
            return null
        }
        return _categories.value?.find { it.id == categoryId }
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            try {
                val backupData = BackupData(
                    categories = _categories.value ?: emptyList(),
                    transactions = _transactions.value ?: emptyList(),
                    budget = _budget.value ?: 0.0
                )
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFileName = "tally_backup_$timestamp.json"
                val backupFile = File(context.getExternalFilesDir(null), backupFileName)
                
                val json = gson.toJson(backupData)
                backupFile.writeText(json)
                
                Toast.makeText(context, "Backup created successfully: ${backupFile.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreData(context: Context) {
        viewModelScope.launch {
            try {
                val backupDir = context.getExternalFilesDir(null)
                val backupFiles = backupDir?.listFiles { file -> 
                    file.name.startsWith("tally_backup_") && file.name.endsWith(".json") 
                }?.sortedByDescending { it.lastModified() }
                
                if (backupFiles.isNullOrEmpty()) {
                    Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val latestBackup = backupFiles.first()
                val json = latestBackup.readText()
                val backupData = gson.fromJson(json, BackupData::class.java)
                
                // Restore data
                repository.saveCategories(backupData.categories)
                repository.saveTransactions(backupData.transactions)
                repository.saveBudget(backupData.budget)
                
                // Refresh view model data
                refreshData()
                
                Toast.makeText(context, "Data restored successfully from: ${latestBackup.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        class Factory(private val application: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                    return FinanceViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

data class BackupData(
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val budget: Double
)
package com.example.tally.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tally.models.Category
import com.example.tally.models.Currency
import com.example.tally.models.Transaction
import com.example.tally.models.BudgetItem
import com.example.tally.repositories.FinanceRepository
import com.example.tally.utils.CurrencyManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(application)
    private val gson = Gson()
    private val currencyManager = CurrencyManager(application)

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> get() = _transactions

    private val _budget = MutableLiveData<Double>()
    val budget: LiveData<Double> get() = _budget
    
    private val _currentCurrency = MutableLiveData<Currency>()
    val currentCurrency: LiveData<Currency> get() = _currentCurrency

    init {
        // Load data
        loadCategories()
        loadTransactions()
        loadBudget()
        loadCurrentCurrency()
    }

    private fun loadCurrentCurrency() {
        _currentCurrency.value = currencyManager.getCurrentCurrency()
    }
    
    fun getCurrencyManager(): CurrencyManager {
        return currencyManager
    }
    
    fun formatAmount(amount: Double): String {
        return currencyManager.formatAmount(amount)
    }
    
    fun convertAmountToUsd(amount: Double): Double {
        return currencyManager.convertAmountToUsd(amount)
    }
    
    fun convertAmountFromUsd(amountInUsd: Double): Double {
        return currencyManager.convertAmountFromUsd(amountInUsd)
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
        // Store the amount in USD for consistency
        val transactionInUsd = transaction.copy(
            amount = currencyManager.convertAmountToUsd(transaction.amount)
        )
        
        val currentTransactions = _transactions.value?.toMutableList() ?: mutableListOf()
        currentTransactions.add(transactionInUsd)
        repository.saveTransactions(currentTransactions)
        _transactions.value = currentTransactions
    }

    fun updateTransaction(updatedTransaction: Transaction) {
        // Store the amount in USD for consistency
        val transactionInUsd = updatedTransaction.copy(
            amount = currencyManager.convertAmountToUsd(updatedTransaction.amount)
        )
        
        val currentTransactions = _transactions.value?.toMutableList() ?: return
        val index = currentTransactions.indexOfFirst { it.id == transactionInUsd.id }
        if (index != -1) {
            currentTransactions[index] = transactionInUsd
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
        // Store budget in USD for consistency
        val budgetInUsd = currencyManager.convertAmountToUsd(budget)
        repository.saveBudget(budgetInUsd)
        _budget.value = budgetInUsd
    }

    fun getMonthlyBudget(): Double {
        // Return the budget in the current currency
        val budgetInUsd = _budget.value ?: 0.0
        return currencyManager.convertAmountFromUsd(budgetInUsd)
    }

    fun getMonthlyBudgetInCurrentCurrency(): Double {
        val budgetInUsd = _budget.value ?: 0.0
        return currencyManager.convertAmountFromUsd(budgetInUsd)
    }
    
    fun setCurrentCurrency(currency: Currency) {
        currencyManager.setCurrentCurrency(currency)
        _currentCurrency.value = currency
    }

    fun refreshData() {
        loadCategories()
        loadTransactions()
        loadBudget()
        loadCurrentCurrency()
    }

    fun getCategoryById(categoryId: String?): Category? {
        if (categoryId == null || categoryId.isEmpty()) {
            return null
        }
        return _categories.value?.find { it.id == categoryId }
    }

    fun backupData(): Uri? {
        try {
            val context = getApplication<Application>()
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "tally_backup_$timestamp.json")

            // Create backup data object
            val backupData = mapOf(
                "categories" to categories.value,
                "transactions" to transactions.value,
                "budget" to budget.value,
                "budgetItems" to repository.getAllBudgetItems(),
                "currencyCode" to currentCurrency.value?.code
            )

            // Convert to JSON
            val backupJson = Gson().toJson(backupData)

            // Write to file
            FileOutputStream(backupFile).use { output ->
                output.write(backupJson.toByteArray())
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Backup failed: ${e.message}", e)
            return null
        }
    }

    fun restoreData(): Boolean {
        try {
            val context = getApplication<Application>()
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists() || backupDir.listFiles()?.isEmpty() == true) {
                Log.e("FinanceViewModel", "No backup files found")
                return false
            }

            // Get the most recent backup file
            val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
            
            val latestBackupFile = backupFiles?.firstOrNull() ?: return false

            // Read file content
            val backupJson = FileInputStream(latestBackupFile).bufferedReader().use { it.readText() }
            
            // Parse JSON
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData: Map<String, Any> = gson.fromJson(backupJson, type)
            
            // Restore categories
            val categoriesType = object : TypeToken<List<Category>>() {}.type
            val categoriesJson = gson.toJson(backupData["categories"])
            val restoredCategories: List<Category> = gson.fromJson(categoriesJson, categoriesType)
            repository.saveCategories(restoredCategories)
            _categories.value = restoredCategories
            
            // Restore transactions
            val transactionsType = object : TypeToken<List<Transaction>>() {}.type
            val transactionsJson = gson.toJson(backupData["transactions"])
            val restoredTransactions: List<Transaction> = gson.fromJson(transactionsJson, transactionsType)
            repository.saveTransactions(restoredTransactions)
            _transactions.value = restoredTransactions
            
            // Restore budget
            val budgetValue = (backupData["budget"] as? Double) ?: 0.0
            repository.saveBudget(budgetValue)
            _budget.value = budgetValue
            
            // Restore budget items
            val budgetItemsType = object : TypeToken<List<BudgetItem>>() {}.type
            val budgetItemsJson = gson.toJson(backupData["budgetItems"])
            val restoredBudgetItems: List<BudgetItem> = gson.fromJson(budgetItemsJson, budgetItemsType)
            repository.saveBudgetItems(restoredBudgetItems)
            
            // Restore currency if available
            val currencyCode = backupData["currencyCode"] as? String
            if (currencyCode != null) {
                val currency = Currency.getByCode(currencyCode)
                currencyManager.setCurrentCurrency(currency)
                _currentCurrency.value = currency
            }
            
            return true
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Restore failed: ${e.message}", e)
            return false
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
    val budget: Double,
    val currencyCode: String? = "USD"
)
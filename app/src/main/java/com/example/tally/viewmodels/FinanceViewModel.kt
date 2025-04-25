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
import com.example.tally.models.AppNotification
import com.example.tally.models.Category
import com.example.tally.models.Currency
import com.example.tally.models.Transaction
import com.example.tally.models.BudgetItem
import com.example.tally.repositories.FinanceRepository
import com.example.tally.utils.BackupRestoreUtil
import com.example.tally.utils.CurrencyManager
import com.example.tally.utils.NotificationUtils
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
    private val backupRestoreUtil = BackupRestoreUtil(application)
    private val notificationUtils = NotificationUtils(application)

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> get() = _transactions

    private val _budget = MutableLiveData<Double>()
    val budget: LiveData<Double> get() = _budget
    
    private val _currentCurrency = MutableLiveData<Currency>()
    val currentCurrency: LiveData<Currency> get() = _currentCurrency

    // Add LiveData for backup operations
    private val _backupFiles = MutableLiveData<List<Pair<File, Date>>>()
    val backupFiles: LiveData<List<Pair<File, Date>>> get() = _backupFiles
    
    // Add LiveData for notifications
    private val _notifications = MutableLiveData<List<AppNotification>>()
    val notifications: LiveData<List<AppNotification>> get() = _notifications

    init {
        // Create notification channel
        notificationUtils.createNotificationChannel()
        
        // Load data
        loadCategories()
        loadTransactions()
        loadBudget()
        loadCurrentCurrency()
        loadBackupFiles()
        loadNotifications()
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

    // New method to load backup files
    private fun loadBackupFiles() {
        viewModelScope.launch {
            _backupFiles.value = backupRestoreUtil.listBackupFiles()
        }
    }
    
    // New method to load notifications
    private fun loadNotifications() {
        viewModelScope.launch {
            _notifications.value = repository.getNotifications()
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
        
        // Send notification for the new transaction
        notificationUtils.notifyTransactionCreated(
            transactionName = transaction.title,
            amount = transaction.amount,
            isExpense = transaction.type == "Expense"
        )
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
            
            // Send notification for the updated transaction
            notificationUtils.notifyTransactionCreated(
                transactionName = updatedTransaction.title,
                amount = updatedTransaction.amount,
                isExpense = updatedTransaction.type == "Expense"
            )
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
        
        // Send notification for budget update
        notificationUtils.notifyBudgetUpdated("Monthly", budget)
    }
    
    fun updateBudgetItem(budgetItem: BudgetItem) {
        val currentItems = repository.getAllBudgetItems().toMutableList()
        val index = currentItems.indexOfFirst { it.id == budgetItem.id }
        
        if (index != -1) {
            currentItems[index] = budgetItem
        } else {
            currentItems.add(budgetItem)
        }
        
        repository.saveBudgetItems(currentItems)
        
        // Send notification for budget item update
        notificationUtils.notifyBudgetUpdated(budgetItem.categoryName, budgetItem.budgetAmount)
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
        loadBackupFiles()
        loadNotifications()
    }

    fun getCategoryById(categoryId: String?): Category? {
        if (categoryId == null || categoryId.isEmpty()) {
            return null
        }
        return _categories.value?.find { it.id == categoryId }
    }

    /**
     * Creates a backup of all app data.
     * @return URI of the backup file or null if backup failed
     */
    fun backupData(): Uri? {
        val categories = _categories.value ?: emptyList()
        val transactions = _transactions.value ?: emptyList()
        val budget = _budget.value ?: 0.0
        val budgetItems = repository.getAllBudgetItems()
        val currencyCode = _currentCurrency.value?.code
        
        val backupUri = backupRestoreUtil.createBackup(
            categories,
            transactions,
            budget,
            budgetItems,
            currencyCode
        )
        
        // Refresh backup files list
        loadBackupFiles()
        
        return backupUri
    }

    /**
     * Restores data from the latest backup file.
     * @return true if restore was successful, false otherwise
     */
    fun restoreData(): Boolean {
        try {
            val backupData = backupRestoreUtil.restoreFromLatestBackup() ?: return false
            
            // Restore categories
            backupData.categories?.let { categories ->
                repository.saveCategories(categories)
                _categories.value = categories
            }
            
            // Restore transactions
            backupData.transactions?.let { transactions ->
                repository.saveTransactions(transactions)
                _transactions.value = transactions
            }
            
            // Restore budget
            backupData.budget?.let { budget ->
                repository.saveBudget(budget)
                _budget.value = budget
            }
            
            // Restore budget items
            backupData.budgetItems?.let { budgetItems ->
                repository.saveBudgetItems(budgetItems)
            }
            
            // Restore currency if available
            backupData.currencyCode?.let { currencyCode ->
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
    
    /**
     * Restores data from a specific backup file.
     * @param backupFile The backup file to restore from
     * @return true if restore was successful, false otherwise
     */
    fun restoreFromFile(backupFile: File): Boolean {
        try {
            val backupData = backupRestoreUtil.restoreFromFile(backupFile) ?: return false
            
            // Restore categories
            backupData.categories?.let { categories ->
                repository.saveCategories(categories)
                _categories.value = categories
            }
            
            // Restore transactions
            backupData.transactions?.let { transactions ->
                repository.saveTransactions(transactions)
                _transactions.value = transactions
            }
            
            // Restore budget
            backupData.budget?.let { budget ->
                repository.saveBudget(budget)
                _budget.value = budget
            }
            
            // Restore budget items
            backupData.budgetItems?.let { budgetItems ->
                repository.saveBudgetItems(budgetItems)
            }
            
            // Restore currency if available
            backupData.currencyCode?.let { currencyCode ->
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
    
    /**
     * Deletes a backup file.
     * @param backupFile The backup file to delete
     * @return true if deletion was successful, false otherwise
     */
    fun deleteBackupFile(backupFile: File): Boolean {
        val success = backupRestoreUtil.deleteBackupFile(backupFile)
        
        // Refresh backup files list
        if (success) {
            loadBackupFiles()
        }
        
        return success
    }
    
    // Notification methods
    fun markNotificationAsRead(notificationId: String) {
        repository.markNotificationAsRead(notificationId)
        loadNotifications()
    }
    
    fun markAllNotificationsAsRead() {
        repository.markAllNotificationsAsRead()
        loadNotifications()
    }
    
    fun deleteNotification(notificationId: String) {
        repository.deleteNotification(notificationId)
        loadNotifications()
    }
    
    fun clearAllNotifications() {
        repository.clearAllNotifications()
        loadNotifications()
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
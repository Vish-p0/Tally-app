package com.example.tally.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tally.models.BudgetItem
import com.example.tally.models.MonthlyBudget
import com.example.tally.models.Transaction
import com.example.tally.models.Category
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.Locale

class BudgetRepository(private val context: Context) {
    private val TAG = "BudgetRepository"
    private val budgetLiveData = MutableLiveData<MonthlyBudget?>()
    private val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Dynamic category name provider
    private var categoryNameProvider: ((String) -> String)? = null
    
    /**
     * Set a custom category provider function that can map from categoryId to categoryName
     */
    fun setCategoryProvider(provider: (String) -> String) {
        categoryNameProvider = provider
    }
    
    init {
        // Initialize with empty budget for current month
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val year = calendar.get(Calendar.YEAR)
        
        fetchMonthlyBudget(month, year)
    }
    
    fun getBudgetForMonth(month: Int, year: Int): LiveData<MonthlyBudget?> {
        fetchMonthlyBudget(month, year)
        return budgetLiveData
    }
    
    private fun fetchMonthlyBudget(month: Int, year: Int) {
        val budgetKey = getBudgetKey(month, year)
        val budgetJson = prefs.getString(budgetKey, null)
        
        val budget = if (budgetJson != null) {
            // Parse existing budget from JSON
            try {
                val type = object : TypeToken<MonthlyBudget>() {}.type
                val parsedBudget = gson.fromJson<MonthlyBudget>(budgetJson, type)
                
                // Validate the budget - if it has no real budget items (beyond just the "Other" category)
                // or if it's otherwise invalid, treat it as if it doesn't exist
                if (parsedBudget == null || 
                    parsedBudget.budgetItems.isEmpty() || 
                    (parsedBudget.budgetItems.size == 1 && parsedBudget.budgetItems[0].categoryName == "Other")) {
                    Log.d(TAG, "Invalid or empty budget found for $month/$year, creating new one")
                    createEmptyBudget(month, year)
                } else {
                    parsedBudget
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing budget: ${e.message}")
                createEmptyBudget(month, year)
            }
        } else {
            // Create a new empty budget
            createEmptyBudget(month, year)
        }
        
        // Ensure the "Other" category exists
        ensureOtherCategoryExists(budget)
        
        budgetLiveData.postValue(budget)
    }
    
    private fun getBudgetKey(month: Int, year: Int): String {
        return "budget_${month}_${year}"
    }
    
    fun createEmptyBudget(month: Int, year: Int): MonthlyBudget {
        return MonthlyBudget(
            id = "budget_${month}_${year}",
            month = month,
            year = year,
            budgetItems = mutableListOf(),
            createdAt = Date()
        )
    }
    
    fun createMockBudget(month: Int, year: Int): MonthlyBudget {
        // This is now just a wrapper around createEmptyBudget for backward compatibility
        return createEmptyBudget(month, year)
    }
    
    fun updateBudgetItem(monthlyBudget: MonthlyBudget, budgetItem: BudgetItem) {
        // Update the existing budget item
        monthlyBudget.addOrUpdateBudgetItem(budgetItem)
        
        // Save the updated budget to SharedPreferences
        saveBudget(monthlyBudget)
        
        // Update LiveData
        budgetLiveData.postValue(monthlyBudget)
    }
    
    private fun saveBudget(budget: MonthlyBudget) {
        val budgetKey = getBudgetKey(budget.month, budget.year)
        val budgetJson = gson.toJson(budget)
        prefs.edit().putString(budgetKey, budgetJson).apply()
    }
    
    fun createBudget(monthlyBudget: MonthlyBudget) {
        // Ensure the "Other" category exists for expenses
        ensureOtherCategoryExists(monthlyBudget)
        
        // Save to SharedPreferences
        saveBudget(monthlyBudget)
        
        // Update LiveData
        budgetLiveData.postValue(monthlyBudget)
    }
    
    /**
     * Ensures that an "Other" category exists in the budget for uncategorized expenses
     */
    private fun ensureOtherCategoryExists(budget: MonthlyBudget) {
        // Check if "Other" already exists (case insensitive search)
        val otherBudget = budget.budgetItems.find { it.categoryName.equals("Other", ignoreCase = true) }
        
        if (otherBudget == null) {
            // "Other" category doesn't exist, create it
            Log.d(TAG, "Creating 'Other' category with default budget")
            val defaultOtherBudget = BudgetItem(
                id = UUID.randomUUID().toString(),
                categoryName = "Other",  // Use proper capitalization
                type = "Expense",
                budgetAmount = 100.0,    // Default budget amount to ensure progress bars show
                expenseAmount = 0.0
            )
            budget.budgetItems.add(defaultOtherBudget)
            Log.d(TAG, "Added 'Other' category with id: ${defaultOtherBudget.id}")
        } else {
            // "Other" exists but we need to ensure it has a budget amount for progress bars
            if (otherBudget.budgetAmount <= 0) {
                Log.d(TAG, "Updating 'Other' category with default budget amount")
                otherBudget.budgetAmount = 100.0 // Ensure progress bars will show
                Log.d(TAG, "Set 'Other' category budget amount to ${otherBudget.budgetAmount}")
            } else {
                Log.d(TAG, "'Other' category already has a budget amount of ${otherBudget.budgetAmount}")
            }
            
            // Make sure the name is properly capitalized
            if (otherBudget.categoryName != "Other") {
                Log.d(TAG, "Fixing capitalization of Other category from '${otherBudget.categoryName}' to 'Other'")
                // Since categoryName is val (immutable), we need to replace the entire item
                val fixedOtherBudget = BudgetItem(
                    id = otherBudget.id,
                    categoryName = "Other",
                    type = otherBudget.type,
                    budgetAmount = otherBudget.budgetAmount,
                    expenseAmount = otherBudget.expenseAmount
                )
                
                // Remove the old one and add the fixed one
                budget.budgetItems.remove(otherBudget)
                budget.budgetItems.add(fixedOtherBudget)
                Log.d(TAG, "Replaced 'Other' category with fixed version")
            }
        }
    }
    
    fun updateExpensesFromTransactions(transactions: List<Transaction>) {
        val currentBudget = budgetLiveData.value ?: return
        
        // Reset all expense amounts
        currentBudget.budgetItems
            .filter { it.isExpense() }
            .forEach { it.expenseAmount = 0.0 }
        
        // Make sure "Other" category exists
        ensureOtherCategoryExists(currentBudget)
        
        // Get the "Other" budget item
        val otherBudgetItem = currentBudget.budgetItems.find { 
            it.categoryName == "Other" && it.isExpense() 
        }
        
        // Check if we found the Other category
        if (otherBudgetItem == null) {
            Log.e(TAG, "Failed to find or create Other category!")
        } else {
            Log.d(TAG, "Found Other category with id ${otherBudgetItem.id}, budget: ${otherBudgetItem.budgetAmount}")
        }
        
        // Track encountered categories to check if we need to add any missing ones
        val processedCategoryNames = mutableSetOf<String>()
        
        // Calculate expenses from transactions
        for (transaction in transactions) {
            if (transaction.type == "Expense") {
                // Map the categoryId to a category name for the budget
                val categoryName = getCategoryNameFromId(transaction.categoryId)
                processedCategoryNames.add(categoryName)
                
                // Find the budget item for this category
                val budgetItem = currentBudget.budgetItems.find { 
                    it.categoryName == categoryName && it.isExpense() 
                }
                
                if (budgetItem != null) {
                    // Add this transaction amount to the existing budget item
                    budgetItem.expenseAmount += transaction.amount
                    Log.d(TAG, "Updated budget item ${budgetItem.categoryName} - spent: ${budgetItem.expenseAmount}")
                } else if (otherBudgetItem != null) {
                    // Add to the "Other" category if no matching budget item found
                    otherBudgetItem.expenseAmount += transaction.amount
                    Log.d(TAG, "Added transaction of $${transaction.amount} to Other category (categoryName: $categoryName)")
                }
            }
        }
        
        // Create budget items for any categories that have transactions but no budget item
        for (categoryName in processedCategoryNames) {
            if (categoryName != "Other" && !currentBudget.budgetItems.any { it.categoryName == categoryName }) {
                // Create a new budget item for this category with default budget
                Log.d(TAG, "Creating missing budget item for category: $categoryName")
                val newBudgetItem = BudgetItem(
                    id = UUID.randomUUID().toString(),
                    categoryName = categoryName,
                    type = "Expense",
                    budgetAmount = 0.0, // Set to zero so it doesn't affect total budget
                    expenseAmount = 0.0
                )
                currentBudget.budgetItems.add(newBudgetItem)
                
                // Now find transactions for this category and update the expense amount
                var totalAmount = 0.0
                for (transaction in transactions) {
                    if (transaction.type == "Expense" && getCategoryNameFromId(transaction.categoryId) == categoryName) {
                        totalAmount += transaction.amount
                    }
                }
                
                if (totalAmount > 0) {
                    newBudgetItem.expenseAmount = totalAmount
                    Log.d(TAG, "Set expense amount for new budget item ${newBudgetItem.categoryName}: ${newBudgetItem.expenseAmount}")
                }
            }
        }
        
        // Add any user-defined categories that aren't in the budget yet
        try {
            categoryNameProvider?.let { provider ->
                // This assumes there's a method to get all available category IDs
                val availableCategoryIds = getAllAvailableCategoryIds()
                
                for (categoryId in availableCategoryIds) {
                    val categoryName = provider(categoryId)
                    
                    // Skip if this is the "Other" category or if it's already in the budget
                    if (categoryName.equals("Other", ignoreCase = true) || 
                        currentBudget.budgetItems.any { it.categoryName.equals(categoryName, ignoreCase = true) }) {
                        continue
                    }
                    
                    // Create a budget item for this category
                    Log.d(TAG, "Adding user-defined category to budget: $categoryName")
                    val newBudgetItem = BudgetItem(
                        id = UUID.randomUUID().toString(),
                        categoryName = categoryName,
                        type = "Expense",
                        budgetAmount = 0.0, // Set to zero so it doesn't affect total budget
                        expenseAmount = 0.0
                    )
                    currentBudget.budgetItems.add(newBudgetItem)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user categories: ${e.message}")
        }
        
        // Log total spent amounts for debugging
        for (item in currentBudget.budgetItems.filter { it.isExpense() }) {
            Log.d(TAG, "Category ${item.categoryName}: Budget ${item.budgetAmount}, Spent ${item.expenseAmount}")
        }
        
        // Save the updated budget
        saveBudget(currentBudget)
        
        // Update LiveData to trigger UI updates
        budgetLiveData.postValue(currentBudget)
    }
    
    // Get all available category IDs from the system
    private fun getAllAvailableCategoryIds(): List<String> {
        // This would normally come from your category repository or database
        // For now, return an empty list to be safe
        return emptyList()
    }
    
    private fun getCategoryNameFromId(categoryId: String): String {
        // Add debug logging
        Log.d(TAG, "Getting category name for ID: '$categoryId'")
        
        // Check if the categoryId is null or empty
        if (categoryId.isNullOrEmpty()) {
            Log.d(TAG, "Empty categoryId found, using 'Other'")
            return "Other"
        }
        
        // Check if the ID is already "other" (case insensitive)
        if (categoryId.equals("other", ignoreCase = true)) {
            Log.d(TAG, "CategoryId is 'other', using 'Other'")
            return "Other"
        }
        
        // Special handling for rent/housing categories
        if (categoryId.equals("rent", ignoreCase = true) || 
            categoryId.equals("housing", ignoreCase = true)) {
            Log.d(TAG, "Mapping '$categoryId' to 'Housing'")
            return "Housing"
        }
        
        // First try the dynamic provider if available - this is the primary method
        categoryNameProvider?.let { provider ->
            try {
                val name = provider(categoryId)
                Log.d(TAG, "CategoryProvider mapped '$categoryId' to '$name'")
                return name
            } catch (e: Exception) {
                Log.e(TAG, "Error in category provider: ${e.message}")
                // Continue with default handling if provider fails
            }
        }
        
        // If we get here and no mapping was found, use the categoryId directly
        // This assumes categoryId might actually be a valid category name
        val capitalizedName = categoryId.capitalize(Locale.getDefault())
        Log.d(TAG, "Using categoryId directly as category name: '$categoryId' -> '$capitalizedName'")
        return capitalizedName
    }
    
    fun checkBudgetWarnings(): List<BudgetItem> {
        val currentBudget = budgetLiveData.value ?: return emptyList()
        
        // Find expense items that are over budget or near limit
        return currentBudget.budgetItems.filter {
            it.isExpense() && (it.isOverBudget() || it.isNearLimit())
        }
    }
    
    /**
     * Deletes the budget for the specified month and year
     */
    fun deleteBudget(month: Int, year: Int) {
        // Get the key for the budget
        val budgetKey = getBudgetKey(month, year)
        
        try {
            // Force clear from memory first
            val editor = prefs.edit()
            editor.remove(budgetKey)
            
            // Immediately commit changes to ensure they're written to disk
            val success = editor.commit()
            Log.d(TAG, "Budget deletion commit result: $success")
            
            // For extra certainty, verify the key was removed
            if (prefs.contains(budgetKey)) {
                Log.w(TAG, "Budget key still exists after deletion, forcing clear...")
                // Try a more aggressive approach
                editor.clear().apply {
                    putStringSet(budgetKey, null)
                    remove(budgetKey)
                }.commit()
            }
            
            // Force clear the LiveData
            budgetLiveData.postValue(null)
            
            // Clear any potentially cached values
            val emptyBudget = createEmptyBudget(month, year)
            emptyBudget.budgetItems.clear()
            
            // Use a handler to post a delayed update
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Re-fetching budget after deletion")
                // Just post the empty budget directly rather than re-fetching
                // which might re-create a budget incorrectly
                budgetLiveData.postValue(null)
            }, 200)
            
            Log.d(TAG, "Budget deleted for $month/$year")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting budget: ${e.message}")
        }
    }
    
    /**
     * Checks if a budget exists for the specified month and year
     */
    fun hasBudget(month: Int, year: Int): Boolean {
        val budgetKey = getBudgetKey(month, year)
        val budgetJson = prefs.getString(budgetKey, null)
        return !budgetJson.isNullOrEmpty()
    }
    
    // Called from AnalyticsFragment to ensure all user's expense categories are in the budget
    fun ensureAllUserCategoriesExist(categoryNames: List<String>) {
        val currentBudget = budgetLiveData.value ?: return
        var budgetUpdated = false
        
        // First make sure the Other category exists
        ensureOtherCategoryExists(currentBudget)
        
        // Add a budget item for each category that doesn't already exist
        for (categoryName in categoryNames) {
            // Skip if this is the Other category or if it already exists
            if (categoryName.equals("Other", ignoreCase = true) ||
                currentBudget.budgetItems.any { it.categoryName.equals(categoryName, ignoreCase = true) }) {
                continue
            }
            
            // Create a new budget item for this category
            Log.d(TAG, "Adding user category to budget: $categoryName")
            val newBudgetItem = BudgetItem(
                id = UUID.randomUUID().toString(),
                categoryName = categoryName,
                type = "Expense",
                budgetAmount = 0.0, // Set to zero so it doesn't affect budget total
                expenseAmount = 0.0
            )
            currentBudget.budgetItems.add(newBudgetItem)
            budgetUpdated = true
        }
        
        // Save and update if changes were made
        if (budgetUpdated) {
            saveBudget(currentBudget)
            budgetLiveData.postValue(currentBudget)
        }
    }
} 
package com.example.tally.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.models.BudgetItem
import com.example.tally.models.MonthlyBudget
import com.example.tally.repository.BudgetRepository
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class BudgetSetupFragment : Fragment() {
    private val TAG = "BudgetSetupFragment"
    
    // View Models and Repositories
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var budgetRepository: BudgetRepository
    
    // Views
    private lateinit var budgetMonthYearText: TextView
    private lateinit var selectMonthYearButton: Button
    private lateinit var incomeItemsContainer: LinearLayout
    private lateinit var expenseItemsContainer: LinearLayout
    private lateinit var addIncomeButton: Button
    private lateinit var addExpenseButton: Button
    private lateinit var otherCategoryBudget: EditText
    private lateinit var autoAssignCheckbox: CheckBox
    private lateinit var totalIncomeSummary: TextView
    private lateinit var totalExpensesSummary: TextView
    private lateinit var remainingBudgetSummary: TextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    
    // State
    private var selectedMonth = 0
    private var selectedYear = 0
    private val incomeEntries = mutableListOf<BudgetEntryView>()
    private val expenseEntries = mutableListOf<BudgetEntryView>()
    private var incomeCategories = listOf<String>()
    private var expenseCategories = listOf<String>()
    private var isEditing = false
    private var existingBudget: MonthlyBudget? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_budget_setup, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get arguments if any
        arguments?.let { args ->
            selectedMonth = args.getInt("month", 0) - 1 // Convert to 0-based month
            selectedYear = args.getInt("year", Calendar.getInstance().get(Calendar.YEAR))
            isEditing = args.getBoolean("isEditing", false)
        }
        
        // If no month is provided, use current month
        if (selectedMonth < 0) {
            val cal = Calendar.getInstance()
            selectedMonth = cal.get(Calendar.MONTH)
            selectedYear = cal.get(Calendar.YEAR)
        }
        
        // Initialize repository
        budgetRepository = BudgetRepository(requireContext())
        
        // Initialize views
        budgetMonthYearText = view.findViewById(R.id.budgetMonthYear)
        selectMonthYearButton = view.findViewById(R.id.selectMonthYearButton)
        incomeItemsContainer = view.findViewById(R.id.incomeItemsContainer)
        expenseItemsContainer = view.findViewById(R.id.expenseItemsContainer)
        addIncomeButton = view.findViewById(R.id.addIncomeButton)
        addExpenseButton = view.findViewById(R.id.addExpenseButton)
        otherCategoryBudget = view.findViewById(R.id.otherCategoryBudget)
        autoAssignCheckbox = view.findViewById(R.id.autoAssignCheckbox)
        totalIncomeSummary = view.findViewById(R.id.totalIncomeSummary)
        totalExpensesSummary = view.findViewById(R.id.totalExpensesSummary)
        remainingBudgetSummary = view.findViewById(R.id.remainingBudgetSummary)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        
        // Set up back button click listener
        view.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Update title if editing
        if (isEditing) {
            view.findViewById<TextView>(R.id.dialogTitle)?.apply {
                text = "Edit Budget"
            }
        }
        
        // Update month/year display
        updateMonthYearDisplay()
        
        // Initialize categories
        loadCategories()
        
        // Set up listeners
        setupListeners()
        
        // If editing, load existing budget
        if (isEditing) {
            loadExistingBudget()
        } else {
            // Add initial budget entries
            addInitialEntries()
        }
        
        // Update summary
        updateBudgetSummary()
    }
    
    private fun loadCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            // Get income and expense categories
            incomeCategories = categories
                .filter { it.type == "Income" }
                .map { it.name }
                .distinct()
            
            // No hardcoded fallbacks - only use user-created categories
            
            expenseCategories = categories
                .filter { it.type == "Expense" }
                .map { it.name }
                .distinct()
            
            // No hardcoded fallbacks - only use user-created categories
            
            // Always add "Other" to expense categories
            if (!expenseCategories.contains("Other")) {
                expenseCategories = expenseCategories + "Other"
            }
            
            // Update existing entries with new categories
            updateCategoriesInExistingEntries()
            
            // If we have no categories at all, show a message
            if (incomeCategories.isEmpty() && expenseCategories.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please create categories first in the Categories section",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun updateCategoriesInExistingEntries() {
        // Update income entries
        for (entry in incomeEntries) {
            val currentSelection = entry.getSelectedCategory()
            entry.updateCategories(incomeCategories, currentSelection)
        }
        
        // Update expense entries
        for (entry in expenseEntries) {
            val currentSelection = entry.getSelectedCategory()
            entry.updateCategories(expenseCategories, currentSelection)
        }
    }
    
    private fun setupListeners() {
        // Month/Year selection
        selectMonthYearButton.setOnClickListener {
            showMonthYearPicker()
        }
        
        // Add income entry
        addIncomeButton.setOnClickListener {
            addIncomeEntry()
        }
        
        // Add expense entry
        addExpenseButton.setOnClickListener {
            addExpenseEntry()
        }
        
        // Other category auto-assign checkbox
        autoAssignCheckbox.setOnCheckedChangeListener { _, isChecked ->
            otherCategoryBudget.isEnabled = !isChecked
            if (isChecked) {
                otherCategoryBudget.setText("0.00")
            }
            updateBudgetSummary()
        }
        
        // Other category budget amount changes
        otherCategoryBudget.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateBudgetSummary()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Save button
        saveButton.setOnClickListener {
            saveBudget()
        }
        
        // Cancel button
        cancelButton.setOnClickListener {
            // Navigate back to analytics fragment
            findNavController().popBackStack()
        }
    }
    
    private fun addInitialEntries() {
        // Only add entries if we have categories
        if (incomeCategories.isNotEmpty()) {
            // Add one income entry by default
            addIncomeEntry()
        }
        
        if (expenseCategories.isNotEmpty()) {
            // Add two expense entries by default
            addExpenseEntry()
            addExpenseEntry()
        }
    }
    
    private fun addIncomeEntry() {
        val entry = BudgetEntryView(requireContext(), incomeCategories, true)
        incomeItemsContainer.addView(entry.rootView)
        incomeEntries.add(entry)
        
        // Add listener for changes
        entry.setOnChangeListener {
            updateBudgetSummary()
        }
        
        // Add listener for removal
        entry.setOnRemoveListener {
            incomeItemsContainer.removeView(entry.rootView)
            incomeEntries.remove(entry)
            updateBudgetSummary()
        }
        
        updateBudgetSummary()
    }
    
    private fun addExpenseEntry() {
        val entry = BudgetEntryView(requireContext(), expenseCategories, false)
        expenseItemsContainer.addView(entry.rootView)
        expenseEntries.add(entry)
        
        // Add listener for changes
        entry.setOnChangeListener {
            updateBudgetSummary()
        }
        
        // Add listener for removal
        entry.setOnRemoveListener {
            expenseItemsContainer.removeView(entry.rootView)
            expenseEntries.remove(entry)
            updateBudgetSummary()
        }
        
        updateBudgetSummary()
    }
    
    private fun updateBudgetSummary() {
        // Calculate total income
        val totalIncome = incomeEntries.sumOf { it.getAmount() }
        
        // Calculate total explicit expenses (excluding "Other")
        val totalExplicitExpenses = expenseEntries
            .filter { it.getSelectedCategory() != "Other" }
            .sumOf { it.getAmount() }
        
        // Get "Other" category amount
        val otherCategoryAmount = expenseEntries
            .filter { it.getSelectedCategory() == "Other" }
            .sumOf { it.getAmount() }
        
        // Manual "Other" amount (if not auto-assigned)
        val manualOtherAmount = if (!autoAssignCheckbox.isChecked) {
            otherCategoryBudget.text.toString().toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
        
        // Total expenses (explicit + other)
        val totalExpenses = totalExplicitExpenses + otherCategoryAmount + 
            (if (!autoAssignCheckbox.isChecked) manualOtherAmount else 0.0)
        
        // Remaining budget
        val remainingBudget = totalIncome - totalExpenses
        
        // Auto-assigned amount for "Other" category
        val autoAssignedAmount = if (autoAssignCheckbox.isChecked) remainingBudget else 0.0
        
        // Update UI
        totalIncomeSummary.text = String.format("$%.2f", totalIncome)
        totalExpensesSummary.text = String.format("$%.2f", totalExpenses)
        
        // Show remaining budget or auto-assigned amount
        if (autoAssignCheckbox.isChecked) {
            remainingBudgetSummary.text = String.format("$%.2f (Auto-assigned to Other)", autoAssignedAmount)
        } else {
            remainingBudgetSummary.text = String.format("$%.2f", remainingBudget)
        }
        
        // Set color for remaining budget
        if (remainingBudget < 0 && !autoAssignCheckbox.isChecked) {
            remainingBudgetSummary.setTextColor(resources.getColor(R.color.expense_red, null))
        } else {
            remainingBudgetSummary.setTextColor(resources.getColor(R.color.primary, null))
        }
    }
    
    private fun loadExistingBudget() {
        budgetRepository.getBudgetForMonth(selectedMonth + 1, selectedYear).observe(viewLifecycleOwner) { budget ->
            // Store the budget
            existingBudget = budget
            
            // Get user categories directly from ViewModel (not from existing budget)
            val userCategories = viewModel.categories.value ?: emptyList()
            val userIncomeCategories = userCategories.filter { it.type == "Income" }.map { it.name }
            val userExpenseCategories = userCategories.filter { it.type == "Expense" }.map { it.name }
            
            // Force update our local lists to ONLY contain user-created categories
            incomeCategories = userIncomeCategories
            // Always add "Other" to expense categories
            expenseCategories = userExpenseCategories + "Other"
            
            // Clear existing entries before adding new ones
            clearExistingEntries()
            
            // If there's no existing budget, just add initial entries
            if (budget == null) {
                addInitialEntries()
                return@observe
            }
            
            // Add entries from budget, but ONLY for categories that the user has explicitly created
            budget.budgetItems.forEach { item ->
                if (item.isIncome() && userIncomeCategories.contains(item.categoryName)) {
                    // Add income item only if it exists in user categories
                    addIncomeEntryWithValues(item.categoryName, item.budgetAmount)
                } else if (item.isExpense()) {
                    if (item.categoryName == "Other") {
                        // Handle Other category
                        otherCategoryBudget.setText(item.budgetAmount.toString())
                        // If other category has a budget, disable auto-assign
                        if (item.budgetAmount > 0) {
                            autoAssignCheckbox.isChecked = false
                            otherCategoryBudget.isEnabled = true
                        }
                    } else if (userExpenseCategories.contains(item.categoryName)) {
                        // Only add expense items for user-created categories
                        addExpenseEntryWithValues(item.categoryName, item.budgetAmount)
                    }
                }
            }
            
            // Update summary
            updateBudgetSummary()
        }
    }
    
    private fun clearExistingEntries() {
        // Remove all income entries
        incomeEntries.forEach { incomeItemsContainer.removeView(it.rootView) }
        incomeEntries.clear()
        
        // Remove all expense entries
        expenseEntries.forEach { expenseItemsContainer.removeView(it.rootView) }
        expenseEntries.clear()
    }
    
    private fun addIncomeEntryWithValues(categoryName: String, amount: Double) {
        // Only add if the category exists in our income categories
        if (!incomeCategories.contains(categoryName)) return
        
        val entry = BudgetEntryView(requireContext(), incomeCategories, true)
        incomeItemsContainer.addView(entry.rootView)
        incomeEntries.add(entry)
        
        // Set values
        entry.updateCategories(incomeCategories, categoryName)
        entry.setAmount(amount)
        
        // Add listeners
        entry.setOnChangeListener { updateBudgetSummary() }
        entry.setOnRemoveListener {
            incomeItemsContainer.removeView(entry.rootView)
            incomeEntries.remove(entry)
            updateBudgetSummary()
        }
    }
    
    private fun addExpenseEntryWithValues(categoryName: String, amount: Double) {
        // Only add if the category exists in our expense categories
        if (!expenseCategories.contains(categoryName)) return
        
        val entry = BudgetEntryView(requireContext(), expenseCategories, false)
        expenseItemsContainer.addView(entry.rootView)
        expenseEntries.add(entry)
        
        // Set values
        entry.updateCategories(expenseCategories, categoryName)
        entry.setAmount(amount)
        
        // Add listeners
        entry.setOnChangeListener { updateBudgetSummary() }
        entry.setOnRemoveListener {
            expenseItemsContainer.removeView(entry.rootView)
            expenseEntries.remove(entry)
            updateBudgetSummary()
        }
    }
    
    private fun saveBudget() {
        // Get user categories directly from ViewModel (not any potentially cached ones)
        val userCategories = viewModel.categories.value ?: emptyList()
        val userIncomeCategories = userCategories.filter { it.type == "Income" }.map { it.name }
        val userExpenseCategories = userCategories.filter { it.type == "Expense" }.map { it.name }
        
        // Update our local lists to ONLY contain user-created categories
        incomeCategories = userIncomeCategories
        // Always add "Other" to expense categories
        expenseCategories = userExpenseCategories + "Other"
        
        // Validate entries
        if (incomeEntries.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one income source", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create budget items list
        val budgetItems = mutableListOf<BudgetItem>()
        
        // Add income items
        for (entry in incomeEntries) {
            val category = entry.getSelectedCategory()
            val amount = entry.getAmount()
            
            // Only add if category is a valid user category and amount > 0
            if (category.isNotBlank() && amount > 0 && userIncomeCategories.contains(category)) {
                budgetItems.add(
                    BudgetItem(
                        id = UUID.randomUUID().toString(),
                        categoryName = category,
                        type = "Income",
                        budgetAmount = amount,
                        expenseAmount = 0.0
                    )
                )
            }
        }
        
        // Add expense items (excluding Other)
        for (entry in expenseEntries) {
            val category = entry.getSelectedCategory()
            val amount = entry.getAmount()
            
            // Only add if category is a valid user category and amount > 0
            if (category.isNotBlank() && category != "Other" && amount > 0 && userExpenseCategories.contains(category)) {
                budgetItems.add(
                    BudgetItem(
                        id = UUID.randomUUID().toString(),
                        categoryName = category,
                        type = "Expense",
                        budgetAmount = amount,
                        expenseAmount = 0.0
                    )
                )
            }
        }
        
        // Add "Other" expense category separately if it has a budget amount
        val otherEntry = expenseEntries.find { it.getSelectedCategory() == "Other" }
        val otherAmount = if (otherEntry != null && otherEntry.getAmount() > 0) {
            otherEntry.getAmount()
        } else if (!autoAssignCheckbox.isChecked) {
            // Use manually entered amount
            otherCategoryBudget.text.toString().toDoubleOrNull() ?: 0.0
        } else {
            // Calculate total income
            val totalIncome = budgetItems
                .filter { it.isIncome() }
                .sumOf { it.budgetAmount }
            
            // Calculate total explicit expenses (excluding Other)
            val totalExplicitExpenses = budgetItems
                .filter { it.isExpense() }
                .sumOf { it.budgetAmount }
            
            // Auto-assign remaining budget to Other
            totalIncome - totalExplicitExpenses
        }
        
        // Add Other category with a minimum budget amount to ensure progress bars show
        budgetItems.add(
            BudgetItem(
                id = "other_${System.currentTimeMillis()}",
                categoryName = "Other",
                type = "Expense",
                budgetAmount = maxOf(100.0, otherAmount), // Ensure minimum 100.0 for progress bar
                expenseAmount = 0.0
            )
        )
        
        // Create or update monthly budget
        val monthlyBudget = MonthlyBudget(
            id = existingBudget?.id ?: UUID.randomUUID().toString(),
            month = selectedMonth + 1, // +1 because Calendar months are 0-based
            year = selectedYear,
            budgetItems = budgetItems.toMutableList(),
            createdAt = existingBudget?.createdAt ?: Date()
        )
        
        // Save to repository
        budgetRepository.createBudget(monthlyBudget)
        
        // Show success message
        val actionText = if (isEditing) "updated" else "saved"
        Toast.makeText(requireContext(), "Budget $actionText successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back
        findNavController().navigateUp()
    }
    
    private fun showMonthYearPicker() {
        val months = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        
        val years = (2020..2030).map { it.toString() }.toTypedArray()
        
        // Inflate dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)
        
        // Configure pickers
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = selectedMonth
        
        yearPicker.minValue = 0
        yearPicker.maxValue = years.size - 1
        yearPicker.displayedValues = years
        yearPicker.value = years.indexOf(selectedYear.toString())
        
        // Show dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Month and Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                selectedMonth = monthPicker.value
                selectedYear = years[yearPicker.value].toInt()
                updateMonthYearDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateMonthYearDisplay() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, selectedMonth)
        cal.set(Calendar.YEAR, selectedYear)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        budgetMonthYearText.text = monthYearFormat.format(cal.time)
    }
    
    /**
     * Inner class to manage budget entry views
     */
    inner class BudgetEntryView(
        private val context: android.content.Context,
        categories: List<String>,
        private val isIncome: Boolean
    ) {
        val rootView: View
        private val categorySpinner: Spinner
        private val amountInput: EditText
        private val removeButton: ImageButton
        
        private var onChangeListener: (() -> Unit)? = null
        private var onRemoveListener: (() -> Unit)? = null
        
        init {
            // Inflate layout
            rootView = LayoutInflater.from(context).inflate(
                R.layout.item_budget_entry,
                null,
                false
            )
            
            // Get views
            categorySpinner = rootView.findViewById(R.id.categorySpinner)
            amountInput = rootView.findViewById(R.id.amountInput)
            removeButton = rootView.findViewById(R.id.removeButton)
            
            // Setup category spinner
            setupCategorySpinner(categories)
            
            // Setup amount input
            amountInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    onChangeListener?.invoke()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            
            // Setup remove button
            removeButton.setOnClickListener {
                onRemoveListener?.invoke()
            }
        }
        
        fun setOnChangeListener(listener: () -> Unit) {
            onChangeListener = listener
        }
        
        fun setOnRemoveListener(listener: () -> Unit) {
            onRemoveListener = listener
        }
        
        fun getSelectedCategory(): String {
            return if (categorySpinner.selectedItem != null) {
                categorySpinner.selectedItem.toString()
            } else {
                ""
            }
        }
        
        fun getAmount(): Double {
            return amountInput.text.toString().toDoubleOrNull() ?: 0.0
        }
        
        fun updateCategories(categories: List<String>, selectedCategory: String? = null) {
            // Create adapter
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                categories
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter
            
            // Set selected category if provided
            if (selectedCategory != null && categories.contains(selectedCategory)) {
                val position = categories.indexOf(selectedCategory)
                if (position >= 0) {
                    categorySpinner.setSelection(position)
                }
            }
        }
        
        private fun setupCategorySpinner(categories: List<String>) {
            // Create adapter
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                categories
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter
            
            // Set listener
            categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onChangeListener?.invoke()
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }
            }
        }
        
        fun setAmount(amount: Double) {
            amountInput.setText(amount.toString())
        }
    }
} 
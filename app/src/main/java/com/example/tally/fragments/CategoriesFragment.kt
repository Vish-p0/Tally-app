package com.example.tally.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.databinding.FragmentCategoriesBinding
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import java.util.UUID
import java.util.regex.Pattern

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    
    private lateinit var incomeCategoryAdapter: CategoryTileAdapter
    private lateinit var expenseCategoryAdapter: CategoryTileAdapter
    
    private enum class CategoryFilter {
        ALL, INCOME, EXPENSE
    }
    
    private var currentFilter = CategoryFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabListeners()
        setupButtonListeners()
        updateFinancialData()
        observeCategories()
    }
    
    private fun setupRecyclerViews() {
        // Income categories adapter
        incomeCategoryAdapter = CategoryTileAdapter(emptyList()) { category ->
            showCategoryOptions(category)
        }
        binding.rvIncomeCategories.adapter = incomeCategoryAdapter
        
        // Expense categories adapter
        expenseCategoryAdapter = CategoryTileAdapter(emptyList()) { category ->
            showCategoryOptions(category)
        }
        binding.rvExpenseCategories.adapter = expenseCategoryAdapter
    }
    
    private fun setupTabListeners() {
        // Set up the filter tabs click listeners
        binding.tabAll.setOnClickListener {
            updateFilter(CategoryFilter.ALL)
        }
        
        binding.tabIncome.setOnClickListener {
            updateFilter(CategoryFilter.INCOME)
        }
        
        binding.tabExpense.setOnClickListener {
            updateFilter(CategoryFilter.EXPENSE)
        }
    }

    private fun setupButtonListeners() {
        // Notification button click listener
        binding.ivNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_categoriesFragment_to_notificationsFragment)
        }
        
        // Add Category button click listener
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
        
        // Long press on Add Category button to reset to defaults (hidden feature)
        binding.btnAddCategory.setOnLongClickListener {
            showResetCategoriesDialog()
            true
        }
    }
    
    private fun updateFilter(filter: CategoryFilter) {
        currentFilter = filter
        
        // Reset all tabs to inactive state
        resetTabsToInactive()
        
        // Update the selected tab's appearance
        val selectedTab = when (filter) {
            CategoryFilter.ALL -> binding.tabAll
            CategoryFilter.INCOME -> binding.tabIncome
            CategoryFilter.EXPENSE -> binding.tabExpense
        }
        selectedTab.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.lime_green))
        (selectedTab.getChildAt(0) as TextView).setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        (selectedTab.getChildAt(0) as TextView).setTypeface(null, Typeface.BOLD)
        
        // Update visibility of category sections based on the filter
        updateCategorySectionsVisibility()
    }
    
    private fun resetTabsToInactive() {
        val tabs = listOf(binding.tabAll, binding.tabIncome, binding.tabExpense)
        
        tabs.forEach { tab ->
            tab.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
            (tab.getChildAt(0) as TextView).setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            (tab.getChildAt(0) as TextView).setTypeface(null, Typeface.NORMAL)
        }
    }
    
    private fun updateCategorySectionsVisibility() {
        when (currentFilter) {
            CategoryFilter.ALL -> {
                binding.incomeCategoriesSection.visibility = View.VISIBLE
                binding.expenseCategoriesSection.visibility = View.VISIBLE
            }
            CategoryFilter.INCOME -> {
                binding.incomeCategoriesSection.visibility = View.VISIBLE
                binding.expenseCategoriesSection.visibility = View.GONE
            }
            CategoryFilter.EXPENSE -> {
                binding.incomeCategoriesSection.visibility = View.GONE
                binding.expenseCategoriesSection.visibility = View.VISIBLE
            }
        }
    }

    private fun updateFinancialData() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateTotalBalance(transactions)
            updateExpenseInfo(transactions)
        }
        
        // Observe currency changes to update the financial data
        viewModel.currentCurrency.observe(viewLifecycleOwner) { _ ->
            // When currency changes, refresh displayed amounts
            viewModel.transactions.value?.let { transactions ->
                updateTotalBalance(transactions)
                updateExpenseInfo(transactions)
            }
        }
    }

    private fun updateTotalBalance(transactions: List<Transaction>) {
        val totalBalance = transactions.sumOf { 
            if (it.type == "Income") it.amount else -it.amount 
        }
        binding.tvTotalBalance.text = viewModel.formatAmount(totalBalance)
    }

    private fun updateExpenseInfo(transactions: List<Transaction>) {
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        binding.tvTotalExpense.text = "-" + viewModel.formatAmount(totalExpense)
    }

    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            // Split categories by type
            val incomeCategories = categories.filter { it.type == "Income" }
            val expenseCategories = categories.filter { it.type == "Expense" }
            
            // Update the adapters
            incomeCategoryAdapter.updateCategories(incomeCategories)
            expenseCategoryAdapter.updateCategories(expenseCategories)
        }
    }

    private fun showCategoryOptions(category: Category) {
        showCategoryDetailsDialog(category)
    }
    
    private fun showCategoryDetailsDialog(category: Category) {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.LightGreenAlertDialogStyle)
        
        // Create a custom view for the dialog
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_category_details, null)
        
        // Find views in the dialog layout
        val tvEmojiDisplay = dialogView.findViewById<TextView>(R.id.tvEmojiDisplay)
        val tvCategoryName = dialogView.findViewById<TextView>(R.id.tvCategoryName)
        val tvCategoryType = dialogView.findViewById<TextView>(R.id.tvCategoryType)
        val tvCategoryEmoji = dialogView.findViewById<TextView>(R.id.tvCategoryEmoji)
        val btnEdit = dialogView.findViewById<MaterialButton>(R.id.btnEdit)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDelete)
        
        // Set category details
        tvEmojiDisplay.text = category.emoji
        tvCategoryName.text = "Name: ${category.name}"
        tvCategoryType.text = "Type: ${category.type}"
        tvCategoryEmoji.text = "Emoji: ${category.emoji}"
        
        // Create the dialog
        val dialog = builder.setView(dialogView).create()
        
        // Set button click listeners
        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditCategoryDialog(category)
        }
        
        btnDelete.setOnClickListener {
            dialog.dismiss()
            checkAndDeleteCategory(category)
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Show the dialog
        dialog.show()
    }

    private fun checkAndDeleteCategory(category: Category) {
        val transactions = viewModel.transactions.value ?: emptyList()
        val hasTransactions = transactions.any { it.categoryId == category.id }
        
        if (hasTransactions) {
            Toast.makeText(requireContext(), "Cannot delete category with existing transactions", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create custom delete confirmation dialog
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.LightGreenAlertDialogStyle)
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_delete_confirmation, null)
        
        // Find views in the dialog layout
        val tvDeleteTitle = dialogView.findViewById<TextView>(R.id.tvDeleteTitle)
        val tvDeleteMessage = dialogView.findViewById<TextView>(R.id.tvDeleteMessage)
        val tvEmojiDisplay = dialogView.findViewById<TextView>(R.id.tvEmojiDisplay)
        val btnConfirmDelete = dialogView.findViewById<MaterialButton>(R.id.btnConfirmDelete)
        val btnCancelDelete = dialogView.findViewById<MaterialButton>(R.id.btnCancelDelete)
        
        // Set confirmation details
        tvDeleteTitle.text = "Delete Category"
        tvDeleteMessage.text = "Are you sure you want to delete this category? This action cannot be undone."
        tvEmojiDisplay.text = category.emoji
        
        // Create and show the dialog
        val dialog = builder.setView(dialogView).create()
        
        // Set button click listeners
        btnConfirmDelete.setOnClickListener {
            viewModel.deleteCategory(category.id)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
        }
        
        btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }
        
        // Show the dialog
        dialog.show()
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val emojiInput = dialogView.findViewById<EditText>(R.id.emojiInput)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.categoryTypeRadioGroup)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.radioIncome)
        
        // Set input filters
        nameInput.filters = arrayOf(InputFilter.LengthFilter(20))
        emojiInput.filters = arrayOf(InputFilter.LengthFilter(2))
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
            
        // Set up button click listeners
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val emoji = emojiInput.text.toString()
            val type = if (radioIncome.isChecked) "Income" else "Expense"
            
            if (name.isNotBlank() && emoji.isNotBlank()) {
                val newCategory = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    emoji = emoji
                )
                viewModel.addCategory(newCategory)
                dialog.dismiss()
            } else if (name.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
            } else if (emoji.isBlank()) {
                Toast.makeText(requireContext(), "Please select an emoji", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val emojiInput = dialogView.findViewById<EditText>(R.id.emojiInput)
        val radioGroupType = dialogView.findViewById<RadioGroup>(R.id.categoryTypeRadioGroup)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.radioIncome)
        val radioExpense = dialogView.findViewById<RadioButton>(R.id.radioExpense)
        
        // Set input filters
        nameInput.filters = arrayOf(InputFilter.LengthFilter(20))
        emojiInput.filters = arrayOf(InputFilter.LengthFilter(2))
        
        // Set values from the existing category
        nameInput.setText(category.name)
        emojiInput.setText(category.emoji)
        if (category.type == "Income") {
            radioIncome.isChecked = true
        } else {
            radioExpense.isChecked = true
        }
        
        // Update title
        dialogView.findViewById<TextView>(R.id.dialogTitle)?.text = "Edit Category"
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
            
        // Set up button click listeners
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val emoji = emojiInput.text.toString()
            val type = if (radioIncome.isChecked) "Income" else "Expense"
            
            if (name.isNotBlank() && emoji.isNotBlank()) {
                val updatedCategory = category.copy(name = name, type = type, emoji = emoji)
                viewModel.updateCategory(updatedCategory)
                dialog.dismiss()
            } else if (name.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
            } else if (emoji.isBlank()) {
                Toast.makeText(requireContext(), "Please select an emoji", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showResetCategoriesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Categories")
            .setMessage("This will clear all categories. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                viewModel.resetCategoriesToDefaults()
                Toast.makeText(requireContext(), "Categories cleared", Toast.LENGTH_SHORT).show()
                refreshCategoriesDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun refreshCategoriesDisplay() {
        // Clear adapter data
        incomeCategoryAdapter.updateCategories(emptyList())
        expenseCategoryAdapter.updateCategories(emptyList())
        
        // Force reload categories
        viewModel.loadCategories()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Inner class for the Category Tile Adapter
    inner class CategoryTileAdapter(
        private var categories: List<Category>,
        private val onItemClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryTileAdapter.CategoryViewHolder>() {
        
        // Category background color for all categories (light_green)
        private val categoryColor = "#DFF7E2" // light_green color
        
        fun updateCategories(newCategories: List<Category>) {
            categories = newCategories
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_tile, parent, false)
            return CategoryViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            if (position < categories.size) {
                // Regular category item
                val category = categories[position]
                holder.bind(category)
            } else {
                // "Add" item at the end
                holder.bindAddItem()
            }
        }
        
        override fun getItemCount(): Int = categories.size + 1 // +1 for the "Add" item
        
        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val emojiTextView: TextView = itemView.findViewById(R.id.tvEmoji)
            private val nameTextView: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val cardView: CardView = itemView as CardView
            
            fun bind(category: Category) {
                emojiTextView.text = category.emoji
                nameTextView.text = category.name
                
                // Set all category backgrounds to light_green
                try {
                    cardView.setCardBackgroundColor(android.graphics.Color.parseColor(categoryColor))
                } catch (e: Exception) {
                    // Fallback if color parsing fails
                    cardView.setCardBackgroundColor(resources.getColor(R.color.light_green, null))
                }
                
                itemView.setOnClickListener { onItemClick(category) }
            }
            
            fun bindAddItem() {
                emojiTextView.text = "+"
                nameTextView.text = "Add"
                
                // Set a different color for the "Add" item
                try {
                    cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                } catch (e: Exception) {
                    // Fallback if color parsing fails
                }
                
                itemView.setOnClickListener { showAddCategoryDialog() }
            }
        }
    }
    
    /**
     * Custom input filter to restrict input to emoji characters
     */
    private class EmojiInputFilter : InputFilter {
        override fun filter(
            source: CharSequence?, 
            start: Int, 
            end: Int, 
            dest: Spanned?, 
            dstart: Int, 
            dend: Int
        ): CharSequence? {
            if (source == null || source.isEmpty()) return null
            
            // Allow any character - we'll validate on save
            return null
        }
        
        companion object {
            /**
             * Simple check if a character is likely an emoji
             * This is a simpler approach that works for most common emoji
             */
            fun isEmojiChar(text: String): Boolean {
                if (text.isEmpty()) return false
                
                // Simple validation - most emoji characters have code points outside the standard ASCII range
                // This is not perfect but works for most common emoji
                val codePoint = text.codePointAt(0)
                return codePoint > 255
            }
        }
    }
    
    /**
     * Check if the input string is a valid emoji
     */
    private fun isEmojiValid(text: String): Boolean {
        // Allow any single character or emoji (emoji can be multiple code points)
        return text.isNotBlank() && text.length <= 2
    }
}
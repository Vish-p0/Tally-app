package com.example.tally.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.tally.R
import com.example.tally.databinding.FragmentAddTransactionBinding
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    
    private val calendar = Calendar.getInstance()
    private var selectedCategoryId: String? = null
    private var transactionType = "Expense" // Default to expense
    
    // Variables for edit mode
    private var isEditMode = false
    private var transactionToEdit: Transaction? = null
    
    companion object {
        const val ARG_TRANSACTION_TYPE = "transaction_type"
        const val ARG_TRANSACTION_ID = "transactionId"
        const val ARG_IS_EDITING = "isEditing"
        
        fun newInstance(transactionType: String): AddTransactionFragment {
            val fragment = AddTransactionFragment()
            val args = Bundle()
            args.putString(ARG_TRANSACTION_TYPE, transactionType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if we're in edit mode
        isEditMode = arguments?.getBoolean(ARG_IS_EDITING, false) ?: false
        
        if (isEditMode) {
            // Get the transaction ID from arguments and find the transaction
            val transactionId = arguments?.getString(ARG_TRANSACTION_ID)
            if (transactionId != null) {
                loadTransactionForEditing(transactionId)
            } else {
                // No transaction ID provided, fallback to adding mode
                isEditMode = false
                setupForAddMode()
            }
        } else {
            setupForAddMode()
        }
        
        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
        
        // Setup listeners
        setupBackButton()
        setupCalendarPicker()
        setupCategorySelection()
        setupSaveButton()
    }
    
    private fun setupForAddMode() {
        // Initialize with current date
        updateDateDisplay()
        
        // Get transaction type from arguments
        arguments?.getString(ARG_TRANSACTION_TYPE)?.let {
            transactionType = when (it) {
                "ForceIncome" -> "Income"
                "ForceExpense" -> "Expense"
                "ALL" -> "Expense" // Default to Expense for ALL tab
                else -> it
            }
            
            // Remember the original type for UI setup
            val originalType = it
            
            // Set up the UI based on type
            setupRadioButtons(originalType)
            updateUIForTransactionType(transactionType)
        } ?: run {
            // No arguments, default to Expense
            transactionType = "Expense"
            setupRadioButtons("Expense")
            updateUIForTransactionType(transactionType)
        }
    }
    
    private fun loadTransactionForEditing(transactionId: String) {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            val transaction = transactions.find { it.id == transactionId }
            if (transaction != null) {
                transactionToEdit = transaction
                populateFormForEditing(transaction)
            } else {
                // Transaction not found, fallback to adding mode
                isEditMode = false
                setupForAddMode()
                Toast.makeText(context, "Transaction not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun populateFormForEditing(transaction: Transaction) {
        // Set transaction type
        transactionType = transaction.type
        setupRadioButtons(transaction.type)
        
        // Set title
        binding.tvFragmentTitle.text = "Edit ${transaction.type}"
        
        // Set amount (remove $ sign and format)
        binding.etAmount.setText(transaction.amount.toString())
        
        // Set transaction title
        binding.etTitle.setText(transaction.title)
        
        // Set description
        binding.etDescription.setText(transaction.description)
        
        // Set category
        selectedCategoryId = transaction.categoryId
        
        // Update category name display
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val category = categories.find { it.id == transaction.categoryId }
            if (category != null) {
                binding.tvSelectedCategory.text = "${category.emoji} ${category.name}"
            }
        }
        
        // Set date
        if (transaction.date != null) {
            calendar.timeInMillis = transaction.date
            updateDateDisplay()
        }
    }
    
    private fun setupRadioButtons(originalType: String) {
        // Disable radio buttons based on forcing or editing
        if (originalType == "ForceIncome") {
            binding.rbIncome.isChecked = true
            binding.rbExpense.isChecked = false
            binding.rbIncome.isEnabled = false
            binding.rbExpense.isEnabled = false
        } else if (originalType == "ForceExpense") {
            binding.rbIncome.isChecked = false 
            binding.rbExpense.isChecked = true
            binding.rbIncome.isEnabled = false
            binding.rbExpense.isEnabled = false
        } else {
            // Handle ALL or regular Income/Expense, and edit mode
            binding.rbIncome.isEnabled = true
            binding.rbExpense.isEnabled = true
            
            if (transactionType == "Income") {
                binding.rbIncome.isChecked = true
                binding.rbExpense.isChecked = false
            } else {
                binding.rbIncome.isChecked = false
                binding.rbExpense.isChecked = true
            }
        }
        
        // Set up radio group change listener
        binding.rgTransactionType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbIncome -> {
                    transactionType = "Income"
                    updateUIForTransactionType(transactionType)
                }
                R.id.rbExpense -> {
                    transactionType = "Expense"
                    updateUIForTransactionType(transactionType)
                }
            }
        }
    }
    
    private fun updateUIForTransactionType(type: String) {
        if (isEditMode) {
            binding.tvFragmentTitle.text = when (type) {
                "Income" -> "Edit Income"
                "Expense" -> "Edit Expense"
                else -> "Edit Transaction"
            }
        } else {
            binding.tvFragmentTitle.text = when (type) {
                "Income", "ForceIncome" -> "Add Income"
                "Expense", "ForceExpense" -> "Add Expense"
                else -> "Add Transaction"
            }
        }
        
        // Clear category selection since categories will change based on type
        binding.tvSelectedCategory.text = "Select the category"
        selectedCategoryId = null
    }
    
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCalendarPicker() {
        binding.btnCalendarPicker.setOnClickListener {
            showDatePicker()
        }
        
        binding.tvSelectedDate.setOnClickListener {
            showDatePicker()
        }
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.LightGreenDatePickerStyle,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(calendar.time)
    }
    
    private fun setupCategorySelection() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val filteredCategories = categories.filter { it.type == transactionType }
            setupCategoryDialog(filteredCategories)
        }
    }
    
    private fun showCategorySelectionDialog() {
        val categories = viewModel.categories.value?.filter { it.type == transactionType } ?: emptyList()
        
        if (categories.isEmpty()) {
            Toast.makeText(context, "No ${transactionType.lowercase()} categories available", Toast.LENGTH_SHORT).show()
            return
        }
        
        val categoryNames = categories.map { "${it.emoji} ${it.name}" }.toTypedArray()
        val categoryIds = categories.map { it.id }.toTypedArray()
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(
            requireContext(), 
            R.style.LightGreenAlertDialogStyle
        )
        builder.setTitle("Select ${transactionType} Category")
        builder.setItems(categoryNames) { dialog, which ->
            val selectedCategory = categories[which]
            showCategoryDetailsDialog(selectedCategory)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.light_green_dialog_background)
        dialog.show()
    }
    
    private fun showCategoryDetailsDialog(category: Category) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(
            requireContext(),
            R.style.LightGreenAlertDialogStyle
        )
        
        // Set up the dialog
        builder.setTitle("Category Details")
        
        // Create a custom layout for details
        val message = "Name: ${category.name}\n" +
                "Type: ${category.type}\n" +
                "Emoji: ${category.emoji}"
        
        builder.setMessage(message)
        
        // Add buttons
        builder.setPositiveButton("Select") { _, _ ->
            // Select this category for the transaction
            selectedCategoryId = category.id
            binding.tvSelectedCategory.text = "${category.emoji} ${category.name}"
        }
        
        builder.setNeutralButton("Edit") { dialog, _ ->
            dialog.dismiss()
            showEditCategoryDialog(category)
        }
        
        // Add a delete button
        builder.setNegativeButton("Delete") { dialog, _ ->
            dialog.dismiss()
            showDeleteCategoryConfirmation(category)
        }
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.light_green_dialog_background)
        dialog.show()
    }
    
    private fun showDeleteCategoryConfirmation(category: Category) {
        // Check if category has transactions
        val transactions = viewModel.transactions.value ?: emptyList()
        val hasTransactions = transactions.any { it.categoryId == category.id }
        
        if (hasTransactions) {
            // Show error toast if category has transactions
            Toast.makeText(
                requireContext(),
                "Cannot delete category with existing transactions",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Show confirmation dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(
            requireContext(),
            R.style.LightGreenAlertDialogStyle
        )
        
        builder.setTitle("Delete Category")
        builder.setMessage("Are you sure you want to delete '${category.emoji} ${category.name}'?")
        
        builder.setPositiveButton("Delete") { _, _ ->
            // Delete the category
            viewModel.deleteCategory(category.id)
            
            // If this was the selected category, clear selection
            if (selectedCategoryId == category.id) {
                selectedCategoryId = null
                binding.tvSelectedCategory.text = "Select the category"
            }
            
            Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
        }
        
        builder.setNegativeButton("Cancel", null)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.light_green_dialog_background)
        dialog.show()
    }
    
    private fun showEditCategoryDialog(category: Category) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(
            requireContext(),
            R.style.LightGreenAlertDialogStyle
        )
        
        // Inflate custom layout for editing
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_category, null)
        
        // Find views in the dialog layout
        val etCategoryName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etCategoryEmoji = dialogView.findViewById<EditText>(R.id.etCategoryEmoji)
        
        // Pre-fill with existing category data
        etCategoryName.setText(category.name)
        etCategoryEmoji.setText(category.emoji)
        
        builder.setTitle("Edit Category")
        builder.setView(dialogView)
        
        builder.setPositiveButton("Save") { _, _ ->
            val newName = etCategoryName.text.toString().trim()
            val newEmoji = etCategoryEmoji.text.toString().trim()
            
            if (newName.isEmpty()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            if (newEmoji.isEmpty()) {
                Toast.makeText(context, "Emoji cannot be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            // Create updated category
            val updatedCategory = category.copy(
                name = newName,
                emoji = newEmoji
            )
            
            // Update in ViewModel
            viewModel.updateCategory(updatedCategory)
            
            // If this was the selected category, update the display
            if (selectedCategoryId == category.id) {
                binding.tvSelectedCategory.text = "${updatedCategory.emoji} ${updatedCategory.name}"
            }
            
            Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
        }
        
        builder.setNegativeButton("Cancel", null)
        
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.light_green_dialog_background)
        dialog.show()
    }
    
    private fun setupCategoryDialog(categories: List<Category>) {
        binding.cardCategorySelect.setOnClickListener {
            showCategorySelectionDialog()
        }
    }
    
    private fun setupSaveButton() {
        // Update button text for edit mode
        if (isEditMode) {
            binding.btnSave.findViewById<android.widget.TextView>(R.id.btnSaveText).text = "Update"
        }
        
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }
    
    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().replace("$", "").trim()
        val description = binding.etDescription.text.toString().trim()
        
        if (title.isEmpty()) {
            Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (amountStr.isEmpty()) {
            Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedCategoryId == null) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val amount = amountStr.toDouble()
            
            if (isEditMode && transactionToEdit != null) {
                // Update existing transaction
                val updatedTransaction = transactionToEdit!!.copy(
                    title = title,
                    amount = amount,
                    categoryId = selectedCategoryId!!,
                    description = description,
                    date = calendar.timeInMillis,
                    type = transactionType  // Update the transaction type
                )
                
                viewModel.updateTransaction(updatedTransaction)
                Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
            } else {
                // Create new transaction
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    amount = amount,
                    categoryId = selectedCategoryId!!,
                    type = transactionType,
                    description = description,
                    date = calendar.timeInMillis
                )
                
                viewModel.addTransaction(transaction)
                Toast.makeText(context, "${transactionType} transaction saved", Toast.LENGTH_SHORT).show()
            }
            
            findNavController().navigateUp()
            
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
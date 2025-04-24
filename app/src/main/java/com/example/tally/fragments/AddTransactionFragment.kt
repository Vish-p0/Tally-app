package com.example.tally.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    
    companion object {
        const val ARG_TRANSACTION_TYPE = "transaction_type"
        
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
    
    private fun setupRadioButtons(originalType: String) {
        // Disable radio buttons based on forcing
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
            // Handle ALL or regular Income/Expense
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
        binding.tvFragmentTitle.text = when (type) {
            "Income", "ForceIncome" -> "Add Income"
            "Expense", "ForceExpense" -> "Add Expense"
            else -> "Add Transaction"
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
        builder.setSingleChoiceItems(categoryNames, -1) { dialog, which ->
            selectedCategoryId = categoryIds[which]
            binding.tvSelectedCategory.text = categoryNames[which]
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
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
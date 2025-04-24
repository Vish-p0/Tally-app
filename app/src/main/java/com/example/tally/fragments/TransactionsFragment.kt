package com.example.tally.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tally.R
import com.example.tally.adapters.TransactionAdapter
import com.example.tally.databinding.FragmentTransactionsBinding
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.utils.formatCurrency
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.navigation.fragment.findNavController
import com.example.tally.fragments.TransactionSwipeHelper

class TransactionsFragment : Fragment(), 
    TransactionDetailsDialogFragment.TransactionDialogListener,
    DeleteConfirmationDialogFragment.DeleteConfirmationListener {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val calendar = Calendar.getInstance()

    // Filter states
    private var currentFilterType = FilterType.ALL
    private var currentTimePeriod = TimePeriod.MONTHLY
    private var selectedYear = calendar.get(Calendar.YEAR)
    private var selectedMonth = calendar.get(Calendar.MONTH)
    private var selectedWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    private var selectedDay = calendar.get(Calendar.DAY_OF_MONTH)

    enum class FilterType {
        ALL, INCOME, EXPENSE
    }

    enum class TimePeriod {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        swipeRefreshLayout = binding.swipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabLayout()
        setupTimePeriodSpinner()
        setupCalendarButton()
        setupSwipeRefresh()
        setupSearch()
        setupSummaryCards()
        observeTransactions()
        updateCurrentPeriodText()
        
        // Add transaction button - navigate to AddTransactionFragment with appropriate type
        binding.fabAddTransaction.setOnClickListener {
            val bundle = Bundle().apply {
                val transactionType = when (currentFilterType) {
                    FilterType.ALL -> "ALL"
                    FilterType.INCOME -> "ForceIncome"
                    FilterType.EXPENSE -> "ForceExpense"
                }
                putString(AddTransactionFragment.ARG_TRANSACTION_TYPE, transactionType)
            }
            findNavController().navigate(R.id.action_transactionsFragment_to_addTransactionFragment, bundle)
        }

        // Observe currency changes
        viewModel.currentCurrency.observe(viewLifecycleOwner) { _ ->
            // When currency changes, refresh displayed amounts
            viewModel.transactions.value?.let { transactions ->
                updateSummaryAmounts(transactions)
                
                // Also refresh the adapter to update transaction amounts
                adapter.submitList(null)
                adapter.submitList(filterTransactionsByDate(transactions))
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            viewModel = viewModel,
            onItemClick = { transaction ->
                showTransactionDetails(transaction)
            }
        )
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TransactionsFragment.adapter
        }
        
        // Setup swipe-to-delete
        val swipeHelper = TransactionSwipeHelper(
            fragment = this,
            recyclerView = binding.rvTransactions,
            adapter = adapter,
            onShowDeleteDialog = { transaction ->
                showDeleteConfirmationDialog(transaction.id)
            }
        )
        swipeHelper.setupSwipeToDelete()
    }

    private fun setupTabLayout() {
        // Set default selected tab (All)
        binding.tabAll.background = resources.getDrawable(R.drawable.segment_selected, null)
        binding.tabAll.setTextColor(resources.getColor(R.color.primary, null))
        binding.tabAll.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        binding.tabIncome.setTextColor(resources.getColor(R.color.primary, null))
        binding.tabExpense.setTextColor(resources.getColor(R.color.primary, null))
        
        // Set filter type to ALL by default
        currentFilterType = FilterType.ALL
        
        binding.tabAll.setOnClickListener {
            updateSelectedFilterTab(FilterType.ALL)
        }
        
        binding.tabIncome.setOnClickListener {
            updateSelectedFilterTab(FilterType.INCOME)
        }
        
        binding.tabExpense.setOnClickListener {
            updateSelectedFilterTab(FilterType.EXPENSE)
        }
    }

    private fun updateSelectedFilterTab(filterType: FilterType) {
        // Reset all backgrounds and text colors
        binding.tabAll.background = null
        binding.tabIncome.background = null
        binding.tabExpense.background = null
        
        // Reset all text styles
        binding.tabAll.setTextColor(resources.getColor(R.color.primary, null))
        binding.tabIncome.setTextColor(resources.getColor(R.color.primary, null))
        binding.tabExpense.setTextColor(resources.getColor(R.color.primary, null))
        binding.tabAll.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        binding.tabIncome.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        binding.tabExpense.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        
        // Set selected tab
        when (filterType) {
            FilterType.ALL -> {
                binding.tabAll.background = resources.getDrawable(R.drawable.segment_selected, null)
                binding.tabAll.setTextColor(resources.getColor(R.color.primary, null))
                binding.tabAll.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            }
            FilterType.INCOME -> {
                binding.tabIncome.background = resources.getDrawable(R.drawable.segment_selected, null)
                binding.tabIncome.setTextColor(resources.getColor(R.color.primary, null))
                binding.tabIncome.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            }
            FilterType.EXPENSE -> {
                binding.tabExpense.background = resources.getDrawable(R.drawable.segment_selected, null)
                binding.tabExpense.setTextColor(resources.getColor(R.color.primary, null))
                binding.tabExpense.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            }
        }
        
        currentFilterType = filterType
        applyFilters()
    }

    private fun setupTimePeriodSpinner() {
        val timePeriods = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_period, timePeriods)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerTimePeriod.adapter = spinnerAdapter

        // Set default selection to Monthly
        binding.spinnerTimePeriod.setSelection(2)

        binding.spinnerTimePeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentTimePeriod = when (position) {
                    0 -> TimePeriod.DAILY
                    1 -> TimePeriod.WEEKLY
                    2 -> TimePeriod.MONTHLY
                    3 -> TimePeriod.YEARLY
                    else -> TimePeriod.MONTHLY
                }
                updateCurrentPeriodText()
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCalendarButton() {
        binding.btnCalendar.setOnClickListener {
            when (currentTimePeriod) {
                TimePeriod.DAILY -> showDatePicker()
                TimePeriod.WEEKLY -> showWeekPicker()
                TimePeriod.MONTHLY -> showMonthYearPicker()
                TimePeriod.YEARLY -> showYearPicker()
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })
    }

    private fun setupSummaryCards() {
        // Setup income card click filter
        binding.cardIncome.setOnClickListener {
            updateSelectedFilterTab(FilterType.INCOME)
        }

        // Setup expense card click filter
        binding.cardExpense.setOnClickListener {
            updateSelectedFilterTab(FilterType.EXPENSE)
        }
    }

    private fun observeTransactions() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            // Apply current filters
            applyFilters()

            // Update summary amounts
            updateSummaryAmounts(transactions)
        }
    }

    private fun updateSummaryAmounts(transactions: List<Transaction>) {
        // First apply the date filter to get transactions for the current time period
        val filteredByDate = filterTransactionsByDate(transactions)
        
        // Calculate income, expense and balance from the date-filtered transactions
        val income = filteredByDate.filter { it.type == "Income" }.sumOf { it.amount }
        val expense = filteredByDate.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = income - expense

        binding.tvTotalBalance.text = viewModel.formatAmount(balance)
        binding.tvIncomeAmount.text = viewModel.formatAmount(income)
        binding.tvExpenseAmount.text = viewModel.formatAmount(expense)
    }

    private fun applyFilters() {
        val allTransactions = viewModel.transactions.value ?: emptyList()

        // First filter by date period
        val dateFilteredTransactions = filterTransactionsByDate(allTransactions)

        // Then filter by transaction type
        val typeFilteredTransactions = when (currentFilterType) {
            FilterType.ALL -> dateFilteredTransactions
            FilterType.INCOME -> dateFilteredTransactions.filter { it.type == "Income" }
            FilterType.EXPENSE -> dateFilteredTransactions.filter { it.type == "Expense" }
        }

        adapter.submitList(typeFilteredTransactions)
        
        // Update the summary amounts based on the current date filter
        // This ensures the summary reflects the current time period
        updateSummaryAmounts(allTransactions)
    }

    private fun filterTransactionsByDate(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()

        return when (currentTimePeriod) {
            TimePeriod.DAILY -> {
                calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                val startOfDay = calendar.timeInMillis

                calendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)
                val endOfDay = calendar.timeInMillis

                transactions.filter {
                    val txDate = it.date ?: System.currentTimeMillis()
                    txDate in startOfDay..endOfDay
                }
            }
            TimePeriod.WEEKLY -> {
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.WEEK_OF_YEAR, selectedWeek)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val startOfWeek = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val endOfWeek = calendar.timeInMillis

                transactions.filter {
                    val txDate = it.date ?: System.currentTimeMillis()
                    txDate in startOfWeek..endOfWeek
                }
            }
            TimePeriod.MONTHLY -> {
                calendar.set(selectedYear, selectedMonth, 1, 0, 0, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.set(selectedYear, selectedMonth, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                val endOfMonth = calendar.timeInMillis

                transactions.filter {
                    val txDate = it.date ?: System.currentTimeMillis()
                    txDate in startOfMonth..endOfMonth
                }
            }
            TimePeriod.YEARLY -> {
                calendar.set(selectedYear, Calendar.JANUARY, 1, 0, 0, 0)
                val startOfYear = calendar.timeInMillis

                calendar.set(selectedYear, Calendar.DECEMBER, 31, 23, 59, 59)
                val endOfYear = calendar.timeInMillis

                transactions.filter {
                    val txDate = it.date ?: System.currentTimeMillis()
                    txDate in startOfYear..endOfYear
                }
            }
        }
    }

    private fun updateCurrentPeriodText() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear)

        // Just show the period name without the period type
        val text = when (currentTimePeriod) {
            TimePeriod.DAILY -> {
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
            }
            TimePeriod.WEEKLY -> {
                calendar.set(Calendar.WEEK_OF_YEAR, selectedWeek)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val startDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val endDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
                "$startDate - $endDate, $selectedYear"
            }
            TimePeriod.MONTHLY -> {
                calendar.set(Calendar.MONTH, selectedMonth)
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
            }
            TimePeriod.YEARLY -> {
                selectedYear.toString()
            }
        }

        binding.tvCurrentPeriod.text = text
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.LightGreenDatePickerStyle,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
                updateCurrentPeriodText()
                applyFilters()
            },
            selectedYear,
            selectedMonth,
            selectedDay
        )
        datePickerDialog.show()
    }

    private fun showWeekPicker() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, selectedYear)
        calendar.set(Calendar.WEEK_OF_YEAR, selectedWeek)

        // Use DatePickerDialog as a starting point to select a day in the desired week
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.LightGreenDatePickerStyle,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                selectedYear = year
                selectedWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                updateCurrentPeriodText()
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setTitle("Select a day in the desired week")
        datePickerDialog.show()
    }

    private fun showMonthYearPicker() {
        val months = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")

        val years = (2020..2030).map { it.toString() }.toTypedArray()

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<android.widget.NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<android.widget.NumberPicker>(R.id.yearPicker)

        // Style the number pickers
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = selectedMonth
        setNumberPickerTextColor(monthPicker, resources.getColor(R.color.primary, null))

        yearPicker.minValue = 0
        yearPicker.maxValue = years.size - 1
        yearPicker.displayedValues = years
        yearPicker.value = years.indexOf(selectedYear.toString())
        setNumberPickerTextColor(yearPicker, resources.getColor(R.color.primary, null))

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.LightGreenAlertDialogStyle)
            .setTitle("Select Month and Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                selectedMonth = monthPicker.value
                selectedYear = years[yearPicker.value].toInt()
                updateCurrentPeriodText()
                applyFilters()
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.primary, null))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.primary, null))
        }
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.light_green_dialog_background)
        dialog.show()
    }

    private fun showYearPicker() {
        val years = (2020..2030).map { it.toString() }.toTypedArray()

        val dialogView = layoutInflater.inflate(R.layout.dialog_year_picker, null)
        val yearPicker = dialogView.findViewById<android.widget.NumberPicker>(R.id.yearPicker)

        yearPicker.minValue = 0
        yearPicker.maxValue = years.size - 1
        yearPicker.displayedValues = years
        yearPicker.value = years.indexOf(selectedYear.toString())
        setNumberPickerTextColor(yearPicker, resources.getColor(R.color.primary, null))

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.LightGreenAlertDialogStyle)
            .setTitle("Select Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                selectedYear = years[yearPicker.value].toInt()
                updateCurrentPeriodText()
                applyFilters()
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.primary, null))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.primary, null))
        }
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.light_green_dialog_background)
        dialog.show()
    }
    
    // Helper function to set NumberPicker text color
    private fun setNumberPickerTextColor(numberPicker: android.widget.NumberPicker, color: Int) {
        try {
            val selectorWheelPaintField = android.widget.NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            selectorWheelPaintField.isAccessible = true
            (selectorWheelPaintField.get(numberPicker) as android.graphics.Paint).color = color
            
            val inputTextField = android.widget.NumberPicker::class.java.getDeclaredField("mInputText")
            inputTextField.isAccessible = true
            (inputTextField.get(numberPicker) as android.widget.EditText).setTextColor(color)
        } catch (e: Exception) {
            // Ignore if styling fails
        }
    }

    // Show transaction details dialog
    private fun showTransactionDetails(transaction: Transaction) {
        val dialog = TransactionDetailsDialogFragment.newInstance(transaction)
        dialog.show(childFragmentManager, "TransactionDetailsDialog")
    }
    
    // Show delete confirmation dialog
    private fun showDeleteConfirmationDialog(transactionId: String) {
        val dialog = DeleteConfirmationDialogFragment.newInstance(transactionId)
        dialog.show(childFragmentManager, "DeleteConfirmationDialog")
    }
    
    // TransactionDetailsDialogFragment.TransactionDialogListener implementation
    override fun onEditTransaction(transaction: Transaction) {
        // Navigate to AddTransactionFragment with the transaction data for editing
        val bundle = Bundle().apply {
            putString("transactionId", transaction.id)
            putBoolean("isEditing", true)
        }
        findNavController().navigate(
            R.id.action_transactionsFragment_to_addTransactionFragment,
            bundle
        )
    }
    
    // DeleteConfirmationDialogFragment.DeleteConfirmationListener implementation
    override fun onConfirmDelete(transactionId: String) {
        // Delete the transaction
        viewModel.deleteTransaction(transactionId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
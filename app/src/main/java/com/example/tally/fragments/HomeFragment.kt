package com.example.tally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.models.Transaction
import com.example.tally.models.MonthlyBudget
import com.example.tally.repository.BudgetRepository
import com.example.tally.viewmodels.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var budgetRepository: BudgetRepository
    
    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvCurrentBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvTotalBudget: TextView
    private lateinit var tvBudgetExpenses: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var budgetProgressText: TextView
    private lateinit var budgetStatusIcon: ImageView
    private lateinit var budgetStatusText: TextView
    private lateinit var recentTransactionsContainer: LinearLayout
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnViewAllTransactions: Button
    
    // Formatting
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize repository
        budgetRepository = BudgetRepository(requireContext())
        
        // Initialize views
        initViews(view)
        
        // Set up greeting based on time of day
        updateGreeting()
        
        // Set up click listeners
        setupClickListeners()
        
        // Observe data changes
        observeData()
    }
    
    private fun initViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance)
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget)
        tvBudgetExpenses = view.findViewById(R.id.tvBudgetExpenses)
        budgetProgressBar = view.findViewById(R.id.budgetProgressBar)
        budgetProgressText = view.findViewById(R.id.budgetProgressText)
        budgetStatusIcon = view.findViewById(R.id.budgetStatusIcon)
        budgetStatusText = view.findViewById(R.id.budgetStatusText)
        recentTransactionsContainer = view.findViewById(R.id.recentTransactionsContainer)
        tvNoTransactions = view.findViewById(R.id.tvNoTransactions)
        btnViewAllTransactions = view.findViewById(R.id.btnViewAllTransactions)
    }
    
    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 18 -> "Good Afternoon"
            else -> "Good Evening"
        }
        
        tvGreeting.text = greeting
    }
    
    private fun setupClickListeners() {
        // Notification bell click
        view?.findViewById<ImageView>(R.id.ivNotification)?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationsFragment)
        }
        
        // View All Transactions button click
        btnViewAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_transactionsFragment)
        }
    }
    
    private fun observeData() {
        // Observe transactions for summary and recent transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateFinancialSummary(transactions)
            updateRecentTransactions(transactions)
        }
        
        // Observe current month's budget
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // +1 because Calendar months are 0-based
        val currentYear = calendar.get(Calendar.YEAR)
        
        budgetRepository.getBudgetForMonth(currentMonth, currentYear).observe(viewLifecycleOwner) { budget ->
            updateBudgetSection(budget)
        }
        
        // Observe currency changes
        viewModel.currentCurrency.observe(viewLifecycleOwner) { _ ->
            // When currency changes, update all displayed amounts
            viewModel.transactions.value?.let { transactions ->
                updateFinancialSummary(transactions)
                updateRecentTransactions(transactions)
            }
            
            // Update budget section when currency changes
            budgetRepository.getBudgetForMonth(currentMonth, currentYear).value?.let { budget ->
                updateBudgetSection(budget)
            }
        }
    }
    
    private fun updateFinancialSummary(transactions: List<Transaction>) {
        // Calculate total income and expenses for the current month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis
        
        val monthlyTransactions = transactions.filter { transaction ->
            transaction.date != null && transaction.date in startOfMonth..endOfMonth
        }
        
        val totalIncome = monthlyTransactions
            .filter { it.type == "Income" }
            .sumOf { it.amount }
            
        val totalExpenses = monthlyTransactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }
            
        val currentBalance = totalIncome - totalExpenses
        
        // Update UI using the viewModel's formatAmount method
        tvCurrentBalance.text = viewModel.formatAmount(currentBalance)
        tvIncome.text = "+${viewModel.formatAmount(totalIncome)}"
        tvExpense.text = "-${viewModel.formatAmount(totalExpenses)}"
    }
    
    private fun updateBudgetSection(budget: MonthlyBudget?) {
        if (budget == null || budget.budgetItems.isEmpty()) {
            // No budget set up yet
            tvTotalBudget.text = viewModel.formatAmount(0.0)
            tvBudgetExpenses.text = viewModel.formatAmount(0.0)
            budgetProgressBar.progress = 0
            budgetProgressText.text = "0%"
            budgetStatusText.text = "No budget set up. Create a budget to track expenses."
            budgetStatusIcon.setImageResource(R.drawable.ic_warning)
            return
        }
        
        // Get budget data
        val totalIncome = budget.getTotalIncome()
        val totalExpenses = budget.getTotalExpenses()
        val percentage = budget.getPercentageSpent()
        
        // Update UI with the viewModel's formatAmount method
        tvTotalBudget.text = viewModel.formatAmount(totalIncome)
        tvBudgetExpenses.text = "-${viewModel.formatAmount(totalExpenses)}"
        budgetProgressBar.progress = percentage
        budgetProgressText.text = "$percentage%"
        
        // Update progress bar and status based on percentage
        if (percentage > 100) {
            // Over budget
            budgetStatusText.text = "Over budget! Consider reducing expenses."
            budgetStatusIcon.setImageResource(R.drawable.ic_warning)
            budgetStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.expense_red))
        } else if (percentage >= 80) {
            // Approaching budget limit
            budgetStatusText.text = "Approaching budget limit. Be cautious!"
            budgetStatusIcon.setImageResource(R.drawable.ic_warning)
            budgetStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning_yellow))
        } else {
            // Within budget
            budgetStatusText.text = "Within budget. Good job!"
            budgetStatusIcon.setImageResource(R.drawable.check)
            budgetStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.income_green))
        }
    }
    
    private fun updateRecentTransactions(transactions: List<Transaction>) {
        // Clear container
        recentTransactionsContainer.removeAllViews()
        
        if (transactions.isEmpty()) {
            // Show empty state
            tvNoTransactions.visibility = View.VISIBLE
            return
        }
        
        // Hide empty state
        tvNoTransactions.visibility = View.GONE
        
        // Show 5 most recent transactions
        val recentTransactions = transactions
            .sortedByDescending { it.date }
            .take(5)
            
        for (transaction in recentTransactions) {
            val transactionView = createTransactionView(transaction)
            recentTransactionsContainer.addView(transactionView)
        }
    }
    
    private fun createTransactionView(transaction: Transaction): View {
        val inflater = LayoutInflater.from(requireContext())
        val transactionView = inflater.inflate(R.layout.item_recent_transaction, recentTransactionsContainer, false)
        
        // Set transaction data
        val ivCategoryIcon = transactionView.findViewById<ImageView>(R.id.ivCategoryIcon)
        val tvTransactionTitle = transactionView.findViewById<TextView>(R.id.tvTransactionTitle)
        val tvTransactionDate = transactionView.findViewById<TextView>(R.id.tvTransactionDate)
        val tvTransactionAmount = transactionView.findViewById<TextView>(R.id.tvTransactionAmount)
        
        // Set title
        tvTransactionTitle.text = transaction.title
        
        // Set date
        if (transaction.date != null) {
            val formattedDate = formatTransactionDate(transaction.date)
            tvTransactionDate.text = formattedDate
        } else {
            tvTransactionDate.text = "No date"
        }
        
        // Set amount and color based on transaction type
        if (transaction.type == "Income") {
            tvTransactionAmount.text = "+${viewModel.formatAmount(transaction.amount)}"
            tvTransactionAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.link))
        } else {
            tvTransactionAmount.text = "-${viewModel.formatAmount(transaction.amount)}"
            tvTransactionAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
        }
        
        // Set icon based on category
        // TODO: Implement proper category icons
        // For now, use default icon based on transaction type
        if (transaction.type == "Income") {
            ivCategoryIcon.setImageResource(R.drawable.up)
            ivCategoryIcon.setBackgroundResource(R.drawable.circle_green_background)
        } else {
            // Try to set icon based on category name
            setIconByCategory(ivCategoryIcon, transaction.categoryId)
        }
        
        return transactionView
    }
    
    private fun setIconByCategory(imageView: ImageView, categoryId: String) {
        // Default to expense icon
        var iconRes = R.drawable.down
        var backgroundRes = R.drawable.circle_red_background
        
        // Try to map category ID to icon
        when (categoryId.toLowerCase(Locale.getDefault())) {
            "food", "groceries", "restaurant" -> {
                iconRes = R.drawable.food
                backgroundRes = R.drawable.circle_green_background
            }
            "transport", "transportation", "travel" -> {
                iconRes = R.drawable.transport
                backgroundRes = R.drawable.circle_orange_background
            }
            "rent", "housing", "mortgage" -> {
                iconRes = R.drawable.house
                backgroundRes = R.drawable.circle_blue_background
            }
            "entertainment", "movie", "games" -> {
                iconRes = R.drawable.entertainment
                backgroundRes = R.drawable.circle_purple_background
            }
        }
        
        imageView.setImageResource(iconRes)
        imageView.setBackgroundResource(backgroundRes)
    }
    
    private fun formatTransactionDate(timestamp: Long): String {
        val date = Date(timestamp)
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance()
        transactionDate.time = date
        
        return when {
            // Today
            isSameDay(today, transactionDate) -> {
                "Today, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
            }
            // Yesterday
            isYesterday(today, transactionDate) -> {
                "Yesterday, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
            }
            // Within the last week
            isWithinLastWeek(today, transactionDate) -> {
                SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(date)
            }
            // Older
            else -> {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
            }
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun isYesterday(today: Calendar, other: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, other)
    }
    
    private fun isWithinLastWeek(today: Calendar, other: Calendar): Boolean {
        val lastWeek = Calendar.getInstance()
        lastWeek.add(Calendar.DAY_OF_YEAR, -7)
        return other.after(lastWeek) && other.before(today)
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
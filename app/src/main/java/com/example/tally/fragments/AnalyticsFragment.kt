package com.example.tally.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.tally.R
import com.example.tally.databinding.FragmentAnalyticsBinding
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import android.widget.ImageView
import android.graphics.drawable.LayerDrawable
import android.graphics.Typeface
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.LegendEntry
import android.app.DatePickerDialog
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var barChart: BarChart
    private lateinit var expensePieChart: PieChart
    private lateinit var incomePieChart: PieChart
    
    // Format currency
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    
    // Date formatters
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val dateDisplayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Current filter settings
    private var currentPeriod = "Daily"
    private var startDate: Long = 0
    private var endDate: Long = 0
    
    // Current budget
    private var budget: Double = 20000.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        barChart = binding.barChart
        expensePieChart = binding.expensePieChart
        incomePieChart = binding.incomePieChart
        
        // Set default date range to current month
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        startDate = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        endDate = cal.timeInMillis
        
        // Set up tab change listener for hidden tabs
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        currentPeriod = "Daily"
                        setupDateRangeForWeek()
                    }
                    1 -> {
                        currentPeriod = "Weekly"
                        setupDateRangeForMonth()
                    }
                    2 -> {
                        currentPeriod = "Monthly"
                        setupDateRangeForYear()
                    }
                    3 -> {
                        currentPeriod = "Year"
                        setupDateRangeForAllTime()
                    }
                }
                // Update filter text
                view.findViewById<TextView>(R.id.periodFilterText)?.text = currentPeriod
                updateDateDisplay()
                updateAnalytics()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Filter dropdown click
        view.findViewById<LinearLayout>(R.id.periodFilterDropdown)?.setOnClickListener {
            showPeriodFilterPopup(it)
        }
        
        // Filter calendar button click
        view.findViewById<LinearLayout>(R.id.filterCalendarButton)?.setOnClickListener {
            showDateRangePicker()
        }
        
        // Observe transactions for changes
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateAnalytics()
        }
        
        // Observe budget for changes
        viewModel.budget.observe(viewLifecycleOwner) { budgetValue ->
            budget = budgetValue
            updateAnalytics()
        }
        
        // Setup charts
        setupBarChart()
        setupPieCharts()
        
        // Update date display
        updateDateDisplay()
        
        // Initial analytics update
        updateAnalytics()
    }
    
    private fun setupDateRangeForWeek() {
        val cal = Calendar.getInstance()
        // Start from the beginning of current week (Sunday)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        startDate = cal.timeInMillis
        
        // End at the end of the week (Saturday)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        endDate = cal.timeInMillis
    }
    
    private fun setupDateRangeForMonth() {
        val cal = Calendar.getInstance()
        // Start from the first day of the month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        startDate = cal.timeInMillis
        
        // End at the last day of the month
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        endDate = cal.timeInMillis
    }
    
    private fun setupDateRangeForYear() {
        val cal = Calendar.getInstance()
        // Start from January 1st of the current year
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        startDate = cal.timeInMillis
        
        // End at December 31st of the current year
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        endDate = cal.timeInMillis
    }
    
    private fun setupDateRangeForAllTime() {
        // Start from 5 years ago
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -5)
        startDate = cal.timeInMillis
        
        // End at today
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        endDate = cal.timeInMillis
    }
    
    private fun showDateRangePicker() {
        when (currentPeriod) {
            "Daily" -> showDayPicker()
            "Weekly" -> showWeekPicker()
            "Monthly" -> showMonthYearPicker()
            "Year" -> showYearPicker()
        }
    }
    
    private fun showDayPicker() {
        // Create a simple date picker dialog
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startDate }
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.LightGreenDatePickerStyle,
            { _, year, month, dayOfMonth ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                
                // Set end of the same day
                val endCalendar = Calendar.getInstance()
                endCalendar.timeInMillis = startCalendar.timeInMillis
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                startDate = startCalendar.timeInMillis
                endDate = endCalendar.timeInMillis
                updateDateDisplay()
                updateAnalytics()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun showWeekPicker() {
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startDate }
        
        // Use DatePickerDialog as a starting point to select a day in the desired week
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.LightGreenDatePickerStyle,
            { _, year, month, dayOfMonth ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // Get the start of the week containing the selected day
                startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.firstDayOfWeek)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                
                // Set end to the end of the week
                val endCalendar = Calendar.getInstance()
                endCalendar.timeInMillis = startCalendar.timeInMillis
                endCalendar.add(Calendar.DAY_OF_WEEK, 6)
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                startDate = startCalendar.timeInMillis
                endDate = endCalendar.timeInMillis
                updateDateDisplay()
                updateAnalytics()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.setTitle("Select a day in the desired week")
        datePickerDialog.show()
    }
    
    private fun showMonthYearPicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
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
        monthPicker.value = cal.get(Calendar.MONTH)
        setNumberPickerTextColor(monthPicker, resources.getColor(R.color.primary, null))

        yearPicker.minValue = 0
        yearPicker.maxValue = years.size - 1
        yearPicker.displayedValues = years
        yearPicker.value = years.indexOf(cal.get(Calendar.YEAR).toString())
        setNumberPickerTextColor(yearPicker, resources.getColor(R.color.primary, null))

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.LightGreenAlertDialogStyle)
            .setTitle("Select Month and Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedMonth = monthPicker.value
                val selectedYear = years[yearPicker.value].toInt()
                
                // Set start date to beginning of selected month
                val startCalendar = Calendar.getInstance()
                startCalendar.set(Calendar.YEAR, selectedYear)
                startCalendar.set(Calendar.MONTH, selectedMonth)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                
                // Set end date to end of selected month
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.YEAR, selectedYear)
                endCalendar.set(Calendar.MONTH, selectedMonth)
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                startDate = startCalendar.timeInMillis
                endDate = endCalendar.timeInMillis
                updateDateDisplay()
                updateAnalytics()
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
        val cal = Calendar.getInstance().apply { timeInMillis = startDate }
        val years = (2020..2030).map { it.toString() }.toTypedArray()

        val dialogView = layoutInflater.inflate(R.layout.dialog_year_picker, null)
        val yearPicker = dialogView.findViewById<android.widget.NumberPicker>(R.id.yearPicker)

        yearPicker.minValue = 0
        yearPicker.maxValue = years.size - 1
        yearPicker.displayedValues = years
        yearPicker.value = years.indexOf(cal.get(Calendar.YEAR).toString())
        setNumberPickerTextColor(yearPicker, resources.getColor(R.color.primary, null))

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.LightGreenAlertDialogStyle)
            .setTitle("Select Year")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedYear = years[yearPicker.value].toInt()
                
                // Set start date to beginning of selected year
                val startCalendar = Calendar.getInstance()
                startCalendar.set(Calendar.YEAR, selectedYear)
                startCalendar.set(Calendar.MONTH, Calendar.JANUARY)
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                
                // Set end date to end of selected year
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.YEAR, selectedYear)
                endCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
                endCalendar.set(Calendar.DAY_OF_MONTH, 31)
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                startDate = startCalendar.timeInMillis
                endDate = endCalendar.timeInMillis
                updateDateDisplay()
                updateAnalytics()
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
            val paint = selectorWheelPaintField.get(numberPicker) as android.graphics.Paint
            paint.color = color
            numberPicker.invalidate()
        } catch (e: Exception) {
            // Handle exception if needed
        }
    }
    
    private fun updateDateDisplay() {
        val currentDateTextView = view?.findViewById<TextView>(R.id.currentDateText)
        val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
        
        val displayText = when (currentPeriod) {
            "Daily" -> {
                // Format as "Apr 24, 2025"
                dateDisplayFormat.format(calendar.time)
            }
            "Weekly" -> {
                // Format as "Apr 24 - Apr 30, 2025"
                val endCal = Calendar.getInstance().apply { timeInMillis = endDate }
                val startText = dateDisplayFormat.format(calendar.time)
                val endText = dateDisplayFormat.format(endCal.time)
                "$startText - $endText"
            }
            "Monthly" -> {
                // Format as "April 2025"
                monthYearFormat.format(calendar.time)
            }
            "Year" -> {
                // Format as "2025"
                calendar.get(Calendar.YEAR).toString()
            }
            else -> dateDisplayFormat.format(calendar.time)
        }
        
        currentDateTextView?.text = displayText
    }
    
    private fun updateAnalytics() {
        val transactions = viewModel.transactions.value ?: emptyList()
        
        // Filter transactions by date range
        val filteredTransactions = transactions.filter { transaction ->
            val transactionDate = transaction.date ?: 0
            transactionDate in startDate..endDate
        }
        
        val incomeTransactions = filteredTransactions.filter { it.type == "Income" }
        val expenseTransactions = filteredTransactions.filter { it.type == "Expense" }
        
        // Calculate totals
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        // Calculate balance only from filtered transactions
        val totalBalance = totalIncome - totalExpenses
        
        // Calculate expense percentage relative to budget
        val expensePercentage = if (totalIncome > 0) (totalExpenses / totalIncome) * 100 else 0.0
        
        // Update UI with values
        updateBalanceAndExpenseView(totalBalance, totalExpenses)
        updateProgressBar(expensePercentage.toInt(), totalExpenses, totalIncome)
        updateChartForPeriod(currentPeriod, filteredTransactions)
        updateIncomeExpenseSummary(totalIncome, totalExpenses)
        
        // Update pie charts
        updateCategoryPieCharts(incomeTransactions, expenseTransactions)
    }
    
    private fun updateBalanceAndExpenseView(balance: Double, expenses: Double) {
        binding.totalBalance.text = currencyFormatter.format(balance)
        binding.totalExpenses.text = "-" + currencyFormatter.format(expenses)
    }
    
    private fun updateProgressBar(percentage: Int, expenses: Double, income: Double) {
        val progressBar = binding.expenseProgressBar
        val progressText = view?.findViewById<TextView>(R.id.progressPercentText)
        val warningIcon = view?.findViewById<ImageView>(R.id.warningIcon)
        
        // Set progress value (this represents percentage of income spent as expenses)
        val cappedPercentage = if (percentage > 100) 100 else percentage
        progressBar.progress = cappedPercentage
        
        // Update progress percentage text view
        progressText?.text = "$percentage%"
        progressText?.setTextColor(Color.BLACK)
        progressText?.setTypeface(progressText.typeface, Typeface.BOLD)
        
        // Update progress bar appearance and message based on expense percentage
        when {
            // Normal state: expenses less than 50% of income - showing positive message with lime_green
            percentage < 50 -> {
                // Create a lime green progress bar drawable programmatically
                val layerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.progress_bar_normal, null) as LayerDrawable
                val progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress)
                DrawableCompat.setTint(progressLayer, ContextCompat.getColor(requireContext(), R.color.lime_green))
                progressBar.progressDrawable = layerDrawable
                
                binding.progressMessage.text = "$percentage% Of Your Expenses, Looks Good."
                binding.progressMessage.setTextColor(Color.parseColor("#000000"))
                warningIcon?.setImageResource(R.drawable.check)
                warningIcon?.setColorFilter(Color.parseColor("#4CAF50"))
                binding.budgetAmount.text = currencyFormatter.format(income)
            }
            
            // Medium state: expenses between 50-75% of income - shown in yellow
            percentage in 50..74 -> {
                // Create a yellow progress bar drawable programmatically
                val layerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.progress_bar_normal, null) as LayerDrawable
                val progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress)
                DrawableCompat.setTint(progressLayer, Color.parseColor("#FFC107")) // Yellow color
                progressBar.progressDrawable = layerDrawable
                
                binding.progressMessage.text = "$percentage% Of Your Expenses. Be Careful."
                binding.progressMessage.setTextColor(Color.parseColor("#FFC107"))
                warningIcon?.setImageResource(R.drawable.warning_icon)
                warningIcon?.setColorFilter(Color.parseColor("#FFC107"))
                binding.budgetAmount.text = currencyFormatter.format(income)
            }
            
            // Warning state: expenses between 75-90% of income - shown in orange
            percentage in 75..89 -> {
                // Create an orange progress bar drawable programmatically
                val layerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.progress_bar_normal, null) as LayerDrawable
                val progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress)
                DrawableCompat.setTint(progressLayer, Color.parseColor("#FF9800")) // Orange color
                progressBar.progressDrawable = layerDrawable
                
                binding.progressMessage.text = "Warning: $percentage% Of Your Budget Used!"
                binding.progressMessage.setTextColor(Color.parseColor("#FF9800"))
                warningIcon?.setImageResource(R.drawable.warning_icon)
                warningIcon?.setColorFilter(Color.parseColor("#FF9800"))
                binding.budgetAmount.text = currencyFormatter.format(income)
            }
            
            // Critical state: expenses between 90-100% or exceeded - shown in red
            percentage >= 90 -> {
                // Create a red progress bar drawable programmatically
                val layerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.progress_bar_normal, null) as LayerDrawable
                val progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress)
                DrawableCompat.setTint(progressLayer, Color.parseColor("#F44336")) // Red color
                progressBar.progressDrawable = layerDrawable
                
                if (percentage > 100) {
                    val exceededAmount = expenses - income
                    binding.progressMessage.text = "Budget Exceeded by ${currencyFormatter.format(exceededAmount)}!"
                    binding.progressMessage.setTextColor(Color.parseColor("#F44336"))
                    warningIcon?.setImageResource(R.drawable.warning_icon)
                    warningIcon?.setColorFilter(Color.parseColor("#F44336"))
                } else {
                    binding.progressMessage.text = "Critical: $percentage% Of Your Budget Used!"
                    binding.progressMessage.setTextColor(Color.parseColor("#F44336"))
                    warningIcon?.setImageResource(R.drawable.warning_icon)
                    warningIcon?.setColorFilter(Color.parseColor("#F44336"))
                }
                binding.budgetAmount.text = currencyFormatter.format(income)
            }
        }
    }
    
    private fun updateIncomeExpenseSummary(income: Double, expenses: Double) {
        binding.incomeAmount.text = currencyFormatter.format(income)
        binding.expenseAmount.text = currencyFormatter.format(expenses)
    }
    
    private fun setupBarChart() {
        // Configure the bar chart appearance
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setScaleEnabled(false)
        
        // Configure X axis
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.primary)
        
        // Configure left Y axis
        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.primary)
        
        // Configure right Y axis
        val rightAxis = barChart.axisRight
        rightAxis.isEnabled = false
    }
    
    private fun updateChartForPeriod(period: String, transactions: List<Transaction>) {
        val entriesIncome = ArrayList<BarEntry>()
        val entriesExpense = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()
        
        when (period) {
            "Daily" -> {
                // Organize transactions by day within the selected range
                val calendar = Calendar.getInstance()
                val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
                val endCal = Calendar.getInstance().apply { timeInMillis = endDate }
                
                // Create a map of day -> (income, expense)
                val dailyData = mutableMapOf<Int, Pair<Double, Double>>()
                
                // Initialize all days in the range
                calendar.timeInMillis = startDate
                while (calendar.timeInMillis <= endDate) {
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    dailyData[dayOfWeek] = Pair(0.0, 0.0)
                    xLabels.add(dayFormat.format(calendar.time))
                    
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    
                    // Break after 7 days to avoid too many bars
                    if (xLabels.size >= 7) break
                }
                
                // Fill in the actual data
                for (transaction in transactions) {
                    val date = transaction.date ?: continue
                    calendar.timeInMillis = date
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    
                    // Only include transactions within the first week
                    if (calendar.timeInMillis >= startDate && calendar.timeInMillis <= endDate && dayOfWeek in dailyData) {
                        val (income, expense) = dailyData[dayOfWeek] ?: Pair(0.0, 0.0)
                        if (transaction.type == "Income") {
                            dailyData[dayOfWeek] = Pair(income + transaction.amount, expense)
                        } else {
                            dailyData[dayOfWeek] = Pair(income, expense + transaction.amount)
                        }
                    }
                }
                
                // Convert to bar entries
                var index = 0
                for (day in arrayOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)) {
                    val (income, expense) = dailyData[day] ?: Pair(0.0, 0.0)
                    entriesIncome.add(BarEntry(index.toFloat(), income.toFloat()))
                    entriesExpense.add(BarEntry(index.toFloat(), expense.toFloat()))
                    index++
                    
                    // Only add the first 7 days
                    if (index >= 7) break
                }
            }
            "Weekly" -> {
                // Organize transactions by week
                val calendar = Calendar.getInstance()
                
                // Create a map of week -> (income, expense)
                val weeklyData = mutableMapOf<Int, Pair<Double, Double>>()
                
                // Initialize all weeks in the range
                calendar.timeInMillis = startDate
                var weekNumber = 1
                
                while (calendar.timeInMillis <= endDate && weekNumber <= 4) {
                    weeklyData[weekNumber] = Pair(0.0, 0.0)
                    xLabels.add("Week $weekNumber")
                    
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    weekNumber++
                }
                
                // Fill in the actual data
                for (transaction in transactions) {
                    val date = transaction.date ?: continue
                    calendar.timeInMillis = date
                    
                    // Calculate which week this transaction belongs to
                    val transactionWeek = ((date - startDate) / (7 * 24 * 60 * 60 * 1000)).toInt() + 1
                    
                    if (transactionWeek in 1..4) {
                        val (income, expense) = weeklyData[transactionWeek] ?: Pair(0.0, 0.0)
                        if (transaction.type == "Income") {
                            weeklyData[transactionWeek] = Pair(income + transaction.amount, expense)
                        } else {
                            weeklyData[transactionWeek] = Pair(income, expense + transaction.amount)
                        }
                    }
                }
                
                // Convert to bar entries
                for (week in 1..4) {
                    val (income, expense) = weeklyData[week] ?: Pair(0.0, 0.0)
                    entriesIncome.add(BarEntry((week - 1).toFloat(), income.toFloat()))
                    entriesExpense.add(BarEntry((week - 1).toFloat(), expense.toFloat()))
                }
            }
            "Monthly" -> {
                // Organize transactions by month
                val calendar = Calendar.getInstance()
                
                // Create a map of month -> (income, expense)
                val monthlyData = mutableMapOf<Int, Pair<Double, Double>>()
                
                // Initialize all months
                for (month in 0..11) {
                    monthlyData[month] = Pair(0.0, 0.0)
                    calendar.set(Calendar.MONTH, month)
                    xLabels.add(monthFormat.format(calendar.time))
                }
                
                // Fill in the actual data
                for (transaction in transactions) {
                    val date = transaction.date ?: continue
                    calendar.timeInMillis = date
                    val month = calendar.get(Calendar.MONTH)
                    
                    val (income, expense) = monthlyData[month] ?: Pair(0.0, 0.0)
                    if (transaction.type == "Income") {
                        monthlyData[month] = Pair(income + transaction.amount, expense)
                    } else {
                        monthlyData[month] = Pair(income, expense + transaction.amount)
                    }
                }
                
                // Convert to bar entries
                for (month in 0..11) {
                    val (income, expense) = monthlyData[month] ?: Pair(0.0, 0.0)
                    entriesIncome.add(BarEntry(month.toFloat(), income.toFloat()))
                    entriesExpense.add(BarEntry(month.toFloat(), expense.toFloat()))
                }
            }
            "Year" -> {
                // Organize transactions by year
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                
                // Create a map of year -> (income, expense)
                val yearlyData = mutableMapOf<Int, Pair<Double, Double>>()
                
                // Initialize recent years
                for (offset in -2..2) {
                    val year = currentYear + offset
                    yearlyData[year] = Pair(0.0, 0.0)
                    xLabels.add(year.toString())
                }
                
                // Fill in the actual data
                for (transaction in transactions) {
                    val date = transaction.date ?: continue
                    calendar.timeInMillis = date
                    val year = calendar.get(Calendar.YEAR)
                    
                    if (year in currentYear - 2..currentYear + 2) {
                        val (income, expense) = yearlyData[year] ?: Pair(0.0, 0.0)
                        if (transaction.type == "Income") {
                            yearlyData[year] = Pair(income + transaction.amount, expense)
                        } else {
                            yearlyData[year] = Pair(income, expense + transaction.amount)
                        }
                    }
                }
                
                // Convert to bar entries
                var index = 0
                for (year in currentYear - 2..currentYear + 2) {
                    val (income, expense) = yearlyData[year] ?: Pair(0.0, 0.0)
                    entriesIncome.add(BarEntry(index.toFloat(), income.toFloat()))
                    entriesExpense.add(BarEntry(index.toFloat(), expense.toFloat()))
                    index++
                }
            }
        }
        
        // Create dataset for income
        val dataSetIncome = BarDataSet(entriesIncome, "Income")
        dataSetIncome.color = Color.parseColor("#4287f5")
        dataSetIncome.setDrawValues(false)
        
        // Create dataset for expenses
        val dataSetExpense = BarDataSet(entriesExpense, "Expenses")
        dataSetExpense.color = Color.parseColor("#f54242")
        dataSetExpense.setDrawValues(false)
        
        // Create grouped bar chart data
        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.3f
        
        val barData = BarData(dataSetIncome, dataSetExpense)
        barData.barWidth = barWidth
        
        // Set X axis labels
        if (xLabels.isNotEmpty()) {
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        }
        
        // Update chart with new data
        barChart.data = barData
        if (entriesIncome.size > 1) {
            barChart.groupBars(0f, groupSpace, barSpace)
        }
        barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPieCharts() {
        // Configure Expense Pie Chart
        expensePieChart.description.isEnabled = false
        expensePieChart.setUsePercentValues(true)
        expensePieChart.setExtraOffsets(5f, 5f, 5f, 25f) // Extra bottom offset for legend
        expensePieChart.dragDecelerationFrictionCoef = 0.95f
        
        // Configure hole in center - SMALLER hole for BIGGER chart
        expensePieChart.isDrawHoleEnabled = true
        expensePieChart.setHoleColor(Color.WHITE)
        expensePieChart.setTransparentCircleColor(Color.WHITE)
        expensePieChart.setTransparentCircleAlpha(110)
        expensePieChart.holeRadius = 30f // Much smaller hole = bigger pie
        expensePieChart.transparentCircleRadius = 35f
        expensePieChart.setDrawCenterText(true)
        expensePieChart.setCenterTextSize(16f)
        expensePieChart.setCenterTextColor(Color.BLACK)
        
        // Enable rotation
        expensePieChart.rotationAngle = 0f
        expensePieChart.isRotationEnabled = true
        
        // Configure legend to be below the chart in a grid
        val expenseLegend = expensePieChart.legend
        expenseLegend.isEnabled = true
        expenseLegend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        expenseLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        expenseLegend.orientation = Legend.LegendOrientation.HORIZONTAL
        expenseLegend.setDrawInside(false)
        expenseLegend.xEntrySpace = 16f
        expenseLegend.yEntrySpace = 8f
        expenseLegend.textSize = 16f
        expenseLegend.formSize = 14f
        expenseLegend.formToTextSpace = 8f
        // Try setting wrapped enabled if available
        try {
            expenseLegend.isWordWrapEnabled = true
        } catch (e: Exception) {
            // Property not available in this version
        }
        expenseLegend.maxSizePercent = 0.5f
        expenseLegend.form = Legend.LegendForm.CIRCLE
        
        // Configure Income Pie Chart with the same settings
        incomePieChart.description.isEnabled = false
        incomePieChart.setUsePercentValues(true)
        incomePieChart.setExtraOffsets(5f, 5f, 5f, 25f) // Extra bottom offset for legend
        incomePieChart.dragDecelerationFrictionCoef = 0.95f
        
        // Configure hole in center - SMALLER hole for BIGGER chart
        incomePieChart.isDrawHoleEnabled = true
        incomePieChart.setHoleColor(Color.WHITE)
        incomePieChart.setTransparentCircleColor(Color.WHITE)
        incomePieChart.setTransparentCircleAlpha(110)
        incomePieChart.holeRadius = 30f // Much smaller hole = bigger pie
        incomePieChart.transparentCircleRadius = 35f
        incomePieChart.setDrawCenterText(true)
        incomePieChart.setCenterTextSize(16f)
        incomePieChart.setCenterTextColor(Color.BLACK)
        
        // Enable rotation
        incomePieChart.rotationAngle = 0f
        incomePieChart.isRotationEnabled = true
        
        // Configure legend to be below the chart in a grid
        val incomeLegend = incomePieChart.legend
        incomeLegend.isEnabled = true
        incomeLegend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        incomeLegend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        incomeLegend.orientation = Legend.LegendOrientation.HORIZONTAL
        incomeLegend.setDrawInside(false)
        incomeLegend.xEntrySpace = 16f
        incomeLegend.yEntrySpace = 8f
        incomeLegend.textSize = 16f
        incomeLegend.formSize = 14f
        incomeLegend.formToTextSpace = 8f
        // Try setting wrapped enabled if available
        try {
            incomeLegend.isWordWrapEnabled = true
        } catch (e: Exception) {
            // Property not available in this version
        }
        incomeLegend.maxSizePercent = 0.5f
        incomeLegend.form = Legend.LegendForm.CIRCLE
    }
    
    private fun updateCategoryPieCharts(incomeTransactions: List<Transaction>, expenseTransactions: List<Transaction>) {
        // Update Expense Category Pie Chart
        val expenseByCategory = mutableMapOf<String, Double>()
        val expenseCategoryNames = mutableMapOf<String, String>()
        val expenseCategoryEmojis = mutableMapOf<String, String>()
        
        // Get all categories from view model
        val categories = viewModel.categories.value ?: emptyList()
        
        // Process expense transactions
        for (transaction in expenseTransactions) {
            val category = categories.find { it.id == transaction.categoryId }
            val categoryId = category?.id ?: "unknown"
            val currentAmount = expenseByCategory[categoryId] ?: 0.0
            expenseByCategory[categoryId] = currentAmount + transaction.amount
            expenseCategoryNames[categoryId] = category?.name ?: "Unknown"
            expenseCategoryEmojis[categoryId] = category?.emoji ?: "❓"
        }
        
        // For expense pie chart
        try {
            if (expenseByCategory.isEmpty()) {
                // No expense data, clear and show empty state
                expensePieChart.clear()
                expensePieChart.setNoDataText("No expense data for this period")
                expensePieChart.legend.isEnabled = false
                expensePieChart.invalidate()
            } else {
                expensePieChart.legend.isEnabled = true
                val expenseEntries = ArrayList<PieEntry>()
                val expenseColors = ArrayList<Int>()
                
                // Warm color palette for expenses (reds, oranges, yellows)
                val expenseColorPalette = arrayOf(
                    Color.parseColor("#FF5252"), // Red
                    Color.parseColor("#FF7043"), // Deep Orange
                    Color.parseColor("#FFAB40"), // Orange
                    Color.parseColor("#FFD740"), // Amber
                    Color.parseColor("#E57373"), // Light Red
                    Color.parseColor("#FF8A65"), // Light Orange
                    Color.parseColor("#FFB74D"), // Light Amber
                    Color.parseColor("#D32F2F"), // Dark Red
                    Color.parseColor("#F4511E"), // Dark Orange
                    Color.parseColor("#FF6F00")  // Dark Amber
                )
                
                // Add entries for expense categories
                var colorIndex = 0
                val legendLabels = ArrayList<String>()
                for ((categoryId, amount) in expenseByCategory) {
                    // We'll keep the label for the legend
                    val label = "${expenseCategoryEmojis[categoryId]} ${expenseCategoryNames[categoryId]}"
                    legendLabels.add(label)
                    // Using empty string as label argument so no text appears on chart
                    expenseEntries.add(PieEntry(amount.toFloat(), ""))
                    expenseColors.add(expenseColorPalette[colorIndex % expenseColorPalette.size])
                    colorIndex++
                }
                
                val expenseDataSet = PieDataSet(expenseEntries, "")
                expenseDataSet.colors = expenseColors
                expenseDataSet.setDrawValues(true)
                expenseDataSet.valueTextSize = 16f
                expenseDataSet.valueTextColor = Color.BLACK
                expenseDataSet.valueFormatter = PercentFormatter()
                expenseDataSet.valueLinePart1Length = 0.2f
                expenseDataSet.valueLinePart2Length = 0.1f
                expenseDataSet.valueLineWidth = 2f
                expenseDataSet.valueLinePart1OffsetPercentage = 75f
                expenseDataSet.valueLineColor = Color.BLACK
                expenseDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                expenseDataSet.sliceSpace = 1f  // Less space between slices for bigger pie
                
                val expenseData = PieData(expenseDataSet)
                expenseData.setValueTextSize(16f)
                expenseData.setValueTextColor(Color.BLACK)
                expenseData.setValueFormatter(object : PercentFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}%"
                    }
                })
                
                // Set custom legend entries to display category names in the legend
                val expenseLegendEntries = ArrayList<LegendEntry>()
                for (i in legendLabels.indices) {
                    val entry = LegendEntry()
                    entry.label = legendLabels[i]
                    entry.formColor = expenseColors[i]
                    entry.form = Legend.LegendForm.CIRCLE
                    expenseLegendEntries.add(entry)
                }
                
                // Make sure we assign the data and legend in the right order
                expensePieChart.legend.setCustom(expenseLegendEntries)
                expensePieChart.data = expenseData
                expensePieChart.highlightValues(null)
                
                // Add a total amount to the chart description
                val totalExpenseAmount = expenseByCategory.values.sum()
                expensePieChart.centerText = "Total\n${currencyFormatter.format(totalExpenseAmount)}"
                expensePieChart.invalidate()
            }
        } catch (e: Exception) {
            // Handle any exceptions to prevent crashes
            expensePieChart.clear()
            expensePieChart.setNoDataText("Error displaying chart")
            expensePieChart.invalidate()
        }
        
        // Update Income Category Pie Chart
        val incomeByCategory = mutableMapOf<String, Double>()
        val incomeCategoryNames = mutableMapOf<String, String>()
        val incomeCategoryEmojis = mutableMapOf<String, String>()
        
        // Process income transactions
        for (transaction in incomeTransactions) {
            val category = categories.find { it.id == transaction.categoryId }
            val categoryId = category?.id ?: "unknown"
            val currentAmount = incomeByCategory[categoryId] ?: 0.0
            incomeByCategory[categoryId] = currentAmount + transaction.amount
            incomeCategoryNames[categoryId] = category?.name ?: "Unknown"
            incomeCategoryEmojis[categoryId] = category?.emoji ?: "❓"
        }
        
        // For income pie chart
        try {
            if (incomeByCategory.isEmpty()) {
                // No income data, clear and show empty state
                incomePieChart.clear()
                incomePieChart.setNoDataText("No income data for this period")
                incomePieChart.legend.isEnabled = false
                incomePieChart.invalidate()
            } else {
                incomePieChart.legend.isEnabled = true
                val incomeEntries = ArrayList<PieEntry>()
                val incomeColors = ArrayList<Int>()
                
                // Cool color palette for incomes (blues, greens)
                val incomeColorPalette = arrayOf(
                    Color.parseColor("#4CAF50"), // Green
                    Color.parseColor("#2196F3"), // Blue
                    Color.parseColor("#00BCD4"), // Cyan
                    Color.parseColor("#009688"), // Teal
                    Color.parseColor("#81C784"), // Light Green
                    Color.parseColor("#64B5F6"), // Light Blue
                    Color.parseColor("#4DB6AC"), // Light Teal
                    Color.parseColor("#388E3C"), // Dark Green
                    Color.parseColor("#1976D2"), // Dark Blue
                    Color.parseColor("#00796B")  // Dark Teal
                )
                
                // Add entries for income categories
                var colorIndex = 0
                val incomeLegendLabels = ArrayList<String>()
                for ((categoryId, amount) in incomeByCategory) {
                    // We'll keep the label for the legend
                    val label = "${incomeCategoryEmojis[categoryId]} ${incomeCategoryNames[categoryId]}"
                    incomeLegendLabels.add(label)
                    // Using empty string as label argument so no text appears on chart
                    incomeEntries.add(PieEntry(amount.toFloat(), ""))
                    incomeColors.add(incomeColorPalette[colorIndex % incomeColorPalette.size])
                    colorIndex++
                }
                
                val incomeDataSet = PieDataSet(incomeEntries, "")
                incomeDataSet.colors = incomeColors
                incomeDataSet.setDrawValues(true)
                incomeDataSet.valueTextSize = 16f
                incomeDataSet.valueTextColor = Color.BLACK
                incomeDataSet.valueFormatter = PercentFormatter()
                incomeDataSet.valueLinePart1Length = 0.2f
                incomeDataSet.valueLinePart2Length = 0.1f
                incomeDataSet.valueLineWidth = 2f
                incomeDataSet.valueLinePart1OffsetPercentage = 75f
                incomeDataSet.valueLineColor = Color.BLACK
                incomeDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                incomeDataSet.sliceSpace = 1f  // Less space between slices for bigger pie
                
                val incomeData = PieData(incomeDataSet)
                incomeData.setValueTextSize(16f)
                incomeData.setValueTextColor(Color.BLACK)
                incomeData.setValueFormatter(object : PercentFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}%"
                    }
                })
                
                // Set custom legend entries to display category names in the legend
                val incomeLegendEntries = ArrayList<LegendEntry>()
                for (i in incomeLegendLabels.indices) {
                    val entry = LegendEntry()
                    entry.label = incomeLegendLabels[i]
                    entry.formColor = incomeColors[i]
                    entry.form = Legend.LegendForm.CIRCLE
                    incomeLegendEntries.add(entry)
                }
                
                // Make sure we assign the data and legend in the right order
                incomePieChart.legend.setCustom(incomeLegendEntries)
                incomePieChart.data = incomeData
                incomePieChart.highlightValues(null)
                
                // Add a total amount to the chart description
                val totalIncomeAmount = incomeByCategory.values.sum()
                incomePieChart.centerText = "Total\n${currencyFormatter.format(totalIncomeAmount)}"
                incomePieChart.invalidate()
            }
        } catch (e: Exception) {
            // Handle any exceptions to prevent crashes
            incomePieChart.clear()
            incomePieChart.setNoDataText("Error displaying chart")
            incomePieChart.invalidate()
        }
    }

    private fun showPeriodFilterPopup(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        val menu = popup.menu
        
        // Add period options
        menu.add(0, 0, 0, "Daily")
        menu.add(0, 1, 1, "Weekly")
        menu.add(0, 2, 2, "Monthly")
        menu.add(0, 3, 3, "Year")
        
        popup.setOnMenuItemClickListener { menuItem ->
            // Programmatically select the corresponding tab
            binding.tabLayout.getTabAt(menuItem.itemId)?.select()
            true
        }
        
        popup.show()
    }
}
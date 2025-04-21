package com.example.tally.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import com.example.tally.R
import com.example.tally.databinding.FragmentAnalyticsBinding
import com.example.tally.viewmodels.FinanceViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class AnalyticsFragment : Fragment() {

    private lateinit var binding: FragmentAnalyticsBinding
    private val viewModel: FinanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Pie Chart
        val pieChart = binding.pieChart
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false

        // Observe transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            val expenses = transactions.filter { it.type == "Expense" }
            val categoryMap = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
            updatePieChart(categoryMap)
        }

        // Time Period Spinner
        binding.spinnerTimePeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Filter transactions by time period (e.g., monthly, weekly)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updatePieChart(categoryMap: Map<String, Float>) {
        val entries = categoryMap.map { PieEntry(it.value, it.key) }
        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }
}
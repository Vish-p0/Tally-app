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

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
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
            val categoryMap = expenses.groupBy { it.categoryId }
                .mapValues { entry ->
                    val categoryId = entry.key
                    val categoryName = if (categoryId != null) {
                        viewModel.getCategoryById(categoryId)?.name ?: "Unknown"
                    } else {
                        "Uncategorized"
                    }
                    entry.value.sumOf { tx -> tx.amount }.toFloat() to categoryName
                }
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

    private fun updatePieChart(categoryMap: Map<String?, Pair<Float, String>>) {
        val entries = categoryMap.map { PieEntry(it.value.first, it.value.second) }
        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
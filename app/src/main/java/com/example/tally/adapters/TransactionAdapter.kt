package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.databinding.ItemTransactionBinding
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val viewModel: FinanceViewModel,
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()), Filterable {

    private var allTransactions = listOf<Transaction>()
    private var filteredTransactions = listOf<Transaction>()

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.toLowerCase(Locale.getDefault()) ?: ""
                val filteredList = if (query.isEmpty()) {
                    allTransactions
                } else {
                    allTransactions.filter { transaction ->
                        (transaction.title ?: "").toLowerCase(Locale.getDefault()).contains(query) ||
                        (transaction.description ?: "").toLowerCase(Locale.getDefault()).contains(query) ||
                        (transaction.categoryId ?: "").toLowerCase(Locale.getDefault()).contains(query) ||
                        (transaction.type ?: "").toLowerCase(Locale.getDefault()).contains(query)
                    }
                }
                return FilterResults().apply {
                    values = filteredList
                    count = filteredList.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredTransactions = results?.values as? List<Transaction> ?: emptyList()
                submitList(filteredTransactions)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<Transaction>?) {
        allTransactions = list ?: emptyList()
        super.submitList(list)
    }

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Format the title with date
                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault())
                    .format(transaction.date)
                
                // Set the title text
                val title = transaction.title ?: ""
                tvTitle.text = if (title.isNotEmpty()) {
                    "$dateStr - $title"
                } else {
                    dateStr
                }
                
                // Set the description if available
                val description = transaction.description ?: ""
                if (description.isNotEmpty()) {
                    tvDescription.text = description
                    tvDescription.visibility = View.VISIBLE
                } else {
                    tvDescription.visibility = View.GONE
                }
                
                tvAmount.text = String.format("$%.2f", transaction.amount)

                // Handle category display
                try {
                    val categoryId = transaction.categoryId ?: ""
                    val category = if (categoryId.isNotEmpty()) viewModel.getCategoryById(categoryId) else null
                    if (category != null) {
                        tvCategory.text = "${category.emoji} ${category.name}"
                        tvCategory.visibility = View.VISIBLE
                    } else {
                        tvCategory.text = "Unknown Category"
                        tvCategory.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvCategory.text = "Unknown Category"
                    tvCategory.visibility = View.VISIBLE
                }

                // Set type-specific styling
                val type = transaction.type ?: "Expense"
                if (type == "Income") {
                    tvAmount.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                } else {
                    tvAmount.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                }

                root.setOnClickListener { onItemClick(transaction) }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
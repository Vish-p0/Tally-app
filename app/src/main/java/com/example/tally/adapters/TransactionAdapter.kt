package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.example.tally.utils.DateFormatter
import com.example.tally.utils.formatCurrency
import com.example.tally.viewmodels.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val viewModel: FinanceViewModel,
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()), Filterable {

    private var originalList: List<Transaction> = emptyList()
    private var filteredList: List<Transaction> = emptyList()
    private var categoryMap: Map<String, Category> = emptyMap()

    init {
        viewModel.categories.observeForever { categories ->
            categoryMap = categories.associateBy { it.id }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    override fun submitList(list: List<Transaction>?) {
        originalList = list ?: emptyList()
        filteredList = originalList
        super.submitList(filteredList)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""
                
                val filtered = if (query.isEmpty()) {
                    originalList
                } else {
                    originalList.filter { transaction ->
                        transaction.title.lowercase().contains(query) ||
                        transaction.description.lowercase().contains(query) ||
                        categoryMap[transaction.categoryId]?.name?.lowercase()?.contains(query) == true
                    }
                }
                
                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as? List<Transaction> ?: emptyList()
                super@TransactionAdapter.submitList(filteredList)
            }
        }
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryEmoji: TextView = itemView.findViewById(R.id.tvCategoryEmoji)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(transaction: Transaction) {
            val category = categoryMap[transaction.categoryId]
            
            // Set category emoji
            tvCategoryEmoji.text = category?.emoji ?: "🔍"
            
            // Set transaction title
            tvTitle.text = transaction.title
            
            // Format and display date
            transaction.date?.let {
                val date = Date(it)
                val formatter = SimpleDateFormat("HH:mm - MMM dd", Locale.getDefault())
                tvTimestamp.text = formatter.format(date)
            } ?: run {
                tvTimestamp.text = "N/A"
            }
            
            // Set category name
            tvCategory.text = category?.name ?: "Unknown"
            
            // Format and set amount (positive for income, negative for expense)
            val amountText = if (transaction.type == "Income") {
                viewModel.formatAmount(transaction.amount)
            } else {
                "-${viewModel.formatAmount(transaction.amount)}"
            }
            tvAmount.text = amountText
            
            // Set amount text color based on transaction type
            tvAmount.setTextColor(
                if (transaction.type == "Income") {
                    ContextCompat.getColor(itemView.context, R.color.link)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.expense_red)
                }
            )
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
    
    // Helper method to get transaction at a specific position
    fun getTransactionAt(position: Int): Transaction {
        return getItem(position)
    }
}
package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.models.Transaction
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    private var allTransactions = listOf<Transaction>()
    private var filteredTransactions = listOf<Transaction>()

    val filter = object : android.widget.Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.toLowerCase() ?: ""
            val filteredList = if (query.isEmpty()) {
                allTransactions
            } else {
                allTransactions.filter { transaction ->
                    transaction.title.toLowerCase().contains(query) ||
                    transaction.category.toLowerCase().contains(query)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<Transaction>?) {
        allTransactions = list ?: emptyList()
        super.submitList(list)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(transaction: Transaction) {
            tvTitle.text = transaction.title
            tvCategory.text = transaction.category
            tvAmount.text = String.format(Locale.getDefault(), "%.2f", transaction.amount)
            itemView.setOnClickListener { onItemClick(transaction) }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
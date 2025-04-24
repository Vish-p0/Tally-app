package com.example.tally.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.models.BudgetItem
import java.text.NumberFormat
import java.util.Locale

class BudgetItemAdapter(
    private val context: Context,
    private var budgetItems: MutableList<BudgetItem>,
    private val onEditClickListener: (BudgetItem) -> Unit
) : RecyclerView.Adapter<BudgetItemAdapter.BudgetItemViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    class BudgetItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryNameTextView: TextView = view.findViewById(R.id.categoryName)
        val budgetAmountTextView: TextView = view.findViewById(R.id.budgetAmount)
        val expenseAmountTextView: TextView = view.findViewById(R.id.expenseAmount)
        val remainingAmountTextView: TextView = view.findViewById(R.id.remainingAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.categoryProgressBar)
        val progressPercentTextView: TextView = view.findViewById(R.id.progressPercentText)
        val warningIconImageView: ImageView = view.findViewById(R.id.warningIcon)
        val editButton: ImageView = view.findViewById(R.id.editBudgetButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_category, parent, false)
        return BudgetItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetItemViewHolder, position: Int) {
        val budgetItem = budgetItems[position]
        
        // Set category name
        holder.categoryNameTextView.text = budgetItem.categoryName
        
        // Format currency amounts
        holder.budgetAmountTextView.text = currencyFormat.format(budgetItem.budgetAmount)
        
        if (budgetItem.isIncome()) {
            // For income items
            holder.expenseAmountTextView.text = "Income"
            holder.expenseAmountTextView.setTextColor(
                ContextCompat.getColor(context, R.color.link)
            )
            
            // Hide remaining section for income
            holder.remainingAmountTextView.visibility = View.GONE
            
            // Hide progress bar for income
            holder.progressBar.visibility = View.GONE
            holder.progressPercentTextView.visibility = View.GONE
            holder.warningIconImageView.visibility = View.GONE
            
            // Hide progress labels for income
            holder.itemView.findViewById<View>(R.id.progressLabels)?.visibility = View.GONE
            holder.itemView.findViewById<TextView>(R.id.warningMessage).visibility = View.GONE
        } else {
            // For expense items
            holder.expenseAmountTextView.text = currencyFormat.format(budgetItem.expenseAmount)
            holder.expenseAmountTextView.setTextColor(
                ContextCompat.getColor(context, R.color.expense_red)
            )
            
            // Show remaining budget
            holder.remainingAmountTextView.visibility = View.VISIBLE
            holder.remainingAmountTextView.text = currencyFormat.format(budgetItem.getRemainingBudget())
            
            // Show progress bar
            holder.progressBar.visibility = View.VISIBLE
            holder.progressPercentTextView.visibility = View.VISIBLE
            
            // Show progress labels for expenses
            holder.itemView.findViewById<View>(R.id.progressLabels)?.visibility = View.VISIBLE
            
            // Set progress and percentage
            val percentage = budgetItem.getPercentageSpent()
            holder.progressBar.progress = percentage
            holder.progressPercentTextView.text = "$percentage%"
            
            // Update the warning message with more details
            val warningMessage = holder.itemView.findViewById<TextView>(R.id.warningMessage)
            warningMessage.visibility = View.VISIBLE
            
            // Set colors and message based on status
            if (budgetItem.isOverBudget()) {
                // Over budget - show red
                holder.progressBar.progressDrawable = 
                    ContextCompat.getDrawable(context, R.drawable.progress_bar_high)
                holder.warningIconImageView.setImageResource(R.drawable.ic_warning)
                holder.warningIconImageView.visibility = View.VISIBLE
                holder.remainingAmountTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.expense_red)
                )
                warningMessage.text = "Over budget by ${currencyFormat.format(budgetItem.expenseAmount - budgetItem.budgetAmount)}"
                warningMessage.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
            } else if (budgetItem.isNearLimit()) {
                // Near limit - show yellow
                holder.progressBar.progressDrawable = 
                    ContextCompat.getDrawable(context, R.drawable.progress_bar_medium)
                holder.warningIconImageView.setImageResource(R.drawable.ic_warning)
                holder.warningIconImageView.visibility = View.VISIBLE
                holder.remainingAmountTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
                warningMessage.text = "Approaching limit - ${percentage}% of budget spent"
                warningMessage.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow))
            } else {
                // Under budget - show green
                holder.progressBar.progressDrawable = 
                    ContextCompat.getDrawable(context, R.drawable.progress_bar_normal)
                holder.warningIconImageView.visibility = View.GONE
                holder.remainingAmountTextView.setTextColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
                warningMessage.text = "${percentage}% of budget spent"
                warningMessage.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
        }
        
        // Set click listener for edit button
        holder.editButton.setOnClickListener {
            onEditClickListener(budgetItem)
        }
    }

    override fun getItemCount() = budgetItems.size

    fun updateBudgetItems(newItems: List<BudgetItem>) {
        budgetItems.clear()
        budgetItems.addAll(newItems)
        notifyDataSetChanged()
    }
} 
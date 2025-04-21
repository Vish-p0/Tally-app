package com.example.tally.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.tally.R
import com.example.tally.adapters.TransactionAdapter
import com.example.tally.databinding.FragmentHomeBinding
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        adapter = TransactionAdapter { transaction ->
            // Handle transaction click (e.g., edit/delete)
            showTransactionOptions(transaction)
        }
        binding.rvRecentTransactions.adapter = adapter

        // Observe transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions.takeLast(5)) // Show last 5 transactions
            updateTotalBalance(transactions)
            updateBudgetProgress(transactions)
        }

        // Add Transaction Button
        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun updateTotalBalance(transactions: List<Transaction>) {
        val total = transactions.sumOf { if (it.type == "Income") it.amount else -it.amount }
        binding.tvTotalBalance.text = "Total Balance: $%.2f".format(total)
    }

    private fun updateBudgetProgress(transactions: List<Transaction>) {
        val budget = viewModel.getMonthlyBudget()
        val spent = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val progress = (spent / budget * 100).toInt().coerceIn(0, 100)
        binding.progressBudget.progress = progress
        binding.tvBudgetStatus.text = "Spent $%.2f of $%.2f".format(spent, budget)
    }
    
    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.categoryInput)
        val typeInput = dialogView.findViewById<TextInputEditText>(R.id.typeInput)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val category = categoryInput.text.toString()
                val type = typeInput.text.toString()
                
                if (title.isNotEmpty() && amount > 0 && category.isNotEmpty() && type.isNotEmpty()) {
                    val transaction = Transaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        category = category,
                        type = type
                    )
                    viewModel.addTransaction(transaction)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTransactionOptions(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Transaction Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTransactionDialog(transaction)
                    1 -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Transaction")
                            .setMessage("Are you sure you want to delete this transaction?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteTransaction(transaction)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }
    
    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.categoryInput)
        val typeInput = dialogView.findViewById<TextInputEditText>(R.id.typeInput)
        
        titleInput.setText(transaction.title)
        amountInput.setText(transaction.amount.toString())
        categoryInput.setText(transaction.category)
        typeInput.setText(transaction.type)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Transaction")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val category = categoryInput.text.toString()
                val type = typeInput.text.toString()
                
                if (title.isNotEmpty() && amount > 0 && category.isNotEmpty() && type.isNotEmpty()) {
                    val updatedTransaction = transaction.copy(
                        title = title,
                        amount = amount,
                        category = category,
                        type = type
                    )
                    viewModel.deleteTransaction(transaction)
                    viewModel.addTransaction(updatedTransaction)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
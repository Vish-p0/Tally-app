package com.example.tally.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.adapters.TransactionAdapter
import com.example.tally.databinding.FragmentHomeBinding
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID
import android.widget.ArrayAdapter

class HomeFragment : Fragment(), TransactionDetailsDialogFragment.TransactionDialogListener {

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
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetails(transaction)
            },
            viewModel = viewModel
        )
        binding.rvRecentTransactions.adapter = adapter

        // Observe transactions and categories
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions.takeLast(5)) // Show last 5 transactions
            updateTotalBalance(transactions)
            updateBudgetProgress(transactions)
        }

        // Add Transaction Button
        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        // See All Transactions Button
        binding.tvSeeAllTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_transactionsFragment)
        }
    }

    // Implement the TransactionDialogListener interface
    override fun onEditTransaction(transaction: Transaction) {
        // Navigate to AddTransactionFragment with the transaction data for editing
        val bundle = Bundle().apply {
            putString("transactionId", transaction.id)
            putBoolean("isEditing", true)
        }
        findNavController().navigate(
            R.id.action_homeFragment_to_addTransactionFragment,
            bundle
        )
    }
    
    private fun updateTotalBalance(transactions: List<Transaction>) {
        val total = transactions.sumOf { if (it.type == "Income") it.amount else -it.amount }
        binding.tvTotalBalance.text = "Total Balance: $%.2f".format(total)
    }

    private fun updateBudgetProgress(transactions: List<Transaction>) {
        val budget = viewModel.getMonthlyBudget()
        val spent = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val progress = if (budget > 0) ((spent / budget * 100).toInt().coerceIn(0, 100)) else 0
        binding.progressBudget.progress = progress
        binding.tvBudgetStatus.text = "Spent $%.2f of $%.2f".format(spent, budget)
    }
    
    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.titleInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.amountInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)
        val categorySpinner = dialogView.findViewById<android.widget.Spinner>(R.id.categorySpinner)
        val typeRadioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.typeRadioGroup)
        
        // Get available categories
        val categories = viewModel.categories.value ?: emptyList()
        
        // Setup category spinner
        val categoryNames = categories.map { "${it.emoji} ${it.name}" }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        
        // Store category IDs for later use
        val categoryIds = categories.map { it.id }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val description = descriptionInput.text.toString()
                val selectedPosition = categorySpinner.selectedItemPosition
                val categoryId = if (selectedPosition >= 0 && selectedPosition < categoryIds.size) {
                    categoryIds[selectedPosition]
                } else {
                    ""
                }
                val type = if (typeRadioGroup.checkedRadioButtonId == R.id.incomeRadio) "Income" else "Expense"
                
                if (title.isNotEmpty() && amount > 0 && categoryId.isNotEmpty()) {
                    val transaction = Transaction(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        type = type,
                        description = description
                    )
                    viewModel.addTransaction(transaction)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTransactionDetails(transaction: Transaction) {
        // Show the transaction details dialog
        val dialog = TransactionDetailsDialogFragment.newInstance(transaction)
        dialog.show(childFragmentManager, "TransactionDetailsDialog")
    }
}
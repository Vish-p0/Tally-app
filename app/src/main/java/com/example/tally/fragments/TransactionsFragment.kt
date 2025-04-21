package com.example.tally.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tally.R
import com.example.tally.adapters.TransactionAdapter
import com.example.tally.databinding.FragmentTransactionsBinding
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import java.util.UUID

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        swipeRefreshLayout = binding.swipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearch()
        observeTransactions()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            showTransactionOptions(transaction)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@TransactionsFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })
    }

    private fun observeTransactions() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
        }
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
        amountInput.setText(String.format(Locale.getDefault(), "%.2f", transaction.amount))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
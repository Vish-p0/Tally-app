package com.example.tally.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.tally.R
import com.example.tally.databinding.DialogTransactionDetailsBinding
import com.example.tally.models.Transaction
import com.example.tally.viewmodels.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class TransactionDetailsDialogFragment : DialogFragment() {

    private var _binding: DialogTransactionDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var transaction: Transaction

    interface TransactionDialogListener {
        fun onEditTransaction(transaction: Transaction)
    }
    private var listener: TransactionDialogListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Parse transaction from JSON in arguments
        arguments?.getString(ARG_TRANSACTION_JSON)?.let { jsonStr ->
            transaction = parseTransactionFromJson(jsonStr)
        } ?: run {
            dismiss() // If no transaction data provided, dismiss the dialog
        }

        // Ensure the listener is attached if the host implements it
        if (parentFragment is TransactionDialogListener) {
            listener = parentFragment as TransactionDialogListener
        } else if (activity is TransactionDialogListener) {
            listener = activity as TransactionDialogListener
        }
    }

    private fun parseTransactionFromJson(jsonStr: String): Transaction {
        val json = JSONObject(jsonStr)
        return Transaction(
            id = json.getString("id"),
            title = json.getString("title"),
            amount = json.getDouble("amount"),
            categoryId = json.getString("categoryId"),
            type = json.getString("type"),
            description = json.optString("description", ""),
            date = if (json.has("date")) json.getLong("date") else null
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransactionDetailsBinding.inflate(inflater, container, false)

        // Set rounded corners for the dialog window
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Ensure dialog stretches to match_parent width, adjust height to wrap_content
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Optional: Add margins if needed
        val windowParams = dialog?.window?.attributes
        windowParams?.horizontalMargin = 0.1f // 10% margin on each side
        dialog?.window?.attributes = windowParams
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateDialog()
        setupButtonClickListeners()
    }

    private fun populateDialog() {
        binding.tvTitle.text = transaction.title
        binding.tvAmount.text = viewModel.formatAmount(transaction.amount)
        binding.tvType.text = transaction.type

        // Format the date - using a local variable to avoid smart cast issues
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val transactionDate = transaction.date
        binding.tvDate.text = if (transactionDate != null) {
            dateFormat.format(Date(transactionDate))
        } else {
            "N/A"
        }

        // Get category details from the ViewModel
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val category = categories.find { it.id == transaction.categoryId }
            binding.tvCategory.text = category?.name ?: "Unknown"
            binding.tvEmoji.text = category?.emoji ?: "❓" // Default emoji if none found
        }

        binding.tvDescription.text = transaction.description.ifEmpty { "No description provided." }

        // Adjust amount color based on type
        val amountColor = if (transaction.type == "Income") {
            resources.getColor(R.color.income_green, null) 
        } else {
            resources.getColor(R.color.expense_red, null)
        }
        binding.tvAmount.setTextColor(amountColor)
        
        // Observe currency changes to update the amount display
        viewModel.currentCurrency.observe(viewLifecycleOwner) { _ ->
            // Update the formatted amount when currency changes
            binding.tvAmount.text = viewModel.formatAmount(transaction.amount)
        }
    }

    private fun setupButtonClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss() // Close the dialog
        }

        binding.btnEdit.setOnClickListener {
            // Notify the listener (e.g., the hosting Fragment/Activity) to handle the edit action
            listener?.onEditTransaction(transaction)
            dismiss() // Close the dialog after initiating edit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
    
    companion object {
        private const val ARG_TRANSACTION_JSON = "transaction_json"
        
        fun newInstance(transaction: Transaction): TransactionDetailsDialogFragment {
            val fragment = TransactionDetailsDialogFragment()
            val args = Bundle()
            
            // Convert Transaction to JSON string (simpler than making Transaction Parcelable)
            val json = JSONObject().apply {
                put("id", transaction.id)
                put("title", transaction.title)
                put("amount", transaction.amount)
                put("categoryId", transaction.categoryId)
                put("type", transaction.type)
                put("description", transaction.description)
                transaction.date?.let { put("date", it) }
            }
            
            args.putString(ARG_TRANSACTION_JSON, json.toString())
            fragment.arguments = args
            return fragment
        }
    }
} 
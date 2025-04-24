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
import com.example.tally.databinding.DialogDeleteConfirmationBinding

class DeleteConfirmationDialogFragment : DialogFragment() {

    private var _binding: DialogDeleteConfirmationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var transactionId: String

    interface DeleteConfirmationListener {
        fun onConfirmDelete(transactionId: String)
    }
    private var listener: DeleteConfirmationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get the transaction ID from arguments
        arguments?.getString(ARG_TRANSACTION_ID)?.let {
            transactionId = it
        } ?: run {
            dismiss() // If no transaction ID provided, dismiss the dialog
        }

        // Attach listener
        if (parentFragment is DeleteConfirmationListener) {
            listener = parentFragment as DeleteConfirmationListener
        } else if (activity is DeleteConfirmationListener) {
            listener = activity as DeleteConfirmationListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeleteConfirmationBinding.inflate(inflater, container, false)

        // Set rounded corners and transparent background for the dialog window
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Set dialog dimensions
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Add horizontal margins for a typical dialog appearance
        val windowParams = dialog?.window?.attributes
        windowParams?.horizontalMargin = 0.1f // 10% margin on each side
        dialog?.window?.attributes = windowParams
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        binding.btnCancelDelete.setOnClickListener {
            dismiss() // Close the dialog
        }

        binding.btnConfirmDelete.setOnClickListener {
            listener?.onConfirmDelete(transactionId) // Notify listener to perform deletion
            dismiss() // Close the dialog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
    
    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"
        
        fun newInstance(transactionId: String): DeleteConfirmationDialogFragment {
            val fragment = DeleteConfirmationDialogFragment()
            val args = Bundle()
            args.putString(ARG_TRANSACTION_ID, transactionId)
            fragment.arguments = args
            return fragment
        }
    }
} 
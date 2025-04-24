package com.example.tally.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.adapters.TransactionAdapter
import com.example.tally.models.Transaction
import com.example.tally.utils.SwipeToDeleteCallback

/**
 * Helper class to set up swipe-to-delete functionality for transactions
 */
class TransactionSwipeHelper(
    private val fragment: Fragment,
    private val recyclerView: RecyclerView,
    private val adapter: TransactionAdapter,
    private val onShowDeleteDialog: (Transaction) -> Unit
) {

    fun setupSwipeToDelete() {
        val context = fragment.requireContext()
        
        val swipeHandler = object : SwipeToDeleteCallback(context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = adapter.getTransactionAt(position)
                
                // Show delete confirmation dialog
                onShowDeleteDialog(transaction)
                
                // Reset the item view to prevent visual glitches if user cancels
                adapter.notifyItemChanged(position)
            }
        }
        
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
} 
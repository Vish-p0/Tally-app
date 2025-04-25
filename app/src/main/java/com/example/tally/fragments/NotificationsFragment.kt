package com.example.tally.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tally.R
import com.example.tally.adapters.NotificationAdapter
import com.example.tally.databinding.FragmentNotificationsBinding
import com.example.tally.models.AppNotification
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onNotificationClicked = { notification -> 
                viewModel.markNotificationAsRead(notification.id)
            },
            onNotificationLongClicked = { notification ->
                showDeleteConfirmationDialog(notification)
                true
            }
        )
        
        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationsFragment.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            adapter.submitList(notifications)
            updateEmptyState(notifications)
        }
    }
    
    private fun updateEmptyState(notifications: List<AppNotification>) {
        if (notifications.isEmpty()) {
            binding.emptyNotificationsView.visibility = View.VISIBLE
            binding.notificationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyNotificationsView.visibility = View.GONE
            binding.notificationsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Mark all as read button
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllNotificationsAsRead()
            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }
        
        // Clear all notifications button
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmationDialog()
        }
    }
    
    private fun showDeleteConfirmationDialog(notification: AppNotification) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_notification)
            .setMessage(R.string.delete_notification_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteNotification(notification.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showClearAllConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_all_notifications)
            .setMessage(R.string.clear_all_notifications_confirm)
            .setPositiveButton(R.string.clear_all) { _, _ ->
                viewModel.clearAllNotifications()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
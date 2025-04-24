package com.example.tally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.adapters.NotificationAdapter
import com.example.tally.models.AppNotification
import com.example.tally.services.NotificationService
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationsFragment : Fragment() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var emptyNotificationsView: LinearLayout
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationService: NotificationService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        
        // Initialize views
        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView)
        emptyNotificationsView = view.findViewById(R.id.emptyNotificationsView)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)
        
        // Set up back button
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up clear all button
        view.findViewById<View>(R.id.btnClearAll).setOnClickListener {
            confirmClearAllNotifications()
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize notification service
        notificationService = NotificationService.getInstance(requireContext())
        
        // Initialize adapter
        notificationAdapter = NotificationAdapter(
            onItemClick = { notification -> markNotificationAsRead(notification) },
            onDismissClick = { notification -> dismissNotification(notification) },
            onActionClick = { notification -> handleNotificationAction(notification) }
        )
        
        // Set up RecyclerView
        notificationsRecyclerView.adapter = notificationAdapter
        
        // Set up filter chips
        setupFilterChips()
        
        // Load notifications
        loadNotifications()
    }
    
    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> loadNotifications(null)
                R.id.chipAlerts -> loadNotifications(AppNotification.NotificationType.ALERT)
                R.id.chipWarnings -> loadNotifications(AppNotification.NotificationType.WARNING)
                R.id.chipTransactions -> loadNotifications(AppNotification.NotificationType.TRANSACTION)
                R.id.chipReminders -> loadNotifications(AppNotification.NotificationType.REMINDER)
            }
        }
    }
    
    private fun loadNotifications(type: AppNotification.NotificationType? = null) {
        val allNotifications = notificationService.getAllNotifications()
        
        // Filter notifications by type if needed
        val filteredNotifications = if (type != null) {
            allNotifications.filter { it.type == type }
        } else {
            allNotifications
        }
        
        // Update UI based on whether there are notifications
        if (filteredNotifications.isEmpty()) {
            notificationsRecyclerView.visibility = View.GONE
            emptyNotificationsView.visibility = View.VISIBLE
        } else {
            notificationsRecyclerView.visibility = View.VISIBLE
            emptyNotificationsView.visibility = View.GONE
            notificationAdapter.submitList(filteredNotifications)
        }
    }
    
    private fun markNotificationAsRead(notification: AppNotification) {
        notificationService.markAsRead(notification.id)
        loadNotifications()
    }
    
    private fun dismissNotification(notification: AppNotification) {
        notificationService.deleteNotification(notification.id)
        Toast.makeText(context, "Notification dismissed", Toast.LENGTH_SHORT).show()
        loadNotifications()
    }
    
    private fun handleNotificationAction(notification: AppNotification) {
        // Handle different actions based on notification type
        when (notification.type) {
            AppNotification.NotificationType.ALERT, 
            AppNotification.NotificationType.WARNING -> {
                // Navigate to budget screen if this is budget related
                if (notification.relatedEntityId != null) {
                    findNavController().navigate(R.id.action_notificationsFragment_to_analyticsFragment)
                }
            }
            AppNotification.NotificationType.TRANSACTION -> {
                // Navigate to transaction detail
                if (notification.relatedEntityId != null) {
                    // TODO: Navigate to transaction detail with ID
                    findNavController().navigate(R.id.action_notificationsFragment_to_transactionsFragment)
                }
            }
            else -> {
                // For other notifications, just mark as read
                markNotificationAsRead(notification)
            }
        }
    }
    
    private fun confirmClearAllNotifications() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to clear all notifications?")
            .setPositiveButton("Clear All") { _, _ ->
                notificationService.clearAllNotifications()
                Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show()
                loadNotifications()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 
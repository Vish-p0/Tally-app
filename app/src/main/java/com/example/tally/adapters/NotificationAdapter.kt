package com.example.tally.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.models.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (AppNotification) -> Unit,
    private val onDismissClick: (AppNotification) -> Unit,
    private val onActionClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val notificationIcon: TextView = itemView.findViewById(R.id.notificationIcon)
        private val notificationTitle: TextView = itemView.findViewById(R.id.notificationTitle)
        private val notificationMessage: TextView = itemView.findViewById(R.id.notificationMessage)
        private val notificationDate: TextView = itemView.findViewById(R.id.notificationDate)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
        private val actionButtonsLayout: LinearLayout = itemView.findViewById(R.id.actionButtonsLayout)
        private val primaryActionButton: Button = itemView.findViewById(R.id.primaryActionButton)
        private val secondaryActionButton: Button = itemView.findViewById(R.id.secondaryActionButton)

        fun bind(notification: AppNotification) {
            // Set notification icon based on type
            notificationIcon.text = notification.getIcon()

            // Set title and message
            notificationTitle.text = notification.title
            notificationMessage.text = notification.message

            // Format and set date
            notificationDate.text = formatNotificationDate(notification)

            // Show/hide unread indicator
            unreadIndicator.visibility = if (notification.isRead) View.INVISIBLE else View.VISIBLE

            // Handle action buttons for different notification types
            setupActionButtons(notification)

            // Set click listener on the entire item
            itemView.setOnClickListener {
                onItemClick(notification)
            }
        }

        private fun setupActionButtons(notification: AppNotification) {
            when (notification.type) {
                AppNotification.NotificationType.ALERT, 
                AppNotification.NotificationType.WARNING -> {
                    if (notification.relatedEntityId != null) {
                        // It's a budget or transaction related alert/warning
                        actionButtonsLayout.visibility = View.VISIBLE
                        
                        primaryActionButton.text = "View"
                        primaryActionButton.setOnClickListener {
                            onActionClick(notification)
                        }
                        
                        secondaryActionButton.text = "Dismiss"
                        secondaryActionButton.setOnClickListener {
                            onDismissClick(notification)
                        }
                    } else {
                        actionButtonsLayout.visibility = View.GONE
                    }
                }
                AppNotification.NotificationType.TRANSACTION -> {
                    actionButtonsLayout.visibility = View.VISIBLE
                    
                    primaryActionButton.text = "View Details"
                    primaryActionButton.setOnClickListener {
                        onActionClick(notification)
                    }
                    
                    secondaryActionButton.text = "Dismiss"
                    secondaryActionButton.setOnClickListener {
                        onDismissClick(notification)
                    }
                }
                else -> {
                    actionButtonsLayout.visibility = View.GONE
                }
            }
        }

        private fun formatNotificationDate(notification: AppNotification): String {
            // Get date group
            val dateGroup = notification.getDateGroup()
            
            return when (dateGroup) {
                AppNotification.DateGroup.TODAY -> "Today"
                AppNotification.DateGroup.YESTERDAY -> "Yesterday"
                AppNotification.DateGroup.THIS_WEEK -> {
                    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                    sdf.format(Date(notification.timestamp))
                }
                AppNotification.DateGroup.OLDER -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(notification.timestamp))
                }
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification): Boolean {
            return oldItem == newItem
        }
    }
} 
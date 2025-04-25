package com.example.tally.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.models.AppNotification
import java.util.Date

/**
 * Adapter for displaying notifications in a RecyclerView.
 */
class NotificationAdapter(
    private val onNotificationClicked: (AppNotification) -> Unit,
    private val onNotificationLongClicked: (AppNotification) -> Boolean
) : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val titleView: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val messageView: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val timeView: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val unreadIndicator: View = itemView.findViewById(R.id.viewUnreadIndicator)

        fun bind(notification: AppNotification) {
            // Set notification icon
            iconView.setImageResource(notification.icon)
            
            // Set notification title and message
            titleView.text = notification.title
            messageView.text = notification.message
            
            // Format and set time
            timeView.text = getRelativeTimeString(notification.timestamp)
            
            // Set unread indicator visibility
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Set background color based on read status
            val backgroundColor = if (notification.isRead) {
                ContextCompat.getColor(itemView.context, R.color.white)
            } else {
                ContextCompat.getColor(itemView.context, R.color.light_gray)
            }
            itemView.setBackgroundColor(backgroundColor)
            
            // Set click listeners
            itemView.setOnClickListener {
                onNotificationClicked(notification)
            }
            
            itemView.setOnLongClickListener {
                onNotificationLongClicked(notification)
            }
        }
        
        private fun getRelativeTimeString(timestamp: Long): String {
            return DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
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
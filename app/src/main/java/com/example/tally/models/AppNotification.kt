package com.example.tally.models

import java.util.Date
import java.util.UUID

/**
 * Data class for in-app notifications.
 */
data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val icon: Int,
    val type: NotificationType = NotificationType.GENERAL,
    val relatedEntityId: String? = null, // Related transaction or budget ID
    var isRead: Boolean = false
) {
    enum class NotificationType {
        TRANSACTION_ADDED,
        TRANSACTION_UPDATED,
        TRANSACTION_DELETED,
        BUDGET_CREATED,
        BUDGET_UPDATED,
        BUDGET_LIMIT_REACHED,
        BACKUP_CREATED,
        BACKUP_RESTORED,
        GENERAL,
        WARNING,      // 75% of budget or 90% of income
        ALERT,        // 100%+ of budget or income
        TRANSACTION,  // New transaction added
        REMINDER,     // General reminders
        NEWER,
        OLDER
    }
    
    fun getIcon(): String {
        return when (type) {
            NotificationType.WARNING -> "⚠️"
            NotificationType.ALERT -> "🚨"
            NotificationType.TRANSACTION, 
            NotificationType.TRANSACTION_ADDED,
            NotificationType.TRANSACTION_UPDATED,
            NotificationType.TRANSACTION_DELETED -> "💵"
            NotificationType.REMINDER -> "🔔"
            NotificationType.BACKUP_CREATED,
            NotificationType.BACKUP_RESTORED -> "💾"
            NotificationType.BUDGET_CREATED,
            NotificationType.BUDGET_UPDATED,
            NotificationType.BUDGET_LIMIT_REACHED -> "📊"
            else -> "📣"
        }
    }
    
    fun getDateGroup(): DateGroup {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp
        
        val dayInMillis = 24 * 60 * 60 * 1000L
        
        return when {
            diff < dayInMillis -> DateGroup.TODAY
            diff < 2 * dayInMillis -> DateGroup.YESTERDAY
            diff < 7 * dayInMillis -> DateGroup.THIS_WEEK
            else -> DateGroup.OLDER
        }
    }
    
    enum class DateGroup {
        TODAY,
        YESTERDAY,
        THIS_WEEK,
        OLDER
    }

    // Custom sort order: unread notifications first, then by timestamp (newer first)
    fun sortValue(): Long {
        return if (isRead) timestamp else Long.MAX_VALUE - timestamp
    }
} 
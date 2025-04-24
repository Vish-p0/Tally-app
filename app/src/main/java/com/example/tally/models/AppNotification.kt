package com.example.tally.models

import java.util.Date
import java.util.UUID

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedEntityId: String? = null, // Related transaction or budget ID
    val isRead: Boolean = false
) {
    enum class NotificationType {
        WARNING,      // 75% of budget or 90% of income
        ALERT,        // 100%+ of budget or income
        TRANSACTION,  // New transaction added
        REMINDER      // General reminders
    }
    
    fun getIcon(): String {
        return when (type) {
            NotificationType.WARNING -> "⚠️"
            NotificationType.ALERT -> "🚨"
            NotificationType.TRANSACTION -> "💵"
            NotificationType.REMINDER -> "🔔"
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
} 
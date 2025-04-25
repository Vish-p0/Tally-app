package com.example.tally.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.tally.activities.MainActivity
import com.example.tally.R
import com.example.tally.models.AppNotification
import com.example.tally.models.BudgetItem
import com.example.tally.models.Transaction
import com.example.tally.utils.PermissionUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_ALERTS = "alerts_channel"
        private const val CHANNEL_ID_WARNINGS = "warnings_channel"
        private const val CHANNEL_ID_TRANSACTIONS = "transactions_channel"
        private const val CHANNEL_ID_REMINDERS = "reminders_channel"
        private const val NOTIFICATION_PREFS = "notification_preferences"
        private const val NOTIFICATIONS_KEY = "saved_notifications"
        
        fun getInstance(context: Context): NotificationService {
            return NotificationService(context)
        }
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        NOTIFICATION_PREFS, Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Budget and income exceeded alerts"
            }
            
            val warningChannel = NotificationChannel(
                CHANNEL_ID_WARNINGS,
                "Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Budget and income warnings"
            }
            
            val transactionChannel = NotificationChannel(
                CHANNEL_ID_TRANSACTIONS,
                "Transactions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Transaction notifications"
            }
            
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminder notifications"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(alertChannel, warningChannel, transactionChannel, reminderChannel)
            )
        }
    }
    
    fun sendBudgetWarning(budgetItem: BudgetItem, currentSpending: Double) {
        val percentUsed = (currentSpending / budgetItem.budgetAmount) * 100
        val title = "Budget Warning: ${budgetItem.categoryName}"
        val message = "You've used ${percentUsed.toInt()}% of your ${budgetItem.categoryName} budget."
        
        val notification = AppNotification(
            type = AppNotification.NotificationType.WARNING,
            title = title,
            message = message,
            relatedEntityId = budgetItem.id,
            icon = R.drawable.warning_icon
        )
        
        saveNotification(notification)
        showPushNotification(notification)
    }
    
    fun sendBudgetAlert(budgetItem: BudgetItem, currentSpending: Double) {
        val percentUsed = (currentSpending / budgetItem.budgetAmount) * 100
        val title = "Budget Alert: ${budgetItem.categoryName}"
        val message = "You've exceeded your ${budgetItem.categoryName} budget by ${(percentUsed - 100).toInt()}%."
        
        val notification = AppNotification(
            type = AppNotification.NotificationType.ALERT,
            title = title,
            message = message,
            relatedEntityId = budgetItem.id,
            icon = R.drawable.ic_warning
        )
        
        saveNotification(notification)
        showPushNotification(notification)
    }
    
    fun sendIncomeVsExpenseWarning(totalIncome: Double, totalExpenses: Double) {
        val percentUsed = (totalExpenses / totalIncome) * 100
        val title = "Expense Warning"
        val message = "Your expenses have reached ${percentUsed.toInt()}% of your income."
        
        val notification = AppNotification(
            type = AppNotification.NotificationType.WARNING,
            title = title,
            message = message,
            icon = R.drawable.warning_icon
        )
        
        saveNotification(notification)
        showPushNotification(notification)
    }
    
    fun sendIncomeVsExpenseAlert(totalIncome: Double, totalExpenses: Double) {
        val percentUsed = (totalExpenses / totalIncome) * 100
        val title = "Expense Alert"
        val message = "Your expenses have exceeded your income by ${(percentUsed - 100).toInt()}%."
        
        val notification = AppNotification(
            type = AppNotification.NotificationType.ALERT,
            title = title,
            message = message,
            icon = R.drawable.ic_warning
        )
        
        saveNotification(notification)
        showPushNotification(notification)
    }
    
    fun sendTransactionNotification(transaction: Transaction) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = transaction.date?.let { sdf.format(Date(it)) } ?: "Unknown date"
        
        val title = "New ${if (transaction.type == "Expense") "Expense" else "Income"} Added"
        val message = "${transaction.title}: ${transaction.amount} (${transaction.categoryId}) on $dateStr"
        
        val notification = AppNotification(
            type = AppNotification.NotificationType.TRANSACTION,
            title = title,
            message = message,
            relatedEntityId = transaction.id,
            icon = R.drawable.money
        )
        
        saveNotification(notification)
        showPushNotification(notification)
    }
    
    fun sendReminder(title: String, message: String) {
        val notification = AppNotification(
            type = AppNotification.NotificationType.REMINDER,
            title = title,
            message = message,
            icon = R.drawable.notifications
        )
        
        saveNotification(notification)
        showPushNotification(notification)
    }
    
    private fun showPushNotification(notification: AppNotification) {
        // Check if we have notification permission
        if (!PermissionUtils.hasNotificationPermission(context)) {
            // Save notification for later display when permission is granted
            saveNotification(notification)
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_NOTIFICATIONS", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = when (notification.type) {
            AppNotification.NotificationType.ALERT -> CHANNEL_ID_ALERTS
            AppNotification.NotificationType.WARNING -> CHANNEL_ID_WARNINGS
            AppNotification.NotificationType.TRANSACTION, 
            AppNotification.NotificationType.TRANSACTION_ADDED,
            AppNotification.NotificationType.TRANSACTION_UPDATED,
            AppNotification.NotificationType.TRANSACTION_DELETED -> CHANNEL_ID_TRANSACTIONS
            AppNotification.NotificationType.REMINDER -> CHANNEL_ID_REMINDERS
            AppNotification.NotificationType.BUDGET_CREATED,
            AppNotification.NotificationType.BUDGET_UPDATED,
            AppNotification.NotificationType.BUDGET_LIMIT_REACHED,
            AppNotification.NotificationType.BACKUP_CREATED,
            AppNotification.NotificationType.BACKUP_RESTORED,
            AppNotification.NotificationType.GENERAL,
            AppNotification.NotificationType.NEWER,
            AppNotification.NotificationType.OLDER -> CHANNEL_ID_REMINDERS
        }
        
        val importance = when (notification.type) {
            AppNotification.NotificationType.ALERT -> NotificationCompat.PRIORITY_HIGH
            AppNotification.NotificationType.WARNING -> NotificationCompat.PRIORITY_DEFAULT
            AppNotification.NotificationType.TRANSACTION,
            AppNotification.NotificationType.TRANSACTION_ADDED,
            AppNotification.NotificationType.TRANSACTION_UPDATED,
            AppNotification.NotificationType.TRANSACTION_DELETED -> NotificationCompat.PRIORITY_DEFAULT
            AppNotification.NotificationType.REMINDER -> NotificationCompat.PRIORITY_DEFAULT
            AppNotification.NotificationType.BUDGET_CREATED,
            AppNotification.NotificationType.BUDGET_UPDATED, 
            AppNotification.NotificationType.BUDGET_LIMIT_REACHED,
            AppNotification.NotificationType.BACKUP_CREATED,
            AppNotification.NotificationType.BACKUP_RESTORED,
            AppNotification.NotificationType.GENERAL,
            AppNotification.NotificationType.NEWER,
            AppNotification.NotificationType.OLDER -> NotificationCompat.PRIORITY_DEFAULT
        }
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${notification.getIcon()} ${notification.title}")
            .setContentText(notification.message)
            .setPriority(importance)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notification.id.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission denied at runtime
            saveNotification(notification)
            e.printStackTrace()
        }
    }
    
    fun saveNotification(notification: AppNotification) {
        val notifications = getAllNotifications().toMutableList()
        notifications.add(0, notification) // Add to the beginning of the list
        saveAllNotifications(notifications)
    }
    
    fun markAsRead(notificationId: String) {
        val notifications = getAllNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        
        if (index != -1) {
            val notification = notifications[index]
            notifications[index] = notification.copy(isRead = true)
            saveAllNotifications(notifications)
        }
    }
    
    fun markAllAsRead() {
        val notifications = getAllNotifications().map { 
            it.copy(isRead = true) 
        }
        saveAllNotifications(notifications.toList())
    }
    
    fun deleteNotification(notificationId: String) {
        val notifications = getAllNotifications().toMutableList()
        notifications.removeAll { it.id == notificationId }
        saveAllNotifications(notifications)
    }
    
    fun clearAllNotifications() {
        saveAllNotifications(emptyList())
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }
    
    fun getAllNotifications(): List<AppNotification> {
        val json = preferences.getString(NOTIFICATIONS_KEY, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<AppNotification>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun saveAllNotifications(notifications: List<AppNotification>) {
        val json = gson.toJson(notifications)
        preferences.edit().putString(NOTIFICATIONS_KEY, json).apply()
    }
} 
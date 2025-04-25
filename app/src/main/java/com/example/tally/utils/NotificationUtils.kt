package com.example.tally.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.tally.R
import com.example.tally.activities.MainActivity
import com.example.tally.models.AppNotification
import com.example.tally.repositories.FinanceRepository
import java.util.Date

/**
 * Utility class for handling notifications in the app.
 */
class NotificationUtils(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "tally_notifications"
        const val CHANNEL_NAME = "Tally Notifications"
        const val CHANNEL_DESCRIPTION = "Notifications for Tally app activities"
        
        const val NOTIFICATION_ID_BACKUP = 1001
        const val NOTIFICATION_ID_RESTORE = 1002
        const val NOTIFICATION_ID_TRANSACTION = 1003
        const val NOTIFICATION_ID_BUDGET = 1004
    }
    
    private val repository = FinanceRepository(context)
    
    /**
     * Creates the notification channel for the app (required for Android 8.0+).
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Shows a notification and adds it to the notifications fragment.
     */
    fun showNotification(title: String, message: String, icon: Int, notificationId: Int) {
        // Create an intent to open the app when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Handle notification permission not granted
                e.printStackTrace()
            }
        }
        
        // Add notification to repository for the notifications fragment
        val appNotification = AppNotification(
            id = System.currentTimeMillis().toString(),
            title = title,
            message = message,
            timestamp = Date().time,
            icon = icon,
            isRead = false
        )
        
        val notifications = repository.getNotifications().toMutableList()
        notifications.add(appNotification)
        repository.saveNotifications(notifications)
    }
    
    /**
     * Shows a notification for backup completion.
     */
    fun notifyBackupComplete(isSuccess: Boolean) {
        val title = if (isSuccess) "Backup Successful" else "Backup Failed"
        val message = if (isSuccess) 
            "Your data has been successfully backed up" 
        else 
            "Failed to create a backup. Please try again"
        
        showNotification(
            title = title,
            message = message,
            icon = R.drawable.backup,
            notificationId = NOTIFICATION_ID_BACKUP
        )
    }
    
    /**
     * Shows a notification for restore completion.
     */
    fun notifyRestoreComplete(isSuccess: Boolean) {
        val title = if (isSuccess) "Restore Successful" else "Restore Failed"
        val message = if (isSuccess) 
            "Your data has been successfully restored" 
        else 
            "Failed to restore data. Please try again"
        
        showNotification(
            title = title,
            message = message,
            icon = R.drawable.restore,
            notificationId = NOTIFICATION_ID_RESTORE
        )
    }
    
    /**
     * Shows a notification for transaction completion.
     */
    fun notifyTransactionCreated(transactionName: String, amount: Double, isExpense: Boolean) {
        val title = if (isExpense) "Expense Added" else "Income Added"
        val message = "$transactionName: ${formatAmount(amount)}"
        
        showNotification(
            title = title,
            message = message,
            icon = R.drawable.money,
            notificationId = NOTIFICATION_ID_TRANSACTION
        )
    }
    
    /**
     * Shows a notification for budget creation or update.
     */
    fun notifyBudgetUpdated(budgetName: String, amount: Double) {
        showNotification(
            title = "Budget Updated",
            message = "Budget for $budgetName set to ${formatAmount(amount)}",
            icon = R.drawable.money,
            notificationId = NOTIFICATION_ID_BUDGET
        )
    }
    
    /**
     * Formats an amount with currency symbol.
     */
    private fun formatAmount(amount: Double): String {
        val currencyManager = CurrencyManager(context)
        return currencyManager.formatAmount(amount)
    }
} 
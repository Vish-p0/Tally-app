package com.example.tally.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.tally.activities.MainActivity
import com.example.tally.R

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "tally_channel"
        const val CHANNEL_NAME = "Tally Notifications"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for Tally app notifications"
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBudgetAlert(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Create custom layout
        val customNotificationLayout = RemoteViews(context.packageName, R.layout.notification_layout)
        customNotificationLayout.setTextViewText(R.id.notification_title, title)
        customNotificationLayout.setTextViewText(R.id.notification_message, message)
        customNotificationLayout.setImageViewResource(R.id.notification_icon, R.drawable.ic_notification)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(customNotificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showTransactionNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val customNotificationLayout = RemoteViews(context.packageName, R.layout.notification_layout)
        customNotificationLayout.setTextViewText(R.id.notification_title, title)
        customNotificationLayout.setTextViewText(R.id.notification_message, message)
        customNotificationLayout.setImageViewResource(R.id.notification_icon, R.drawable.ic_notification)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(customNotificationLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
} 
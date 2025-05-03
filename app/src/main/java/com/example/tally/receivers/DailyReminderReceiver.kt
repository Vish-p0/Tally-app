package com.example.tally.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.tally.R
import com.example.tally.activities.LandingScreenActivity

class DailyReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // Create notification to remind user to enter their daily transactions
        showReminderNotification(context)
    }
    
    private fun showReminderNotification(context: Context) {
        val channelId = "daily_reminder_channel"
        val notificationId = 1001
        
        // Create an Intent for the activity you want to start
        val contentIntent = Intent(context, LandingScreenActivity::class.java)
        contentIntent.putExtra("OPEN_ADD_TRANSACTION", true)
        contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        // Create the PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.money)
            .setContentTitle(context.getString(R.string.daily_reminder_title))
            .setContentText(context.getString(R.string.daily_reminder_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // Get the NotificationManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Send the notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
} 
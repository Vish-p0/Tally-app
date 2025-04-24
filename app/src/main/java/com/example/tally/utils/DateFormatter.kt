package com.example.tally.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.NumberFormat

/**
 * Formats a timestamp into a readable date string
 */
object DateFormatter {
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "N/A"
        return fullDateFormat.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        return timeFormat.format(Date(timestamp))
    }
    
    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null) return "N/A"
        val date = Date(timestamp)
        return "${fullDateFormat.format(date)} at ${shortTimeFormat.format(date)}"
    }
}

/**
 * Formats a number as currency using the app's CurrencyManager
 * 
 * @param context The context to get CurrencyManager from
 * @param amount The amount to format
 * @return The formatted amount string with appropriate currency symbol
 */
fun formatCurrency(context: Context, amount: Double): String {
    return CurrencyManager(context).formatAmount(amount)
}

/**
 * Backward compatibility method for existing code
 * @deprecated Use formatCurrency(context, amount) instead
 */
@Deprecated("Use the version with context parameter", ReplaceWith("formatCurrency(context, amount)"))
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(amount)
} 
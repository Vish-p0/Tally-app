package com.example.tally.utils

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
 * Formats a number as currency
 */
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(amount)
} 
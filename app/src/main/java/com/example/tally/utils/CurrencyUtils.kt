package com.example.tally.utils

import android.content.Context
import androidx.databinding.BindingAdapter
import android.widget.TextView

/**
 * Utility class for currency formatting that can be used in XML layouts
 */
object CurrencyUtils {
    
    /**
     * Formats an amount according to the current currency settings
     */
    @JvmStatic
    fun formatAmount(context: Context, amount: Double): String {
        return CurrencyManager(context).formatAmount(amount)
    }
    
    /**
     * Binding adapter for TextView to format amount according to current currency
     * Usage in XML: app:currencyAmount="@{amount}"
     */
    @JvmStatic
    @BindingAdapter("currencyAmount")
    fun setCurrencyAmount(textView: TextView, amount: Double) {
        textView.text = formatAmount(textView.context, amount)
    }
} 
package com.example.tally.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.tally.models.Currency
import java.text.NumberFormat
import java.util.Locale

class CurrencyManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getCurrentCurrency(): Currency {
        val currencyCode = prefs.getString(KEY_CURRENCY, Currency.USD.code) ?: Currency.USD.code
        return Currency.getByCode(currencyCode)
    }
    
    fun setCurrentCurrency(currency: Currency) {
        prefs.edit().putString(KEY_CURRENCY, currency.code).apply()
    }
    
    fun formatAmount(amount: Double): String {
        val currency = getCurrentCurrency()
        
        // Convert amount from USD to the current currency
        val convertedAmount = convertAmount(amount, Currency.USD, currency)
        
        // Format the amount with the currency symbol
        return when (currency.code) {
            "JPY", "KRW" -> "${currency.symbol}${convertedAmount.toInt()}" // No decimals for these currencies
            else -> "${currency.symbol}${String.format("%.2f", convertedAmount)}"
        }
    }
    
    fun convertAmount(amount: Double, fromCurrency: Currency, toCurrency: Currency): Double {
        // First convert to USD (base currency)
        val amountInUsd = amount / fromCurrency.conversionRate
        
        // Then convert from USD to target currency
        return amountInUsd * toCurrency.conversionRate
    }
    
    fun convertAmountToUsd(amount: Double): Double {
        val currency = getCurrentCurrency()
        return amount / currency.conversionRate
    }
    
    fun convertAmountFromUsd(amountInUsd: Double): Double {
        val currency = getCurrentCurrency()
        return amountInUsd * currency.conversionRate
    }
    
    companion object {
        private const val PREFS_NAME = "tally_currency_prefs"
        private const val KEY_CURRENCY = "current_currency"
    }
} 
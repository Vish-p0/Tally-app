package com.example.tally.models

data class Currency(
    val code: String,         // Currency code (e.g., USD, EUR)
    val name: String,         // Currency name (e.g., United States Dollar)
    val symbol: String,       // Currency symbol (e.g., $, €, £)
    val flag: String,         // Flag emoji
    val conversionRate: Double = 1.0  // Conversion rate relative to USD (default 1.0 for USD)
) {
    override fun toString(): String {
        return "$flag $name – $code – $symbol"
    }
    
    companion object {
        val USD = Currency("USD", "United States Dollar", "$", "🇺🇸", 1.0)
        val EUR = Currency("EUR", "Euro", "€", "🇪🇺", 0.91)
        val GBP = Currency("GBP", "British Pound Sterling", "£", "🇬🇧", 0.78)
        val JPY = Currency("JPY", "Japanese Yen", "¥", "🇯🇵", 149.82)
        val CNY = Currency("CNY", "Chinese Yuan Renminbi", "¥", "🇨🇳", 7.24)
        val INR = Currency("INR", "Indian Rupee", "₹", "🇮🇳", 83.48)
        val AUD = Currency("AUD", "Australian Dollar", "A$", "🇦🇺", 1.52)
        val CAD = Currency("CAD", "Canadian Dollar", "C$", "🇨🇦", 1.37)
        val CHF = Currency("CHF", "Swiss Franc", "Fr", "🇨🇭", 0.90)
        val SGD = Currency("SGD", "Singapore Dollar", "S$", "🇸🇬", 1.34)
        val HKD = Currency("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", 7.81)
        val NZD = Currency("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 1.66)
        val SAR = Currency("SAR", "Saudi Riyal", "﷼", "🇸🇦", 3.75)
        val AED = Currency("AED", "United Arab Emirates Dirham", "د.إ", "🇦🇪", 3.67)
        val RUB = Currency("RUB", "Russian Ruble", "₽", "🇷🇺", 90.19)
        val BRL = Currency("BRL", "Brazilian Real", "R$", "🇧🇷", 5.19)
        val ZAR = Currency("ZAR", "South African Rand", "R", "🇿🇦", 18.74)
        val KRW = Currency("KRW", "South Korean Won", "₩", "🇰🇷", 1367.56)
        val LKR = Currency("LKR", "Sri Lankan Rupee", "රු", "🇱🇰", 307.50)
        val MYR = Currency("MYR", "Malaysian Ringgit", "RM", "🇲🇾", 4.73)
        
        val availableCurrencies = listOf(
            USD, EUR, GBP, JPY, CNY, INR, AUD, CAD, CHF, SGD,
            HKD, NZD, SAR, AED, RUB, BRL, ZAR, KRW, LKR, MYR
        )
        
        fun getByCode(code: String): Currency {
            return availableCurrencies.find { it.code == code } ?: USD
        }
    }
} 
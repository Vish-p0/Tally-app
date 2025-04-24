package com.example.tally.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PinManager(private val context: Context) {
    
    companion object {
        private const val PREF_NAME = "tally_pin_prefs"
        private const val KEY_PIN = "pin_code"
        private const val KEY_PIN_ATTEMPTS = "pin_attempts"
        private const val KEY_LAST_ATTEMPT_TIME = "last_attempt_time"
        private const val KEY_COOLDOWN_ACTIVE = "cooldown_active"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"
        private const val KEY_PIN_CREATED = "pin_created"
        
        private const val MAX_ATTEMPTS = 3
        private const val COOLDOWN_DURATION = 120000L // 2 minutes in milliseconds
        
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "TallyPinKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = ";"
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // Check if pin has been created
    fun isPinCreated(): Boolean {
        return preferences.getBoolean(KEY_PIN_CREATED, false)
    }
    
    // Create or update pin
    fun savePin(pin: String) {
        val encryptedPin = encryptPin(pin)
        preferences.edit()
            .putString(KEY_PIN, encryptedPin)
            .putBoolean(KEY_PIN_CREATED, true)
            .putInt(KEY_PIN_ATTEMPTS, 0)
            .putBoolean(KEY_COOLDOWN_ACTIVE, false)
            .apply()
    }
    
    // Verify pin
    fun verifyPin(enteredPin: String): Boolean {
        if (isCooldownActive()) {
            return false
        }
        
        val storedPin = getStoredPin()
        if (storedPin == null) {
            return false
        }
        
        val isValid = decryptPin(storedPin) == enteredPin
        
        if (isValid) {
            // Reset attempts on successful verification
            resetAttempts()
        } else {
            // Increment failed attempts
            incrementAttempts()
        }
        
        return isValid
    }
    
    // Save security question and answer
    fun saveSecurityQuestion(question: String, answer: String) {
        val encryptedAnswer = encryptPin(answer.lowercase().trim())
        preferences.edit()
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER, encryptedAnswer)
            .apply()
    }
    
    // Get security question
    fun getSecurityQuestion(): String? {
        return preferences.getString(KEY_SECURITY_QUESTION, null)
    }
    
    // Verify security answer
    fun verifySecurityAnswer(answer: String): Boolean {
        val storedAnswer = preferences.getString(KEY_SECURITY_ANSWER, null) ?: return false
        return decryptPin(storedAnswer) == answer.lowercase().trim()
    }
    
    // Check if cooldown is active
    fun isCooldownActive(): Boolean {
        val cooldownActive = preferences.getBoolean(KEY_COOLDOWN_ACTIVE, false)
        
        if (!cooldownActive) {
            return false
        }
        
        val lastAttemptTime = preferences.getLong(KEY_LAST_ATTEMPT_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        // Check if cooldown period has passed
        if (currentTime - lastAttemptTime >= COOLDOWN_DURATION) {
            // Reset cooldown state
            preferences.edit()
                .putBoolean(KEY_COOLDOWN_ACTIVE, false)
                .apply()
            return false
        }
        
        return true
    }
    
    // Get remaining cooldown time in milliseconds
    fun getRemainingCooldownTime(): Long {
        val lastAttemptTime = preferences.getLong(KEY_LAST_ATTEMPT_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastAttemptTime
        
        return if (elapsed >= COOLDOWN_DURATION) 0 else COOLDOWN_DURATION - elapsed
    }
    
    // Get remaining allowed attempts
    fun getRemainingAttempts(): Int {
        val attempts = preferences.getInt(KEY_PIN_ATTEMPTS, 0)
        return MAX_ATTEMPTS - attempts
    }
    
    // Reset pin (for use with security question)
    fun resetPin() {
        preferences.edit()
            .putBoolean(KEY_PIN_CREATED, false)
            .putInt(KEY_PIN_ATTEMPTS, 0)
            .putBoolean(KEY_COOLDOWN_ACTIVE, false)
            .apply()
    }
    
    // Private methods
    private fun incrementAttempts() {
        val attempts = preferences.getInt(KEY_PIN_ATTEMPTS, 0) + 1
        preferences.edit()
            .putInt(KEY_PIN_ATTEMPTS, attempts)
            .putLong(KEY_LAST_ATTEMPT_TIME, System.currentTimeMillis())
            .apply()
        
        // Check if max attempts reached
        if (attempts >= MAX_ATTEMPTS) {
            activateCooldown()
        }
    }
    
    private fun resetAttempts() {
        preferences.edit()
            .putInt(KEY_PIN_ATTEMPTS, 0)
            .putBoolean(KEY_COOLDOWN_ACTIVE, false)
            .apply()
    }
    
    private fun activateCooldown() {
        preferences.edit()
            .putBoolean(KEY_COOLDOWN_ACTIVE, true)
            .putLong(KEY_LAST_ATTEMPT_TIME, System.currentTimeMillis())
            .apply()
    }
    
    private fun getStoredPin(): String? {
        return preferences.getString(KEY_PIN, null)
    }
    
    // Encryption/decryption methods
    private fun encryptPin(pin: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(pin.toByteArray())
            
            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            
            return "$ivString$IV_SEPARATOR$encryptedString"
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to plain text for older devices that don't support encryption
            return pin
        }
    }
    
    private fun decryptPin(encryptedPin: String): String {
        try {
            if (!encryptedPin.contains(IV_SEPARATOR)) {
                // Handle the case where pin is not properly encrypted
                return encryptedPin
            }
            
            val parts = encryptedPin.split(IV_SEPARATOR)
            if (parts.size != 2) return encryptedPin
            
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            // Return the encrypted text if decryption fails
            return encryptedPin
        }
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }
        
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
} 
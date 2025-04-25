package com.example.tally.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.tally.models.AppNotification
import com.example.tally.models.BudgetItem
import com.example.tally.models.Category
import com.example.tally.models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for handling backup and restore operations.
 */
class BackupRestoreUtil(private val context: Context) {
    
    private val TAG = "BackupRestoreUtil"
    private val gson = Gson()
    private val notificationUtils = NotificationUtils(context)
    
    /**
     * Backup data structure to store all app data.
     */
    data class BackupData(
        val categories: List<Category>,
        val transactions: List<Transaction>,
        val budget: Double,
        val budgetItems: List<BudgetItem>,
        val currencyCode: String? = "USD",
        val timestamp: Long = System.currentTimeMillis(),
        val appVersion: String = "1.0" // Add app version for future compatibility checks
    )
    
    /**
     * Creates a backup of the app data in internal storage.
     * 
     * @param categories List of categories
     * @param transactions List of transactions
     * @param budget Budget amount
     * @param budgetItems List of budget items
     * @param currencyCode Current currency code
     * @return URI of the backup file or null if backup failed
     */
    fun createBackup(
        categories: List<Category>,
        transactions: List<Transaction>,
        budget: Double,
        budgetItems: List<BudgetItem>,
        currencyCode: String?
    ): Uri? {
        try {
            // Create backup data object
            val backupData = BackupData(
                categories = categories,
                transactions = transactions,
                budget = budget,
                budgetItems = budgetItems,
                currencyCode = currencyCode
            )
            
            // Convert to JSON
            val backupJson = gson.toJson(backupData)
            Log.d(TAG, "Backup JSON created, length: ${backupJson.length}")
            
            // Create backup file in internal storage
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "tally_backup_$timestamp.json"
            
            // Ensure directories exist
            val filesDir = context.filesDir
            if (!filesDir.exists()) {
                Log.d(TAG, "Creating filesDir: ${filesDir.mkdirs()}")
            }
            
            Log.d(TAG, "Files directory exists: ${filesDir.exists()}, path: ${filesDir.absolutePath}")
            
            // Save file to internal storage
            val backupFile = File(context.filesDir, backupFileName)
            Log.d(TAG, "Backup file path: ${backupFile.absolutePath}")
            
            FileOutputStream(backupFile).use { output ->
                output.write(backupJson.toByteArray())
                output.flush()
            }
            
            Log.d(TAG, "Backup file created, size: ${backupFile.length()} bytes")
            
            // Return URI for the file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
            
            Log.d(TAG, "Backup file URI: $uri")
            
            // Send notification
            notificationUtils.notifyBackupComplete(true)
            
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}", e)
            e.printStackTrace()
            
            // Send failure notification
            notificationUtils.notifyBackupComplete(false)
            
            return null
        }
    }
    
    /**
     * Restores data from a backup file in internal storage.
     * 
     * @return Backup data object or null if restore failed
     */
    fun restoreFromLatestBackup(): BackupData? {
        try {
            // Look for backup files in internal storage
            val backupFiles = context.filesDir.listFiles { file ->
                file.name.startsWith("tally_backup_") && file.name.endsWith(".json")
            }
            
            Log.d(TAG, "Found ${backupFiles?.size ?: 0} backup files in internal storage")
            
            if (backupFiles.isNullOrEmpty()) {
                Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show()
                notificationUtils.notifyRestoreComplete(false)
                return null
            }
            
            // Get the most recent backup file
            val latestBackupFile = backupFiles.maxByOrNull { it.lastModified() }
                ?: return null
                
            Log.d(TAG, "Latest backup file: ${latestBackupFile.name}, size: ${latestBackupFile.length()} bytes")
                
            // Read file content
            val backupJson = FileInputStream(latestBackupFile).bufferedReader().use { it.readText() }
            
            // Parse JSON to BackupData
            val type = object : TypeToken<BackupData>() {}.type
            val result = gson.fromJson<BackupData>(backupJson, type)
            
            // Send success notification
            notificationUtils.notifyRestoreComplete(true)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            e.printStackTrace()
            
            // Send failure notification
            notificationUtils.notifyRestoreComplete(false)
            
            return null
        }
    }
    
    /**
     * Lists all available backup files.
     * 
     * @return List of backup files with their timestamps
     */
    fun listBackupFiles(): List<Pair<File, Date>> {
        val backupFiles = context.filesDir.listFiles { file -> 
            file.name.startsWith("tally_backup_") && file.name.endsWith(".json")
        } ?: return emptyList()
        
        Log.d(TAG, "Found ${backupFiles.size} backup files in internal storage")
        
        return backupFiles.map { file ->
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = file.name
            val dateString = fileName.substring(13, 28) // Extract date part from filename
            val date = try {
                dateFormat.parse(dateString) ?: Date(file.lastModified())
            } catch (e: Exception) {
                Date(file.lastModified())
            }
            
            Pair(file, date)
        }.sortedByDescending { it.second } // Sort by date, newest first
    }
    
    /**
     * Restores data from a specific backup file.
     * 
     * @param backupFile Backup file to restore from
     * @return Backup data object or null if restore failed
     */
    fun restoreFromFile(backupFile: File): BackupData? {
        try {
            // Read file content
            val backupJson = FileInputStream(backupFile).bufferedReader().use { it.readText() }
            Log.d(TAG, "Read backup file content, length: ${backupJson.length}")
            
            // Parse JSON to BackupData
            val type = object : TypeToken<BackupData>() {}.type
            val result = gson.fromJson<BackupData>(backupJson, type)
            
            // Send success notification
            notificationUtils.notifyRestoreComplete(true)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Restore from file failed: ${e.message}", e)
            e.printStackTrace()
            
            // Send failure notification
            notificationUtils.notifyRestoreComplete(false)
            
            return null
        }
    }
    
    /**
     * Deletes a backup file.
     * 
     * @param backupFile Backup file to delete
     * @return True if deletion was successful, false otherwise
     */
    fun deleteBackupFile(backupFile: File): Boolean {
        return backupFile.exists() && backupFile.delete()
    }
} 
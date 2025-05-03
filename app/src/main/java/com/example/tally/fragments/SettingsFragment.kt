package com.example.tally.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tally.R
import com.example.tally.adapters.BackupFilesAdapter
import com.example.tally.adapters.CurrencyAdapter
import com.example.tally.databinding.DialogBackupSelectionBinding
import com.example.tally.databinding.FragmentSettingsBinding
import com.example.tally.models.Currency
import com.example.tally.receivers.DailyReminderReceiver
import com.example.tally.utils.CurrencyManager
import com.example.tally.utils.PinManager
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var currencyManager: CurrencyManager
    private lateinit var pinManager: PinManager

    private var currencyDialog: AlertDialog? = null
    private var backupDialog: AlertDialog? = null
    private var selectedBackupFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currencyManager = CurrencyManager(requireContext())
        pinManager = PinManager(requireContext())

        // Initialize UI with current settings
        updateCurrencyDisplay()
        initializeDailyReminderUI()

        setupClickListeners()

        // Save Settings button
        binding.btnSaveSettings.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.settings_saved), Snackbar.LENGTH_SHORT).show()
        }

        // Observe backup files list
        viewModel.backupFiles.observe(viewLifecycleOwner) { backupFiles ->
            // Update UI based on available backups
            binding.btnRestoreData.isEnabled = backupFiles.isNotEmpty()
        }
    }

    private fun updateCurrencyDisplay() {
        val currentCurrency = currencyManager.getCurrentCurrency()
        binding.tvCurrentCurrency.text = currentCurrency.toString()
    }

    private fun initializeDailyReminderUI() {
        // Set initial visibility of reminder time selection based on saved preference
        val isReminderEnabled = viewModel.isReminderEnabled()
        binding.switchDailyReminder.isChecked = isReminderEnabled
        binding.btnReminderTime.visibility = if (isReminderEnabled) View.VISIBLE else View.GONE
        
        // Initialize the time display
        val hour = viewModel.getReminderHour()
        val minute = viewModel.getReminderMinute()
        updateReminderTimeDisplay(hour, minute)
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Currency selection
        binding.btnCurrency.setOnClickListener {
            showCurrencySelectionDialog()
        }

        // Backup Data button
        binding.btnBackupData.setOnClickListener {
            showBackupConfirmationDialog()
        }

        // Restore Data button
        binding.btnRestoreData.setOnClickListener {
            showBackupSelectionDialog()
        }

        // Export to Excel button
        binding.btnExportExcel.setOnClickListener {
            exportTransactionsToExcel()
        }

        // Notifications button
        binding.btnNotifications.setOnClickListener {
            showNotificationSettings()
        }
        
        // Daily Reminder toggle
        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            toggleDailyReminder(isChecked)
        }
        
        // Reminder Time selection
        binding.btnReminderTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showCurrencySelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_currency_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.currencyRecyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create adapter with all available currencies
        val adapter = CurrencyAdapter(Currency.availableCurrencies) { selectedCurrency ->
            // Save the selected currency
            currencyManager.setCurrentCurrency(selectedCurrency)

            // Update UI
            updateCurrencyDisplay()

            // Show confirmation
            Toast.makeText(
                requireContext(),
                "Currency changed to ${selectedCurrency.name}",
                Toast.LENGTH_SHORT
            ).show()

            // Dismiss dialog
            currencyDialog?.dismiss()
        }

        recyclerView.adapter = adapter

        // Show dialog
        currencyDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        currencyDialog?.show()
    }

    private fun showBackupConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.backup_title))
            .setMessage(getString(R.string.backup_message))
            .setPositiveButton(getString(R.string.backup)) { _, _ ->
                backupData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showBackupSelectionDialog() {
        val backupFiles = viewModel.backupFiles.value ?: emptyList()

        if (backupFiles.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_backups), Toast.LENGTH_SHORT).show()
            return
        }

        // Inflate the dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_backup_selection, null)

        // Get views
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.backupRecyclerView)
        val noBackupsMessage = dialogView.findViewById<View>(R.id.tvNoBackupsMessage)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnRestore = dialogView.findViewById<Button>(R.id.btnRestore)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Setup adapter
        val adapter = BackupFilesAdapter(backupFiles) { file ->
            selectedBackupFile = file
        }
        recyclerView.adapter = adapter

        // Select the most recent backup by default
        adapter.selectMostRecentBackup()

        // Create and show dialog
        backupDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Setup buttons
        btnCancel.setOnClickListener {
            backupDialog?.dismiss()
        }

        btnRestore.setOnClickListener {
            val fileToRestore = adapter.getSelectedBackupFile()
            if (fileToRestore != null) {
                backupDialog?.dismiss()
                showRestoreConfirmationDialog(fileToRestore)
            } else {
                Toast.makeText(requireContext(), getString(R.string.select_backup_file), Toast.LENGTH_SHORT).show()
            }
        }

        // Show empty state if needed
        if (backupFiles.isEmpty()) {
            recyclerView.visibility = View.GONE
            noBackupsMessage.visibility = View.VISIBLE
            btnRestore.isEnabled = false
        } else {
            recyclerView.visibility = View.VISIBLE
            noBackupsMessage.visibility = View.GONE
            btnRestore.isEnabled = true
        }

        backupDialog?.show()
    }

    private fun showRestoreOptionsDialog() {
        val backupFiles = viewModel.backupFiles.value ?: emptyList()

        if (backupFiles.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_backups), Toast.LENGTH_SHORT).show()
            return
        }

        // Create a list of backup options with dates
        val dateFormat = SimpleDateFormat("MMM dd, yyyy (HH:mm)", Locale.getDefault())
        val options = backupFiles.map {
            "Backup from ${dateFormat.format(it.second)}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_backup))
            .setItems(options) { _, which ->
                val selectedBackup = backupFiles[which].first
                showRestoreConfirmationDialog(selectedBackup)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showRestoreConfirmationDialog(backupFile: File? = null) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.restore_title))
            .setMessage(getString(R.string.restore_message))
            .setPositiveButton(getString(R.string.restore)) { _, _ ->
                if (backupFile != null) {
                    restoreFromFile(backupFile)
                } else {
                    restoreData()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf(
            getString(R.string.light), 
            getString(R.string.dark), 
            getString(R.string.system_default)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_theme))
            .setItems(themes) { _, which ->
                val themeName = themes[which]
                Snackbar.make(binding.root, getString(R.string.theme_set, themeName), Snackbar.LENGTH_SHORT).show()

                // Apply selected theme
                when (which) {
                    0 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) // Light
                    1 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) // Dark
                    2 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // System Default
                }
            }
            .show()
    }

    private fun showNotificationSettings() {
        // Here you would typically navigate to notification settings
        findNavController().navigate(R.id.action_settingsFragment_to_notificationsFragment)
    }

    private fun backupData() {
        val backupUri = viewModel.backupData()

        if (backupUri != null) {
            // Show success message with a button to share the backup
            val snackbar = Snackbar.make(
                binding.root,
                getString(R.string.backup_success),
                Snackbar.LENGTH_LONG
            )

            snackbar.setAction(getString(R.string.share)) {
                // Share the backup file
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "application/json"
                shareIntent.putExtra(Intent.EXTRA_STREAM, backupUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
            }

            snackbar.show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.backup_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreData() {
        val success = viewModel.restoreData()

        if (success) {
            // Show success message and refresh data
            Snackbar.make(binding.root, getString(R.string.restore_success), Snackbar.LENGTH_SHORT).show()
            viewModel.refreshData()
            updateCurrencyDisplay()
        } else {
            Toast.makeText(requireContext(), getString(R.string.restore_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreFromFile(backupFile: File) {
        val success = viewModel.restoreFromFile(backupFile)

        if (success) {
            // Show success message and refresh data
            Snackbar.make(binding.root, getString(R.string.restore_success), Snackbar.LENGTH_SHORT).show()
            viewModel.refreshData()
            updateCurrencyDisplay()
        } else {
            Toast.makeText(requireContext(), getString(R.string.restore_invalid), Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportTransactionsToExcel() {
        val transactions = viewModel.getAllTransactions()
        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_transactions), Toast.LENGTH_SHORT).show()
            return
        }

        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Transactions")

        val headerRow = sheet.createRow(0)
        val headers = listOf("ID", "Title", "Amount", "Category ID", "Type", "Description", "Date")
        for ((index, header) in headers.withIndex()) {
            headerRow.createCell(index).setCellValue(header)
        }

        for ((i, txn) in transactions.withIndex()) {
            val row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(txn.id)
            row.createCell(1).setCellValue(txn.title)
            row.createCell(2).setCellValue(txn.amount)
            row.createCell(3).setCellValue(txn.categoryId)
            row.createCell(4).setCellValue(txn.type)
            row.createCell(5).setCellValue(txn.description)
            row.createCell(6).setCellValue(txn.date?.toString() ?: "")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "transactions_${timestamp}.xlsx"

        // Save to Downloads folder
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            Toast.makeText(requireContext(), "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currencyDialog?.dismiss()
        currencyDialog = null
        backupDialog?.dismiss()
        backupDialog = null
        _binding = null
    }
    
    // Daily Reminder Functions
    
    private fun toggleDailyReminder(enabled: Boolean) {
        val reminderTimeContainer = binding.btnReminderTime
        viewModel.setReminderEnabled(enabled)
        
        if (enabled) {
            reminderTimeContainer.visibility = View.VISIBLE
            // Get saved time or use default
            val savedHour = viewModel.getReminderHour()
            val savedMinute = viewModel.getReminderMinute()
            updateReminderTimeDisplay(savedHour, savedMinute)
            scheduleReminder(savedHour, savedMinute)
        } else {
            reminderTimeContainer.visibility = View.GONE
            cancelReminder()
        }
    }
    
    private fun showTimePickerDialog() {
        val currentHour = viewModel.getReminderHour()
        val currentMinute = viewModel.getReminderMinute()
        
        val timePickerDialog = android.app.TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                updateReminderTimeDisplay(hourOfDay, minute)
                viewModel.saveReminderTime(hourOfDay, minute)
                scheduleReminder(hourOfDay, minute)
            },
            currentHour,
            currentMinute,
            false
        )
        
        timePickerDialog.show()
    }
    
    private fun updateReminderTimeDisplay(hour: Int, minute: Int) {
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        
        binding.tvReminderTimeValue.text = timeFormat.format(calendar.time)
    }
    
    private fun scheduleReminder(hour: Int, minute: Int) {
        // Create a notification channel (required for Android 8.0+)
        createNotificationChannel()
        
        // Schedule daily alarm for reminder
        val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val reminderIntent = android.content.Intent(requireContext(), DailyReminderReceiver::class.java)
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(),
            0,
            reminderIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        // Set the alarm to start at specified time
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        
        // If the time is already passed for today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        // Schedule the alarm to repeat daily
        alarmManager.setRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        
        Snackbar.make(binding.root, getString(R.string.daily_reminder_set, binding.tvReminderTimeValue.text), Snackbar.LENGTH_SHORT).show()
    }
    
    private fun cancelReminder() {
        val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val reminderIntent = android.content.Intent(requireContext(), DailyReminderReceiver::class.java)
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(),
            0,
            reminderIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        // Cancel the alarm
        alarmManager.cancel(pendingIntent)
        
        Snackbar.make(binding.root, getString(R.string.daily_reminder_cancelled), Snackbar.LENGTH_SHORT).show()
    }
    
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "daily_reminder_channel"
            val channelName = "Daily Reminder"
            val description = "Channel for daily income/expense entry reminders"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            
            val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                this.description = description
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            
            val notificationManager = requireContext().getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
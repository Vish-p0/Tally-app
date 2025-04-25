package com.example.tally.fragments

import android.content.Intent
import android.os.Bundle
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
import com.example.tally.utils.CurrencyManager
import com.example.tally.utils.PinManager
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
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
        
        setupClickListeners()

        // Save Settings button
        binding.btnSaveSettings.setOnClickListener {
            Snackbar.make(binding.root, "Settings saved successfully", Snackbar.LENGTH_SHORT).show()
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

        // Theme button
        binding.btnTheme.setOnClickListener {
            showThemeSelectionDialog()
        }

        // Notifications button
        binding.btnNotifications.setOnClickListener {
            showNotificationSettings()
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
            .setTitle("Backup Data")
            .setMessage("Do you want to create a backup of your data? This will save all your transactions, categories, and budget information to your device's internal storage.")
            .setPositiveButton("Backup") { _, _ ->
                backupData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackupSelectionDialog() {
        val backupFiles = viewModel.backupFiles.value ?: emptyList()
        
        if (backupFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No backup files found", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Please select a backup file", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a list of backup options with dates
        val dateFormat = SimpleDateFormat("MMM dd, yyyy (HH:mm)", Locale.getDefault())
        val options = backupFiles.map { 
            "Backup from ${dateFormat.format(it.second)}"
        }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Backup to Restore")
            .setItems(options) { _, which ->
                val selectedBackup = backupFiles[which].first
                showRestoreConfirmationDialog(selectedBackup)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreConfirmationDialog(backupFile: File? = null) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Data")
            .setMessage("This will replace all your current data with the selected backup. Are you sure you want to continue?")
            .setPositiveButton("Restore") { _, _ ->
                if (backupFile != null) {
                    restoreFromFile(backupFile)
                } else {
                    restoreData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Light", "Dark", "System Default")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Theme")
            .setItems(themes) { _, which ->
                val themeName = themes[which]
                Snackbar.make(binding.root, "Theme set to $themeName", Snackbar.LENGTH_SHORT).show()
                // Implement theme change logic here
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
                "Backup created successfully",
                Snackbar.LENGTH_LONG
            )
            
            snackbar.setAction("Share") {
                // Share the backup file
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "application/json"
                shareIntent.putExtra(Intent.EXTRA_STREAM, backupUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
            }
            
            snackbar.show()
        } else {
            Toast.makeText(requireContext(), "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreData() {
        val success = viewModel.restoreData()
        
        if (success) {
            // Show success message and refresh data
            Snackbar.make(binding.root, "Data restored successfully", Snackbar.LENGTH_SHORT).show()
            viewModel.refreshData()
            updateCurrencyDisplay()
        } else {
            Toast.makeText(requireContext(), "Restore failed. No valid backup file found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreFromFile(backupFile: File) {
        val success = viewModel.restoreFromFile(backupFile)
        
        if (success) {
            // Show success message and refresh data
            Snackbar.make(binding.root, "Data restored successfully", Snackbar.LENGTH_SHORT).show()
            viewModel.refreshData()
            updateCurrencyDisplay()
        } else {
            Toast.makeText(requireContext(), "Restore failed. Invalid backup file.", Toast.LENGTH_SHORT).show()
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
} 
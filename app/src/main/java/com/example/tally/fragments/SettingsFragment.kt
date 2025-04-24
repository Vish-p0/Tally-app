package com.example.tally.fragments

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tally.R
import com.example.tally.adapters.CurrencyAdapter
import com.example.tally.databinding.FragmentSettingsBinding
import com.example.tally.models.Currency
import com.example.tally.utils.CurrencyManager
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinanceViewModel by activityViewModels()
    private lateinit var currencyManager: CurrencyManager

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

        // Initialize UI with current settings
        updateCurrencyDisplay()
        
        setupClickListeners()

        // Save Settings button
        binding.btnSaveSettings.setOnClickListener {
            Snackbar.make(binding.root, "Settings saved successfully", Snackbar.LENGTH_SHORT).show()
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
            showRestoreConfirmationDialog()
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
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.currencyRecyclerView)
        
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

    private var currencyDialog: androidx.appcompat.app.AlertDialog? = null

    private fun showBackupConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Backup Data")
            .setMessage("Do you want to create a backup of your data? This will save all your transactions, categories, and budget information.")
            .setPositiveButton("Backup") { _, _ ->
                backupData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRestoreConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Data")
            .setMessage("This will replace all your current data with the most recent backup. Are you sure you want to continue?")
            .setPositiveButton("Restore") { _, _ ->
                restoreData()
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
            // Show success message
            Toast.makeText(requireContext(), "Backup created successfully", Toast.LENGTH_SHORT).show()
            
            // Share the backup file
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/json"
            shareIntent.putExtra(Intent.EXTRA_STREAM, backupUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share Backup File"))
        } else {
            Toast.makeText(requireContext(), "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreData() {
        val success = viewModel.restoreData()
        
        if (success) {
            Toast.makeText(requireContext(), "Data restored successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Restore failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currencyDialog?.dismiss()
        currencyDialog = null
        _binding = null
    }
} 
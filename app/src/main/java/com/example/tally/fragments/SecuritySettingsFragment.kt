package com.example.tally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.utils.PinManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SecuritySettingsFragment : Fragment() {

    private lateinit var pinManager: PinManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_security_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        pinManager = PinManager(requireContext())
        
        // Set up back button
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // PIN Management button
        view.findViewById<View>(R.id.btnPinManagement).setOnClickListener {
            showPinOptions()
        }
        
        // Security Question button
        view.findViewById<View>(R.id.btnSecurityQuestion).setOnClickListener {
            if (pinManager.isPinCreated()) {
                // Request PIN verification before changing security question
                val bundle = Bundle().apply {
                    putBoolean("isChangingSecurityQuestion", true)
                }
                findNavController().navigate(R.id.action_securitySettingsFragment_to_pinEntryFragment, bundle)
            } else {
                // Show a message that PIN must be set first
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("PIN Required")
                    .setMessage("You need to set up a PIN before you can create a security question.")
                    .setPositiveButton("Set PIN") { _, _ ->
                        findNavController().navigate(R.id.action_securitySettingsFragment_to_pinSetupFragment)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        
        // Biometric Authentication (placeholder for future implementation)
        view.findViewById<View>(R.id.btnBiometric).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Coming Soon")
                .setMessage("Biometric authentication will be available in a future update.")
                .setPositiveButton("OK", null)
                .show()
        }
        
        // App Lock settings
        view.findViewById<View>(R.id.btnAppLock).setOnClickListener {
            showAppLockOptions()
        }
    }
    
    private fun showPinOptions() {
        val options = if (pinManager.isPinCreated()) {
            arrayOf("Change PIN", "Reset PIN")
        } else {
            arrayOf("Set up PIN")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("PIN Management")
            .setItems(options) { _, which ->
                when {
                    options[which] == "Set up PIN" -> {
                        findNavController().navigate(R.id.action_securitySettingsFragment_to_pinSetupFragment)
                    }
                    options[which] == "Change PIN" -> {
                        val bundle = Bundle().apply {
                            putBoolean("isChangingPin", true)
                        }
                        findNavController().navigate(R.id.action_securitySettingsFragment_to_pinEntryFragment, bundle)
                    }
                    options[which] == "Reset PIN" -> {
                        findNavController().navigate(R.id.action_securitySettingsFragment_to_pinRecoveryFragment)
                    }
                }
            }
            .show()
    }
    
    private fun showAppLockOptions() {
        val options = arrayOf(
            "Always (require PIN every time)",
            "After 1 minute",
            "After 5 minutes",
            "After 15 minutes",
            "Never"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("App Lock Timeout")
            .setItems(options) { _, which ->
                // Save the selected option (implementation would depend on app's preferences system)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Setting Applied")
                    .setMessage("App lock timeout has been set to: ${options[which]}")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .show()
    }
} 
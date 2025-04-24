package com.example.tally.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.utils.PinManager
import com.example.tally.views.NumericKeypadView
import com.example.tally.views.PinIndicatorView
import java.util.concurrent.TimeUnit

class PinEntryFragment : Fragment() {

    private lateinit var pinManager: PinManager
    private lateinit var pinIndicator: PinIndicatorView
    private lateinit var keypad: NumericKeypadView
    private lateinit var errorText: TextView
    private lateinit var remainingAttemptsText: TextView
    private lateinit var cooldownText: TextView
    private lateinit var forgotPinText: TextView
    private lateinit var titleText: TextView

    private val enteredPin = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private var cooldownRunnable: Runnable? = null
    
    // Special modes
    private var isChangingPin = false
    private var isChangingSecurityQuestion = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pin_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize components
        pinManager = PinManager(requireContext())
        pinIndicator = view.findViewById(R.id.pinIndicator)
        keypad = view.findViewById(R.id.numericKeypad)
        errorText = view.findViewById(R.id.errorText)
        remainingAttemptsText = view.findViewById(R.id.remainingAttemptsText)
        cooldownText = view.findViewById(R.id.cooldownText)
        forgotPinText = view.findViewById(R.id.forgotPinText)
        titleText = view.findViewById(R.id.titleText)

        // Check for arguments
        arguments?.let { args ->
            isChangingPin = args.getBoolean("isChangingPin", false)
            isChangingSecurityQuestion = args.getBoolean("isChangingSecurityQuestion", false)
            
            // Update title based on mode
            if (isChangingPin) {
                titleText.text = "Verify PIN"
            } else if (isChangingSecurityQuestion) {
                titleText.text = "Verify PIN"
            }
        }
        
        // If no PIN has been set, navigate to PIN setup
        if (!pinManager.isPinCreated()) {
            findNavController().navigate(R.id.action_pinEntryFragment_to_pinSetupFragment)
            return
        }

        // Set up keypad listeners
        setupKeypad()

        // Set up forgot PIN
        forgotPinText.setOnClickListener {
            findNavController().navigate(R.id.action_pinEntryFragment_to_pinRecoveryFragment)
        }

        // Check for cooldown
        if (pinManager.isCooldownActive()) {
            showCooldown()
        } else {
            hideMessages()
        }

        // Disable back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing to disable back press
                }
            }
        )
    }

    private fun setupKeypad() {
        // Digit key press listener
        keypad.setOnKeyPressListener { digit ->
            if (enteredPin.length < 4) {
                enteredPin.append(digit)
                pinIndicator.setFilledCount(enteredPin.length)
                
                // If 4 digits entered, validate PIN
                if (enteredPin.length == 4) {
                    validatePin()
                }
            }
        }

        // Backspace key press listener
        keypad.setOnBackspaceListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                pinIndicator.setFilledCount(enteredPin.length)
                hideMessages()
            }
        }
    }

    private fun validatePin() {
        val isValid = pinManager.verifyPin(enteredPin.toString())
        
        if (isValid) {
            // Handle different navigation based on the mode
            when {
                isChangingPin -> {
                    // Navigate to pin setup for changing PIN
                    val bundle = Bundle().apply {
                        putBoolean("isChangingPin", true)
                    }
                    findNavController().navigate(R.id.action_pinEntryFragment_to_pinSetupFragment, bundle)
                }
                isChangingSecurityQuestion -> {
                    // Navigate to pin setup with flag to show only security question section
                    val bundle = Bundle().apply {
                        putBoolean("isChangingSecurityQuestion", true)
                    }
                    findNavController().navigate(R.id.action_pinEntryFragment_to_pinSetupFragment, bundle)
                }
                else -> {
                    // Regular PIN verification - navigate to main screen
                    findNavController().navigate(R.id.action_pinEntryFragment_to_homeFragment)
                }
            }
        } else {
            // Check for cooldown activation
            if (pinManager.isCooldownActive()) {
                showCooldown()
            } else {
                showError()
                resetPin()
            }
        }
    }

    private fun resetPin() {
        enteredPin.clear()
        pinIndicator.setFilledCount(0)
    }

    private fun showError() {
        errorText.visibility = View.VISIBLE
        remainingAttemptsText.visibility = View.VISIBLE
        remainingAttemptsText.text = "Attempts remaining: ${pinManager.getRemainingAttempts()}"
    }

    private fun showCooldown() {
        errorText.visibility = View.VISIBLE
        cooldownText.visibility = View.VISIBLE
        keypad.setButtonsEnabled(false)

        // Start countdown timer
        startCooldownTimer()
    }

    private fun hideMessages() {
        errorText.visibility = View.INVISIBLE
        remainingAttemptsText.visibility = View.INVISIBLE
        cooldownText.visibility = View.INVISIBLE
    }

    private fun startCooldownTimer() {
        // Cancel any existing timer
        cooldownRunnable?.let { handler.removeCallbacks(it) }
        
        // Create new timer
        cooldownRunnable = object : Runnable {
            override fun run() {
                if (view == null || !isAdded) return
                
                val remainingTime = pinManager.getRemainingCooldownTime()
                if (remainingTime <= 0) {
                    // Cooldown finished
                    cooldownText.visibility = View.INVISIBLE
                    keypad.setButtonsEnabled(true)
                    resetPin()
                } else {
                    // Update cooldown text
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) - 
                            TimeUnit.MINUTES.toSeconds(minutes)
                    cooldownText.text = "Too many attempts. Try again in ${minutes}:${String.format("%02d", seconds)}"
                    
                    // Schedule next update
                    handler.postDelayed(this, 1000)
                }
            }
        }
        
        // Start the timer
        handler.post(cooldownRunnable!!)
    }

    override fun onPause() {
        super.onPause()
        cooldownRunnable?.let { handler.removeCallbacks(it) }
    }
} 
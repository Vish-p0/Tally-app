package com.example.tally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.utils.PinManager
import com.example.tally.views.NumericKeypadView
import com.example.tally.views.PinIndicatorView

class PinSetupFragment : Fragment() {

    private lateinit var pinManager: PinManager
    private lateinit var pinIndicator: PinIndicatorView
    private lateinit var keypad: NumericKeypadView
    private lateinit var stepText: TextView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var errorText: TextView
    private lateinit var securityQuestionLayout: LinearLayout
    private lateinit var securityQuestionSpinner: Spinner
    private lateinit var securityAnswerInput: EditText
    private lateinit var saveButton: Button

    private val enteredPin = StringBuilder()
    private var confirmPin = StringBuilder()
    private var isConfirmStep = false
    private var isChangingPin = false
    private var isChangingSecurityQuestion = false
    
    // Security questions
    private val securityQuestions = arrayOf(
        "What was your childhood nickname?",
        "What is the name of your first pet?",
        "In what city were you born?",
        "What is your mother's maiden name?",
        "What was your favorite food as a child?",
        "What was the make of your first car?",
        "What was the name of your elementary school?"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pin_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize components
        pinManager = PinManager(requireContext())
        pinIndicator = view.findViewById(R.id.pinIndicator)
        keypad = view.findViewById(R.id.numericKeypad)
        stepText = view.findViewById(R.id.stepText)
        titleText = view.findViewById(R.id.titleText)
        subtitleText = view.findViewById(R.id.subtitleText)
        errorText = view.findViewById(R.id.errorText)
        securityQuestionLayout = view.findViewById(R.id.securityQuestionLayout)
        securityQuestionSpinner = view.findViewById(R.id.securityQuestionSpinner)
        securityAnswerInput = view.findViewById(R.id.securityAnswerInput)
        saveButton = view.findViewById(R.id.saveButton)

        // Check arguments
        arguments?.let { args ->
            isChangingPin = args.getBoolean("isChangingPin", false)
            isChangingSecurityQuestion = args.getBoolean("isChangingSecurityQuestion", false)
            
            // Update UI based on mode
            if (isChangingPin) {
                titleText.text = "Change Your PIN"
                subtitleText.text = "Enter a new 4-digit PIN"
            } else if (isChangingSecurityQuestion) {
                showSecurityQuestionSectionOnly()
                return
            }
        }
        
        // Setup security question spinner
        setupSecurityQuestionSpinner()

        // Set up keypad listeners
        setupKeypad()

        // Set up save button
        saveButton.setOnClickListener {
            saveSecurityQuestion()
        }
    }

    private fun showSecurityQuestionSectionOnly() {
        // Hide PIN entry UI
        keypad.visibility = View.GONE
        pinIndicator.visibility = View.GONE
        stepText.visibility = View.GONE
        
        // Update texts
        titleText.text = "Security Question"
        subtitleText.text = "Update your security question in case you forget your PIN"
        
        // Show security question section
        securityQuestionLayout.visibility = View.VISIBLE
    }
    
    private fun setupSecurityQuestionSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            securityQuestions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        securityQuestionSpinner.adapter = adapter
    }

    private fun setupKeypad() {
        // Digit key press listener
        keypad.setOnKeyPressListener { digit ->
            if (!isConfirmStep) {
                // First PIN entry
                if (enteredPin.length < 4) {
                    enteredPin.append(digit)
                    pinIndicator.setFilledCount(enteredPin.length)
                    
                    // If 4 digits entered, move to confirm step
                    if (enteredPin.length == 4) {
                        moveToConfirmStep()
                    }
                }
            } else {
                // Confirm PIN entry
                if (confirmPin.length < 4) {
                    confirmPin.append(digit)
                    pinIndicator.setFilledCount(confirmPin.length)
                    
                    // If 4 digits entered, validate PINs match
                    if (confirmPin.length == 4) {
                        validatePins()
                    }
                }
            }
        }

        // Backspace key press listener
        keypad.setOnBackspaceListener {
            if (!isConfirmStep) {
                if (enteredPin.isNotEmpty()) {
                    enteredPin.deleteCharAt(enteredPin.length - 1)
                    pinIndicator.setFilledCount(enteredPin.length)
                }
            } else {
                if (confirmPin.isNotEmpty()) {
                    confirmPin.deleteCharAt(confirmPin.length - 1)
                    pinIndicator.setFilledCount(confirmPin.length)
                }
            }
            
            errorText.visibility = View.INVISIBLE
        }
    }

    private fun moveToConfirmStep() {
        isConfirmStep = true
        stepText.text = "Confirm PIN"
        pinIndicator.setFilledCount(0)
    }

    private fun validatePins() {
        if (enteredPin.toString() == confirmPin.toString()) {
            // PINs match
            if (isChangingPin) {
                // Just save the PIN and return to settings
                pinManager.savePin(enteredPin.toString())
                findNavController().navigateUp()
            } else {
                // Show security question section for new PIN setup
                showSecurityQuestionSection()
            }
        } else {
            // PINs don't match, show error
            errorText.visibility = View.VISIBLE
            
            // Reset confirm PIN
            resetConfirmPin()
        }
    }

    private fun resetConfirmPin() {
        confirmPin.clear()
        pinIndicator.setFilledCount(0)
    }

    private fun showSecurityQuestionSection() {
        // Hide keypad and PIN indicator
        keypad.visibility = View.GONE
        pinIndicator.visibility = View.GONE
        stepText.visibility = View.GONE
        errorText.visibility = View.GONE
        
        // Show security question section
        securityQuestionLayout.visibility = View.VISIBLE
    }

    private fun saveSecurityQuestion() {
        val selectedQuestion = securityQuestionSpinner.selectedItem.toString()
        val answer = securityAnswerInput.text.toString().trim()
        
        if (answer.isEmpty()) {
            securityAnswerInput.error = "Please enter an answer"
            return
        }
        
        // Different behavior based on mode
        if (isChangingSecurityQuestion) {
            // Just save the security question
            pinManager.saveSecurityQuestion(selectedQuestion, answer)
            findNavController().navigateUp()
        } else {
            // Save PIN and security question for new setup
            pinManager.savePin(enteredPin.toString())
            pinManager.saveSecurityQuestion(selectedQuestion, answer)
            
            // Navigate to main screen
            findNavController().navigate(R.id.action_pinSetupFragment_to_homeFragment)
        }
    }
} 
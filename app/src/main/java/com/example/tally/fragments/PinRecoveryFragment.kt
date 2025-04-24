package com.example.tally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tally.R
import com.example.tally.utils.PinManager

class PinRecoveryFragment : Fragment() {

    private lateinit var pinManager: PinManager
    private lateinit var securityQuestionText: TextView
    private lateinit var answerInput: EditText
    private lateinit var errorText: TextView
    private lateinit var verifyButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pin_recovery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize components
        pinManager = PinManager(requireContext())
        securityQuestionText = view.findViewById(R.id.securityQuestionText)
        answerInput = view.findViewById(R.id.answerInput)
        errorText = view.findViewById(R.id.errorText)
        verifyButton = view.findViewById(R.id.verifyButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        // Load security question
        val securityQuestion = pinManager.getSecurityQuestion()
        if (securityQuestion != null) {
            securityQuestionText.text = securityQuestion
        } else {
            // No security question found
            securityQuestionText.text = "No security question has been set."
            answerInput.isEnabled = false
            verifyButton.isEnabled = false
        }

        // Set up button listeners
        verifyButton.setOnClickListener {
            verifySecurityAnswer()
        }

        cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun verifySecurityAnswer() {
        val answer = answerInput.text.toString().trim()
        
        if (answer.isEmpty()) {
            answerInput.error = "Please enter your answer"
            return
        }
        
        // Verify security answer
        val isValid = pinManager.verifySecurityAnswer(answer)
        
        if (isValid) {
            // Reset PIN and navigate to PIN setup
            pinManager.resetPin()
            findNavController().navigate(R.id.action_pinRecoveryFragment_to_pinSetupFragment)
        } else {
            // Show error
            errorText.visibility = View.VISIBLE
            answerInput.text.clear()
        }
    }
} 
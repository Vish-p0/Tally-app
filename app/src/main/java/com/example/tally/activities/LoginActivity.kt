package com.example.tally.activities

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.tally.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: MaterialButton
    private lateinit var buttonSignUp: MaterialButton
    private lateinit var imageViewEye: ImageView
    private lateinit var textViewForgotPassword: TextView
    private lateinit var textViewSignUpBottom: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize views
        initializeViews()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonSignUp = findViewById(R.id.buttonSignUp)
        imageViewEye = findViewById(R.id.imageViewEye)
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword)
        textViewSignUpBottom = findViewById(R.id.textViewSignUpBottom)
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (validateInput(email, password)) {
                // Here you would typically authenticate with your backend
                // For now, we'll just proceed to MainActivity
                proceedToMainActivity()
            }
        }

        buttonSignUp.setOnClickListener {
            navigateToSignup()
        }

        textViewSignUpBottom.setOnClickListener {
            navigateToSignup()
        }

        textViewForgotPassword.setOnClickListener {
            // Handle forgot password click
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showError("Please enter your email")
            return false
        }

        if (password.isEmpty()) {
            showError("Please enter your password")
            return false
        }

        // Add more validation as needed
        return true
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun proceedToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back
    }

    private fun navigateToSignup() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }
}
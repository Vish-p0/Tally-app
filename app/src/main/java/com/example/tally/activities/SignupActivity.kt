package com.example.tally.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.tally.R
import com.google.android.material.button.MaterialButton
import android.widget.Button

class SignupActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobileNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerDay: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner
    private lateinit var ivPasswordToggle: ImageView
    private lateinit var ivConfirmPasswordToggle: ImageView
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView
    private lateinit var tvLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize views
        initViews()

        // Setup date spinners
        setupDateSpinners()

        // Setup password toggles
        setupPasswordToggles()

        // Setup button and text click listeners
        setupClickListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etMobileNumber = findViewById(R.id.et_mobile_number)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        spinnerDay = findViewById(R.id.spinner_day)
        spinnerMonth = findViewById(R.id.spinner_month)
        spinnerYear = findViewById(R.id.spinner_year)
        ivPasswordToggle = findViewById(R.id.iv_password_toggle)
        ivConfirmPasswordToggle = findViewById(R.id.iv_confirm_password_toggle)
        btnSignUp = findViewById(R.id.btn_sign_up)
        tvLogin = findViewById(R.id.tv_login)
        tvLoginLink = findViewById(R.id.tv_login_link)
    }

    private fun setupDateSpinners() {
        // Setup day spinner
        val days = (1..31).map { if (it < 10) "0$it" else "$it" }.toMutableList()
        days.add(0, getString(R.string.day))
        val dayAdapter = ArrayAdapter(this, R.layout.spinner_item, days)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = dayAdapter

        // Setup month spinner
        val months = resources.getStringArray(R.array.months).toMutableList()
        months.add(0, getString(R.string.month))
        val monthAdapter = ArrayAdapter(this, R.layout.spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        // Setup year spinner
        val years = (1930..2023).map { "$it" }.reversed().toMutableList()
        years.add(0, getString(R.string.year))
        val yearAdapter = ArrayAdapter(this, R.layout.spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter
    }

    private fun setupPasswordToggles() {
        // Password toggle
        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility(etPassword, ivPasswordToggle)
        }

        // Confirm password toggle
        ivConfirmPasswordToggle.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivConfirmPasswordToggle)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        if (editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.eye_open)
        } else {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.eye_closed)
        }
        // Keep cursor at the end
        editText.setSelection(editText.text.length)
    }

    private fun setupClickListeners() {
        // Sign up button click listener
        btnSignUp.setOnClickListener {
            if (validateInputs()) {
                // Perform sign up operation
                Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
                // Navigate to the next screen or show success message
            }
        }

        // Login text click listener
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Login link text click listener
        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate full name
        if (etFullName.text.toString().trim().isEmpty()) {
            etFullName.error = getString(R.string.error_full_name)
            isValid = false
        }

        // Validate email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            etEmail.error = getString(R.string.error_email_empty)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.error_email_invalid)
            isValid = false
        }

        // Validate mobile number
        val mobileNumber = etMobileNumber.text.toString().trim()
        if (mobileNumber.isEmpty()) {
            etMobileNumber.error = getString(R.string.error_mobile_empty)
            isValid = false
        } else if (!mobileNumber.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            etMobileNumber.error = getString(R.string.error_mobile_invalid)
            isValid = false
        }

        // Validate date of birth
        if (spinnerDay.selectedItemPosition == 0 ||
            spinnerMonth.selectedItemPosition == 0 ||
            spinnerYear.selectedItemPosition == 0) {
            Toast.makeText(this, getString(R.string.error_dob), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Validate password
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            etPassword.error = getString(R.string.error_password_empty)
            isValid = false
        } else if (password.length < 8) {
            etPassword.error = getString(R.string.error_password_length)
            isValid = false
        }

        // Validate confirm password
        val confirmPassword = etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = getString(R.string.error_confirm_password_empty)
            isValid = false
        } else if (password != confirmPassword) {
            etConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        }

        return isValid
    }
}
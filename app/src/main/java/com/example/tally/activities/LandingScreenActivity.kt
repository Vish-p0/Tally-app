package com.example.tally.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tally.R

class LandingScreenActivity : AppCompatActivity() {

    private lateinit var tvSignup: TextView
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_screen)

        // Initialize views
        tvSignup = findViewById(R.id.tvSignup)
        tvLogin = findViewById(R.id.tvLogin)

        // Set click listeners
        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
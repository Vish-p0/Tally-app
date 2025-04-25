package com.example.tally.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.tally.R
import com.example.tally.databinding.ActivityOnboardingBinding // Import View Binding
import com.example.tally.fragments.HomeFragment

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding // Declare View Binding

    private val titles = listOf(
        R.string.welcome_title,
        R.string.budget_title,
        R.string.transactions_title,
        R.string.security_title
    )
    private val descriptions = listOf(
        R.string.welcome_desc,
        R.string.budget_desc,
        R.string.transactions_desc,
        R.string.security_desc
    )
    private val images = listOf(
        R.drawable.onboarding1,
        R.drawable.onboarding2,
        R.drawable.onboarding3,
        R.drawable.onboarding4
    )
    private var currentScreenIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater) // Initialize View Binding
        setContentView(binding.root)

        loadScreen(currentScreenIndex)

        binding.nextButton.setOnClickListener {
            if (currentScreenIndex < titles.size - 1) {
                currentScreenIndex++
                loadScreen(currentScreenIndex)
            } else {
                // Save that onboarding is complete and launch main activity with pin setup flag
                val preferences = getSharedPreferences("tally_prefs", MODE_PRIVATE)
                preferences.edit().putBoolean("is_first_launch", false).apply()

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("SHOW_PIN_SETUP", true)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun loadScreen(index: Int) {
        binding.titleText.setText(titles[index])
        binding.descriptionText.setText(descriptions[index])
        binding.onboardingImage.setImageResource(images[index])

        // Update dot indicators
        updateDotIndicators(index)
    }

    private fun updateDotIndicators(currentIndex: Int) {
        binding.dotIndicators.removeAllViews()

        for (i in titles.indices) {
            val dot = View(this)
            val size = if (i == currentIndex) 16 else 8
            val layoutParams = LinearLayout.LayoutParams(size, size)
            layoutParams.marginEnd = 8
            dot.layoutParams = layoutParams
            dot.background = ContextCompat.getDrawable(
                this,
                if (i == currentIndex) R.drawable.selected_dot else R.drawable.unselected_dot
            )
            binding.dotIndicators.addView(dot)
        }
    }
}
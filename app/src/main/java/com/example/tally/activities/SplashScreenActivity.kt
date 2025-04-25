package com.example.tally.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.example.tally.R
import kotlin.math.hypot

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var ivLogo: ImageView
    private lateinit var ivCoin1: ImageView
    private lateinit var ivCoin3: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var circleView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Initialize views
        ivLogo = findViewById(R.id.ivLogo)
        ivCoin1 = findViewById(R.id.ivCoin1)
        ivCoin3 = findViewById(R.id.ivCoin3)
        tvAppName = findViewById(R.id.tvAppName)
        circleView = findViewById(R.id.circleView)

        // Set circle color
        circleView.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        circleView.visibility = View.INVISIBLE

        // Start animations after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            startAnimations()
        }, 500)
    }

    private fun startAnimations() {
        // Hide all coins initially
        ivCoin1.visibility = View.INVISIBLE
        ivCoin3.visibility = View.INVISIBLE

        // Only animate first coin drop
        animateCoinDrop(ivCoin1) {
            // After first coin animation completes, show third coin and app name together
            animateLastCoinAndText()
        }
    }

    private fun animateCoinDrop(coinView: ImageView, onComplete: () -> Unit) {
        // Initial setup
        coinView.visibility = View.VISIBLE
        coinView.alpha = 1f
        coinView.translationY = -300f  // Start position well above the piggy bank

        // Calculate target position - center of the piggy bank's top portion
        val targetY = 400f  // This is the translationY needed to position near the piggy bank center

        // Coin drop animation - drops into the piggy bank
        val coinDrop = ObjectAnimator.ofFloat(coinView, "translationY", -300f, targetY).apply {
            duration = 1000
            interpolator = BounceInterpolator()
        }

        // Coin fade animation (coin "enters" the piggy bank)
        val coinFade = ObjectAnimator.ofFloat(coinView, "alpha", 1f, 0f).apply {
            duration = 300
            startDelay = 1000  // Start fading after the bounce completes
        }

        // Run animations
        AnimatorSet().apply {
            playSequentially(coinDrop, coinFade)
            start()
            doOnEnd {
                // Make sure coin is invisible after animation
                coinView.visibility = View.INVISIBLE
                coinView.translationY = -300f  // Reset position
                // Call the completion callback
                onComplete()
            }
        }
    }

    private fun animateLastCoinAndText() {
        // Initial setup for third coin
        ivCoin3.visibility = View.VISIBLE
        ivCoin3.alpha = 0f
        ivCoin3.translationY = 400f  // Position above the piggy bank

        // Third coin fade-in animation
        val coin3Appear = ObjectAnimator.ofFloat(ivCoin3, "alpha", 0f, 1f).apply {
            duration = 800
        }

        // Text animation - same duration as coin appearance
        tvAppName.alpha = 0f
        tvAppName.visibility = View.VISIBLE
        val textAppear = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).apply {
            duration = 800
        }

        // Run animations simultaneously
        AnimatorSet().apply {
            play(coin3Appear).with(textAppear)
            start()
            doOnEnd {
                // Start circle reveal animation after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    animateCircleReveal()
                }, 1000)
            }
        }
    }

    private fun animateCircleReveal() {
        // Make circle view visible
        circleView.visibility = View.VISIBLE

        // Get the center of the view
        val centerX = (circleView.width / 2)
        val centerY = (circleView.height / 2)

        // Calculate the final radius of the circle
        val finalRadius = hypot(centerX.toFloat(), centerY.toFloat())

        // Create the reveal animation
        val circleAnim = ViewAnimationUtils.createCircularReveal(
            circleView,
            centerX,
            centerY,
            0f,
            finalRadius
        )

        // Set animation duration and interpolator
        circleAnim.duration = 1000
        circleAnim.interpolator = AccelerateInterpolator()

        // Start the animation
        circleAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Start landing screen activity
                startLandingScreenActivity()
            }
        })

        circleAnim.start()
    }

    private fun startLandingScreenActivity() {
        val preferences = getSharedPreferences("tally_prefs", MODE_PRIVATE)
        val isFirstLaunch = preferences.getBoolean("is_first_launch", true)
        val pinManager = com.example.tally.utils.PinManager(this)

        when {
            isFirstLaunch -> {
                // Launch onboarding activity
                val intent = Intent(this, OnboardingActivity::class.java)
                startActivity(intent)
            }
            !pinManager.isPinCreated() -> {
                // Launch main activity with pin setup flag
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("SHOW_PIN_SETUP", true)
                startActivity(intent)
            }
            else -> {
                // Launch main activity with pin entry flag
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("SHOW_PIN_ENTRY", true)
                startActivity(intent)
            }
        }

        finish()
    }
}
package com.example.tally.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tally.R
import com.example.tally.repositories.FinanceRepository
import com.example.tally.services.NotificationService
import com.example.tally.utils.PermissionUtils
import com.example.tally.utils.PinManager
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var navController: NavController
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var pinManager: PinManager
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize PinManager
        pinManager = PinManager(this)

        // Initialize BottomNavigationView first
        bottomNav = findViewById(R.id.bottom_nav)

        // Setup NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Link BottomNavigationView to NavController
        bottomNav.setupWithNavController(navController)
        
        // Add destination change listener (after bottomNav is initialized)
        navController.addOnDestinationChangedListener(this)
        
        // Initialize notification service and channels
        val notificationService = NotificationService(this)
        
        // Register permission launcher
        notificationPermissionLauncher = PermissionUtils.registerNotificationPermissionLauncher(this) { isGranted ->
            if (isGranted) {
                // Permission granted, can now show notifications
            } else {
                // Permission denied, show dialog explaining importance
                showNotificationPermissionExplanation()
            }
        }
        
        // Check and request notification permission
        checkNotificationPermission()

        // Handle navigation to PIN entry if needed
        handlePinAuthentication()
    }
    
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Hide bottom navigation on PIN-related screens
        when (destination.id) {
            R.id.pinEntryFragment, R.id.pinSetupFragment, R.id.pinRecoveryFragment -> {
                bottomNav.visibility = android.view.View.GONE
            }
            else -> {
                bottomNav.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun handlePinAuthentication() {
        val showPinEntry = intent.getBooleanExtra("SHOW_PIN_ENTRY", false)
        val startDestination = if (showPinEntry || pinManager.isPinCreated()) {
            R.id.pinEntryFragment
        } else {
            R.id.homeFragment
        }

        // Set the start destination
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph
    }
    
    private fun checkNotificationPermission() {
        if (!PermissionUtils.hasNotificationPermission(this)) {
            showNotificationPermissionRationale()
        }
    }
    
    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Notification Permission")
            .setMessage("Tally needs notification permission to alert you about your budget status and important financial updates.")
            .setPositiveButton("Grant Permission") { _, _ ->
                PermissionUtils.requestNotificationPermission(notificationPermissionLauncher)
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showNotificationPermissionExplanation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Denied")
            .setMessage("Without notification permission, you won't receive budget alerts and important financial updates. You can enable this permission in the app settings.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
package com.example.tally.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tally.R
import com.example.tally.repositories.FinanceRepository
import com.example.tally.viewmodels.FinanceViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Clear existing categories to start fresh
        clearCategories()

        // Setup NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Link BottomNavigationView to NavController
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)
    }
    
    private fun clearCategories() {
        // Create repository instance and clear categories
        val repository = FinanceRepository(applicationContext)
        repository.clearCategories()
    }
}
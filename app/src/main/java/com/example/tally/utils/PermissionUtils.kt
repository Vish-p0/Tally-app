package com.example.tally.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {
    
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For API levels below 33, notification permission is granted by default
            true
        }
    }
    
    fun registerNotificationPermissionLauncher(activity: AppCompatActivity, onResult: (Boolean) -> Unit): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onResult(isGranted)
        }
    }
    
    fun registerNotificationPermissionLauncher(fragment: Fragment, onResult: (Boolean) -> Unit): ActivityResultLauncher<String> {
        return fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onResult(isGranted)
        }
    }
    
    fun requestNotificationPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
} 
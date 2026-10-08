package com.majidbahmani.cesto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val gmailAuthorizer get() = (application as CestoApp).gmailAuthorizer

    // Registered before onCreate returns, so a result after Activity recreation still arrives.
    private val consentLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        gmailAuthorizer.onConsentResult(it)
    }

    // Denied: the daily spending notification just isn't shown. Android stops asking after two denials.
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        gmailAuthorizer.attach(consentLauncher)
        if (savedInstanceState == null) askForNotificationPermission()

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        gmailAuthorizer.detach(consentLauncher)
        super.onDestroy()
    }

    // Android 13+ only; older versions allow notifications by default.
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

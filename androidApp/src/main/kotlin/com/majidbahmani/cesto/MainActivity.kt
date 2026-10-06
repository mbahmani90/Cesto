package com.majidbahmani.cesto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val gmailAuthorizer get() = (application as CestoApp).gmailAuthorizer

    // Registered before onCreate returns, so a result after Activity recreation still arrives.
    private val consentLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        gmailAuthorizer.onConsentResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        gmailAuthorizer.attach(consentLauncher)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        gmailAuthorizer.detach(consentLauncher)
        super.onDestroy()
    }
}

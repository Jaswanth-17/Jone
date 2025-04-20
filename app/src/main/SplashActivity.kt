package com.example.jone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Immediately navigate to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

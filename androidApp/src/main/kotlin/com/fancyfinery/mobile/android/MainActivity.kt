package com.fancyfinery.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fancyfinery.mobile.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // The ACTIVITY, not applicationContext: Credential Manager presents
            // the Google account sheet and needs an Activity to present from.
            // The database and preference factories take applicationContext off
            // it themselves, so nothing long-lived retains this.
            App(this)
        }
    }
}

package com.fancyfinery.mobile.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import com.fancyfinery.mobile.App

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Held as state rather than read once, so a link arriving while the
            // app is already running (onNewIntent) still opens its screen. The
            // activity is singleTask, so that is the normal case rather than an
            // edge one — without this, tapping a reset link with the app open
            // would bring it to the front showing whatever was there before.
            var link by remember { mutableStateOf(intent?.dataString) }

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    newIntent.dataString?.let { link = it }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            // The ACTIVITY, not applicationContext: Credential Manager presents
            // the Google account sheet and needs an Activity to present from.
            // The database and preference factories take applicationContext off
            // it themselves, so nothing long-lived retains this.
            App(context = this, deepLink = link)
        }
    }
}

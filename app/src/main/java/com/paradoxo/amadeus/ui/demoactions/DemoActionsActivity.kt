package com.paradoxo.amadeus.ui.demoactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme

internal class DemoActionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, darkScrim = android.graphics.Color.DKGRAY),
        )
        setContent {
            MaterialTheme {
                DemoActionsScreen(onBack = { finish() })
            }
        }
    }
}

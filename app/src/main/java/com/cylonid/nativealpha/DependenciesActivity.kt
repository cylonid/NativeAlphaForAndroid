package com.cylonid.nativealpha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.about.DependenciesScreen
import com.cylonid.nativealpha.ui.theme.NativeAlphaTheme

class DependenciesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NativeAlphaTheme {
                DependenciesScreen(
                    onNavigateBack = { finish() },
                )
            }
        }
    }
}

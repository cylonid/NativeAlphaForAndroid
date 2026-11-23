package com.cylonid.nativealpha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cylonid.nativealpha.ui.about.AboutScreen
import com.cylonid.nativealpha.ui.theme.NativeAlphaTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NativeAlphaTheme {
                AboutScreen(onNavigateBack = { finish() })
            }
        }
    }
}

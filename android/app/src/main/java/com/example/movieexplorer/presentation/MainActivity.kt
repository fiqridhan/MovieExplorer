package com.example.movieexplorer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD9AD6A),
                    secondary = Color(0xFF9AA9C8),
                    background = Color(0xFF0A101B),
                    surface = Color(0xFF151E2E),
                    surfaceVariant = Color(0xFF243047)
                )
            ) {
                Surface(
                    Modifier.fillMaxSize()
                ) { MovieApp() }
            }
        }
    }
}


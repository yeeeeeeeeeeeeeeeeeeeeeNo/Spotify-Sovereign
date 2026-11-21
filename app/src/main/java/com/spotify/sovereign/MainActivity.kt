package com.spotify.sovereign

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.spotify.sovereign.ui.SpotifyViewModel
import com.spotify.sovereign.ui.screens.MainScreen
import com.spotify.sovereign.ui.theme.SpotifySovereignTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SpotifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsState()
            SpotifySovereignTheme(
                themePreset = state.settings.visual.themePreset,
                customColors = state.settings.visual.customColors,
                trackPalette = state.trackPalette,
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

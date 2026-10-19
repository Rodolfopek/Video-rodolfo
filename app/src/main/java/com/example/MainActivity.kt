package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.VideoLibraryScreen
import com.example.ui.theme.NovaPlayerTheme
import com.example.viewmodel.PlayerViewModel

enum class AppScreen {
    LIBRARY,
    PLAYER
}

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NovaPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    NovaPlayerApp(viewModel = playerViewModel)
                }
            }
        }
    }
}

@Composable
fun NovaPlayerApp(
    viewModel: PlayerViewModel
) {
    var currentScreen by remember { mutableStateOf(AppScreen.LIBRARY) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == AppScreen.PLAYER) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.LIBRARY -> {
                VideoLibraryScreen(
                    viewModel = viewModel,
                    onOpenPlayer = { currentScreen = AppScreen.PLAYER }
                )
            }
            AppScreen.PLAYER -> {
                PlayerScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.saveCurrentPlaybackProgress()
                        viewModel.player.pause()
                        currentScreen = AppScreen.LIBRARY
                    }
                )
            }
        }
    }
}

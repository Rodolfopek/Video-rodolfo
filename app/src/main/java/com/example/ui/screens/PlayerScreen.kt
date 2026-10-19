package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.AspectRatioMode
import com.example.ui.components.BrightnessHud
import com.example.ui.components.CapturedFrameDialog
import com.example.ui.components.DoubleTapRippleFeedback
import com.example.ui.components.GestureDetectorBox
import com.example.ui.components.HudToastMessage
import com.example.ui.components.NightFilterDialog
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.PlayerControlsOverlay
import com.example.ui.components.SeekGestureHud
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SpeedBoostHud
import com.example.ui.components.VolumeHud
import com.example.ui.theme.CyanAccent
import com.example.util.DeviceUtils
import com.example.viewmodel.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val currentVideo by viewModel.currentVideo.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val bufferedPositionMs by viewModel.bufferedPositionMs.collectAsState()

    val areControlsVisible by viewModel.areControlsVisible.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val showRemainingTime by viewModel.showRemainingTime.collectAsState()

    val brightnessFraction by viewModel.brightnessFraction.collectAsState()
    val showBrightnessHud by viewModel.showBrightnessHud.collectAsState()
    val volumeFraction by viewModel.volumeFraction.collectAsState()
    val showVolumeHud by viewModel.showVolumeHud.collectAsState()

    val showSeekHud by viewModel.showSeekHud.collectAsState()
    val seekTargetMs by viewModel.seekTargetMs.collectAsState()
    val seekDeltaMs by viewModel.seekDeltaMs.collectAsState()

    val isSpeedBoostActive by viewModel.isSpeedBoostActive.collectAsState()
    val doubleTapRipple by viewModel.doubleTapRipple.collectAsState()

    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()

    val aspectRatioMode by viewModel.aspectRatioMode.collectAsState()
    val hudToast by viewModel.hudToast.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val nightFilterAlpha by viewModel.nightFilterAlpha.collectAsState()

    val abLoopPointA by viewModel.abLoopPointA.collectAsState()
    val abLoopPointB by viewModel.abLoopPointB.collectAsState()

    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val sleepTimerSecondsRemaining by viewModel.sleepTimerSecondsRemaining.collectAsState()
    val capturedFrame by viewModel.capturedFrame.collectAsState()

    // Dialogs state
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showNightFilterDialog by remember { mutableStateOf(false) }

    // Screen rotation state
    var isLandscape by remember { mutableStateOf(false) }

    // Subtitle picker launcher
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "subtítulo.srt"
            viewModel.setExternalSubtitle(uri, name)
        }
    }

    // Apply brightness to activity window
    LaunchedEffect(brightnessFraction) {
        if (activity != null) {
            DeviceUtils.setActivityBrightness(activity, brightnessFraction)
        }
    }

    // Keep screen on while player is active
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Restore default orientation on exit
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        if (isLocked) {
            viewModel.showHudToast("Desbloquea primero la pantalla")
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Player Surface with Zoom and Pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomScale,
                    scaleY = zoomScale,
                    translationX = panOffset.x,
                    translationY = panOffset.y
                )
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        player = viewModel.player
                        resizeMode = when (aspectRatioMode) {
                            AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        }
                    }
                },
                update = { view ->
                    view.player = viewModel.player
                    view.resizeMode = when (aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Night Filter / Extra Dim Scrim
        if (nightFilterAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = nightFilterAlpha))
            )
        }

        // Gesture Detection Overlay
        GestureDetectorBox(
            isLocked = isLocked,
            onSingleTap = { viewModel.toggleControls() },
            onDoubleTapLeft = { viewModel.onDoubleTapLeft() },
            onDoubleTapRight = { viewModel.onDoubleTapRight() },
            onDoubleTapCenter = { viewModel.togglePlayPause() },
            onLongPressStart = { viewModel.onLongPressStart() },
            onLongPressEnd = { viewModel.onLongPressEnd() },
            onBrightnessChange = { delta -> viewModel.updateBrightness(delta) },
            onVolumeChange = { delta -> viewModel.updateVolume(delta) },
            onSeekStart = { viewModel.onSeekGestureStart() },
            onSeekDelta = { delta -> viewModel.onSeekGestureDelta(delta) },
            onSeekEnd = { viewModel.onSeekGestureEnd() },
            onZoomAndPan = { zoomChange, panChange ->
                viewModel.updateZoomAndPan(zoomChange, panChange)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // HUD Indicators
            Box(modifier = Modifier.fillMaxSize()) {
                // Brightness HUD
                BrightnessHud(
                    visible = showBrightnessHud,
                    brightnessFraction = brightnessFraction,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                )

                // Volume HUD
                VolumeHud(
                    visible = showVolumeHud,
                    volumeFraction = volumeFraction,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                )

                // Seek Preview HUD
                SeekGestureHud(
                    visible = showSeekHud,
                    seekTargetMs = seekTargetMs,
                    seekDeltaMs = seekDeltaMs,
                    totalDurationMs = durationMs,
                    modifier = Modifier.align(Alignment.Center)
                )

                // 2x Speed Boost HUD
                SpeedBoostHud(
                    visible = isSpeedBoostActive,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                )

                // Double Tap Ripples (-10s / +10s)
                if (doubleTapRipple == "-10s") {
                    DoubleTapRippleFeedback(
                        label = doubleTapRipple,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 48.dp)
                    )
                } else if (doubleTapRipple == "+10s") {
                    DoubleTapRippleFeedback(
                        label = doubleTapRipple,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 48.dp)
                    )
                }

                // Toast status message
                HudToastMessage(
                    message = hudToast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp)
                )
            }
        }

        // On-Screen Display (OSD) Controls
        if (!isLocked) {
            PlayerControlsOverlay(
                visible = areControlsVisible,
                title = currentVideo?.title ?: "Reproduciendo Video",
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                showRemainingTime = showRemainingTime,
                zoomScale = zoomScale,
                aspectRatioMode = aspectRatioMode,
                playbackSpeed = playbackSpeed,
                abLoopPointA = abLoopPointA,
                abLoopPointB = abLoopPointB,
                sleepTimerSecondsRemaining = sleepTimerSecondsRemaining,
                onBackClick = onBack,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { pos -> viewModel.seekTo(pos) },
                onRewind10 = { viewModel.seekRelative(-10_000L) },
                onForward10 = { viewModel.seekRelative(10_000L) },
                onPlayPrevious = { viewModel.playPrevious() },
                onPlayNext = { viewModel.playNext() },
                onToggleRemainingTime = { viewModel.toggleRemainingTime() },
                onResetZoom = { viewModel.resetZoom() },
                onLockClick = { viewModel.toggleLock() },
                onRotateClick = {
                    isLandscape = !isLandscape
                    activity?.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                },
                onAspectRatioClick = { viewModel.cycleAspectRatio() },
                onSpeedClick = { showSpeedDialog = true },
                onABLoopClick = { viewModel.toggleABLoop() },
                onScreenshotClick = { viewModel.captureCurrentFrame() },
                onSleepTimerClick = { showSleepTimerDialog = true },
                onSubtitleClick = {
                    subtitlePickerLauncher.launch(arrayOf("*/*"))
                },
                onNightFilterClick = { showNightFilterDialog = true }
            )
        }

        // Lock button when in Locked mode
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.5.dp, CyanAccent.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.toggleLock() },
                    modifier = Modifier.testTag("unlock_screen_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Desbloquear Pantalla",
                        tint = CyanAccent
                    )
                }
            }
        }

        // Dialogs
        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentMinutes = sleepTimerMinutes,
                onTimerSelected = { viewModel.setSleepTimer(it) },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        if (showNightFilterDialog) {
            NightFilterDialog(
                currentAlpha = nightFilterAlpha,
                onAlphaChanged = { viewModel.setNightFilterAlpha(it) },
                onDismiss = { showNightFilterDialog = false }
            )
        }

        capturedFrame?.let { bitmap ->
            CapturedFrameDialog(
                bitmap = bitmap,
                onDismiss = { viewModel.dismissCapturedFrame() }
            )
        }
    }
}

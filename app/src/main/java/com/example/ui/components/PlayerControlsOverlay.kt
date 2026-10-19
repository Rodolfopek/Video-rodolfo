package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioMode
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanAccentGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.util.DeviceUtils
import com.example.util.Formatters
import kotlinx.coroutines.delay

@Composable
fun PlayerControlsOverlay(
    visible: Boolean,
    title: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    showRemainingTime: Boolean,
    zoomScale: Float,
    aspectRatioMode: AspectRatioMode,
    playbackSpeed: Float,
    abLoopPointA: Long?,
    abLoopPointB: Long?,
    sleepTimerSecondsRemaining: Int?,
    onBackClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleRemainingTime: () -> Unit,
    onResetZoom: () -> Unit,
    onLockClick: () -> Unit,
    onRotateClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onABLoopClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onNightFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(100) }
    var currentTimeString by remember { mutableStateOf(DeviceUtils.getCurrentTimeString()) }

    LaunchedEffect(Unit) {
        while (true) {
            batteryLevel = DeviceUtils.getBatteryLevel(context)
            currentTimeString = DeviceUtils.getCurrentTimeString()
            delay(15_000)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        ) {
            // Top Bar
            PlayerTopBar(
                title = title,
                currentTimeString = currentTimeString,
                batteryLevel = batteryLevel,
                zoomScale = zoomScale,
                onBackClick = onBackClick,
                onResetZoom = onResetZoom,
                onSubtitleClick = onSubtitleClick,
                onNightFilterClick = onNightFilterClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            )

            // Center Playback Controls
            PlayerCenterControls(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onTogglePlayPause = onTogglePlayPause,
                onRewind10 = onRewind10,
                onForward10 = onForward10,
                onPlayPrevious = onPlayPrevious,
                onPlayNext = onPlayNext,
                modifier = Modifier.align(Alignment.Center)
            )

            // Bottom Section: Progress Timeline & Tools Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                // Seekbar & Timing
                PlayerTimeline(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    bufferedPositionMs = bufferedPositionMs,
                    showRemainingTime = showRemainingTime,
                    abLoopPointA = abLoopPointA,
                    abLoopPointB = abLoopPointB,
                    onSeekTo = onSeekTo,
                    onToggleRemainingTime = onToggleRemainingTime,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Tools Toolbar
                PlayerBottomToolbar(
                    aspectRatioMode = aspectRatioMode,
                    playbackSpeed = playbackSpeed,
                    abLoopPointA = abLoopPointA,
                    abLoopPointB = abLoopPointB,
                    sleepTimerSecondsRemaining = sleepTimerSecondsRemaining,
                    onLockClick = onLockClick,
                    onRotateClick = onRotateClick,
                    onAspectRatioClick = onAspectRatioClick,
                    onSpeedClick = onSpeedClick,
                    onABLoopClick = onABLoopClick,
                    onScreenshotClick = onScreenshotClick,
                    onSleepTimerClick = onSleepTimerClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    currentTimeString: String,
    batteryLevel: Int,
    zoomScale: Float,
    onBackClick: () -> Unit,
    onResetZoom: () -> Unit,
    onSubtitleClick: () -> Unit,
    onNightFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("player_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Title with Marquee
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee()
        )

        Spacer(modifier = Modifier.width(8.dp))

        // System Battery and Clock Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.BatteryChargingFull,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$batteryLevel%",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentTimeString,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (zoomScale != 1.0f) {
            IconButton(
                onClick = onResetZoom,
                modifier = Modifier.testTag("reset_zoom_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomInMap,
                    contentDescription = "Restablecer Zoom",
                    tint = CyanAccent
                )
            }
        }

        IconButton(
            onClick = onNightFilterClick,
            modifier = Modifier.testTag("night_filter_button")
        ) {
            Icon(
                imageVector = Icons.Filled.NightlightRound,
                contentDescription = "Filtro Nocturno",
                tint = TextPrimary
            )
        }

        IconButton(
            onClick = onSubtitleClick,
            modifier = Modifier.testTag("subtitles_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Subtitles,
                contentDescription = "Subtítulos",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun PlayerCenterControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onTogglePlayPause: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onPlayPrevious,
            modifier = Modifier
                .size(48.dp)
                .testTag("prev_video_button")
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Anterior",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        IconButton(
            onClick = onRewind10,
            modifier = Modifier
                .size(52.dp)
                .testTag("rewind_10_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = "-10s",
                tint = TextPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        // Center Play / Pause button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(CyanAccentGlow)
                .clickable { onTogglePlayPause() }
                .testTag("play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = TextPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        IconButton(
            onClick = onForward10,
            modifier = Modifier
                .size(52.dp)
                .testTag("forward_10_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = "+10s",
                tint = TextPrimary,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        IconButton(
            onClick = onPlayNext,
            modifier = Modifier
                .size(48.dp)
                .testTag("next_video_button")
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Siguiente",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PlayerTimeline(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    showRemainingTime: Boolean,
    abLoopPointA: Long?,
    abLoopPointB: Long?,
    onSeekTo: (Long) -> Unit,
    onToggleRemainingTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val sliderValue = if (isDragging) dragProgress else (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Formatters.formatDuration(if (isDragging) (dragProgress * safeDuration).toLong() else currentPositionMs),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Slider with custom styling
            Slider(
                value = sliderValue,
                onValueChange = {
                    isDragging = true
                    dragProgress = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeekTo((dragProgress * safeDuration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("player_timeline_slider")
            )

            Text(
                text = if (showRemainingTime) {
                    Formatters.formatRemaining(currentPositionMs, durationMs)
                } else {
                    Formatters.formatDuration(durationMs)
                },
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onToggleRemainingTime() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("toggle_remaining_time")
            )
        }

        // A-B Loop indicators if set
        if (abLoopPointA != null || abLoopPointB != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (abLoopPointA != null) "A: ${Formatters.formatDuration(abLoopPointA)}" else "",
                    color = VioletAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (abLoopPointB != null) "B: ${Formatters.formatDuration(abLoopPointB)}" else "",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomToolbar(
    aspectRatioMode: AspectRatioMode,
    playbackSpeed: Float,
    abLoopPointA: Long?,
    abLoopPointB: Long?,
    sleepTimerSecondsRemaining: Int?,
    onLockClick: () -> Unit,
    onRotateClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onABLoopClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Lock
        IconButton(
            onClick = onLockClick,
            modifier = Modifier.testTag("lock_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Bloquear",
                tint = TextPrimary
            )
        }

        // Screen Rotation
        IconButton(
            onClick = onRotateClick,
            modifier = Modifier.testTag("rotate_screen_button")
        ) {
            Icon(
                imageVector = Icons.Filled.ScreenRotation,
                contentDescription = "Rotar Pantalla",
                tint = TextPrimary
            )
        }

        // Aspect Ratio
        IconButton(
            onClick = onAspectRatioClick,
            modifier = Modifier.testTag("aspect_ratio_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AspectRatio,
                    contentDescription = "Relación de Aspecto",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when (aspectRatioMode) {
                        AspectRatioMode.FIT -> "Ajustar"
                        AspectRatioMode.FILL -> "Rellenar"
                        AspectRatioMode.STRETCH -> "Estirar"
                        AspectRatioMode.RATIO_16_9 -> "16:9"
                        AspectRatioMode.RATIO_4_3 -> "4:3"
                    },
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Playback Speed
        IconButton(
            onClick = onSpeedClick,
            modifier = Modifier.testTag("speed_button")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Velocidad",
                    tint = if (playbackSpeed != 1.0f) CyanAccent else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${playbackSpeed}x",
                    color = if (playbackSpeed != 1.0f) CyanAccent else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // A-B Repeat
        IconButton(
            onClick = onABLoopClick,
            modifier = Modifier.testTag("ab_loop_button")
        ) {
            val isLooping = abLoopPointA != null && abLoopPointB != null
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "Bucle A-B",
                    tint = if (isLooping) CyanAccent else if (abLoopPointA != null) VioletAccent else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isLooping) "A-B ON" else if (abLoopPointA != null) "A..." else "A-B",
                    color = if (isLooping) CyanAccent else if (abLoopPointA != null) VioletAccent else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Screenshot
        IconButton(
            onClick = onScreenshotClick,
            modifier = Modifier.testTag("screenshot_button")
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Capturar Fotograma",
                tint = TextPrimary
            )
        }

        // Sleep Timer
        IconButton(
            onClick = onSleepTimerClick,
            modifier = Modifier.testTag("sleep_timer_button")
        ) {
            val isActive = sleepTimerSecondsRemaining != null && sleepTimerSecondsRemaining > 0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = "Temporizador",
                    tint = if (isActive) CyanAccent else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                if (isActive) {
                    val m = (sleepTimerSecondsRemaining ?: 0) / 60
                    val s = (sleepTimerSecondsRemaining ?: 0) % 60
                    Text(
                        text = String.format("%02d:%02d", m, s),
                        color = CyanAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

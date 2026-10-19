package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PlayerDarkBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletAccent
import com.example.util.Formatters

@Composable
fun BrightnessHud(
    visible: Boolean,
    brightnessFraction: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.85f),
        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.85f),
        modifier = modifier
    ) {
        VerticalHudBar(
            icon = {
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = "Brillo",
                    tint = CyanAccent,
                    modifier = Modifier.size(26.dp)
                )
            },
            percentage = (brightnessFraction * 100).toInt(),
            fraction = brightnessFraction,
            accentColor = CyanAccent
        )
    }
}

@Composable
fun VolumeHud(
    visible: Boolean,
    volumeFraction: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.85f),
        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.85f),
        modifier = modifier
    ) {
        val volumeIcon = when {
            volumeFraction <= 0.01f -> Icons.Filled.VolumeMute
            volumeFraction < 0.5f -> Icons.Filled.VolumeDown
            else -> Icons.Filled.VolumeUp
        }

        VerticalHudBar(
            icon = {
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = "Volumen",
                    tint = VioletAccent,
                    modifier = Modifier.size(26.dp)
                )
            },
            percentage = (volumeFraction * 100).toInt(),
            fraction = volumeFraction,
            accentColor = VioletAccent
        )
    }
}

@Composable
private fun VerticalHudBar(
    icon: @Composable () -> Unit,
    percentage: Int,
    fraction: Float,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$percentage%",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Vertical Progress Bar
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0.01f, 1.0f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun SeekGestureHud(
    visible: Boolean,
    seekTargetMs: Long,
    seekDeltaMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.9f),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        val isForward = seekDeltaMs >= 0
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.82f))
                .border(1.5.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Formatters.formatTimeDelta(seekDeltaMs),
                        color = CyanAccent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${Formatters.formatDuration(seekTargetMs)} / ${Formatters.formatDuration(totalDurationMs)}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SpeedBoostHud(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.8f),
        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "speedAlpha"
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.5.dp, CyanAccent.copy(alpha = alpha), RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Velocidad 2x »",
                    color = CyanAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun DoubleTapRippleFeedback(
    label: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = label != null,
        enter = fadeIn(tween(80)) + scaleIn(tween(120), initialScale = 0.6f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.3f),
        modifier = modifier
    ) {
        val isRewind = label == "-10s"
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .border(2.dp, CyanAccent.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isRewind) Icons.Filled.Replay10 else Icons.Filled.Forward10,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label ?: "",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HudToastMessage(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.85f),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(PlayerDarkBg.copy(alpha = 0.9f))
                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message ?: "",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

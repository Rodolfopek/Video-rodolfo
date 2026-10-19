package com.example.ui.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

private enum class DragMode {
    NONE,
    SEEK,
    BRIGHTNESS,
    VOLUME
}

@Composable
fun GestureDetectorBox(
    modifier: Modifier = Modifier,
    isLocked: Boolean,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onSeekDelta: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    onZoomAndPan: (zoomChange: Float, panChange: Offset) -> Unit,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Double tap detection state
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    // 2-finger zoom vs 1-finger gesture handling
    var isMultiTouchActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            // Two-finger pinch-to-zoom and pan
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f || pan != Offset.Zero) {
                        isMultiTouchActive = true
                        onZoomAndPan(zoom, pan)
                    }
                }
            }
            // One-finger gestures: brightness, volume, horizontal seek, double taps, long press
            .pointerInput(isLocked, size) {
                if (isLocked) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val down = event.changes.firstOrNull { it.pressed }
                            if (down != null) {
                                // In locked mode, any tap informs user
                                onSingleTap()
                                down.consume()
                            }
                        }
                    }
                    return@pointerInput
                }

                val touchSlop = 18f
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent(PointerEventPass.Main)
                        val downChange = downEvent.changes.firstOrNull() ?: continue
                        if (!downChange.pressed) continue

                        val pointers = downEvent.changes.filter { it.pressed }
                        if (pointers.size > 1) {
                            // Multiple fingers down, let transform gesture take over
                            continue
                        }

                        val startPos = downChange.position
                        val startTime = System.currentTimeMillis()
                        var dragMode = DragMode.NONE
                        var isLongPressTriggered = false
                        var hasMovedPastSlop = false

                        val isLeftSide = startPos.x < (size.width / 2f)

                        var lastPos = startPos

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val currentPointers = event.changes.filter { it.pressed }

                            if (currentPointers.isEmpty()) {
                                // Pointer lifted!
                                if (isLongPressTriggered) {
                                    onLongPressEnd()
                                } else if (dragMode == DragMode.SEEK) {
                                    onSeekEnd()
                                } else if (!hasMovedPastSlop) {
                                    // It was a tap!
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime < 320L && (startPos - lastTapPosition).getDistance() < 120f) {
                                        // Double tap confirmed!
                                        val third = size.width / 3f
                                        when {
                                            startPos.x < third -> onDoubleTapLeft()
                                            startPos.x > 2f * third -> onDoubleTapRight()
                                            else -> onDoubleTapCenter()
                                        }
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = now
                                        lastTapPosition = startPos
                                        onSingleTap()
                                    }
                                }
                                break
                            }

                            if (currentPointers.size > 1) {
                                // Multi-finger started mid-gesture
                                if (isLongPressTriggered) onLongPressEnd()
                                if (dragMode == DragMode.SEEK) onSeekEnd()
                                break
                            }

                            val change = currentPointers.first()
                            val currentPos = change.position
                            val totalDeltaX = currentPos.x - startPos.x
                            val totalDeltaY = currentPos.y - startPos.y

                            // Check long press threshold (400ms without movement)
                            val elapsed = System.currentTimeMillis() - startTime
                            if (!hasMovedPastSlop && elapsed > 400L && !isLongPressTriggered && dragMode == DragMode.NONE) {
                                isLongPressTriggered = true
                                onLongPressStart()
                            }

                            if (!hasMovedPastSlop) {
                                if (abs(totalDeltaX) > touchSlop || abs(totalDeltaY) > touchSlop) {
                                    hasMovedPastSlop = true
                                    if (isLongPressTriggered) {
                                        onLongPressEnd()
                                        isLongPressTriggered = false
                                    }

                                    dragMode = if (abs(totalDeltaX) > abs(totalDeltaY)) {
                                        DragMode.SEEK
                                    } else {
                                        if (isLeftSide) DragMode.BRIGHTNESS else DragMode.VOLUME
                                    }

                                    if (dragMode == DragMode.SEEK) {
                                        onSeekStart()
                                    }
                                }
                            }

                            if (hasMovedPastSlop && !isLongPressTriggered) {
                                val stepDeltaX = currentPos.x - lastPos.x
                                val stepDeltaY = currentPos.y - lastPos.y

                                when (dragMode) {
                                    DragMode.SEEK -> {
                                        // Sensitivity: width of screen corresponds to ~90 seconds
                                        val deltaSeconds = (stepDeltaX / size.width.coerceAtLeast(1)) * 90f
                                        onSeekDelta(deltaSeconds)
                                    }
                                    DragMode.BRIGHTNESS -> {
                                        // Upward swipe (negative Y) increases brightness
                                        val brightnessDelta = -(stepDeltaY / size.height.coerceAtLeast(1))
                                        onBrightnessChange(brightnessDelta)
                                    }
                                    DragMode.VOLUME -> {
                                        // Upward swipe (negative Y) increases volume
                                        val volumeDelta = -(stepDeltaY / size.height.coerceAtLeast(1))
                                        onVolumeChange(volumeDelta)
                                    }
                                    DragMode.NONE -> {}
                                }
                                change.consume()
                            }

                            lastPos = currentPos
                        }
                    }
                }
            }
    ) {
        content()
    }
}

package com.example.util

import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Formatters {

    fun formatDuration(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatRemaining(currentMs: Long, totalMs: Long): String {
        val remaining = (totalMs - currentMs).coerceAtLeast(0L)
        return "-" + formatDuration(remaining)
    }

    fun formatTimeDelta(deltaMs: Long): String {
        val sign = if (deltaMs >= 0) "+" else "-"
        val absMs = abs(deltaMs)
        val totalSec = TimeUnit.MILLISECONDS.toSeconds(absMs)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return String.format(Locale.getDefault(), "%s%02d:%02d", sign, minutes, seconds)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}

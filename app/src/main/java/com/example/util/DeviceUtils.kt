package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.BatteryManager
import android.view.WindowManager
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object DeviceUtils {

    fun getBatteryLevel(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100
        }
    }

    fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun setActivityBrightness(activity: Activity, brightnessFraction: Float) {
        val lp = activity.window.attributes
        lp.screenBrightness = brightnessFraction.coerceIn(0.01f, 1.0f)
        activity.window.attributes = lp
    }

    fun getActivityBrightness(activity: Activity): Float {
        val b = activity.window.attributes.screenBrightness
        return if (b < 0f) 0.5f else b
    }

    fun getSystemVolumeFraction(context: Context): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0f
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current.toFloat() / max).coerceIn(0f, 1f)
    }

    fun setSystemVolumeFraction(context: Context, fraction: Float) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return
        val targetVolume = (fraction.coerceIn(0f, 1f) * max).toInt()
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        } catch (_: Exception) {}
    }

    suspend fun extractVideoMetadata(context: Context, uri: Uri, fallbackTitle: String): VideoItem =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            var duration = 0L
            var resolution = "HD"
            var bitmap: Bitmap? = null
            var size = 0L

            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}

            try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLongOrNull() ?: 0L

                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val w = widthStr?.toIntOrNull() ?: 0
                val h = heightStr?.toIntOrNull() ?: 0

                resolution = when {
                    w >= 3840 || h >= 2160 -> "4K Ultra HD"
                    w >= 2560 || h >= 1440 -> "2K QHD"
                    w >= 1920 || h >= 1080 -> "1080p Full HD"
                    w >= 1280 || h >= 720 -> "720p HD"
                    w > 0 && h > 0 -> "${w}x${h}"
                    else -> "HD"
                }

                // Extract a frame around 10% into the video or at 1s
                val targetUs = if (duration > 5000) 2_000_000L else 500_000L
                bitmap = retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            VideoItem(
                id = UUID.randomUUID().toString(),
                title = fallbackTitle,
                uriString = uri.toString(),
                durationMs = duration,
                resolution = resolution,
                sizeBytes = size,
                dateModifiedSec = System.currentTimeMillis() / 1000L,
                folderName = "Manual",
                thumbnailBitmap = bitmap
            )
        }

    suspend fun captureFrame(context: Context, uri: Uri, positionMs: Long): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                if (uri.scheme == "http" || uri.scheme == "https") {
                    retriever.setDataSource(uri.toString(), HashMap())
                } else {
                    retriever.setDataSource(context, uri)
                }
                retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
}

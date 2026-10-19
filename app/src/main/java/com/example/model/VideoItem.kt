package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class VideoItem(
    val id: String,
    val title: String,
    val uriString: String,
    val durationMs: Long = 0L,
    val resolution: String = "HD",
    val sizeBytes: Long = 0L,
    val dateModifiedSec: Long = 0L,
    val folderName: String = "Interno",
    val folderPath: String = "",
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val lastPlaybackPositionMs: Long = 0L,
    val thumbnailBitmap: Bitmap? = null,
    val subtitleUriString: String? = null,
    val subtitleName: String? = null
) {
    val uri: Uri get() = Uri.parse(uriString)

    val progressPercent: Float
        get() = if (durationMs > 0 && lastPlaybackPositionMs > 0) {
            (lastPlaybackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

enum class AspectRatioMode(val label: String) {
    FIT("Ajustar (Fit)"),
    FILL("Rellenar / Recortar (Crop)"),
    STRETCH("Estirar (Stretch)"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

enum class FeedTab(val label: String) {
    ALL("Todos"),
    RECENT("Recientes"),
    FOLDERS("Carpetas"),
    FAVORITES("Favoritos"),
    HISTORY("Historial")
}

enum class SortOption(val label: String) {
    DATE_NEWEST("Más recientes (Nuevos)"),
    DATE_OLDEST("Más antiguos"),
    NAME_AZ("Alfabético (A - Z)"),
    NAME_ZA("Alfabético (Z - A)"),
    DURATION_LONGEST("Más largos"),
    DURATION_SHORTEST("Más cortos"),
    SIZE_LARGEST("Mayor tamaño")
}

enum class DurationFilter(val label: String) {
    ALL("Cualquier duración"),
    SHORT("Cortos (< 5 min)"),
    MEDIUM("Medios (5 - 30 min)"),
    LONG("Películas / Largos (> 30 min)")
}

data class VideoFolder(
    val folderName: String,
    val folderPath: String,
    val videoCount: Int,
    val latestVideo: VideoItem? = null
)

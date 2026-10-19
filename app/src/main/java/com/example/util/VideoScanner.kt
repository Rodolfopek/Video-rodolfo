package com.example.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.example.model.VideoFolder
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoScanner {

    suspend fun scanDeviceVideos(context: Context, preferences: VideoPreferences): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val videoList = mutableListOf<VideoItem>()
            val favorites = preferences.getFavoriteIds()
            val history = preferences.getPlaybackHistory()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val projection = mutableListOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Video.Media.RELATIVE_PATH)
                    add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else {
                    @Suppress("DEPRECATION")
                    add(MediaStore.Video.Media.DATA)
                }
            }.toTypedArray()

            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

            try {
                context.contentResolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                    val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                    } else -1

                    val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    } else -1

                    val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    } else -1

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Video_$id"
                        val duration = cursor.getLong(durationCol)
                        val size = cursor.getLong(sizeCol)
                        val dateModified = cursor.getLong(dateCol)
                        val width = cursor.getInt(widthCol)
                        val height = cursor.getInt(heightCol)

                        var folderName = "Interno"
                        var folderPath = ""

                        if (bucketCol != -1 && !cursor.isNull(bucketCol)) {
                            folderName = cursor.getString(bucketCol) ?: "Interno"
                        }

                        if (relativePathCol != -1 && !cursor.isNull(relativePathCol)) {
                            val rel = cursor.getString(relativePathCol) ?: ""
                            folderPath = rel
                            if (folderName == "Interno" && rel.isNotBlank()) {
                                folderName = rel.trimEnd('/').substringAfterLast('/')
                            }
                        } else if (dataCol != -1 && !cursor.isNull(dataCol)) {
                            @Suppress("DEPRECATION")
                            val dataPath = cursor.getString(dataCol) ?: ""
                            folderPath = dataPath
                            val file = File(dataPath)
                            folderName = file.parentFile?.name ?: "Interno"
                        }

                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val idString = id.toString()

                        val resString = when {
                            width >= 3840 || height >= 2160 -> "4K Ultra HD"
                            width >= 2560 || height >= 1440 -> "2K QHD"
                            width >= 1920 || height >= 1080 -> "1080p Full HD"
                            width >= 1280 || height >= 720 -> "720p HD"
                            width > 0 && height > 0 -> "${width}x${height}"
                            else -> "HD"
                        }

                        val historyRecord = history[idString]
                        val isFav = favorites.contains(idString)

                        // Thumbnail extraction: fast API 29+ or lazy
                        val thumbBitmap = loadThumbnailSafe(context, contentUri)

                        videoList.add(
                            VideoItem(
                                id = idString,
                                title = name.substringBeforeLast('.'),
                                uriString = contentUri.toString(),
                                durationMs = duration,
                                resolution = resString,
                                sizeBytes = size,
                                dateModifiedSec = dateModified,
                                folderName = folderName,
                                folderPath = folderPath,
                                isFavorite = isFav,
                                lastPlayedTimestamp = historyRecord?.first ?: 0L,
                                lastPlaybackPositionMs = historyRecord?.second ?: 0L,
                                thumbnailBitmap = thumbBitmap
                            )
                        )
                    }
                }
            } catch (_: Exception) {}

            videoList
        }

    fun groupVideosByFolder(videos: List<VideoItem>): List<VideoFolder> {
        val groups = videos.groupBy { it.folderName }
        return groups.map { (name, list) ->
            val sortedList = list.sortedByDescending { it.dateModifiedSec }
            VideoFolder(
                folderName = name,
                folderPath = sortedList.firstOrNull()?.folderPath ?: "",
                videoCount = list.size,
                latestVideo = sortedList.firstOrNull()
            )
        }.sortedByDescending { it.videoCount }
    }

    private fun loadThumbnailSafe(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(320, 240), null)
            } else {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val bitmap = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}

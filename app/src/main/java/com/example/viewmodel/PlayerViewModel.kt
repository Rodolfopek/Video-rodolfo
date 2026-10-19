package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.AspectRatioMode
import com.example.model.DurationFilter
import com.example.model.FeedTab
import com.example.model.SortOption
import com.example.model.VideoFolder
import com.example.model.VideoItem
import com.example.util.DeviceUtils
import com.example.util.Formatters
import com.example.util.VideoPreferences
import com.example.util.VideoScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player: ExoPlayer = ExoPlayer.Builder(application).build()
    private val preferences = VideoPreferences(application)

    // Master Video List from Device Scan
    private val _rawVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val rawVideos: StateFlow<List<VideoItem>> = _rawVideos.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Feed Navigation & Filter States
    private val _currentTab = MutableStateFlow(FeedTab.ALL)
    val currentTab: StateFlow<FeedTab> = _currentTab.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _durationFilter = MutableStateFlow(DurationFilter.ALL)
    val durationFilter: StateFlow<DurationFilter> = _durationFilter.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

private data class FilterState(
    val tab: FeedTab,
    val sort: SortOption,
    val duration: DurationFilter,
    val folder: String?,
    val query: String
)

    private val filterStateFlow = combine(
        _currentTab,
        _sortOption,
        _durationFilter,
        _selectedFolder,
        _searchQuery
    ) { tab, sort, duration, folder, query ->
        FilterState(tab, sort, duration, folder, query)
    }

    // Filtered Video Feed
    val filteredVideos: StateFlow<List<VideoItem>> = combine(
        _rawVideos,
        filterStateFlow
    ) { videos, filter ->
        var list = videos

        // Tab filter
        list = when (filter.tab) {
            FeedTab.ALL -> list
            FeedTab.RECENT -> list.sortedByDescending { it.dateModifiedSec }
            FeedTab.FOLDERS -> {
                if (filter.folder != null) list.filter { it.folderName == filter.folder } else list
            }
            FeedTab.FAVORITES -> list.filter { it.isFavorite }
            FeedTab.HISTORY -> list.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
        }

        // Folder filter if selected outside FOLDERS tab
        if (filter.tab != FeedTab.FOLDERS && filter.folder != null) {
            list = list.filter { it.folderName == filter.folder }
        }

        // Duration filter
        list = when (filter.duration) {
            DurationFilter.ALL -> list
            DurationFilter.SHORT -> list.filter { it.durationMs in 1..299_999L }
            DurationFilter.MEDIUM -> list.filter { it.durationMs in 300_000L..1_799_999L }
            DurationFilter.LONG -> list.filter { it.durationMs >= 1_800_000L }
        }

        // Search query
        if (filter.query.isNotBlank()) {
            list = list.filter {
                it.title.contains(filter.query, ignoreCase = true) || it.folderName.contains(filter.query, ignoreCase = true)
            }
        }

        // Sort option
        when (filter.sort) {
            SortOption.DATE_NEWEST -> list.sortedByDescending { it.dateModifiedSec }
            SortOption.DATE_OLDEST -> list.sortedBy { it.dateModifiedSec }
            SortOption.NAME_AZ -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_ZA -> list.sortedByDescending { it.title.lowercase() }
            SortOption.DURATION_LONGEST -> list.sortedByDescending { it.durationMs }
            SortOption.DURATION_SHORTEST -> list.sortedBy { it.durationMs }
            SortOption.SIZE_LARGEST -> list.sortedByDescending { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Grouped Folders
    val folderList: StateFlow<List<VideoFolder>> = _rawVideos.combine(_searchQuery) { videos, q ->
        val filtered = if (q.isBlank()) videos else videos.filter { it.title.contains(q, ignoreCase = true) }
        VideoScanner.groupVideosByFolder(filtered)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Currently playing state
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // OSD & Lock
    private val _areControlsVisible = MutableStateFlow(true)
    val areControlsVisible: StateFlow<Boolean> = _areControlsVisible.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _showRemainingTime = MutableStateFlow(false)
    val showRemainingTime: StateFlow<Boolean> = _showRemainingTime.asStateFlow()

    // Gestures HUD
    private val _brightnessFraction = MutableStateFlow(0.7f)
    val brightnessFraction: StateFlow<Float> = _brightnessFraction.asStateFlow()

    private val _showBrightnessHud = MutableStateFlow(false)
    val showBrightnessHud: StateFlow<Boolean> = _showBrightnessHud.asStateFlow()

    private val _volumeFraction = MutableStateFlow(0.8f)
    val volumeFraction: StateFlow<Float> = _volumeFraction.asStateFlow()

    private val _showVolumeHud = MutableStateFlow(false)
    val showVolumeHud: StateFlow<Boolean> = _showVolumeHud.asStateFlow()

    private val _showSeekHud = MutableStateFlow(false)
    val showSeekHud: StateFlow<Boolean> = _showSeekHud.asStateFlow()

    private val _seekTargetMs = MutableStateFlow(0L)
    val seekTargetMs: StateFlow<Long> = _seekTargetMs.asStateFlow()

    private val _seekDeltaMs = MutableStateFlow(0L)
    val seekDeltaMs: StateFlow<Long> = _seekDeltaMs.asStateFlow()

    private val _isSpeedBoostActive = MutableStateFlow(false)
    val isSpeedBoostActive: StateFlow<Boolean> = _isSpeedBoostActive.asStateFlow()

    private val _doubleTapRipple = MutableStateFlow<String?>(null)
    val doubleTapRipple: StateFlow<String?> = _doubleTapRipple.asStateFlow()

    // Zoom & Pan
    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    // Modes & Tools
    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _hudToast = MutableStateFlow<String?>(null)
    val hudToast: StateFlow<String?> = _hudToast.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _nightFilterAlpha = MutableStateFlow(0.0f)
    val nightFilterAlpha: StateFlow<Float> = _nightFilterAlpha.asStateFlow()

    private val _abLoopPointA = MutableStateFlow<Long?>(null)
    val abLoopPointA: StateFlow<Long?> = _abLoopPointA.asStateFlow()

    private val _abLoopPointB = MutableStateFlow<Long?>(null)
    val abLoopPointB: StateFlow<Long?> = _abLoopPointB.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerSecondsRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsRemaining: StateFlow<Int?> = _sleepTimerSecondsRemaining.asStateFlow()

    private val _capturedFrame = MutableStateFlow<Bitmap?>(null)
    val capturedFrame: StateFlow<Bitmap?> = _capturedFrame.asStateFlow()

    private val _externalSubtitleUri = MutableStateFlow<Uri?>(null)
    val externalSubtitleUri: StateFlow<Uri?> = _externalSubtitleUri.asStateFlow()

    // Coroutine Jobs
    private var progressJob: Job? = null
    private var hideControlsJob: Job? = null
    private var hudDismissJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        _volumeFraction.value = DeviceUtils.getSystemVolumeFraction(application)

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startControlsHideTimer()
                } else {
                    saveCurrentPlaybackProgress()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                }
            }
        })

        // Poll position and loop checks
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val buf = player.bufferedPosition.coerceAtLeast(0L)

                    if (!_showSeekHud.value) {
                        _currentPositionMs.value = pos
                    }
                    _durationMs.value = dur
                    _bufferedPositionMs.value = buf

                    // Check A-B Loop
                    val a = _abLoopPointA.value
                    val b = _abLoopPointB.value
                    if (a != null && b != null && b > a) {
                        if (pos >= b) {
                            player.seekTo(a)
                        }
                    }
                }
                delay(250)
            }
        }
    }

    // Scan Device MediaStore
    fun scanDeviceVideos() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val scanned = VideoScanner.scanDeviceVideos(getApplication(), preferences)
                _rawVideos.value = scanned
            } catch (_: Exception) {
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun addManualVideos(newVideos: List<VideoItem>) {
        val current = _rawVideos.value.toMutableList()
        current.addAll(0, newVideos)
        _rawVideos.value = current
    }

    // Toggle Favorite
    fun toggleFavorite(video: VideoItem) {
        val updatedFav = preferences.toggleFavorite(video.id)
        _rawVideos.value = _rawVideos.value.map {
            if (it.id == video.id) it.copy(isFavorite = updatedFav) else it
        }
        showHudToast(if (updatedFav) "Añadido a Favoritos ⭐" else "Eliminado de Favoritos")
    }

    // Navigation and filters
    fun setTab(tab: FeedTab) {
        _currentTab.value = tab
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setDurationFilter(filter: DurationFilter) {
        _durationFilter.value = filter
    }

    fun setSelectedFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    // Playback control
    fun playVideo(video: VideoItem) {
        val currentFeed = filteredVideos.value
        val index = currentFeed.indexOfFirst { it.id == video.id }
        if (index != -1) {
            _currentIndex.value = index
        }
        _currentVideo.value = video

        loadVideoMediaItem(video)
        showControls()

        // Resume position if video was previously watched
        if (video.lastPlaybackPositionMs > 5000L && video.lastPlaybackPositionMs < (video.durationMs - 10000L)) {
            player.seekTo(video.lastPlaybackPositionMs)
            showHudToast("Reanudando desde ${Formatters.formatDuration(video.lastPlaybackPositionMs)}")
        }

        // Update history timestamp immediately
        preferences.savePlaybackPosition(video.id, video.lastPlaybackPositionMs)
    }

    fun playNext() {
        val list = filteredVideos.value
        if (list.isEmpty()) return
        val nextIndex = (_currentIndex.value + 1) % list.size
        _currentIndex.value = nextIndex
        val video = list[nextIndex]
        playVideo(video)
    }

    fun playPrevious() {
        val list = filteredVideos.value
        if (list.isEmpty()) return
        val prevIndex = if (_currentIndex.value - 1 < 0) list.size - 1 else _currentIndex.value - 1
        _currentIndex.value = prevIndex
        val video = list[prevIndex]
        playVideo(video)
    }

    private fun loadVideoMediaItem(video: VideoItem) {
        val uri = video.uri
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        val subUri = _externalSubtitleUri.value ?: video.subtitleUriString?.let { Uri.parse(it) }
        if (subUri != null) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(if (subUri.toString().endsWith(".vtt", ignoreCase = true)) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("es")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
        }

        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()
        player.play()
        applyPlaybackSpeed()
    }

    fun saveCurrentPlaybackProgress() {
        val video = _currentVideo.value ?: return
        val pos = player.currentPosition
        preferences.savePlaybackPosition(video.id, pos)
        _rawVideos.value = _rawVideos.value.map {
            if (it.id == video.id) it.copy(lastPlaybackPositionMs = pos, lastPlayedTimestamp = System.currentTimeMillis()) else it
        }
    }

    fun togglePlayPause() {
        if (_isLocked.value) return
        if (player.isPlaying) {
            player.pause()
            showControls(persistent = true)
        } else {
            player.play()
            showControls()
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs.coerceIn(0L, _durationMs.value)
        player.seekTo(_currentPositionMs.value)
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, _durationMs.value)
        seekTo(newPos)
    }

    fun onDoubleTapLeft() {
        if (_isLocked.value) return
        seekRelative(-10_000L)
        triggerRippleAnimation("-10s")
    }

    fun onDoubleTapRight() {
        if (_isLocked.value) return
        seekRelative(10_000L)
        triggerRippleAnimation("+10s")
    }

    private fun triggerRippleAnimation(label: String) {
        _doubleTapRipple.value = label
        viewModelScope.launch {
            delay(650)
            if (_doubleTapRipple.value == label) {
                _doubleTapRipple.value = null
            }
        }
    }

    // Gestures: Brightness & Volume
    fun updateBrightness(delta: Float) {
        val current = _brightnessFraction.value
        val updated = (current + delta).coerceIn(0.01f, 1.0f)
        _brightnessFraction.value = updated
        _showBrightnessHud.value = true
        resetHudTimer()
    }

    fun updateVolume(delta: Float) {
        val current = _volumeFraction.value
        val updated = (current + delta).coerceIn(0.0f, 1.0f)
        _volumeFraction.value = updated
        _showVolumeHud.value = true
        DeviceUtils.setSystemVolumeFraction(getApplication(), updated)
        player.volume = updated
        resetHudTimer()
    }

    // Horizontal Seek Gesture
    fun onSeekGestureStart() {
        _showSeekHud.value = true
        _seekDeltaMs.value = 0L
        _seekTargetMs.value = player.currentPosition
    }

    fun onSeekGestureDelta(deltaSeconds: Float) {
        val deltaMs = (deltaSeconds * 1000).toLong()
        _seekDeltaMs.value += deltaMs
        val target = (player.currentPosition + _seekDeltaMs.value).coerceIn(0L, _durationMs.value)
        _seekTargetMs.value = target
        _currentPositionMs.value = target
    }

    fun onSeekGestureEnd() {
        _showSeekHud.value = false
        player.seekTo(_seekTargetMs.value)
        resetHudTimer()
    }

    // Long Press 2.0x Boost
    fun onLongPressStart() {
        if (_isLocked.value) return
        _isSpeedBoostActive.value = true
        player.playbackParameters = PlaybackParameters(2.0f)
    }

    fun onLongPressEnd() {
        if (_isSpeedBoostActive.value) {
            _isSpeedBoostActive.value = false
            applyPlaybackSpeed()
        }
    }

    // Zoom and Pan
    fun updateZoomAndPan(zoomChange: Float, panChange: Offset) {
        val newScale = (_zoomScale.value * zoomChange).coerceIn(0.5f, 3.0f)
        _zoomScale.value = newScale
        _panOffset.value = _panOffset.value + panChange
    }

    fun resetZoom() {
        _zoomScale.value = 1.0f
        _panOffset.value = Offset.Zero
        showHudToast("Zoom restablecido (1.0x)")
    }

    // OSD controls
    fun toggleControls() {
        if (_isLocked.value) {
            showHudToast("Pantalla bloqueada")
            return
        }
        if (_areControlsVisible.value) {
            _areControlsVisible.value = false
        } else {
            showControls()
        }
    }

    fun showControls(persistent: Boolean = false) {
        _areControlsVisible.value = true
        hideControlsJob?.cancel()
        if (!persistent && _isPlaying.value) {
            startControlsHideTimer()
        }
    }

    private fun startControlsHideTimer() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(3000)
            if (_isPlaying.value && !_isLocked.value) {
                _areControlsVisible.value = false
            }
        }
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        if (_isLocked.value) {
            _areControlsVisible.value = false
            showHudToast("Controles bloqueados")
        } else {
            _areControlsVisible.value = true
            showHudToast("Controles desbloqueados")
            startControlsHideTimer()
        }
    }

    fun toggleRemainingTime() {
        _showRemainingTime.value = !_showRemainingTime.value
    }

    // Aspect Ratio
    fun cycleAspectRatio() {
        val modes = AspectRatioMode.entries
        val nextIndex = (_aspectRatioMode.value.ordinal + 1) % modes.size
        val nextMode = modes[nextIndex]
        _aspectRatioMode.value = nextMode
        showHudToast("Modo: ${nextMode.label}")
    }

    // Playback Speed
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed()
        showHudToast("Velocidad: ${speed}x")
    }

    private fun applyPlaybackSpeed() {
        player.playbackParameters = PlaybackParameters(_playbackSpeed.value)
    }

    // Night Filter
    fun setNightFilterAlpha(alpha: Float) {
        _nightFilterAlpha.value = alpha.coerceIn(0.0f, 0.8f)
    }

    // A-B Loop
    fun toggleABLoop() {
        val pos = player.currentPosition
        when {
            _abLoopPointA.value == null -> {
                _abLoopPointA.value = pos
                showHudToast("Punto A fijado en ${Formatters.formatDuration(pos)}")
            }
            _abLoopPointB.value == null -> {
                if (pos > (_abLoopPointA.value ?: 0L)) {
                    _abLoopPointB.value = pos
                    showHudToast("Punto B fijado. Bucle A-B activo")
                } else {
                    showHudToast("El punto B debe ser posterior al punto A")
                }
            }
            else -> {
                _abLoopPointA.value = null
                _abLoopPointB.value = null
                showHudToast("Bucle A-B cancelado")
            }
        }
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerSecondsRemaining.value = null
            showHudToast("Temporizador desactivado")
            return
        }
        val totalSec = minutes * 60
        _sleepTimerSecondsRemaining.value = totalSec
        showHudToast("Temporizador: apagado en $minutes min")

        sleepTimerJob = viewModelScope.launch {
            var left = totalSec
            while (left > 0 && isActive) {
                delay(1000)
                left--
                _sleepTimerSecondsRemaining.value = left
            }
            if (left <= 0) {
                player.pause()
                _sleepTimerMinutes.value = null
                _sleepTimerSecondsRemaining.value = null
                showHudToast("Temporizador: Reproducción pausada")
            }
        }
    }

    fun setExternalSubtitle(uri: Uri, name: String) {
        _externalSubtitleUri.value = uri
        showHudToast("Subtítulo cargado: $name")
        _currentVideo.value?.let { loadVideoMediaItem(it) }
    }

    fun captureCurrentFrame() {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            val bitmap = DeviceUtils.captureFrame(getApplication(), video.uri, player.currentPosition)
            if (bitmap != null) {
                _capturedFrame.value = bitmap
                showHudToast("¡Fotograma capturado con éxito!")
            } else {
                showHudToast("Captura tomada del reproductor")
            }
        }
    }

    fun dismissCapturedFrame() {
        _capturedFrame.value = null
    }

    fun showHudToast(message: String) {
        _hudToast.value = message
        hudDismissJob?.cancel()
        hudDismissJob = viewModelScope.launch {
            delay(2200)
            if (_hudToast.value == message) {
                _hudToast.value = null
            }
        }
    }

    private fun resetHudTimer() {
        hudDismissJob?.cancel()
        hudDismissJob = viewModelScope.launch {
            delay(1200)
            _showBrightnessHud.value = false
            _showVolumeHud.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentPlaybackProgress()
        progressJob?.cancel()
        hideControlsJob?.cancel()
        sleepTimerJob?.cancel()
        player.release()
    }
}

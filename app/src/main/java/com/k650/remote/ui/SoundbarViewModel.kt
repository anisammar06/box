package com.k650.remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k650.remote.data.ApiResult
import com.k650.remote.data.Settings
import com.k650.remote.data.SoundbarApi
import com.k650.remote.data.SoundbarRepository
import com.k650.remote.data.SoundbarState
import com.k650.remote.data.Source
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the [SoundbarState] the UI observes and serialises all commands.
 *
 * Design rules baked in:
 *  - Optimistic UI for the slider/mute (snappy), but the repository always
 *    re-reads the real value after settling and overwrites the optimistic guess.
 *  - A single background poll loop keeps state fresh (handles changes made from
 *    other controllers, and the Wi-Fi drop bug becoming "unreachable").
 */
class SoundbarViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val api = SoundbarApi(settings.host, settings.port)
    private val repo = SoundbarRepository(api)

    private val _state = MutableStateFlow(SoundbarState())
    val state: StateFlow<SoundbarState> = _state.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    val host: String get() = api.host
    val port: Int get() = api.port

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                _state.update { repo.refresh(it) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshNow() = viewModelScope.launch {
        _state.update { repo.refresh(it) }
    }

    fun updateEndpoint(host: String, port: Int) {
        settings.host = host
        settings.port = port
        api.host = host
        api.port = port
        _state.value = SoundbarState()
        startPolling()
    }

    // ---- Commands -----------------------------------------------------------

    /** Fired continuously while dragging: optimistic, no readback, no delay. */
    fun previewVolume(level: Int) {
        _state.update { it.copy(volume = level.coerceIn(0, SoundbarState.VOLUME_MAX)) }
    }

    /** Fired on drag end: commit and reconcile against the real readback. */
    fun commitVolume(level: Int) = viewModelScope.launch {
        val real = repo.setVolume(level)
        _state.update { it.copy(volume = real) }
    }

    fun toggleMute() = viewModelScope.launch {
        val target = !_state.value.muted
        _state.update { it.copy(muted = target) } // optimistic
        val real = repo.setMute(target)
        _state.update { it.copy(muted = real) }
    }

    fun selectSource(source: Source) = viewModelScope.launch {
        _state.update { it.copy(source = source) } // optimistic
        val real = repo.setSource(source)
        if (real != null) _state.update { it.copy(source = real) }
    }

    /** Convenience for the "Switch to Bluetooth" shortcut (YouTube path). */
    fun switchToBluetooth() = selectSource(Source.BLUETOOTH)

    fun togglePlayPause() = viewModelScope.launch {
        val playing = _state.value.nowPlaying.playStatus == com.k650.remote.data.PlayStatus.PLAY
        val np = repo.setPlayback(!playing)
        if (np != null) _state.update { it.copy(nowPlaying = np) }
    }

    fun skipNext() = viewModelScope.launch {
        val np = repo.skipNext()
        if (np != null) _state.update { it.copy(nowPlaying = np) }
    }

    fun playUrl(url: String) = viewModelScope.launch {
        when (val r = repo.playUrl(url.trim())) {
            is ApiResult.Ok -> _snackbar.value = "Lecture de l'URL démarrée"
            is ApiResult.Timeout -> _snackbar.value = "Commande envoyée (pas d'accusé)"
            is ApiResult.Error -> _snackbar.value = "Échec : ${r.message}"
        }
        refreshNow()
    }

    fun consumeSnackbar() { _snackbar.value = null }

    companion object {
        private const val POLL_INTERVAL_MS = 4000L
    }
}

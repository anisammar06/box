package com.k650.remote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k650.remote.data.ApiResult
import com.k650.remote.data.BrowseItem
import com.k650.remote.data.CpService
import com.k650.remote.data.PlayStatus
import com.k650.remote.data.RepeatMode
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

/** Ephemeral state for the Services tab (browsing is interactive, not polled). */
data class ServicesState(
    val services: List<CpService> = emptyList(),
    val loading: Boolean = false,
    val activeServiceId: Int? = null,
    val breadcrumb: List<String> = emptyList(),
    val items: List<BrowseItem> = emptyList(),
    val browsing: Boolean = false,
)

class SoundbarViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val api = SoundbarApi(settings.host, settings.port)
    private val repo = SoundbarRepository(api)

    private val _state = MutableStateFlow(SoundbarState())
    val state: StateFlow<SoundbarState> = _state.asStateFlow()

    private val _services = MutableStateFlow(ServicesState())
    val services: StateFlow<ServicesState> = _services.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    val host: String get() = api.host
    val port: Int get() = api.port

    private var pollJob: Job? = null

    init { startPolling() }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                val includeStatic = tick % STATIC_EVERY == 0
                _state.update { repo.refresh(it, includeStatic) }
                tick++
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshNow() = viewModelScope.launch {
        _state.update { repo.refresh(it, includeStatic = true) }
    }

    fun updateEndpoint(host: String, port: Int) {
        settings.host = host; settings.port = port
        api.host = host; api.port = port
        _state.value = SoundbarState()
        _services.value = ServicesState()
        startPolling()
    }

    // ---- Volume / mute ------------------------------------------------------

    fun previewVolume(level: Int) =
        _state.update { it.copy(volume = level.coerceIn(0, SoundbarState.VOLUME_MAX)) }

    fun commitVolume(level: Int) = viewModelScope.launch {
        _state.update { it.copy(volume = repo.setVolume(level)) }
    }

    fun nudgeVolume(delta: Int) = viewModelScope.launch {
        _state.update { it.copy(volume = repo.setVolume(it.volume + delta)) }
    }

    fun toggleMute() = viewModelScope.launch {
        val target = !_state.value.muted
        _state.update { it.copy(muted = target) }
        _state.update { it.copy(muted = repo.setMute(target)) }
    }

    // ---- Sources ------------------------------------------------------------

    fun selectSource(source: Source) = viewModelScope.launch {
        _state.update { it.copy(source = source) }
        repo.setSource(source)?.let { real -> _state.update { it.copy(source = real) } }
    }

    fun switchToBluetooth() = selectSource(Source.BLUETOOTH)

    // ---- Transport ----------------------------------------------------------

    fun togglePlayPause() = viewModelScope.launch {
        val playing = _state.value.nowPlaying.playStatus == PlayStatus.PLAY
        repo.setPlayback(!playing)?.let { np -> _state.update { it.copy(nowPlaying = np) } }
    }

    fun skipNext() = viewModelScope.launch {
        repo.skipNext()?.let { np -> _state.update { it.copy(nowPlaying = np) } }
    }

    fun previous() = viewModelScope.launch {
        repo.previous()?.let { np -> _state.update { it.copy(nowPlaying = np) } }
    }

    fun seek(seconds: Int) = viewModelScope.launch {
        repo.seek(seconds)?.let { pos -> _state.update { it.copy(position = pos) } }
    }

    fun cycleRepeat() = viewModelScope.launch {
        val next = when (_state.value.repeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeat = repo.setRepeat(next)) }
    }

    fun toggleShuffle() = viewModelScope.launch {
        _state.update { it.copy(shuffle = repo.setShuffle(!it.shuffle)) }
    }

    // ---- Sound / EQ ---------------------------------------------------------

    fun selectEqPreset(index: Int) = viewModelScope.launch {
        _state.update { it.copy(currentEqIndex = index) }
        repo.setEqPreset(index)?.let { real -> _state.update { it.copy(currentEqIndex = real) } }
    }

    fun setWoofer(level: Int) = viewModelScope.launch {
        _state.update { it.copy(wooferLevel = level) }
        _state.update { it.copy(wooferLevel = repo.setWoofer(level)) }
    }

    // ---- Power / standby ----------------------------------------------------

    fun standby(afterSeconds: Int) = viewModelScope.launch {
        repo.standby(afterSeconds)
        _snackbar.value = if (afterSeconds == 0) "Mise en veille demandée" else "Veille programmée"
    }

    fun cancelStandby() = viewModelScope.launch {
        repo.cancelStandby(); _snackbar.value = "Minuterie de veille annulée"
    }

    // ---- URL / name ---------------------------------------------------------

    fun playUrl(url: String) = viewModelScope.launch {
        when (repo.playUrl(url.trim())) {
            is ApiResult.Ok -> _snackbar.value = "Lecture de l'URL démarrée"
            is ApiResult.Timeout -> _snackbar.value = "Commande envoyée (pas d'accusé)"
            is ApiResult.Error -> _snackbar.value = "Échec de la lecture d'URL"
        }
        refreshNow()
    }

    fun rename(name: String) = viewModelScope.launch {
        repo.setSpeakerName(name); _snackbar.value = "Nom mis à jour"; refreshNow()
    }

    // ---- Services / account linking -----------------------------------------

    fun loadServices() = viewModelScope.launch {
        _services.update { it.copy(loading = true) }
        val list = repo.getServices()
        _services.update { it.copy(services = list, loading = false) }
    }

    fun openService(service: CpService) = viewModelScope.launch {
        _services.update { it.copy(browsing = true, activeServiceId = service.id, items = emptyList(), breadcrumb = listOf(service.name)) }
        repo.selectService(service.id)
        val items = if (service.isTuneIn) repo.browseRadioRoot() else repo.browseServiceMenu()
        _services.update { it.copy(items = items, browsing = false) }
    }

    fun openItem(item: BrowseItem) = viewModelScope.launch {
        if (item.playable) {
            repo.play(item)
            _snackbar.value = "Lecture : ${item.title}"
            refreshNow()
            return@launch
        }
        _services.update { it.copy(browsing = true) }
        val tuneIn = _services.value.services.firstOrNull { it.id == _services.value.activeServiceId }?.isTuneIn == true
        val items = if (tuneIn) repo.openRadioFolder(item.contentId) else repo.openSubmenu(item.contentId)
        _services.update { it.copy(items = items, breadcrumb = it.breadcrumb + item.title, browsing = false) }
    }

    fun login(service: CpService, username: String, password: String) = viewModelScope.launch {
        repo.selectService(service.id)
        when (repo.login(username, password)) {
            is ApiResult.Error -> _snackbar.value = "Échec de connexion"
            else -> _snackbar.value = "Connexion envoyée à ${service.name}"
        }
        delay(1200)
        loadServices()
    }

    fun logout(service: CpService) = viewModelScope.launch {
        repo.selectService(service.id); repo.logout()
        _snackbar.value = "Déconnecté de ${service.name}"
        delay(800); loadServices()
    }

    fun consumeSnackbar() { _snackbar.value = null }

    companion object {
        private const val POLL_INTERVAL_MS = 4000L
        private const val STATIC_EVERY = 5 // static info every ~20s
    }
}

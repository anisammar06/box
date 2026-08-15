package com.k650.remote.data

import kotlinx.coroutines.delay

/**
 * Turns raw [SoundbarApi] calls into trustworthy state.
 *
 * QUIRK this class exists for: the bar queues notifications and will happily
 * return the reply to a *previous* request. So a write's acknowledgement means
 * nothing. Every mutating call here is followed by a short settle delay and a
 * fresh read of the affected value — the readback, not the ack, is the truth.
 */
class SoundbarRepository(val api: SoundbarApi) {

    companion object {
        const val SETTLE_MS = 600L
    }

    // ---- Reachability -------------------------------------------------------

    /** Canonical probe. GetMainInfo answering XML = the bar is really there. */
    suspend fun probeReachable(): Boolean = api.getMainInfo() is ApiResult.Ok

    /**
     * Poll of live state. [includeStatic] additionally refreshes rarely-changing
     * info (device/Wi-Fi, EQ presets, woofer) — the ViewModel does that less
     * often. Fields not read this pass are carried over from [previous].
     */
    suspend fun refresh(previous: SoundbarState, includeStatic: Boolean): SoundbarState {
        val mainRes = api.getMainInfo()
        val reachable = when (mainRes) {
            is ApiResult.Ok -> true
            is ApiResult.Timeout -> previous.reachable // tolerate a busy moment
            is ApiResult.Error -> false
        }
        if (!reachable) {
            return previous.copy(
                reachable = false,
                lastError = (mainRes as? ApiResult.Error)?.message,
            )
        }
        val main = (mainRes as? ApiResult.Ok)?.response

        var device = previous.device
        main?.let {
            device = device.copy(
                modelName = it.str("spkmodelname") ?: device.modelName,
                macAddress = it.str("spkmacaddr") ?: device.macAddress,
            )
        }

        val name = api.getSpkName().ok()?.str("spkname") ?: previous.speakerName
        val volume = api.getVolume().ok()?.int("volume") ?: previous.volume
        val muted = api.getMute().ok()?.bool("mute") ?: previous.muted
        val funcR = api.getFunc().ok()
        val source = Source.fromApi(funcR?.str("function")) ?: previous.source
        val submode = funcR?.str("submode") ?: previous.submode
        val nowPlaying = readNowPlaying() ?: previous.nowPlaying
        val position = readPosition() ?: previous.position
        val repeat = api.getRepeatMode().ok()?.let { RepeatMode.fromApi(it.str("repeatmode") ?: it.str("repeat")) } ?: previous.repeat
        val shuffle = api.getShuffleMode().ok()?.let { it.bool("shufflemode") ?: it.bool("shuffle") } ?: previous.shuffle

        var eqPresets = previous.eqPresets
        var currentEq = previous.currentEqIndex
        var woofer = previous.wooferLevel
        if (includeStatic) {
            device = device.copy(
                softwareVersion = api.getSoftwareVersion().ok()?.let { it.str("displayversion") ?: it.str("version") } ?: device.softwareVersion,
            )
            api.getApInfo().ok()?.let { ap ->
                device = device.copy(
                    wifiSsid = ap.str("ssid") ?: device.wifiSsid,
                    wifiRssi = ap.int("rssi") ?: device.wifiRssi,
                    wifiChannel = ap.str("ch") ?: ap.str("channel") ?: device.wifiChannel,
                    connectionType = ap.str("connectiontype") ?: device.connectionType,
                )
            }
            readEqPresets()?.let { eqPresets = it }
            api.getCurrentEqMode().ok()?.int("presetindex")?.let { currentEq = it }
            api.getWooferLevel().ok()?.let { (it.int("wooferlevel") ?: it.int("level")) }?.let { woofer = it }
        }

        return SoundbarState(
            speakerName = name,
            volume = volume,
            muted = muted,
            source = source,
            submode = submode,
            nowPlaying = nowPlaying,
            position = position,
            repeat = repeat,
            shuffle = shuffle,
            eqPresets = eqPresets,
            currentEqIndex = currentEq,
            wooferLevel = woofer,
            device = device,
            reachable = true,
            lastError = null,
        )
    }

    private suspend fun readNowPlaying(): NowPlaying? {
        val r = api.getRadioInfo().ok() ?: return null
        return NowPlaying(
            cpName = r.str("cpname"),
            title = r.str("title"),
            artist = r.str("artist") ?: r.str("description"),
            album = r.str("album"),
            thumbnailUrl = r.str("thumbnail"),
            trackLengthSec = r.int("tracklength"),
            playStatus = PlayStatus.fromApi(r.str("playstatus")),
        )
    }

    private suspend fun readPosition(): PlayPosition? {
        val r = api.getCurrentPlayTime().ok() ?: return null
        val pos = r.int("playtime") ?: 0
        val len = r.int("tracklength") ?: r.int("timelength") ?: 0
        return PlayPosition(pos, len)
    }

    private suspend fun readEqPresets(): List<EqPreset>? {
        val r = api.get7bandEqList().ok() ?: return null
        val items = r.items("preset")
        if (items.isEmpty()) return null
        return items.mapNotNull { m ->
            val idx = (m["presetindex"] ?: m["@index"])?.toIntOrNull() ?: return@mapNotNull null
            val nm = m["presetname"] ?: "Preset $idx"
            EqPreset(idx, nm)
        }
    }

    // ---- Mutations: act, settle, verify by readback -------------------------

    suspend fun setVolume(level: Int): Int {
        api.setVolume(level); delay(SETTLE_MS)
        return api.getVolume().ok()?.int("volume") ?: level
    }

    suspend fun setMute(on: Boolean): Boolean {
        api.setMute(on); delay(SETTLE_MS)
        return api.getMute().ok()?.bool("mute") ?: on
    }

    suspend fun setSource(source: Source): Source? {
        api.setFunc(source); delay(SETTLE_MS)
        return Source.fromApi(api.getFunc().ok()?.str("function"))
    }

    suspend fun setPlayback(play: Boolean): NowPlaying? {
        api.setPlayback(play); delay(SETTLE_MS)
        return readNowPlaying()
    }

    suspend fun skipNext(): NowPlaying? {
        api.skipNext(); delay(SETTLE_MS)
        return readNowPlaying()
    }

    /**
     * "Previous": no true previous command exists on this firmware. Restart the
     * current track (playtime=0) — a real remote's single back-press — and also
     * fire the generic TrickMode previous in case the source honours it.
     */
    suspend fun previous(): NowPlaying? {
        api.trickPrevious()
        api.seek(0)
        delay(SETTLE_MS)
        return readNowPlaying()
    }

    suspend fun seek(seconds: Int): PlayPosition? {
        api.seek(seconds); delay(SETTLE_MS)
        return readPosition()
    }

    suspend fun setRepeat(mode: RepeatMode): RepeatMode {
        api.setRepeat(mode); delay(SETTLE_MS)
        return api.getRepeatMode().ok()?.let { RepeatMode.fromApi(it.str("repeatmode") ?: it.str("repeat")) } ?: mode
    }

    suspend fun setShuffle(on: Boolean): Boolean {
        api.setShuffle(on); delay(SETTLE_MS)
        return api.getShuffleMode().ok()?.let { it.bool("shufflemode") ?: it.bool("shuffle") } ?: on
    }

    suspend fun setEqPreset(index: Int): Int? {
        api.set7bandEqMode(index); delay(SETTLE_MS)
        return api.getCurrentEqMode().ok()?.int("presetindex") ?: index
    }

    suspend fun setWoofer(level: Int): Int {
        api.setWooferLevel(level); delay(SETTLE_MS)
        return api.getWooferLevel().ok()?.let { it.int("wooferlevel") ?: it.int("level") } ?: level
    }

    suspend fun setSpeakerName(name: String) { api.setSpkName(name); delay(SETTLE_MS) }
    suspend fun standby(afterSeconds: Int): ApiResult = api.setSleepTimer(afterSeconds)
    suspend fun cancelStandby(): ApiResult = api.cancelSleepTimer()
    suspend fun playUrl(url: String): ApiResult = api.playUrl(url)

    // ---- Content-provider services -----------------------------------------

    suspend fun getServices(): List<CpService> {
        val r = api.getCpList().ok() ?: return emptyList()
        return r.items("cp").mapNotNull { m ->
            val id = m["cpid"]?.toIntOrNull() ?: return@mapNotNull null
            val nm = m["cpname"] ?: return@mapNotNull null
            CpService(
                id = id,
                name = nm,
                signedIn = (m["signinstatus"] == "1" || m["signinstatus"].equals("true", true)),
                username = m["username"],
            )
        }
    }

    suspend fun selectService(id: Int): ApiResult { val r = api.setCpService(id); delay(SETTLE_MS); return r }
    suspend fun login(username: String, password: String): ApiResult = api.signIn(username, password)
    suspend fun logout(): ApiResult = api.signOut()

    suspend fun browseServiceMenu(): List<BrowseItem> =
        api.getCpSubmenu().ok()?.items("submenuitem")?.mapNotNull { it.toBrowseItem("id") } ?: emptyList()

    suspend fun openSubmenu(contentId: String): List<BrowseItem> =
        api.selectCpSubmenu(contentId).ok()?.items("menuitem")?.mapNotNull { it.toBrowseItem("contentid") } ?: emptyList()

    suspend fun browseRadioRoot(): List<BrowseItem> =
        api.browseMain().ok()?.items("menuitem")?.mapNotNull { it.toBrowseItem("contentid") } ?: emptyList()

    suspend fun openRadioFolder(contentId: String): List<BrowseItem> =
        api.getSelectRadioList(contentId).ok()?.items("menuitem")?.mapNotNull { it.toBrowseItem("contentid") } ?: emptyList()

    suspend fun radioUp(): List<BrowseItem> =
        api.getUpperRadioList().ok()?.items("menuitem")?.mapNotNull { it.toBrowseItem("contentid") } ?: emptyList()

    suspend fun getPresets(): List<BrowseItem> =
        api.getPresetList().ok()?.items("preset")?.mapNotNull { m ->
            val idx = m["presetindex"] ?: m["@index"] ?: return@mapNotNull null
            BrowseItem(idx, m["title"] ?: m["presetname"] ?: "Preset $idx", playable = true, thumbnail = m["thumbnail"])
        } ?: emptyList()

    suspend fun play(item: BrowseItem): ApiResult = api.playSelect(item.contentId)
    suspend fun playPreset(index: Int): ApiResult = api.playPreset(index)
}

private fun ApiResult.ok(): WamResponse? = (this as? ApiResult.Ok)?.response

private fun Map<String, String>.toBrowseItem(idKey: String): BrowseItem? {
    val id = this[idKey] ?: this["@id"] ?: this["contentid"] ?: return null
    val title = this["title"] ?: this["submenuitem_localized"] ?: this["name"] ?: "—"
    // @type: 0 = folder, 2 = radio/playable, 1 also playable on app services.
    val type = this["@type"]
    val playable = type == "2" || type == "1" || this.containsKey("mediaid")
    return BrowseItem(id, title, playable, this["thumbnail"])
}

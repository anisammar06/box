package com.k650.remote.data

/** Input sources the HW-K650 accepts via UIC SetFunc. */
enum class Source(val apiValue: String, val label: String) {
    WIFI("wifi", "Wi-Fi"),
    BLUETOOTH("bt", "Bluetooth"),
    AUX("aux", "AUX"),
    HDMI("hdmi", "HDMI"),
    OPTICAL("d.in", "Optique"),
    TV_SOUNDCONNECT("soundshare", "TV SoundConnect");

    companion object {
        fun fromApi(value: String?): Source? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

/**
 * Music services baked into the firmware. Not extensible via the API.
 * [available] hides the dead/obsolete ones (Napster, JUKE, 7digital, Murfie).
 * [nativelySupported] is false for services the K650 firmware simply does not
 * know (YouTube, Amazon Music) — shown only to explain the Bluetooth route.
 */
enum class MusicService(
    val label: String,
    val available: Boolean,
    val nativelySupported: Boolean = true,
    val note: String = "",
) {
    SPOTIFY("Spotify", true, true, "Via Spotify Connect (Premium)"),
    TUNEIN("TuneIn", true, true, "Radios & podcasts"),
    DEEZER("Deezer", true, true, "Compte requis"),
    QOBUZ("Qobuz", true, true, "Compte requis"),
    TIDAL("TIDAL", true, true, "Compte requis"),
    // Present in firmware but dead services — hidden.
    NAPSTER("Napster", false),
    JUKE("JUKE", false),
    SEVENDIGITAL("7digital", false),
    MURFIE("Murfie", false),
    // NOT in firmware — only reachable through Bluetooth from the phone.
    YOUTUBE("YouTube", true, false, "Non natif — via Bluetooth"),
    YOUTUBE_MUSIC("YouTube Music", true, false, "Non natif — via Bluetooth"),
    AMAZON_MUSIC("Amazon Music", true, false, "Non natif — via Bluetooth");

    companion object {
        val nativeVisible: List<MusicService> get() = entries.filter { it.available && it.nativelySupported }
        val bluetoothOnly: List<MusicService> get() = entries.filter { it.available && !it.nativelySupported }
    }
}

/** Transport state for CPM-driven playback (Spotify Connect renderer, etc.). */
enum class PlayStatus { PLAY, PAUSE, STOP, UNKNOWN;
    companion object {
        fun fromApi(value: String?): PlayStatus = when (value?.lowercase()) {
            "play", "playing" -> PLAY
            "pause", "paused" -> PAUSE
            "stop", "stopped" -> STOP
            else -> UNKNOWN
        }
    }
}

enum class RepeatMode(val apiValue: String) { OFF("off"), ALL("all"), ONE("one");
    companion object { fun fromApi(v: String?) = entries.firstOrNull { it.apiValue.equals(v, true) } ?: OFF }
}

/** One entry from Get7bandEQList. */
data class EqPreset(val index: Int, val name: String)

/** A firmware content-provider service (from GetCpList). */
data class CpService(
    val id: Int,
    val name: String,
    val signedIn: Boolean,
    val username: String? = null,
) {
    /** Services that accept a username/password login via SetSignIn. */
    val supportsLogin: Boolean
        get() = name.lowercase().let {
            it.contains("deezer") || it.contains("tidal") || it.contains("qobuz") ||
                it.contains("napster") || it.contains("rhapsody") || it.contains("anghami")
        }
    val isSpotify: Boolean get() = name.equals("Spotify", ignoreCase = true)
    val isTuneIn: Boolean get() = name.contains("TuneIn", ignoreCase = true)
}

/** A browsable/playable item from a CP browse or radio list. */
data class BrowseItem(
    val contentId: String,
    val title: String,
    val playable: Boolean,
    val thumbnail: String? = null,
)

/** Now-playing metadata from CPM GetRadioInfo. */
data class NowPlaying(
    val cpName: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val trackLengthSec: Int? = null,
    val playStatus: PlayStatus = PlayStatus.UNKNOWN,
) {
    val hasContent: Boolean get() = !title.isNullOrBlank() || !artist.isNullOrBlank()
}

/** Current playback position (from GetCurrentPlayTime). */
data class PlayPosition(val positionSec: Int = 0, val lengthSec: Int = 0) {
    val fraction: Float get() = if (lengthSec > 0) (positionSec.toFloat() / lengthSec).coerceIn(0f, 1f) else 0f
}

/** Device identity + network state (GetMainInfo, GetApInfo, GetSoftwareVersion). */
data class DeviceInfo(
    val modelName: String? = null,
    val macAddress: String? = null,
    val softwareVersion: String? = null,
    val wifiSsid: String? = null,
    val wifiRssi: Int? = null,
    val wifiChannel: String? = null,
    val connectionType: String? = null,
) {
    /** RSSI (dBm) → 0..4 bars, best-effort. */
    val wifiBars: Int? get() = wifiRssi?.let {
        when {
            it >= -55 -> 4
            it >= -65 -> 3
            it >= -75 -> 2
            it >= -85 -> 1
            else -> 0
        }
    }
}

/**
 * The full state the UI renders. Everything shown is derived from real
 * readbacks — nothing is inferred from a write's acknowledgement (see the
 * notification-stacking quirk handled in [SoundbarRepository]).
 */
data class SoundbarState(
    val speakerName: String? = null,
    val volume: Int = 0,            // 0..30
    val muted: Boolean = false,
    val source: Source? = null,
    val submode: String? = null,
    val nowPlaying: NowPlaying = NowPlaying(),
    val position: PlayPosition = PlayPosition(),
    val repeat: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val eqPresets: List<EqPreset> = emptyList(),
    val currentEqIndex: Int? = null,
    val wooferLevel: Int = 0,       // -6..+6
    val device: DeviceInfo = DeviceInfo(),
    val reachable: Boolean = false,
    val lastError: String? = null,
) {
    companion object {
        const val VOLUME_MAX = 30
        const val WOOFER_MIN = -6
        const val WOOFER_MAX = 6
    }
}

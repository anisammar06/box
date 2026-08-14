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
        /** Map a GetFunc <function> value back to an enum, best-effort. */
        fun fromApi(value: String?): Source? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

/**
 * Music services baked into the firmware. Not extensible via the API.
 * [available] hides the dead/obsolete ones (Napster, JUKE, 7digital, Murfie).
 */
enum class MusicService(val label: String, val available: Boolean) {
    SPOTIFY("Spotify", true),
    TUNEIN("TuneIn", true),
    DEEZER("Deezer", true),
    QOBUZ("Qobuz", true),
    TIDAL("TIDAL", true),
    NAPSTER("Napster", false),
    JUKE("JUKE", false),
    SEVENDIGITAL("7digital", false),
    MURFIE("Murfie", false);

    companion object {
        val visible: List<MusicService> get() = entries.filter { it.available }
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

/** Now-playing metadata from CPM GetRadioInfo. */
data class NowPlaying(
    val cpName: String? = null,      // e.g. "Spotify"
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val trackLengthSec: Int? = null,
    val playStatus: PlayStatus = PlayStatus.UNKNOWN,
) {
    val hasContent: Boolean get() = !title.isNullOrBlank() || !artist.isNullOrBlank()
}

/**
 * The full state the UI renders. Everything the app shows is derived from real
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
    val reachable: Boolean = false,
    val lastError: String? = null,
) {
    companion object {
        const val VOLUME_MAX = 30
    }
}

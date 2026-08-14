package com.k650.remote.data

import kotlinx.coroutines.delay

/**
 * Turns raw [SoundbarApi] calls into trustworthy state.
 *
 * QUIRK this class exists for: the bar queues its notifications and will happily
 * return the reply to a *previous* request. So a write's acknowledgement means
 * nothing. Every mutating call here is followed by a short settle delay and a
 * fresh read of the affected value — the readback, not the ack, is the truth.
 */
class SoundbarRepository(val api: SoundbarApi) {

    companion object {
        /** Time for the bar to actually apply a change before we re-read it. */
        const val SETTLE_MS = 600L
    }

    /** Full poll of everything the UI shows. Tolerant of partial failures. */
    suspend fun refresh(previous: SoundbarState = SoundbarState()): SoundbarState {
        val nameRes = api.getSpkName()
        if (nameRes is ApiResult.Error) {
            return previous.copy(reachable = false, lastError = nameRes.message)
        }
        val name = (nameRes as? ApiResult.Ok)?.response?.str("spkname") ?: previous.speakerName

        val volume = readVolume() ?: previous.volume
        val muted = readMute() ?: previous.muted
        val (source, submode) = readFunc() ?: (previous.source to previous.submode)
        val nowPlaying = readNowPlaying() ?: previous.nowPlaying

        return SoundbarState(
            speakerName = name,
            volume = volume,
            muted = muted,
            source = source,
            submode = submode,
            nowPlaying = nowPlaying,
            reachable = true,
            lastError = null,
        )
    }

    private suspend fun readVolume(): Int? =
        (api.getVolume() as? ApiResult.Ok)?.response?.int("volume")

    private suspend fun readMute(): Boolean? =
        (api.getMute() as? ApiResult.Ok)?.response?.bool("mute")

    private suspend fun readFunc(): Pair<Source?, String?>? {
        val r = (api.getFunc() as? ApiResult.Ok)?.response ?: return null
        return Source.fromApi(r.str("function")) to r.str("submode")
    }

    private suspend fun readNowPlaying(): NowPlaying? {
        val r = (api.getRadioInfo() as? ApiResult.Ok)?.response ?: return null
        return NowPlaying(
            cpName = r.str("cpname"),
            title = r.str("title"),
            artist = r.str("artist"),
            album = r.str("album"),
            thumbnailUrl = r.str("thumbnail"),
            trackLengthSec = r.int("tracklength"),
            playStatus = PlayStatus.fromApi(r.str("playstatus")),
        )
    }

    // ---- Mutations: act, settle, then verify by reading back ----------------

    suspend fun setVolume(level: Int): Int {
        api.setVolume(level)
        delay(SETTLE_MS)
        return readVolume() ?: level
    }

    suspend fun setMute(on: Boolean): Boolean {
        api.setMute(on)
        delay(SETTLE_MS)
        return readMute() ?: on
    }

    /**
     * Switch source. SetFunc to an already-active source may never answer
     * ([ApiResult.Timeout]) — that is expected, so we ignore the ack entirely
     * and report whatever GetFunc says after settling.
     */
    suspend fun setSource(source: Source): Source? {
        api.setFunc(source)
        delay(SETTLE_MS)
        return readFunc()?.first
    }

    suspend fun setPlayback(play: Boolean): NowPlaying? {
        api.setPlayback(play)
        delay(SETTLE_MS)
        return readNowPlaying()
    }

    suspend fun skipNext(): NowPlaying? {
        api.skipNext()
        delay(SETTLE_MS)
        return readNowPlaying()
    }

    suspend fun playUrl(url: String): ApiResult = api.playUrl(url)
}

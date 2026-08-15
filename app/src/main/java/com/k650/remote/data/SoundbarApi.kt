package com.k650.remote.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/** Outcome of one HTTP call to the bar. */
sealed interface ApiResult {
    data class Ok(val response: WamResponse) : ApiResult
    /**
     * Timed out with no reply. On this firmware that is often NORMAL (e.g.
     * SetFunc to an already-active source never answers), so it must NOT be
     * surfaced as a failure by itself.
     */
    data object Timeout : ApiResult
    data class Error(val message: String) : ApiResult
}

/**
 * Thin HTTP client for the WAM/UIC/CPM API.
 *
 * Every command is a GET to `http://<host>:<port>/<UIC|CPM>?cmd=<XML>` where the
 * XML is percent-encoded via [UrlEncoding] (spaces → `%20`). No auth, no tokens.
 *
 * Command coverage & module choices are grounded in the reverse-engineered
 * libraries pywam, krygal/samsung_multiroom and bacl/WAM_API_DOC.
 */
class SoundbarApi(
    @Volatile var host: String,
    @Volatile var port: Int,
    private val connectTimeoutMs: Int = 2000,
    private val readTimeoutMs: Int = 3000,
) {
    enum class Module { UIC, CPM }

    suspend fun send(module: Module, commandXml: String): ApiResult =
        withContext(Dispatchers.IO) {
            val url = URL("http://$host:$port/${module.name}?cmd=${UrlEncoding.encode(commandXml)}")
            var conn: HttpURLConnection? = null
            try {
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    useCaches = false
                    setRequestProperty("Connection", "close")
                }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                ApiResult.Ok(WamResponse.parse(body))
            } catch (_: SocketTimeoutException) {
                ApiResult.Timeout
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: e.javaClass.simpleName)
            } finally {
                conn?.disconnect()
            }
        }

    private suspend fun uic(cmd: String) = send(Module.UIC, cmd)
    private suspend fun cpm(cmd: String) = send(Module.CPM, cmd)

    private fun p(type: String, name: String, value: String) =
        "<p type=\"$type\" name=\"$name\" val=\"$value\"/>"

    // ---- Device / network (reachability + verification) ---------------------

    suspend fun getMainInfo() = uic("<name>GetMainInfo</name>")
    suspend fun getSpkName() = uic("<name>GetSpkName</name>")
    suspend fun getSoftwareVersion() = uic("<name>GetSoftwareVersion</name>")
    suspend fun getApInfo() = uic("<name>GetApInfo</name>")
    suspend fun getPowerStatus() = uic("<name>GetPowerStatus</name>")

    // ---- State reads --------------------------------------------------------

    suspend fun getVolume() = uic("<name>GetVolume</name>")
    suspend fun getMute() = uic("<name>GetMute</name>")
    suspend fun getFunc() = uic("<name>GetFunc</name>")
    suspend fun getRadioInfo() = cpm("<name>GetRadioInfo</name>")
    suspend fun getCurrentPlayTime() = uic("<name>GetCurrentPlayTime</name>")
    suspend fun getRepeatMode() = uic("<name>GetRepeatMode</name>")
    suspend fun getShuffleMode() = uic("<name>GetShuffleMode</name>")

    // ---- Basic writes -------------------------------------------------------

    suspend fun setVolume(level: Int): ApiResult {
        val v = level.coerceIn(0, SoundbarState.VOLUME_MAX)
        return uic("<name>SetVolume</name>${p("dec", "volume", v.toString())}")
    }

    suspend fun setMute(on: Boolean) =
        uic("<name>SetMute</name>${p("str", "mute", if (on) "on" else "off")}")

    suspend fun setFunc(source: Source) =
        uic("<name>SetFunc</name>${p("str", "function", source.apiValue)}")

    // ---- Transport ----------------------------------------------------------

    suspend fun setPlayback(play: Boolean) =
        cpm("<name>SetPlaybackControl</name>${p("str", "playbackcontrol", if (play) "play" else "pause")}")

    /** Next track — the ONLY skip the firmware honours, and only WITHOUT a param. */
    suspend fun skipNext() = cpm("<name>SetSkipCurrentTrack</name>")

    /**
     * Seek within the current track. Also used for "previous": no true previous
     * command exists on this firmware, so playtime=0 restarts the current track
     * (exactly like a real remote's single back-press).
     */
    suspend fun seek(seconds: Int) =
        uic("<name>SetSearchTime</name>${p("dec", "playtime", seconds.coerceAtLeast(0).toString())}")

    /** Fallback true-previous attempt (usually a no-op on K-series). */
    suspend fun trickPrevious() =
        uic("<name>SetTrickMode</name>${p("str", "trickmode", "previous")}")

    suspend fun setRepeat(mode: RepeatMode) =
        uic("<name>SetRepeatMode</name>${p("str", "repeatmode", mode.apiValue)}")

    suspend fun setShuffle(on: Boolean) =
        uic("<name>SetShuffleMode</name>${p("str", "shufflemode", if (on) "on" else "off")}")

    // ---- Equalizer ----------------------------------------------------------

    suspend fun get7bandEqList() = uic("<name>Get7bandEQList</name>")
    suspend fun getCurrentEqMode() = uic("<name>GetCurrentEQMode</name>")
    suspend fun set7bandEqMode(presetIndex: Int) =
        uic("<name>Set7bandEQMode</name>${p("dec", "presetindex", presetIndex.toString())}")

    suspend fun set7bandEqValues(presetIndex: Int, values: List<Int>): ApiResult {
        val bands = (0 until 7).joinToString("") { i ->
            val v = values.getOrElse(i) { 0 }.coerceIn(-6, 6)
            p("dec", "eqvalue${i + 1}", v.toString())
        }
        return uic("<name>Set7bandEQValue</name>${p("dec", "presetindex", presetIndex.toString())}$bands")
    }

    // ---- Woofer -------------------------------------------------------------

    suspend fun getWooferLevel() = uic("<name>GetWooferLevel</name>")
    suspend fun setWooferLevel(level: Int) =
        uic("<name>SetWooferLevel</name>${p("dec", "wooferlevel", level.coerceIn(-6, 6).toString())}")

    // ---- Device settings ----------------------------------------------------

    suspend fun setSpkName(name: String) =
        uic("<name>SetSpkName</name><p type=\"cdata\" name=\"spkname\" val=\"empty\"><![CDATA[$name]]></p>")

    suspend fun getLed() = uic("<name>GetLed</name>")
    suspend fun setLed(on: Boolean) =
        uic("<name>SetLed</name>${p("str", "option", if (on) "on" else "off")}")

    suspend fun getSleepTimer() = uic("<name>GetSleepTimer</name>")

    /** Schedule standby. [seconds]=0 with option=start requests near-immediate standby. */
    suspend fun setSleepTimer(seconds: Int) =
        uic("<name>SetSleepTimer</name>${p("str", "option", "start")}${p("dec", "sleeptime", seconds.coerceAtLeast(0).toString())}")

    suspend fun cancelSleepTimer() =
        uic("<name>SetSleepTimer</name>${p("str", "option", "off")}${p("dec", "sleeptime", "0")}")

    // ---- URL playback -------------------------------------------------------

    /**
     * Play an arbitrary HTTP audio stream (NAS files, webradios, TTS).
     * Verified sample (bacl §94) is UIC, with a <pwron>on</pwron> prefix and
     * resume=1. Never used to inject extracted YouTube streams — that breaks
     * YouTube ToS.
     */
    suspend fun playUrl(streamUrl: String): ApiResult {
        val cmd = buildString {
            append("<pwron>on</pwron>")
            append("<name>SetUrlPlayback</name>")
            append("<p type=\"cdata\" name=\"url\" val=\"empty\"><![CDATA[")
            append(streamUrl)
            append("]]></p>")
            append(p("dec", "buffersize", "0"))
            append(p("dec", "seektime", "0"))
            append(p("dec", "resume", "1"))
        }
        return uic(cmd)
    }

    // ---- Content-provider music services (CPM) ------------------------------

    /** List the firmware's baked-in services with sign-in status. */
    suspend fun getCpList(startIndex: Int = 0, count: Int = 30) =
        cpm("<name>GetCpList</name>${p("dec", "liststartindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    /** Info on the currently active service. */
    suspend fun getCpInfo() = cpm("<name>GetCpInfo</name>")

    /** Activate a service by its cpid (read from GetCpList on the real device). */
    suspend fun setCpService(cpId: Int) =
        cpm("<name>SetCpService</name>${p("dec", "cpservice_id", cpId.toString())}")

    /** Cleartext username/password login against the active service. */
    suspend fun signIn(username: String, password: String) =
        cpm("<name>SetSignIn</name>${p("str", "username", username)}${p("str", "password", password)}")

    suspend fun signOut() = cpm("<name>SetSignOut</name>")

    /** Top-level menu of the active service (categories, playlists…). */
    suspend fun getCpSubmenu() = cpm("<name>GetCpSubmenu</name>")

    suspend fun selectCpSubmenu(contentId: String, startIndex: Int = 0, count: Int = 30) =
        cpm("<name>SetSelectCpSubmenu</name>${p("dec", "contentid", contentId)}${p("dec", "startindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    // TuneIn / radio browsing
    suspend fun browseMain(startIndex: Int = 0, count: Int = 30) =
        cpm("<name>BrowseMain</name>${p("dec", "startindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    suspend fun getSelectRadioList(contentId: String, startIndex: Int = 0, count: Int = 30) =
        cpm("<name>GetSelectRadioList</name>${p("dec", "contentid", contentId)}${p("dec", "startindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    suspend fun getCurrentRadioList(startIndex: Int = 0, count: Int = 30) =
        cpm("<name>GetCurrentRadioList</name>${p("dec", "startindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    suspend fun getUpperRadioList(startIndex: Int = 0, count: Int = 30) =
        cpm("<name>GetUpperRadioList</name>${p("dec", "startindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    /** Play a chosen browsed item by its contentid. */
    suspend fun playSelect(selectItemId: String) =
        cpm("<name>SetPlaySelect</name>${p("dec", "selectitemid", selectItemId)}")

    // Presets / favourites (TuneIn)
    suspend fun getPresetList(startIndex: Int = 0, count: Int = 30) =
        cpm("<name>GetPresetList</name>${p("dec", "startindex", startIndex.toString())}${p("dec", "listcount", count.toString())}")

    suspend fun playPreset(presetIndex: Int, presetType: Int = 1): ApiResult {
        // Two-step: select then commit (SetSelectRadio actually starts it).
        cpm("<name>SetPlayPreset</name>${p("dec", "presettype", presetType.toString())}${p("dec", "presetindex", presetIndex.toString())}")
        return cpm("<name>SetSelectRadio</name>")
    }

    // ---- Reachability -------------------------------------------------------

    /** Canonical liveness probe: a GetMainInfo that returns XML means it's alive. */
    suspend fun ping(): Boolean = getMainInfo() is ApiResult.Ok
}

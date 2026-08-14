package com.k650.remote.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/** Outcome of one HTTP call to the bar. */
sealed interface ApiResult {
    /** Parsed XML came back. */
    data class Ok(val response: WamResponse) : ApiResult
    /**
     * The request timed out with no reply. On this firmware that is often
     * NORMAL (e.g. SetFunc to an already-active source never answers), so it
     * must NOT be surfaced as a failure by itself.
     */
    data object Timeout : ApiResult
    /** A real transport error (connection refused, host unreachable, ...). */
    data class Error(val message: String) : ApiResult
}

/**
 * Thin HTTP client for the WAM/UIC/CPM API.
 *
 * Every command is a GET to `http://<host>:<port>/<UIC|CPM>?cmd=<XML>` where the
 * XML is percent-encoded via [UrlEncoding] (spaces → `%20`). No auth, no tokens.
 */
class SoundbarApi(
    @Volatile var host: String,
    @Volatile var port: Int,
    private val connectTimeoutMs: Int = 2000,
    private val readTimeoutMs: Int = 3000,
) {
    private enum class Module { UIC, CPM }

    private suspend fun send(module: Module, commandXml: String): ApiResult =
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

    // ---- Reads (verified) ---------------------------------------------------

    suspend fun getSpkName() = uic("<name>GetSpkName</name>")
    suspend fun getVolume() = uic("<name>GetVolume</name>")
    suspend fun getMute() = uic("<name>GetMute</name>")
    suspend fun getFunc() = uic("<name>GetFunc</name>")
    suspend fun getRadioInfo() = cpm("<name>GetRadioInfo</name>")

    // ---- Writes (verified) --------------------------------------------------

    suspend fun setVolume(level: Int): ApiResult {
        val v = level.coerceIn(0, SoundbarState.VOLUME_MAX)
        return uic("<name>SetVolume</name><p type=\"dec\" name=\"volume\" val=\"$v\"/>")
    }

    suspend fun setMute(on: Boolean) =
        uic("<name>SetMute</name><p type=\"str\" name=\"mute\" val=\"${if (on) "on" else "off"}\"/>")

    suspend fun setFunc(source: Source) =
        uic("<name>SetFunc</name><p type=\"str\" name=\"function\" val=\"${source.apiValue}\"/>")

    suspend fun setPlayback(play: Boolean) =
        cpm("<name>SetPlaybackControl</name><p type=\"str\" name=\"playbackcontrol\" val=\"${if (play) "play" else "pause"}\"/>")

    /** Next track — the ONLY skip the firmware honours, and only WITHOUT a param. */
    suspend fun skipNext() = cpm("<name>SetSkipCurrentTrack</name>")

    /**
     * Play an arbitrary HTTP audio stream (NAS files, webradios, TTS).
     * Never used to inject extracted YouTube streams — that breaks YouTube ToS.
     */
    suspend fun playUrl(streamUrl: String): ApiResult {
        val cmd = buildString {
            append("<name>SetUrlPlayback</name>")
            append("<p type=\"cdata\" name=\"url\" val=\"empty\"><![CDATA[")
            append(streamUrl)
            append("]]></p>")
            append("<p type=\"dec\" name=\"buffersize\" val=\"0\"/>")
            append("<p type=\"dec\" name=\"seektime\" val=\"0\"/>")
            append("<p type=\"dec\" name=\"resume\" val=\"0\"/>")
        }
        return cpm(cmd)
    }

    // ---- Reachability -------------------------------------------------------

    /** Cheap liveness probe using a read that always answers. */
    suspend fun ping(): Boolean = getSpkName() is ApiResult.Ok
}

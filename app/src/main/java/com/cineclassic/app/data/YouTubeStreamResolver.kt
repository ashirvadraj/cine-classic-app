package com.cineclassic.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object YouTubeStreamResolver {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val ENDPOINT_HOSTS = listOf(
        "https://loader.to",
        "https://en.loader.to"
    )

    /**
     * Extracts a clean YouTube video ID (11 characters) from various URL formats
     * or custom prefixes like 'youtube:2MizqUuFOkA'.
     */
    fun extractVideoId(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("youtube:") -> trimmed.removePrefix("youtube:").trim()
            trimmed.contains("v=") -> trimmed.substringAfter("v=").substringBefore("&").trim()
            trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").trim()
            trimmed.contains("embed/") -> trimmed.substringAfter("embed/").substringBefore("?").trim()
            else -> trimmed
        }
    }

    /**
     * Resolves a YouTube video ID into a direct downloadable MP4 URL.
     * Tries 720p first, then falls back to 480p/360p if needed.
     */
    suspend fun resolveMp4StreamUrl(videoIdOrUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(videoIdOrUrl)
        if (videoId.isEmpty()) return@withContext null

        // Try standard HD formats in preference order
        val formatsToTry = listOf("720", "480", "360")
        for (format in formatsToTry) {
            val url = attemptResolveFormat(videoId, format)
            if (url != null && url.startsWith("http")) {
                return@withContext url
            }
        }
        null
    }

    private suspend fun attemptResolveFormat(videoId: String, format: String): String? {
        val ytWatchUrl = "https://www.youtube.com/watch?v=$videoId"
        val encodedYt = try {
            URLEncoder.encode(ytWatchUrl, "UTF-8")
        } catch (e: Exception) {
            return null
        }

        for (host in ENDPOINT_HOSTS) {
            try {
                val initEndpoint = "$host/ajax/download.php?button=1&start=1&end=1&format=$format&url=$encodedYt"
                val initConn = (URL(initEndpoint).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", USER_AGENT)
                    connectTimeout = 12000
                    readTimeout = 12000
                }

                if (initConn.responseCode != HttpURLConnection.HTTP_OK) {
                    initConn.disconnect()
                    continue
                }

                val initJson = initConn.inputStream.bufferedReader().use { it.readText() }
                initConn.disconnect()

                val root = JSONObject(initJson)
                val directDownload = root.optString("download_url", "")
                if (directDownload.startsWith("http")) {
                    return directDownload
                }

                val progressUrl = root.optString("progress_url", "")
                if (progressUrl.isEmpty()) {
                    continue
                }

                // Poll progress URL up to 20 times (every 1200ms = ~24s)
                for (attempt in 1..20) {
                    delay(1200)
                    var pollConn: HttpURLConnection? = null
                    try {
                        pollConn = (URL(progressUrl).openConnection() as HttpURLConnection).apply {
                            setRequestProperty("User-Agent", USER_AGENT)
                            connectTimeout = 8000
                            readTimeout = 8000
                        }
                        if (pollConn.responseCode == HttpURLConnection.HTTP_OK) {
                            val pollText = pollConn.inputStream.bufferedReader().use { it.readText() }
                            val pObj = JSONObject(pollText)
                            val readyUrl = pObj.optString("download_url", "")
                            if (readyUrl.startsWith("http")) {
                                return readyUrl
                            }
                        }
                    } catch (e: Exception) {
                        // ignore and continue polling
                    } finally {
                        pollConn?.disconnect()
                    }
                }
            } catch (e: Exception) {
                // Try next host
            }
        }
        return null
    }
}

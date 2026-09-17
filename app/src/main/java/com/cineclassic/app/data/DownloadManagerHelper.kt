package com.cineclassic.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadManagerHelper(private val context: Context) {
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("cine_downloads", Context.MODE_PRIVATE)

    enum class DownloadState {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED
    }

    data class DownloadProgressInfo(
        val state: DownloadState,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progressPercent: Int
    )

    fun getDownloadState(movieId: String): DownloadState {
        return getDownloadProgress(movieId).state
    }

    /**
     * Resolves all HTTP/HTTPS redirects (e.g. 301, 302, 307) so DownloadManager
     * directly accesses the final media storage node without failing on cross-domain redirects.
     */
    fun resolveFinalDirectUrl(initialUrl: String, maxRedirects: Int = 6): String {
        var curr = initialUrl
        for (i in 0 until maxRedirects) {
            var conn: HttpURLConnection? = null
            try {
                val u = URL(curr)
                conn = u.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; CineClassic)")
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        curr = if (loc.startsWith("http://") || loc.startsWith("https://")) {
                            loc
                        } else {
                            URL(u, loc).toString()
                        }
                        continue
                    }
                }
                return curr
            } catch (e: Exception) {
                return curr
            } finally {
                conn?.disconnect()
            }
        }
        return curr
    }

    fun getDownloadProgress(movieId: String): DownloadProgressInfo {
        val localPath = prefs.getString("dl_path_$movieId", null)
        val isCompleted = prefs.getBoolean("dl_completed_$movieId", false)

        // 1. If explicitly marked completed, verify physical file on disk (> 1 MB)
        if (isCompleted && localPath != null) {
            val file = File(localPath)
            if (file.exists() && file.length() > 1024 * 1024) {
                val len = file.length()
                return DownloadProgressInfo(DownloadState.DOWNLOADED, len, len, 100)
            } else {
                prefs.edit().putBoolean("dl_completed_$movieId", false).apply()
            }
        }

        // 2. Otherwise query native DownloadManager
        val downloadId = prefs.getLong("dl_id_$movieId", -1L)
        if (downloadId != -1L) {
            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesSoFarIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    val downloaded = if (bytesSoFarIdx != -1) cursor.getLong(bytesSoFarIdx) else 0L
                    val total = if (totalBytesIdx != -1) cursor.getLong(totalBytesIdx) else 0L
                    val status = if (statusIdx != -1) cursor.getInt(statusIdx) else 0
                    cursor.close()

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val f = if (localPath != null) File(localPath) else null
                            if (f != null && f.exists() && f.length() > 1024 * 1024) {
                                prefs.edit().putBoolean("dl_completed_$movieId", true).apply()
                                return DownloadProgressInfo(DownloadState.DOWNLOADED, f.length(), f.length(), 100)
                            } else {
                                removeDownload(movieId)
                                return DownloadProgressInfo(DownloadState.NOT_DOWNLOADED, 0L, 0L, 0)
                            }
                        }
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> {
                            prefs.edit().putBoolean("dl_completed_$movieId", false).apply()
                            val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 99) else 0
                            return DownloadProgressInfo(DownloadState.DOWNLOADING, downloaded, total, percent)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            removeDownload(movieId)
                            return DownloadProgressInfo(DownloadState.NOT_DOWNLOADED, 0L, 0L, 0)
                        }
                        else -> {
                            return DownloadProgressInfo(DownloadState.NOT_DOWNLOADED, 0L, 0L, 0)
                        }
                    }
                }
                cursor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return DownloadProgressInfo(DownloadState.NOT_DOWNLOADED, 0L, 0L, 0)
    }

    fun getAllDownloadMovieIds(): Set<String> {
        return prefs.getStringSet("all_download_ids", emptySet()) ?: emptySet()
    }

    private fun addDownloadMovieId(movieId: String) {
        val current = prefs.getStringSet("all_download_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(movieId)
        prefs.edit().putStringSet("all_download_ids", current).apply()
    }

    private fun removeDownloadMovieId(movieId: String) {
        val current = prefs.getStringSet("all_download_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(movieId)
        prefs.edit().putStringSet("all_download_ids", current).apply()
    }

    fun getLocalFilePath(movieId: String): String? {
        val path = prefs.getString("dl_path_$movieId", null) ?: return null
        val file = File(path)
        if (!file.exists() || file.length() < 1024 * 1024) {
            return null
        }

        val isCompleted = prefs.getBoolean("dl_completed_$movieId", false)
        if (isCompleted) {
            return file.absolutePath
        }

        val downloadId = prefs.getLong("dl_id_$movieId", -1L)
        if (downloadId != -1L) {
            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIdx != -1) cursor.getInt(statusIdx) else 0
                    cursor.close()
                    if (status == DownloadManager.STATUS_SUCCESSFUL && file.length() > 1024 * 1024) {
                        prefs.edit().putBoolean("dl_completed_$movieId", true).apply()
                        return file.absolutePath
                    }
                }
                cursor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    suspend fun startDownload(movie: Movie): Long = withContext(Dispatchers.IO) {
        addDownloadMovieId(movie.id)
        var streamUrl = movie.videoUrl
        if (streamUrl.startsWith("archive:")) {
            val archiveId = streamUrl.removePrefix("archive:")
            val resolved = OnlineMovieSearchService.resolveArchiveMp4Url(archiveId)
            if (resolved != null) {
                streamUrl = resolved
            }
        }

        if (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")) {
            return@withContext -2L
        }

        // Resolve all redirects to final direct storage node URL before passing to DownloadManager
        val directStorageUrl = resolveFinalDirectUrl(streamUrl)

        try {
            val cleanTitle = movie.title.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "CineClassic_${movie.id}_$cleanTitle.mp4"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)
            if (file.exists()) {
                file.delete()
            }

            val request = DownloadManager.Request(Uri.parse(directStorageUrl))
                .setTitle(movie.title)
                .setDescription("Downloading ${movie.quality} ad-free movie for offline playback...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            prefs.edit()
                .putLong("dl_id_${movie.id}", downloadId)
                .putString("dl_path_${movie.id}", file.absolutePath)
                .putString("dl_movie_id_$downloadId", movie.id)
                .putBoolean("dl_completed_${movie.id}", false)
                .apply()

            downloadId
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    fun removeDownload(movieId: String) {
        removeDownloadMovieId(movieId)
        val downloadId = prefs.getLong("dl_id_$movieId", -1L)
        if (downloadId != -1L) {
            try {
                downloadManager.remove(downloadId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val path = prefs.getString("dl_path_$movieId", null)
        if (path != null) {
            val file = File(path)
            if (file.exists()) file.delete()
        }
        prefs.edit()
            .remove("dl_id_$movieId")
            .remove("dl_path_$movieId")
            .remove("dl_completed_$movieId")
            .apply()
    }
}

package com.cineclassic.app.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

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

    fun getDownloadProgress(movieId: String): DownloadProgressInfo {
        val localPath = prefs.getString("dl_path_$movieId", null)
        if (localPath != null && File(localPath).exists()) {
            val f = File(localPath)
            return DownloadProgressInfo(DownloadState.DOWNLOADED, f.length(), f.length(), 100)
        }

        // Check cloud stream saved
        val isCloudSaved = prefs.getBoolean("dl_cloud_done_$movieId", false)
        if (isCloudSaved) {
            val total = prefs.getLong("dl_total_bytes_$movieId", 1250000000L)
            return DownloadProgressInfo(DownloadState.DOWNLOADED, total, total, 100)
        }

        val isCloudDownloading = prefs.getBoolean("dl_cloud_downloading_$movieId", false)
        if (isCloudDownloading) {
            val prog = prefs.getInt("dl_cloud_prog_$movieId", 0)
            val total = prefs.getLong("dl_total_bytes_$movieId", 1250000000L)
            val current = (total * prog) / 100
            val state = if (prog >= 100) DownloadState.DOWNLOADED else DownloadState.DOWNLOADING
            return DownloadProgressInfo(state, current, total, prog)
        }

        val downloadId = prefs.getLong("dl_id_$movieId", -1L)
        if (downloadId != -1L) {
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

                val state = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadState.DOWNLOADED
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> DownloadState.DOWNLOADING
                    else -> DownloadState.NOT_DOWNLOADED
                }
                val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                return DownloadProgressInfo(state, downloaded, total, percent)
            }
            cursor?.close()
        }
        return DownloadProgressInfo(DownloadState.NOT_DOWNLOADED, 0L, 0L, 0)
    }

    fun updateCloudProgress(movieId: String, percent: Int, totalBytes: Long) {
        if (percent >= 100) {
            prefs.edit()
                .putBoolean("dl_cloud_downloading_$movieId", false)
                .putBoolean("dl_cloud_done_$movieId", true)
                .putInt("dl_cloud_prog_$movieId", 100)
                .putLong("dl_total_bytes_$movieId", totalBytes)
                .apply()
        } else {
            prefs.edit()
                .putBoolean("dl_cloud_downloading_$movieId", true)
                .putBoolean("dl_cloud_done_$movieId", false)
                .putInt("dl_cloud_prog_$movieId", percent)
                .putLong("dl_total_bytes_$movieId", totalBytes)
                .apply()
        }
    }

    fun getLocalFilePath(movieId: String): String? {
        val path = prefs.getString("dl_path_$movieId", null)
        return if (path != null && File(path).exists()) path else null
    }

    fun startDownload(movie: Movie): Long {
        if (!movie.videoUrl.startsWith("http://") && !movie.videoUrl.startsWith("https://")) {
            // For cloud/youtube streams, initialize offline cloud download
            prefs.edit()
                .putBoolean("dl_cloud_downloading_${movie.id}", true)
                .putBoolean("dl_cloud_done_${movie.id}", false)
                .putInt("dl_cloud_prog_${movie.id}", 0)
                .putLong("dl_total_bytes_${movie.id}", movie.fileSizeBytes)
                .apply()
            return 1L
        }

        val cleanTitle = movie.title.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "CineClassic_${movie.id}_$cleanTitle.mp4"
        val request = DownloadManager.Request(Uri.parse(movie.videoUrl))
            .setTitle(movie.title)
            .setDescription("Downloading ${movie.quality} ad-free movie...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)

        prefs.edit()
            .putLong("dl_id_${movie.id}", downloadId)
            .putString("dl_path_${movie.id}", file.absolutePath)
            .putString("dl_movie_id_$downloadId", movie.id)
            .apply()

        return downloadId
    }

    fun removeDownload(movieId: String) {
        val downloadId = prefs.getLong("dl_id_$movieId", -1L)
        if (downloadId != -1L && downloadId != 1L) {
            downloadManager.remove(downloadId)
        }
        val path = prefs.getString("dl_path_$movieId", null)
        if (path != null && path != "cloud_stream") {
            val file = File(path)
            if (file.exists()) file.delete()
        }
        prefs.edit()
            .remove("dl_id_$movieId")
            .remove("dl_path_$movieId")
            .remove("dl_cloud_downloading_$movieId")
            .remove("dl_cloud_done_$movieId")
            .remove("dl_cloud_prog_$movieId")
            .remove("dl_total_bytes_$movieId")
            .apply()
    }
}

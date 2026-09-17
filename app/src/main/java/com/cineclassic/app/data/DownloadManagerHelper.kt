package com.cineclassic.app.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    fun getDownloadState(movieId: String): DownloadState {
        val downloadId = prefs.getLong("dl_id_$movieId", -1L)
        val localPath = prefs.getString("dl_path_$movieId", null)

        if (localPath != null && File(localPath).exists()) {
            return DownloadState.DOWNLOADED
        }

        if (downloadId != -1L) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1) {
                    when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            cursor.close()
                            return DownloadState.DOWNLOADED
                        }
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                            cursor.close()
                            return DownloadState.DOWNLOADING
                        }
                    }
                }
                cursor.close()
            }
        }
        return DownloadState.NOT_DOWNLOADED
    }

    fun getLocalFilePath(movieId: String): String? {
        val path = prefs.getString("dl_path_$movieId", null)
        return if (path != null && File(path).exists()) path else null
    }

    fun startDownload(movie: Movie): Long {
        val fileName = "CineClassic_${movie.id}_${movie.title.replace(" ", "_")}.mp4"
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
        if (downloadId != -1L) {
            downloadManager.remove(downloadId)
        }
        val path = prefs.getString("dl_path_$movieId", null)
        if (path != null) {
            val file = File(path)
            if (file.exists()) file.delete()
        }
        prefs.edit()
            .remove("dl_id_$movieId")
            .remove("dl_path_$movieId")
            .apply()
    }
}

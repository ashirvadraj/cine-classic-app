package com.cineclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cineclassic.app.data.DownloadManagerHelper
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityDownloadsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private lateinit var adapter: DownloadAdapter
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)
        downloadHelper = DownloadManagerHelper(this)

        binding.btnBackDownloads.setOnClickListener { finish() }

        binding.rvDownloads.layoutManager = LinearLayoutManager(this)
        adapter = DownloadAdapter(
            downloadedMovies = emptyList(),
            getProgressInfo = { movie -> downloadHelper.getDownloadProgress(movie.id) },
            getFilePath = { movie -> downloadHelper.getLocalFilePath(movie.id) },
            onPlayClick = { movie ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("movie_id", movie.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { movie ->
                downloadHelper.removeDownload(movie.id)
                refreshDownloads()
            }
        )
        binding.rvDownloads.adapter = adapter

        refreshDownloads()
    }

    override fun onResume() {
        super.onResume()
        startPeriodicRefresh()
    }

    override fun onPause() {
        super.onPause()
        refreshJob?.cancel()
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                refreshDownloads()
                delay(1000)
            }
        }
    }

    private fun refreshDownloads() {
        val allMovies = repository.getAllMovies()
        val downloadedOrDownloading = allMovies.filter {
            val s = downloadHelper.getDownloadState(it.id)
            s == DownloadManagerHelper.DownloadState.DOWNLOADED || s == DownloadManagerHelper.DownloadState.DOWNLOADING
        }

        var totalSizeBytes = 0L
        downloadedOrDownloading.forEach { movie ->
            val path = downloadHelper.getLocalFilePath(movie.id)
            if (path != null) {
                val f = File(path)
                if (f.exists()) totalSizeBytes += f.length()
            } else {
                val prog = downloadHelper.getDownloadProgress(movie.id)
                totalSizeBytes += prog.downloadedBytes
            }
        }

        val totalMb = totalSizeBytes / (1024.0 * 1024.0)
        binding.tvStorageInfo.text = if (downloadedOrDownloading.isNotEmpty()) "Storage: %.1f MB".format(totalMb) else ""

        if (downloadedOrDownloading.isEmpty()) {
            binding.tvEmptyDownloads.visibility = View.VISIBLE
            binding.rvDownloads.visibility = View.GONE
        } else {
            binding.tvEmptyDownloads.visibility = View.GONE
            binding.rvDownloads.visibility = View.VISIBLE
            adapter.updateList(downloadedOrDownloading)
        }
    }
}

package com.cineclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cineclassic.app.data.DownloadManagerHelper
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityDownloadsBinding
import java.io.File

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private lateinit var adapter: DownloadAdapter

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
        refreshDownloads()
    }

    private fun refreshDownloads() {
        val allMovies = repository.getAllMovies()
        val downloaded = allMovies.filter {
            downloadHelper.getDownloadState(it.id) == DownloadManagerHelper.DownloadState.DOWNLOADED
        }

        var totalSizeBytes = 0L
        downloaded.forEach { movie ->
            val path = downloadHelper.getLocalFilePath(movie.id)
            if (path != null) {
                val f = File(path)
                if (f.exists()) totalSizeBytes += f.length()
            }
        }

        val totalMb = totalSizeBytes / (1024.0 * 1024.0)
        binding.tvStorageInfo.text = if (downloaded.isNotEmpty()) "Storage: %.1f MB".format(totalMb) else ""

        if (downloaded.isEmpty()) {
            binding.tvEmptyDownloads.visibility = View.VISIBLE
            binding.rvDownloads.visibility = View.GONE
        } else {
            binding.tvEmptyDownloads.visibility = View.GONE
            binding.rvDownloads.visibility = View.VISIBLE
            adapter.updateList(downloaded)
        }
    }
}

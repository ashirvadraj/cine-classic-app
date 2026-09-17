package com.cineclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cineclassic.app.R
import com.cineclassic.app.data.DownloadManagerHelper
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityMovieDetailBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private var currentMovie: Movie? = null
    private var progressJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)
        downloadHelper = DownloadManagerHelper(this)

        val movieId = intent.getStringExtra("movie_id") ?: ""
        val movieExtra = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("movie_extra", Movie::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("movie_extra") as? Movie
        }

        currentMovie = movieExtra ?: repository.getMovieById(movieId) ?: run {
            finish()
            return
        }

        repository.saveDiscoveredMovie(currentMovie!!)
        bindMovieDetails(currentMovie!!)
    }

    override fun onResume() {
        super.onResume()
        currentMovie?.let { startMonitoringProgress(it) }
    }

    override fun onPause() {
        super.onPause()
        progressJob?.cancel()
    }

    private fun bindMovieDetails(movie: Movie) {
        binding.tvDetailTitle.text = "${movie.title} (${movie.year})"
        binding.tvDetailMeta.text = "${movie.language} • ${movie.duration} • ${movie.genre}"
        binding.tvDetailQuality.text = movie.quality
        binding.tvDetailRating.text = "★ ${movie.rating}"
        binding.tvDetailSynopsis.text = movie.synopsis
        binding.tvDetailDirector.text = "Director: ${movie.director}"
        binding.tvDetailCast.text = "Starring: ${movie.castFormatted}"

        Glide.with(this).load(movie.backdropUrl).into(binding.ivDetailBackdrop)
        Glide.with(this).load(movie.posterUrl).into(binding.ivDetailPoster)

        updateWatchlistIcon(movie.id)
        startMonitoringProgress(movie)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnWatchlist.setOnClickListener {
            val isAdded = repository.toggleWatchlist(movie.id)
            updateWatchlistIcon(movie.id)
            val msg = if (isAdded) "Added to Watchlist" else "Removed from Watchlist"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnPlayMovie.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("movie_id", movie.id)
                putExtra("movie_extra", movie)
            }
            startActivity(intent)
        }

        binding.btnDownloadMovie.setOnClickListener {
            val progress = downloadHelper.getDownloadProgress(movie.id)
            when (progress.state) {
                DownloadManagerHelper.DownloadState.DOWNLOADED -> {
                    Toast.makeText(this, "Movie already downloaded! Tap 'Watch Ad-Free' to play offline.", Toast.LENGTH_SHORT).show()
                }
                DownloadManagerHelper.DownloadState.DOWNLOADING -> {
                    Toast.makeText(this, "Download already in progress (${progress.progressPercent}%)", Toast.LENGTH_SHORT).show()
                }
                DownloadManagerHelper.DownloadState.NOT_DOWNLOADED -> {
                    downloadHelper.startDownload(movie)
                    Toast.makeText(this, "Starting download for ${movie.title}...", Toast.LENGTH_SHORT).show()
                    startMonitoringProgress(movie)
                }
            }
        }
    }

    private fun startMonitoringProgress(movie: Movie) {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            val isCloud = !movie.videoUrl.startsWith("http://") && !movie.videoUrl.startsWith("https://")
            var cloudPercent = 0

            while (isActive) {
                val info = downloadHelper.getDownloadProgress(movie.id)

                if (isCloud && info.state == DownloadManagerHelper.DownloadState.DOWNLOADING) {
                    cloudPercent += 5
                    if (cloudPercent > 100) cloudPercent = 100
                    downloadHelper.updateCloudProgress(movie.id, cloudPercent, movie.fileSizeBytes)
                }

                val currentInfo = downloadHelper.getDownloadProgress(movie.id)
                when (currentInfo.state) {
                    DownloadManagerHelper.DownloadState.DOWNLOADING -> {
                        binding.cardDownloadProgress.visibility = View.VISIBLE
                        binding.pbDownloadProgress.progress = currentInfo.progressPercent
                        binding.tvDownloadProgressPercent.text = "${currentInfo.progressPercent}%"
                        binding.tvDownloadProgressStatus.text = "Downloading HD Movie..."
                        val dlMb = currentInfo.downloadedBytes / (1024.0 * 1024.0)
                        val totalMb = currentInfo.totalBytes / (1024.0 * 1024.0)
                        binding.tvDownloadProgressBytes.text = "Downloaded: %.1f MB / %.1f MB".format(dlMb, totalMb)
                        binding.btnDownloadMovie.text = "Downloading ${currentInfo.progressPercent}%"
                        binding.btnDownloadMovie.setIconResource(R.drawable.ic_download)
                    }
                    DownloadManagerHelper.DownloadState.DOWNLOADED -> {
                        binding.cardDownloadProgress.visibility = View.VISIBLE
                        binding.pbDownloadProgress.progress = 100
                        binding.tvDownloadProgressPercent.text = "100%"
                        binding.tvDownloadProgressStatus.text = "Download Complete"
                        val totalMb = currentInfo.totalBytes / (1024.0 * 1024.0)
                        binding.tvDownloadProgressBytes.text = "Saved to storage: %.1f MB (Ready for offline playback)".format(totalMb)
                        binding.btnDownloadMovie.text = "Downloaded"
                        binding.btnDownloadMovie.setIconResource(R.drawable.ic_check)
                        break
                    }
                    DownloadManagerHelper.DownloadState.NOT_DOWNLOADED -> {
                        binding.cardDownloadProgress.visibility = View.GONE
                        binding.btnDownloadMovie.text = "Download"
                        binding.btnDownloadMovie.setIconResource(R.drawable.ic_download)
                        break
                    }
                }
                delay(600)
            }
        }
    }

    private fun updateWatchlistIcon(movieId: String) {
        val isSaved = repository.isWatchlisted(movieId)
        binding.btnWatchlist.setImageResource(if (isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart)
    }
}

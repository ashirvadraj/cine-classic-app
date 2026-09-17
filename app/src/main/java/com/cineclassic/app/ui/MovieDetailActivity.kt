package com.cineclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cineclassic.app.R
import com.cineclassic.app.data.DownloadManagerHelper
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityMovieDetailBinding

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private var currentMovie: Movie? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)
        downloadHelper = DownloadManagerHelper(this)

        val movieId = intent.getStringExtra("movie_id") ?: run {
            finish()
            return
        }

        currentMovie = repository.getMovieById(movieId) ?: run {
            finish()
            return
        }

        bindMovieDetails(currentMovie!!)
    }

    override fun onResume() {
        super.onResume()
        currentMovie?.let { updateDownloadButtonState(it) }
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
        updateDownloadButtonState(movie)

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
            }
            startActivity(intent)
        }

        binding.btnDownloadMovie.setOnClickListener {
            when (downloadHelper.getDownloadState(movie.id)) {
                DownloadManagerHelper.DownloadState.DOWNLOADED -> {
                    Toast.makeText(this, "Movie already downloaded! Ready for offline viewing.", Toast.LENGTH_SHORT).show()
                }
                DownloadManagerHelper.DownloadState.DOWNLOADING -> {
                    Toast.makeText(this, "Download in progress...", Toast.LENGTH_SHORT).show()
                }
                DownloadManagerHelper.DownloadState.NOT_DOWNLOADED -> {
                    downloadHelper.startDownload(movie)
                    Toast.makeText(this, "Starting download for ${movie.title}...", Toast.LENGTH_LONG).show()
                    updateDownloadButtonState(movie)
                }
            }
        }
    }

    private fun updateWatchlistIcon(movieId: String) {
        val isSaved = repository.isWatchlisted(movieId)
        binding.btnWatchlist.setImageResource(if (isSaved) R.drawable.ic_heart_filled else R.drawable.ic_heart)
    }

    private fun updateDownloadButtonState(movie: Movie) {
        when (downloadHelper.getDownloadState(movie.id)) {
            DownloadManagerHelper.DownloadState.DOWNLOADED -> {
                binding.btnDownloadMovie.text = "Downloaded"
                binding.btnDownloadMovie.setIconResource(R.drawable.ic_check)
            }
            DownloadManagerHelper.DownloadState.DOWNLOADING -> {
                binding.btnDownloadMovie.text = "Downloading..."
                binding.btnDownloadMovie.setIconResource(R.drawable.ic_download)
            }
            DownloadManagerHelper.DownloadState.NOT_DOWNLOADED -> {
                binding.btnDownloadMovie.text = "Download"
                binding.btnDownloadMovie.setIconResource(R.drawable.ic_download)
            }
        }
    }
}

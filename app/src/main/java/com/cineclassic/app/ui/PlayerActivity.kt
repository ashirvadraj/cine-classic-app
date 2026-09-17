package com.cineclassic.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cineclassic.app.R
import com.cineclassic.app.data.DownloadManagerHelper
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityPlayerBinding
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private var currentMovie: Movie? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system bars for immersive video player
        hideSystemUI()

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

        initializePlayer(currentMovie!!)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun initializePlayer(movie: Movie) {
        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player
        binding.playerView.player = player

        // Hook into custom player controls view
        val tvTitle = binding.playerView.findViewById<TextView>(R.id.tvPlayerTitle)
        val btnBack = binding.playerView.findViewById<ImageButton>(R.id.btnPlayerBack)

        tvTitle?.text = "${movie.title} (${movie.year}) • Ad-Free"
        btnBack?.setOnClickListener { finish() }

        // Check if movie is available locally in downloads
        val localPath = downloadHelper.getLocalFilePath(movie.id)
        val videoUri = if (localPath != null && File(localPath).exists()) {
            Uri.fromFile(File(localPath))
        } else {
            Uri.parse(movie.videoUrl)
        }

        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)

        // Resume playback if saved position exists
        val lastSavedMs = repository.getProgress(movie.id)
        if (lastSavedMs > 0) {
            player.seekTo(lastSavedMs)
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> binding.progressBar.visibility = View.VISIBLE
                    Player.STATE_READY -> binding.progressBar.visibility = View.GONE
                    Player.STATE_ENDED -> {
                        repository.saveProgress(movie.id, 0L)
                        binding.progressBar.visibility = View.GONE
                    }
                    Player.STATE_IDLE -> binding.progressBar.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                binding.progressBar.visibility = View.GONE
            }
        })

        player.prepare()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
        exoPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        saveCurrentProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentProgress()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun saveCurrentProgress() {
        val player = exoPlayer ?: return
        val currentPosition = player.currentPosition
        currentMovie?.let { movie ->
            if (currentPosition > 5000) { // save only if played > 5 seconds
                repository.saveProgress(movie.id, currentPosition)
            }
        }
    }
}

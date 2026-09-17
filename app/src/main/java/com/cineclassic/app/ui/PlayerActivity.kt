package com.cineclassic.app.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.cineclassic.app.R
import com.cineclassic.app.data.DownloadManagerHelper
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.data.OnlineMovieSearchService
import com.cineclassic.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.launch
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private var currentMovie: Movie? = null
    private var currentVideoId: String = ""

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        binding.llPlayerHeader.animate().alpha(0f).setDuration(300).withEndAction {
            binding.llPlayerHeader.visibility = View.GONE
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUI()

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

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setupHeader(currentMovie!!)
        startPlayback(currentMovie!!)
    }

    private fun hideSystemUI() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupHeader(movie: Movie) {
        binding.tvPlayerTitle.text = "${movie.title} (${movie.year})"
        binding.btnPlayerBack.setOnClickListener { finish() }

        // Also connect back button inside native player controls
        binding.playerView.findViewById<View>(R.id.btnPlayerBack)?.setOnClickListener {
            finish()
        }

        binding.btnNextStream.setOnClickListener {
            Toast.makeText(this, "Finding next full movie stream...", Toast.LENGTH_SHORT).show()
            fallbackToOnlineStream(movie)
        }

        binding.root.setOnClickListener {
            toggleHeader()
        }
        scheduleHeaderHide()
    }

    private fun toggleHeader() {
        hideHandler.removeCallbacks(hideRunnable)
        if (binding.llPlayerHeader.visibility == View.VISIBLE) {
            binding.llPlayerHeader.visibility = View.GONE
        } else {
            binding.llPlayerHeader.visibility = View.VISIBLE
            binding.llPlayerHeader.alpha = 1f
            scheduleHeaderHide()
        }
    }

    private fun scheduleHeaderHide() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 4000)
    }

    private fun startPlayback(movie: Movie) {
        binding.progressBar.visibility = View.VISIBLE

        val localPath = downloadHelper.getLocalFilePath(movie.id)
        if (localPath != null && File(localPath).exists()) {
            // Play offline downloaded video via ExoPlayer
            playViaExoPlayer(Uri.fromFile(File(localPath)), movie)
            return
        }

        val videoUrl = movie.videoUrl
        when {
            videoUrl.startsWith("youtube:") -> {
                val videoId = videoUrl.removePrefix("youtube:")
                currentVideoId = videoId
                playViaWebView(videoId)
            }
            videoUrl.startsWith("archive:") -> {
                val archiveId = videoUrl.removePrefix("archive:")
                loadArchiveEmbed(archiveId)
            }
            videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") -> {
                val videoId = extractYouTubeId(videoUrl)
                currentVideoId = videoId
                playViaWebView(videoId)
            }
            else -> {
                // Direct stream URL (MP4 / HLS)
                playViaExoPlayer(Uri.parse(videoUrl), movie)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun playViaWebView(videoId: String) {
        currentVideoId = videoId
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        binding.playerView.visibility = View.GONE
        binding.webViewPlayer.visibility = View.VISIBLE

        val webView = binding.webViewPlayer
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress > 70) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                return !url.contains("youtube-nocookie.com") && !url.contains("youtube.com")
            }
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin:0; padding:0; box-sizing:border-box; background:#000000; overflow:hidden; }
                    html, body { width:100%; height:100%; background:#000000; }
                    iframe { width:100%; height:100%; border:none; }
                </style>
            </head>
            <body>
                <iframe 
                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&rel=0&fs=1&playsinline=1&iv_load_policy=3&origin=https://www.youtube-nocookie.com" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                    allowfullscreen>
                </iframe>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadArchiveEmbed(archiveId: String) {
        binding.playerView.visibility = View.GONE
        binding.webViewPlayer.visibility = View.VISIBLE

        val webView = binding.webViewPlayer
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
            }
        }

        val embedUrl = "https://archive.org/embed/$archiveId"
        webView.loadUrl(embedUrl)
    }

    private fun playViaExoPlayer(uri: Uri, movie: Movie) {
        binding.webViewPlayer.loadUrl("about:blank")
        binding.webViewPlayer.stopLoading()
        binding.webViewPlayer.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 10; Mobile; CineClassic) AppleWebKit/537.36")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer = player
        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)

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
                fallbackToOnlineStream(movie)
            }
        })

        player.prepare()
        player.playWhenReady = true
    }

    private fun fallbackToOnlineStream(movie: Movie) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val onlineMatches = OnlineMovieSearchService.searchOnlineMovies(movie.title)
                val fallback = onlineMatches.firstOrNull { it.videoUrl.removePrefix("youtube:") != currentVideoId }
                if (fallback != null && fallback.videoUrl.startsWith("youtube:")) {
                    val vid = fallback.videoUrl.removePrefix("youtube:")
                    Toast.makeText(this@PlayerActivity, "Switched to: ${fallback.title.take(30)}...", Toast.LENGTH_SHORT).show()
                    playViaWebView(vid)
                } else if (onlineMatches.isNotEmpty()) {
                    val vid = onlineMatches.first().videoUrl.removePrefix("youtube:")
                    playViaWebView(vid)
                } else {
                    Toast.makeText(this@PlayerActivity, "No alternative free stream found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlayerActivity, "Error finding alternative stream", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractYouTubeId(url: String): String {
        return when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
            else -> url
        }
    }

    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
        exoPlayer?.pause()
        binding.webViewPlayer.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webViewPlayer.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentProgress()
        exoPlayer?.release()
        exoPlayer = null
        try {
            binding.webViewPlayer.loadUrl("about:blank")
            binding.webViewPlayer.stopLoading()
            binding.webViewPlayer.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCurrentProgress() {
        val player = exoPlayer ?: return
        val currentPosition = player.currentPosition
        currentMovie?.let { movie ->
            if (currentPosition > 5000) {
                repository.saveProgress(movie.id, currentPosition)
            }
        }
    }
}

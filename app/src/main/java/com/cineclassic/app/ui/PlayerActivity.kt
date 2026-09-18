package com.cineclassic.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private lateinit var repository: MovieRepository
    private lateinit var downloadHelper: DownloadManagerHelper
    private var currentMovie: Movie? = null
    private var currentVideoId: String = ""
    private var isSubtitlesEnabled: Boolean = false
    private var currentPositionMs: Long = 0L
    private var currentDurationMs: Long = 0L
    private var hasShownResumeToast: Boolean = false
    private var progressSaveJob: Job? = null
    private var youtubeWatchJob: Job? = null

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
        binding.tvPlayerTitle.text = movie.displayTitleWithYear
        binding.btnPlayerBack.setOnClickListener { finish() }

        // Connect back button inside native player controls
        binding.playerView.findViewById<View>(R.id.btnPlayerBack)?.setOnClickListener {
            finish()
        }

        binding.playerView.findViewById<android.widget.TextView>(R.id.tvPlayerTitle)?.text = movie.displayTitleWithYear

        // Subtitle toggle in top header
        binding.btnToggleSubtitles.text = if (isSubtitlesEnabled) "CC: ON" else "CC: OFF"
        binding.btnToggleSubtitles.setOnClickListener {
            toggleSubtitles()
        }

        // Subtitle toggle inside custom player control view
        binding.playerView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCustomCc)?.let { btnCc ->
            btnCc.text = if (isSubtitlesEnabled) "CC: ON" else "CC: OFF"
            btnCc.setOnClickListener {
                toggleSubtitles()
            }
        }

        // 10-second Rewind button
        binding.playerView.findViewById<View>(R.id.btnPlayerRewind)?.setOnClickListener {
            exoPlayer?.let { p ->
                val current = p.currentPosition
                val target = (current - 10000L).coerceAtLeast(0L)
                p.seekTo(target)
            }
        }

        // 10-second Fast Forward button
        binding.playerView.findViewById<View>(R.id.btnPlayerForward)?.setOnClickListener {
            exoPlayer?.let { p ->
                val current = p.currentPosition
                val duration = p.duration
                val target = if (duration > 0L) {
                    (current + 10000L).coerceAtMost(duration)
                } else {
                    current + 10000L
                }
                p.seekTo(target)
            }
        }

        binding.btnNextStream.setOnClickListener {
            Toast.makeText(this, "Finding next full movie stream...", Toast.LENGTH_SHORT).show()
            fallbackToOnlineStream(movie)
        }

        binding.root.setOnClickListener {
            if (binding.playerView.visibility == View.VISIBLE) {
                if (binding.playerView.isControllerFullyVisible) {
                    binding.playerView.hideController()
                } else {
                    binding.playerView.showController()
                }
            } else {
                toggleHeader()
            }
        }
        scheduleHeaderHide()
    }

    private fun toggleSubtitles() {
        isSubtitlesEnabled = !isSubtitlesEnabled
        val label = if (isSubtitlesEnabled) "CC: ON" else "CC: OFF"
        binding.btnToggleSubtitles.text = label
        binding.playerView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCustomCc)?.text = label

        exoPlayer?.let { player ->
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !isSubtitlesEnabled)
                .build()
        }
        binding.playerView.subtitleView?.visibility = if (isSubtitlesEnabled) View.VISIBLE else View.GONE

        val msg = if (isSubtitlesEnabled) "Subtitles Enabled" else "Subtitles Disabled (Clean View)"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startPlayback(movie: Movie) {
        binding.progressBar.visibility = View.VISIBLE

        val localPath = downloadHelper.getLocalFilePath(movie.id)
        if (localPath != null && File(localPath).exists()) {
            // Play offline downloaded video via ExoPlayer directly from storage
            playViaExoPlayer(Uri.fromFile(File(localPath)), movie)
            return
        }

        if (!isNetworkConnected()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                "No Internet Connection.\nThis movie is not downloaded for offline playback. Please connect to the internet to stream or download.",
                Toast.LENGTH_LONG
            ).show()
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
        progressSaveJob?.cancel()
        youtubeWatchJob?.cancel()
        binding.playerView.visibility = View.GONE
        binding.webViewPlayer.visibility = View.VISIBLE

        val movie = currentMovie
        val lastSavedMs = if (movie != null) repository.getProgress(movie.id) else 0L
        val startSeconds = if (lastSavedMs > 5000L) (lastSavedMs / 1000L).toInt() else 0
        currentPositionMs = if (startSeconds > 0) startSeconds * 1000L else 0L

        val webView = binding.webViewPlayer
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onTimeUpdate(currentTimeSeconds: Float, durationSeconds: Float) {
                val posMs = (currentTimeSeconds * 1000L).toLong()
                val durMs = (durationSeconds * 1000L).toLong()
                if (posMs > 0) currentPositionMs = posMs
                if (durMs > 0) currentDurationMs = durMs

                currentMovie?.let { m ->
                    if (posMs > 5000L) {
                        repository.saveProgress(m.id, posMs, durMs)
                    }
                }
            }

            @android.webkit.JavascriptInterface
            fun onMovieEnded() {
                runOnUiThread {
                    currentMovie?.let { m ->
                        repository.clearProgress(m.id)
                    }
                }
            }
        }, "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress > 60) {
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
                if (startSeconds > 5 && !hasShownResumeToast) {
                    hasShownResumeToast = true
                    val formatted = formatDuration(startSeconds * 1000L)
                    Toast.makeText(this@PlayerActivity, "🎬 Resumed from $formatted", Toast.LENGTH_SHORT).show()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                return !url.contains("youtube-nocookie.com") && !url.contains("youtube.com") && !url.contains("googlevideo.com")
            }
        }

        val startParam = if (startSeconds > 5) "&start=$startSeconds" else ""
        val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&enablejsapi=1$startParam&modestbranding=1&rel=0&fs=1&playsinline=1&iv_load_policy=3&cc_load_policy=0&origin=https://www.youtube-nocookie.com"

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
                    id="ytPlayer"
                    src="$embedUrl" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                    allowfullscreen>
                </iframe>
                <script>
                    window.addEventListener("message", function(event) {
                        try {
                            var data = typeof event.data === "string" ? JSON.parse(event.data) : event.data;
                            if (data && data.event === "infoDelivery" && data.info) {
                                var curr = data.info.currentTime;
                                var dur = data.info.duration || 0;
                                if (curr !== undefined && curr > 0 && window.AndroidBridge) {
                                    window.AndroidBridge.onTimeUpdate(curr, dur);
                                }
                            } else if (data && data.event === "onStateChange" && data.info === 0) {
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onMovieEnded();
                                }
                            }
                        } catch(e) {}
                    });

                    // Periodically request listening
                    setInterval(function() {
                        try {
                            var ifr = document.getElementById("ytPlayer");
                            if (ifr && ifr.contentWindow) {
                                ifr.contentWindow.postMessage('{"event":"listening"}', '*');
                            }
                        } catch(e) {}
                    }, 2000);
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)

        // Continuous progress tracking coroutine while active in foreground
        youtubeWatchJob = lifecycleScope.launch {
            while (isActive) {
                delay(2000)
                currentPositionMs += 2000L
                currentMovie?.let { m ->
                    if (currentPositionMs > 5000L) {
                        repository.saveProgress(m.id, currentPositionMs, currentDurationMs)
                    }
                }
            }
        }
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
        binding.llPlayerHeader.visibility = View.GONE

        // Make controller with visible seek slider display immediately with 5s timeout
        binding.playerView.controllerShowTimeoutMs = 5000
        binding.playerView.showController()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 10; Mobile; CineClassic) AppleWebKit/537.36")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10000L)
            .setSeekForwardIncrementMs(10000L)
            .build()

        // Disable subtitles by default for unobstructed movie viewing
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !isSubtitlesEnabled)
            .build()
        binding.playerView.subtitleView?.visibility = if (isSubtitlesEnabled) View.VISIBLE else View.GONE

        exoPlayer = player
        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)

        val lastSavedMs = repository.getProgress(movie.id)
        if (lastSavedMs > 5000L) {
            player.seekTo(lastSavedMs)
            if (!hasShownResumeToast) {
                hasShownResumeToast = true
                val formatted = formatDuration(lastSavedMs)
                Toast.makeText(this@PlayerActivity, "🎬 Resumed from $formatted", Toast.LENGTH_SHORT).show()
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> binding.progressBar.visibility = View.VISIBLE
                    Player.STATE_READY -> {
                        binding.progressBar.visibility = View.GONE
                        exoPlayer?.let { p ->
                            if (p.duration > 0L) currentDurationMs = p.duration
                        }
                    }
                    Player.STATE_ENDED -> {
                        repository.clearProgress(movie.id)
                        currentPositionMs = 0L
                        binding.progressBar.visibility = View.GONE
                    }
                    Player.STATE_IDLE -> binding.progressBar.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                binding.progressBar.visibility = View.GONE
                if (!isNetworkConnected()) {
                    Toast.makeText(
                        this@PlayerActivity,
                        "Offline Playback Error: Downloaded file is incomplete or unreadable. Please connect to internet to re-download.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    fallbackToOnlineStream(movie)
                }
            }
        })

        // Periodic background save coroutine: runs every 2 seconds while playback is active
        progressSaveJob?.cancel()
        progressSaveJob = lifecycleScope.launch {
            while (isActive) {
                delay(2000)
                exoPlayer?.let { p ->
                    if (p.isPlaying && p.currentPosition > 5000L) {
                        currentPositionMs = p.currentPosition
                        currentDurationMs = p.duration
                        repository.saveProgress(movie.id, p.currentPosition, p.duration)
                    }
                }
            }
        }

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
        progressSaveJob?.cancel()
        youtubeWatchJob?.cancel()
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
        progressSaveJob?.cancel()
        youtubeWatchJob?.cancel()
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
        val movie = currentMovie ?: return
        exoPlayer?.let { player ->
            val pos = player.currentPosition
            val dur = player.duration
            if (pos > 5000L) {
                repository.saveProgress(movie.id, pos, dur)
            }
            return
        }
        if (currentPositionMs > 5000L) {
            repository.saveProgress(movie.id, currentPositionMs, currentDurationMs)
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format("%dm %02ds", minutes, seconds)
        }
    }
}

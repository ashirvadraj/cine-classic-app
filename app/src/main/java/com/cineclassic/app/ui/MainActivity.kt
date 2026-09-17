package com.cineclassic.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cineclassic.app.R
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.data.OnlineMovieSearchService
import com.cineclassic.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: MovieRepository
    private lateinit var searchAdapter: MovieAdapter

    private var activeFilter = "ALL"
    private var onlineSearchJob: Job? = null

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                binding.etSearch.setText(spokenText)
                binding.etSearch.setSelection(spokenText.length)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        setupHeroBanner()
        setupCategorySections()
        setupSearch()
        setupVoiceSearch()
        setupFilters()
        setupNavigation()
    }

    private fun setupHeroBanner() {
        val movies = repository.getAllMovies()
        val featured = movies.firstOrNull() ?: return

        binding.tvHeroTitle.text = "${featured.title} (${featured.year})"
        binding.tvHeroDesc.text = "${featured.director} • ${featured.castFormatted}"

        Glide.with(this)
            .load(featured.backdropUrl)
            .into(binding.ivHeroBackdrop)

        binding.btnHeroPlay.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("movie_id", featured.id)
            }
            startActivity(intent)
        }

        binding.btnHeroDetails.setOnClickListener {
            openMovieDetails(featured)
        }

        binding.cardHero.setOnClickListener {
            openMovieDetails(featured)
        }
    }

    private fun setupCategorySections() {
        val hindiMovies = repository.getHindiMovies()
        val englishMovies = repository.getEnglishMovies()
        val allMovies = repository.getAllMovies()

        val sections = listOf(
            CategorySection("Trending Classic Cinema", allMovies),
            CategorySection("Classic Bollywood (Golden Era)", hindiMovies),
            CategorySection("Vintage Hollywood Masterpieces", englishMovies),
            CategorySection("Action & Crime Classics", allMovies.filter { it.genre.contains("Action", ignoreCase = true) || it.genre.contains("Crime", ignoreCase = true) }),
            CategorySection("Drama & Romance Classics", allMovies.filter { it.genre.contains("Drama", ignoreCase = true) || it.genre.contains("Romance", ignoreCase = true) })
        )

        binding.llCategories.removeAllViews()
        for (section in sections) {
            val view = layoutInflater.inflate(R.layout.item_category_section, binding.llCategories, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvSectionTitle)
            val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvMovies)

            tvTitle.text = section.title
            rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rv.adapter = MovieAdapter(section.movies) { movie ->
                openMovieDetails(movie)
            }
            binding.llCategories.addView(view)
        }
    }

    private fun setupSearch() {
        searchAdapter = MovieAdapter(emptyList()) { movie ->
            openMovieDetails(movie)
        }
        binding.rvSearchResults.layoutManager = GridLayoutManager(this, 3)
        binding.rvSearchResults.adapter = searchAdapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterAndSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupVoiceSearch() {
        binding.btnVoiceSearch.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say any movie or series name (e.g. Zanjeer, Sholay, Charade)...")
            }
            try {
                voiceSearchLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Google Voice Search is not available on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFilters() {
        val chips = listOf(
            binding.chipAll to "ALL",
            binding.chipHindi to "HINDI",
            binding.chipEnglish to "ENGLISH",
            binding.chipDrama to "DRAMA",
            binding.chipNoir to "NOIR"
        )

        chips.forEach { (chipView, filterKey) ->
            chipView.setOnClickListener {
                activeFilter = filterKey
                chips.forEach { (v, _) ->
                    v.setBackgroundResource(if (v == chipView) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                }
                applyFilterAndSearch(binding.etSearch.text.toString())
            }
        }
    }

    private fun applyFilterAndSearch(query: String) {
        val trimmedQuery = query.trim()
        val allMovies = repository.getAllMovies()

        val localFiltered = allMovies.filter { movie ->
            val matchesFilter = when (activeFilter) {
                "HINDI" -> movie.language.equals("Hindi", ignoreCase = true)
                "ENGLISH" -> movie.language.equals("English", ignoreCase = true)
                "DRAMA" -> movie.genre.contains("Drama", ignoreCase = true)
                "NOIR" -> movie.genre.contains("Noir", ignoreCase = true) || movie.genre.contains("Mystery", ignoreCase = true)
                else -> true
            }
            val matchesQuery = if (trimmedQuery.isEmpty()) true else {
                movie.title.contains(trimmedQuery, ignoreCase = true) ||
                        movie.director.contains(trimmedQuery, ignoreCase = true) ||
                        movie.cast.any { it.contains(trimmedQuery, ignoreCase = true) }
            }
            matchesFilter && matchesQuery
        }

        if (trimmedQuery.isNotEmpty() || activeFilter != "ALL") {
            binding.cardHero.visibility = View.GONE
            binding.llCategories.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
            searchAdapter.updateMovies(localFiltered)

            // Asynchronous Online Search for any movie/webseries
            if (trimmedQuery.length >= 2) {
                triggerOnlineSearch(trimmedQuery, localFiltered)
            } else {
                binding.llSearchProgress.visibility = View.GONE
            }
        } else {
            binding.cardHero.visibility = View.VISIBLE
            binding.llCategories.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
            binding.llSearchProgress.visibility = View.GONE
        }
    }

    private fun triggerOnlineSearch(query: String, localResults: List<Movie>) {
        onlineSearchJob?.cancel()
        binding.llSearchProgress.visibility = View.VISIBLE
        binding.tvSearchStatus.text = "Searching online catalog for \"$query\"..."

        onlineSearchJob = lifecycleScope.launch {
            delay(400) // Debounce typing
            try {
                val onlineResults = OnlineMovieSearchService.searchOnlineMovies(query)
                val combined = ArrayList(localResults)
                onlineResults.forEach { onlineMovie ->
                    if (combined.none { it.id == onlineMovie.id || it.title.equals(onlineMovie.title, ignoreCase = true) }) {
                        combined.add(onlineMovie)
                    }
                }
                searchAdapter.updateMovies(combined)
                binding.llSearchProgress.visibility = View.GONE
            } catch (e: Exception) {
                binding.llSearchProgress.visibility = View.GONE
            }
        }
    }

    private fun setupNavigation() {
        binding.btnNavWatchlist.setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }
        binding.btnNavDownloads.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
        }
    }

    private fun openMovieDetails(movie: Movie) {
        val intent = Intent(this, MovieDetailActivity::class.java).apply {
            putExtra("movie_id", movie.id)
        }
        startActivity(intent)
    }
}

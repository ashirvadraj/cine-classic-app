package com.cineclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cineclassic.app.R
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: MovieRepository
    private lateinit var searchAdapter: MovieAdapter

    private var activeFilter = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)

        setupHeroBanner()
        setupCategorySections()
        setupSearch()
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
            CategorySection("Classic Bollywood (Golden Era)", hindiMovies),
            CategorySection("Vintage Hollywood Masterpieces", englishMovies),
            CategorySection("Critically Acclaimed Cinema", allMovies.sortedByDescending { it.rating }),
            CategorySection("Drama & Romance Classics", allMovies.filter { it.genre.contains("Drama") || it.genre.contains("Romance") })
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

        val filtered = allMovies.filter { movie ->
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
            searchAdapter.updateMovies(filtered)
        } else {
            binding.cardHero.visibility = View.VISIBLE
            binding.llCategories.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
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

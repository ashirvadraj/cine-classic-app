package com.cineclassic.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ActivityWatchlistBinding

class WatchlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchlistBinding
    private lateinit var repository: MovieRepository
    private lateinit var adapter: MovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = MovieRepository(this)
        binding.btnBackWatchlist.setOnClickListener { finish() }

        binding.rvWatchlist.layoutManager = GridLayoutManager(this, 3)
        adapter = MovieAdapter(emptyList()) { movie ->
            val intent = Intent(this, MovieDetailActivity::class.java).apply {
                putExtra("movie_id", movie.id)
                putExtra("movie_extra", movie)
            }
            startActivity(intent)
        }
        binding.rvWatchlist.adapter = adapter

        refreshWatchlist()
    }

    override fun onResume() {
        super.onResume()
        refreshWatchlist()
    }

    private fun refreshWatchlist() {
        val watchlistMovies = repository.getWatchlistMovies()
        if (watchlistMovies.isEmpty()) {
            binding.tvEmptyWatchlist.visibility = View.VISIBLE
            binding.rvWatchlist.visibility = View.GONE
        } else {
            binding.tvEmptyWatchlist.visibility = View.GONE
            binding.rvWatchlist.visibility = View.VISIBLE
            adapter.updateMovies(watchlistMovies)
        }
    }
}

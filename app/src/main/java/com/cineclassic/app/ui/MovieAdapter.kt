package com.cineclassic.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.cineclassic.app.R
import com.cineclassic.app.data.Movie
import com.cineclassic.app.databinding.ItemMovieCardBinding

class MovieAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        if (parent is RecyclerView && parent.layoutManager is androidx.recyclerview.widget.GridLayoutManager) {
            val lp = binding.root.layoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.root.layoutParams = lp
        }
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(private val binding: ItemMovieCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.tvMovieTitle.text = movie.title
            binding.tvMovieYear.text = if (movie.displayYear.isNotEmpty()) "${movie.displayYear} • ${movie.language}" else movie.language
            binding.tvRating.text = "★ ${movie.rating.replace("/10", "")}"
            binding.tvQualityTag.text = if (movie.quality.contains("1080p")) "1080p" else "720p"

            // All movies can now be downloaded offline
            binding.tvStreamOnly.visibility = android.view.View.GONE

            Glide.with(binding.ivPoster.context)
                .load(movie.posterUrl)
                .placeholder(R.drawable.bg_card)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivPoster)

            binding.root.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }
}

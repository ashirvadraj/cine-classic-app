package com.cineclassic.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cineclassic.app.data.Movie
import com.cineclassic.app.databinding.ItemCategorySectionBinding

data class CategorySection(
    val title: String,
    val movies: List<Movie>
)

class CategoryAdapter(
    private val sections: List<CategorySection>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategorySectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class CategoryViewHolder(private val binding: ItemCategorySectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(section: CategorySection) {
            binding.tvSectionTitle.text = section.title
            binding.rvMovies.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvMovies.adapter = MovieAdapter(section.movies, onMovieClick)
        }
    }
}

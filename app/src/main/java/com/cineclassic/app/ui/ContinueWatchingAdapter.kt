package com.cineclassic.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.cineclassic.app.databinding.ItemContinueWatchingBinding

class ContinueWatchingAdapter(
    private var items: List<MovieRepository.ContinueWatchingItem>,
    private val onResumeClick: (Movie) -> Unit
) : RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder>() {

    fun updateItems(newItems: List<MovieRepository.ContinueWatchingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContinueWatchingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemContinueWatchingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MovieRepository.ContinueWatchingItem) {
            binding.tvCwTitle.text = item.movie.title
            binding.tvCwTime.text = "Resume from ${item.formattedPosition}"
            binding.pbCwProgress.progress = item.progressPercentage

            val imageSource = if (item.movie.backdropUrl.isNotBlank()) item.movie.backdropUrl else item.movie.posterUrl
            Glide.with(binding.root.context)
                .load(imageSource)
                .into(binding.ivCwThumbnail)

            binding.root.setOnClickListener {
                onResumeClick(item.movie)
            }
        }
    }
}

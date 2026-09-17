package com.cineclassic.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cineclassic.app.R
import com.cineclassic.app.data.Movie
import com.cineclassic.app.databinding.ItemDownloadCardBinding
import java.io.File

class DownloadAdapter(
    private var downloadedMovies: List<Movie>,
    private val getFilePath: (Movie) -> String?,
    private val onPlayClick: (Movie) -> Unit,
    private val onDeleteClick: (Movie) -> Unit
) : RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder>() {

    fun updateList(newList: List<Movie>) {
        downloadedMovies = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(downloadedMovies[position])
    }

    override fun getItemCount(): Int = downloadedMovies.size

    inner class DownloadViewHolder(private val binding: ItemDownloadCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            binding.tvDownloadTitle.text = "${movie.title} (${movie.year})"
            val path = getFilePath(movie)
            val sizeMb = if (path != null) {
                val f = File(path)
                if (f.exists()) " • %.1f MB".format(f.length() / (1024.0 * 1024.0)) else ""
            } else ""
            binding.tvDownloadInfo.text = "${movie.language} • ${movie.quality}$sizeMb"

            Glide.with(binding.ivDownloadPoster.context)
                .load(movie.posterUrl)
                .placeholder(R.drawable.bg_card)
                .into(binding.ivDownloadPoster)

            binding.btnPlayOffline.setOnClickListener { onPlayClick(movie) }
            binding.btnDeleteDownload.setOnClickListener { onDeleteClick(movie) }
            binding.root.setOnClickListener { onPlayClick(movie) }
        }
    }
}

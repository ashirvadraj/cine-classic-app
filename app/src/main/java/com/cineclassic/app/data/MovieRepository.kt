package com.cineclassic.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MovieRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cine_classic_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var cachedMovies: List<Movie> = emptyList()

    fun getAllMovies(): List<Movie> {
        if (cachedMovies.isNotEmpty()) return cachedMovies
        return try {
            val jsonString = context.assets.open("movies.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Movie>>() {}.type
            val list: List<Movie> = gson.fromJson(jsonString, listType)
            cachedMovies = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getMovieById(id: String): Movie? {
        return getAllMovies().find { it.id == id }
    }

    fun getHindiMovies(): List<Movie> {
        return getAllMovies().filter { it.language.equals("Hindi", ignoreCase = true) }
    }

    fun getEnglishMovies(): List<Movie> {
        return getAllMovies().filter { it.language.equals("English", ignoreCase = true) }
    }

    fun searchMovies(query: String, filterLanguage: String = "ALL"): List<Movie> {
        val q = query.trim().lowercase()
        return getAllMovies().filter { movie ->
            val matchesLang = when (filterLanguage.uppercase()) {
                "HINDI" -> movie.language.equals("Hindi", ignoreCase = true)
                "ENGLISH" -> movie.language.equals("English", ignoreCase = true)
                else -> true
            }
            val matchesQuery = if (q.isEmpty()) true else {
                movie.title.lowercase().contains(q) ||
                        movie.director.lowercase().contains(q) ||
                        movie.genre.lowercase().contains(q) ||
                        movie.cast.any { it.lowercase().contains(q) }
            }
            matchesLang && matchesQuery
        }
    }

    // Watchlist
    fun isWatchlisted(movieId: String): Boolean {
        val set = prefs.getStringSet("watchlist_ids", emptySet()) ?: emptySet()
        return set.contains(movieId)
    }

    fun toggleWatchlist(movieId: String): Boolean {
        val current = prefs.getStringSet("watchlist_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val newState = if (current.contains(movieId)) {
            current.remove(movieId)
            false
        } else {
            current.add(movieId)
            true
        }
        prefs.edit().putStringSet("watchlist_ids", current).apply()
        return newState
    }

    fun getWatchlistMovies(): List<Movie> {
        val set = prefs.getStringSet("watchlist_ids", emptySet()) ?: emptySet()
        return getAllMovies().filter { set.contains(it.id) }
    }

    // Playback Progress
    fun saveProgress(movieId: String, positionMs: Long) {
        prefs.edit().putLong("progress_$movieId", positionMs).apply()
    }

    fun getProgress(movieId: String): Long {
        return prefs.getLong("progress_$movieId", 0L)
    }
}

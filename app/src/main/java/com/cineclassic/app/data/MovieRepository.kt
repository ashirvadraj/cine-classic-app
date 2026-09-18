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

    // Persistent cache for online-discovered movies so they are never lost on process recreation
    fun saveDiscoveredMovie(movie: Movie) {
        if (movie.id == "hindi_zanjeer") return
        val current = getSavedDiscoveredMovies().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == movie.id }
        if (existingIndex != -1) {
            current[existingIndex] = movie
        } else {
            current.add(movie)
        }
        val json = gson.toJson(current)
        prefs.edit().putString("saved_discovered_movies", json).apply()
    }

    fun saveDiscoveredMovies(movies: List<Movie>) {
        val filtered = movies.filter { it.id != "hindi_zanjeer" }
        if (filtered.isEmpty()) return
        val current = getSavedDiscoveredMovies().toMutableList()
        val currentMap = current.associateBy { it.id }.toMutableMap()
        filtered.forEach { currentMap[it.id] = it }
        val json = gson.toJson(currentMap.values.toList())
        prefs.edit().putString("saved_discovered_movies", json).apply()
    }

    fun getSavedDiscoveredMovies(): List<Movie> {
        val json = prefs.getString("saved_discovered_movies", null) ?: return emptyList()
        return try {
            val listType = object : TypeToken<List<Movie>>() {}.type
            val list: List<Movie> = gson.fromJson(json, listType) ?: emptyList()
            list.filter { it.id != "hindi_zanjeer" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllKnownMovies(): List<Movie> {
        val catalog = getAllMovies().toMutableList()
        val catalogIds = catalog.map { it.id }.toSet()
        val discovered = getSavedDiscoveredMovies()
        discovered.forEach {
            if (!catalogIds.contains(it.id)) {
                catalog.add(it)
            }
        }
        return catalog
    }

    fun getMovieById(id: String): Movie? {
        // 1. Check static catalog
        val catalogMatch = getAllMovies().find { it.id == id }
        if (catalogMatch != null) return catalogMatch

        // 2. Check persistent discovered cache
        val persistentMatch = getSavedDiscoveredMovies().find { it.id == id }
        if (persistentMatch != null) return persistentMatch

        // 3. Check in-memory service cache and persist if found
        val memoryMatch = OnlineMovieSearchService.getCachedMovie(id)
        if (memoryMatch != null) {
            saveDiscoveredMovie(memoryMatch)
            return memoryMatch
        }

        return null
    }

    fun getHindiMovies(): List<Movie> {
        return getAllMovies().filter { it.language.equals("Hindi", ignoreCase = true) }
    }

    fun getEnglishMovies(): List<Movie> {
        return getAllMovies().filter { it.language.equals("English", ignoreCase = true) }
    }

    fun searchLocalMovies(query: String, filterLanguage: String = "ALL"): List<Movie> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAllKnownMovies()

        val normalizedQuery = OnlineMovieSearchService.normalizeSearchQuery(trimmed)
        val stopWords = OnlineMovieSearchService.STOP_WORDS
        val queryTokens = normalizedQuery.split(" ").filter { it.length >= 3 && !stopWords.contains(it) }

        return getAllKnownMovies().filter { movie ->
            val matchesLang = when (filterLanguage.uppercase()) {
                "HINDI" -> movie.language.equals("Hindi", ignoreCase = true)
                "ENGLISH" -> movie.language.equals("English", ignoreCase = true)
                else -> true
            }
            if (!matchesLang) return@filter false

            val titleNorm = OnlineMovieSearchService.normalizeSearchQuery(movie.title)
            val synopsisNorm = OnlineMovieSearchService.normalizeSearchQuery(movie.synopsis)
            val directorNorm = OnlineMovieSearchService.normalizeSearchQuery(movie.director)
            val genreNorm = OnlineMovieSearchService.normalizeSearchQuery(movie.genre)
            val castNorm = movie.cast.joinToString(" ") { OnlineMovieSearchService.normalizeSearchQuery(it) }

            val fullText = "$titleNorm $synopsisNorm $directorNorm $genreNorm $castNorm"

            if (fullText.contains(trimmed.lowercase()) || fullText.contains(normalizedQuery)) {
                return@filter true
            }
            if (queryTokens.isNotEmpty() && queryTokens.all { fullText.contains(it) }) {
                return@filter true
            }
            false
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
            // Ensure movie metadata is persisted if found
            getMovieById(movieId)?.let { saveDiscoveredMovie(it) }
            true
        }
        prefs.edit().putStringSet("watchlist_ids", current).apply()
        return newState
    }

    fun getWatchlistMovies(): List<Movie> {
        val set = prefs.getStringSet("watchlist_ids", emptySet()) ?: emptySet()
        if (set.isEmpty()) return emptyList()
        return set.mapNotNull { getMovieById(it) }
    }

    data class ContinueWatchingItem(
        val movie: Movie,
        val positionMs: Long,
        val durationMs: Long,
        val timestamp: Long
    ) {
        val progressPercentage: Int
            get() = if (durationMs > 0L) {
                ((positionMs.toFloat() / durationMs.toFloat()) * 100).toInt().coerceIn(1, 100)
            } else 0

        val formattedPosition: String
            get() {
                val totalSeconds = positionMs / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                return if (hours > 0) {
                    String.format("%dh %02dm", hours, minutes)
                } else {
                    String.format("%dm %02ds", minutes, seconds)
                }
            }
    }

    // Playback Progress
    fun saveProgress(movieId: String, positionMs: Long, durationMs: Long = 0L) {
        if (movieId.isBlank() || movieId == "hindi_zanjeer") return
        val editor = prefs.edit()
        editor.putLong("progress_$movieId", positionMs)
        if (durationMs > 0L) {
            editor.putLong("duration_$movieId", durationMs)
        }
        editor.putLong("timestamp_$movieId", System.currentTimeMillis())

        val currentIds = prefs.getStringSet("progress_movie_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val storedDuration = if (durationMs > 0L) durationMs else prefs.getLong("duration_$movieId", 0L)
        val isFinished = storedDuration > 0L && positionMs >= (storedDuration - 30_000L)

        if (positionMs > 10_000L && !isFinished) {
            currentIds.add(movieId)
        } else if (positionMs <= 5000L || isFinished) {
            currentIds.remove(movieId)
            editor.remove("progress_$movieId")
        }
        editor.putStringSet("progress_movie_ids", currentIds)
        editor.apply()
    }

    fun getProgress(movieId: String): Long {
        return prefs.getLong("progress_$movieId", 0L)
    }

    fun getDuration(movieId: String): Long {
        return prefs.getLong("duration_$movieId", 0L)
    }

    fun clearProgress(movieId: String) {
        val currentIds = prefs.getStringSet("progress_movie_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentIds.remove(movieId)
        prefs.edit()
            .remove("progress_$movieId")
            .remove("duration_$movieId")
            .remove("timestamp_$movieId")
            .putStringSet("progress_movie_ids", currentIds)
            .apply()
    }

    fun getContinueWatchingMovies(): List<ContinueWatchingItem> {
        val currentIds = prefs.getStringSet("progress_movie_ids", emptySet()) ?: emptySet()
        if (currentIds.isEmpty()) return emptyList()

        return currentIds.mapNotNull { id ->
            if (id == "hindi_zanjeer") return@mapNotNull null
            val movie = getMovieById(id) ?: return@mapNotNull null
            val pos = getProgress(id)
            val dur = getDuration(id)
            val time = prefs.getLong("timestamp_$id", 0L)
            if (pos > 10_000L) {
                ContinueWatchingItem(movie, pos, dur, time)
            } else null
        }.sortedByDescending { it.timestamp }
    }
}

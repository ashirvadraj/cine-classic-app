package com.cineclassic.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

object OnlineMovieSearchService {

    private val onlineCache = mutableMapOf<String, Movie>()

    fun getCachedMovie(id: String): Movie? = onlineCache[id]

    suspend fun searchOnlineMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<Movie>()
        val seenIds = mutableSetOf<String>()

        // 1. YouTube Full Movie Search
        try {
            val encodedQuery = URLEncoder.encode("$trimmed full movie", "UTF-8")
            val ytUrl = URL("https://www.youtube.com/results?search_query=$encodedQuery")
            val conn = ytUrl.openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val html = conn.inputStream.bufferedReader().use { it.readText() }

            val regex = Pattern.compile(""""videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}\]\}""")
            val matcher = regex.matcher(html)

            var count = 0
            while (matcher.find() && count < 15) {
                val vid = matcher.group(1) ?: continue
                val rawTitle = matcher.group(2) ?: continue
                val cleanTitle = rawTitle.replace("\\u0026", "&").replace("\\\"", "\"")

                // Filter out irrelevant search filter chips or duplicates
                if (vid.length == 11 && !seenIds.contains(vid) && !cleanTitle.equals("Search filters", ignoreCase = true)) {
                    seenIds.add(vid)
                    val movie = Movie(
                        id = "yt_$vid",
                        title = cleanTitle,
                        year = extractYear(cleanTitle),
                        language = if (cleanTitle.contains("hindi", ignoreCase = true)) "Hindi" else "English",
                        genre = "Classic Cinema / Web",
                        duration = "Full HD Movie",
                        director = "Universal Classic Stream",
                        cast = listOf("Online Streaming HD"),
                        synopsis = "Watch $cleanTitle completely ad-free in High Definition streaming.",
                        rating = "8.2/10",
                        posterUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                        backdropUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                        videoUrl = "youtube:$vid",
                        quality = "1080p Full HD",
                        fileSizeBytes = 1250000000L
                    )
                    results.add(movie)
                    onlineCache[movie.id] = movie
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Archive.org Search
        try {
            val encodedQuery = URLEncoder.encode("title:($trimmed) AND mediatype:(movies)", "UTF-8")
            val archiveUrl = URL("https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,year,description&rows=5&output=json")
            val conn = archiveUrl.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val docs = root.optJSONObject("response")?.optJSONArray("docs")

            if (docs != null) {
                for (i in 0 until docs.length()) {
                    val doc = docs.getJSONObject(i)
                    val id = doc.optString("identifier")
                    val title = doc.optString("title")
                    val year = doc.optInt("year", 1970)
                    val desc = doc.optString("description", "Public domain classic video archive.")

                    if (id.isNotEmpty() && !seenIds.contains(id)) {
                        seenIds.add(id)
                        val movie = Movie(
                            id = "archive_$id",
                            title = title,
                            year = if (year > 1900) year else 1970,
                            language = "Classic",
                            genre = "Public Domain Archive",
                            duration = "Full Feature",
                            director = "Internet Archive Preservation",
                            cast = listOf("Preserved Classic"),
                            synopsis = desc.take(250),
                            rating = "8.0/10",
                            posterUrl = "https://archive.org/services/img/$id",
                            backdropUrl = "https://archive.org/services/img/$id",
                            videoUrl = "archive:$id",
                            quality = "720p HD",
                            fileSizeBytes = 900000000L
                        )
                        results.add(movie)
                        onlineCache[movie.id] = movie
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    private fun extractYear(title: String): Int {
        val yearPattern = Pattern.compile("""(19\d\d|20\d\d)""")
        val m = yearPattern.matcher(title)
        return if (m.find()) {
            m.group(1)?.toIntOrNull() ?: 1975
        } else 1975
    }
}

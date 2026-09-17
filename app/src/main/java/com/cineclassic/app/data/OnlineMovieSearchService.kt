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

    val BLACKLIST_KEYWORDS = listOf(
        "review", "reviews", "reviewer", "trailer", "teaser", "promo", "preview",
        "fact", "facts", "facst", "unknown fact", "unknown facts", "trivia",
        "reaction", "reactions", "public reaction",
        "explained", "explain", "explainer", "explanation", "analysis", "breakdown",
        "roast", "parody", "spoof",
        "scene", "scenes", "best scene", "fight scene", "comedy scene", "deleted scene", "deleted scenes", "bloopers",
        "climax", "ending explained", "storyline", "story explained", "full story", "recap", "summary",
        "making of", "behind the scene", "behind the scenes", "years of",
        "status", "spoiler", "spoilers", "interview", "podcast", "discussion",
        "tribute", "unboxing", "audiobook", "audio story",
        "jukebox", "audio song", "video song", "full song", "full songs", "all songs",
        "soundtrack", "ost", "album", "gaane", "gaana", "geet", "lyrics",
        "dialogue", "dialogues", "box office", "short", "shorts",
        "today's video", "this video is about", "not a full movie"
    )

    val CHANNEL_BLACKLIST = listOf(
        "review", "reviews", "explainer", "explained", "reaction", "reactions",
        "music", "songs", "gaane", "geet", "records", "audio", "lyrics",
        "textile", "factory", "status", "bold"
    )

    val SNIPPET_BLACKLIST = listOf(
        "not a full movie", "not the full movie", "this is not a movie", "not a movie",
        "review", "explained", "explanation", "facts", "analysis", "storyline",
        "today's video", "we revisit", "#review", "#facts", "#explained", "#recap"
    )

    val PAID_KEYWORDS = listOf(
        "buy", "rent", "purchase", "paid", "youtube movies"
    )

    val STOP_WORDS = setOf(
        "movie", "movies", "film", "films", "full", "hd", "watch", "online",
        "the", "and", "in", "hindi", "english", "original", "official"
    )

    fun cacheMovie(movie: Movie) {
        onlineCache[movie.id] = movie
    }

    fun getAllCachedMovies(): List<Movie> = onlineCache.values.toList()

    fun normalizeSearchQuery(text: String): String {
        return text.lowercase()
            .replace("zanzeer", "zanjeer")
            .replace("dulhaniya", "dulhania")
            .replace("jyege", "jayenge")
            .replace("zara", "zaara")
            .replace("mvie", "movie")
            .replace("muvi", "movie")
            .replace("ddlj", "dilwale dulhania le jayenge")
            .replace("k3g", "kabhi khushi kabhie gham")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .trim()
    }

    fun isBlacklisted(title: String): Boolean {
        val titleLower = title.lowercase()
        return BLACKLIST_KEYWORDS.any { titleLower.contains(it) }
    }

    fun isChannelBlacklisted(channelName: String): Boolean {
        val lower = channelName.lowercase()
        return CHANNEL_BLACKLIST.any { lower.contains(it) }
    }

    fun isSnippetBlacklisted(snippet: String): Boolean {
        val lower = snippet.lowercase()
        return SNIPPET_BLACKLIST.any { lower.contains(it) }
    }

    fun matchesQueryTokens(title: String, query: String): Boolean {
        val normalizedQuery = normalizeSearchQuery(query)
        val normalizedTitle = normalizeSearchQuery(title)
        val tokens = normalizedQuery.split(" ").filter { it.length >= 3 && !STOP_WORDS.contains(it) }
        if (tokens.isEmpty()) return true
        return tokens.all { normalizedTitle.contains(it) }
    }

    fun extractSnippetText(v: JSONObject): String {
        val detailed = v.optJSONArray("detailedMetadataSnippets")
        if (detailed != null && detailed.length() > 0) {
            val runs = detailed.optJSONObject(0)?.optJSONObject("snippetText")?.optJSONArray("runs")
            if (runs != null) {
                val sb = java.lang.StringBuilder()
                for (i in 0 until runs.length()) {
                    sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
                }
                return sb.toString()
            }
        }
        val snippetObj = v.optJSONObject("descriptionSnippet")
        if (snippetObj != null) {
            val runs = snippetObj.optJSONArray("runs")
            if (runs != null) {
                val sb = java.lang.StringBuilder()
                for (i in 0 until runs.length()) {
                    sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
                }
                return sb.toString()
            }
        }
        return ""
    }

    fun extractYtInitialData(html: String): String? {
        val marker = "ytInitialData"
        val markerIndex = html.indexOf(marker)
        if (markerIndex == -1) return null

        val equalsIndex = html.indexOf('=', markerIndex + marker.length)
        if (equalsIndex == -1) return null

        val startIndex = html.indexOf('{', equalsIndex)
        if (startIndex == -1) return null

        var depth = 0
        var inString = false
        var escape = false
        for (i in startIndex until html.length) {
            val c = html[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        return html.substring(startIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    suspend fun searchOnlineMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val normalized = normalizeSearchQuery(trimmed)
        val results = mutableListOf<Movie>()
        val seenIds = mutableSetOf<String>()

        val isSeriesQuery = normalized.contains("series") ||
                normalized.contains("episode") ||
                normalized.contains("season")

        val minMinutesRequired = if (isSeriesQuery) 25 else 65

        // 1. YouTube Search with Long Video Filter (&sp=EgIYAg%253D%253D)
        var ytConn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode("$normalized full movie", "UTF-8")
            val ytUrl = URL("https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIYAg%253D%253D")
            ytConn = ytUrl.openConnection() as HttpURLConnection
            ytConn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            ytConn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
            ytConn.connectTimeout = 8000
            ytConn.readTimeout = 8000

            if (ytConn.responseCode == HttpURLConnection.HTTP_OK) {
                val html = ytConn.inputStream.bufferedReader().use { it.readText() }

                val jsonStr = extractYtInitialData(html)
                if (!jsonStr.isNullOrEmpty()) {
                    val root = JSONObject(jsonStr)
                    val contents = root.optJSONObject("contents")
                        ?.optJSONObject("twoColumnSearchResultsRenderer")
                        ?.optJSONObject("primaryContents")
                        ?.optJSONObject("sectionListRenderer")
                        ?.optJSONArray("contents")

                    if (contents != null) {
                        for (sIdx in 0 until contents.length()) {
                            val section = contents.optJSONObject(sIdx)
                            val items = section?.optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue
                            for (iIdx in 0 until items.length()) {
                                val item = items.optJSONObject(iIdx) ?: continue
                                val v = item.optJSONObject("videoRenderer") ?: continue

                                val vid = v.optString("videoId")
                                if (vid.length != 11 || seenIds.contains(vid)) continue

                                val rawTitle = v.optJSONObject("title")
                                    ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                                val title = unescapeHtml(rawTitle)
                                if (title.isBlank()) continue

                                // 1. Title blacklist check
                                if (isBlacklisted(title)) continue

                                // 2. Channel check
                                val rawOwner = v.optJSONObject("ownerText")
                                    ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                                val ownerName = unescapeHtml(rawOwner)
                                if (ownerName.contains("YouTube Movies", ignoreCase = true)) continue
                                if (isChannelBlacklisted(ownerName)) continue

                                // 3. Snippet / Description check
                                val snippet = extractSnippetText(v)
                                if (isSnippetBlacklisted(snippet)) continue

                                // 4. Paid / rental badge check
                                val badges = v.optJSONArray("badges")
                                var isPaid = false
                                if (badges != null) {
                                    for (bIdx in 0 until badges.length()) {
                                        val badgeLabel = badges.optJSONObject(bIdx)
                                            ?.optJSONObject("metadataBadgeRenderer")?.optString("label") ?: ""
                                        if (PAID_KEYWORDS.any { badgeLabel.lowercase().contains(it) }) {
                                            isPaid = true
                                            break
                                        }
                                    }
                                }
                                if (isPaid) continue

                                // 5. Duration check (>= 65 mins for movies, >= 25 mins for series)
                                val durText = v.optJSONObject("lengthText")?.optString("simpleText") ?: ""
                                val durationMins = parseDurationMinutes(durText)
                                if (durationMins < minMinutesRequired) continue

                                // 6. Query token relevance check
                                if (!matchesQueryTokens(title, normalized)) continue

                                seenIds.add(vid)
                                val titleLower = title.lowercase()
                                val movie = Movie(
                                    id = "yt_$vid",
                                    title = title,
                                    year = extractYear(title),
                                    language = if (titleLower.contains("hindi") || titleLower.contains("bollywood")) "Hindi" else "English",
                                    genre = if (isSeriesQuery) "Web Series HD" else "Full Movie HD",
                                    duration = if (durText.isNotBlank()) formatDurationDisplay(durText) else "Full Feature",
                                    director = if (ownerName.isNotBlank()) ownerName else "HD Cinema Stream",
                                    cast = listOf("Full HD Feature"),
                                    synopsis = "Watch $title in 1080p High Definition completely ad-free. No payment or subscription required.",
                                    rating = "8.4/10",
                                    posterUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                                    backdropUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                                    videoUrl = "youtube:$vid",
                                    quality = "1080p Full HD",
                                    fileSizeBytes = 1350000000L
                                )
                                results.add(movie)
                                cacheMovie(movie)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ytConn?.disconnect()
        }

        // 2. Archive.org Full Movie Search (Strictly full feature films >= 250MB)
        var archiveConn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode("title:($normalized) AND mediatype:(movies)", "UTF-8")
            val archiveUrl = URL("https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,year,description,item_size&rows=8&output=json")
            archiveConn = archiveUrl.openConnection() as HttpURLConnection
            archiveConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            archiveConn.connectTimeout = 6000
            archiveConn.readTimeout = 6000

            if (archiveConn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = archiveConn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                val docs = root.optJSONObject("response")?.optJSONArray("docs")

                val minItemSize = if (isSeriesQuery) 100_000_000L else 250_000_000L

                if (docs != null) {
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val id = doc.optString("identifier")
                        val rawTitle = doc.optString("title")
                        val title = unescapeHtml(rawTitle)
                        val year = doc.optInt("year", 1970)
                        val desc = unescapeHtml(doc.optString("description", "Public domain classic video archive."))
                        val itemSize = doc.optLong("item_size", 0L)

                        // Strict size check eliminates clips, short songs, promos
                        if (itemSize < minItemSize) continue

                        if (id.isNotEmpty() && !seenIds.contains(id)) {
                            if (isBlacklisted(title) || isSnippetBlacklisted(desc)) continue
                            if (!matchesQueryTokens(title, normalized)) continue

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
                                fileSizeBytes = if (itemSize > 0L) itemSize else 900000000L
                            )
                            results.add(movie)
                            cacheMovie(movie)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            archiveConn?.disconnect()
        }

        results
    }

    fun parseDurationMinutes(dur: String): Int {
        val parts = dur.split(":")
        return when (parts.size) {
            3 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            2 -> parts[0].toIntOrNull() ?: 0
            else -> 0
        }
    }

    fun formatDurationDisplay(dur: String): String {
        val parts = dur.split(":")
        return when (parts.size) {
            3 -> "${parts[0]}h ${parts[1]}m"
            2 -> "${parts[0]} min"
            else -> dur
        }
    }

    fun extractYear(title: String): Int {
        val yearPattern = Pattern.compile("""(19\d\d|20\d\d)""")
        val m = yearPattern.matcher(title)
        return if (m.find()) {
            m.group(1)?.toIntOrNull() ?: 1975
        } else 1975
    }
}

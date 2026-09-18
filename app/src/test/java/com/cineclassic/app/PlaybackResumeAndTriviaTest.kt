package com.cineclassic.app

import com.cineclassic.app.data.CinemaTriviaProvider
import com.cineclassic.app.data.Movie
import com.cineclassic.app.data.MovieRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaybackResumeAndTriviaTest {

    @Test
    fun testZanjeer2013RemovedAnd1973Preserved() {
        val jsonFile = File("src/main/assets/movies.json")
        assertTrue("movies.json must exist", jsonFile.exists())

        val jsonString = jsonFile.readText()
        val listType = object : TypeToken<List<Movie>>() {}.type
        val movies: List<Movie> = Gson().fromJson(jsonString, listType)

        // 1. Zanjeer 2013 remake MUST NOT exist
        val zanjeer2013 = movies.find { it.id == "hindi_zanjeer" }
        assertNull("2013 Zanjeer remake must be completely removed from catalog", zanjeer2013)

        // 2. Zanjeer 1973 Original Classic MUST exist
        val zanjeer1973 = movies.find { it.id == "hindi_zanjeer_1973" }
        assertNotNull("1973 Amitabh Bachchan Zanjeer classic must exist", zanjeer1973)
        assertEquals(1973, zanjeer1973?.year)
        assertTrue("Must star Amitabh Bachchan", zanjeer1973?.cast?.contains("Amitabh Bachchan") == true)
    }

    @Test
    fun testCinemaTriviaProviderCuratedFacts() {
        val allTrivia = CinemaTriviaProvider.getAllTrivia()
        assertTrue("Should have at least 20 cinema trivia entries", allTrivia.size >= 20)

        // Every trivia must have non-blank fields
        for (t in allTrivia) {
            assertTrue("Movie title cannot be blank", t.movieTitle.isNotBlank())
            assertTrue("Fact cannot be blank", t.fact.isNotBlank())
            assertTrue("Category cannot be blank", t.category.isNotBlank())
            assertTrue("Badge cannot be blank", t.badge.isNotBlank())
        }

        // Specific classic movie trivia lookups
        val zanjeerTrivia = CinemaTriviaProvider.getTriviaForMovie("hindi_zanjeer_1973")
        assertNotNull("Zanjeer 1973 trivia must exist", zanjeerTrivia)
        assertTrue("Should mention Angry Young Man", zanjeerTrivia!!.fact.contains("Angry Young Man"))

        val motherIndiaTrivia = CinemaTriviaProvider.getTriviaForMovie("hindi_02")
        assertNotNull("Mother India trivia must exist", motherIndiaTrivia)
        assertTrue("Should mention Academy Awards / Oscar", motherIndiaTrivia!!.fact.contains("Academy Awards") || motherIndiaTrivia.fact.contains("Oscar"))

        val awaraTrivia = CinemaTriviaProvider.getTriviaForMovie("hindi_01")
        assertNotNull("Awara trivia must exist", awaraTrivia)
        assertTrue("Should mention Awara Hoon or Moscow", awaraTrivia!!.fact.contains("Awara Hoon") || awaraTrivia.fact.contains("Moscow"))
    }

    @Test
    fun testCinemaTriviaRandomShuffle() {
        val trivia1 = CinemaTriviaProvider.getRandomTrivia()
        assertNotNull(trivia1)

        val trivia2 = CinemaTriviaProvider.getRandomTrivia(excludeId = trivia1.id)
        assertNotNull(trivia2)
        assertTrue("Excluded trivia must not be selected again in shuffle", trivia1.id != trivia2.id)
    }

    @Test
    fun testContinueWatchingItemProgressAndFormatting() {
        val movie = Movie(
            id = "test_movie",
            title = "Test Classic",
            year = 1975,
            language = "Hindi",
            genre = "Action",
            duration = "2h 30m",
            director = "Director",
            cast = listOf("Actor 1"),
            synopsis = "Synopsis",
            rating = "8.5",
            posterUrl = "http://example.com/p.jpg",
            backdropUrl = "http://example.com/b.jpg",
            videoUrl = "youtube:test",
            quality = "1080p HD",
            fileSizeBytes = 1000L
        )

        // Exactly 59 minutes into a 120-minute movie
        val position59Min = 59L * 60L * 1000L // 3,540,000 ms
        val duration120Min = 120L * 60L * 1000L // 7,200,000 ms

        val item = MovieRepository.ContinueWatchingItem(
            movie = movie,
            positionMs = position59Min,
            durationMs = duration120Min,
            timestamp = System.currentTimeMillis()
        )

        // 59 / 120 = 49%
        assertEquals(49, item.progressPercentage)
        assertEquals("59m 00s", item.formattedPosition)

        // Over 1 hour: 1 hour 45 minutes
        val position1h45m = (105L * 60L * 1000L) + 30000L
        val item2 = MovieRepository.ContinueWatchingItem(
            movie = movie,
            positionMs = position1h45m,
            durationMs = duration120Min,
            timestamp = System.currentTimeMillis()
        )
        assertEquals(87, item2.progressPercentage)
        assertEquals("1h 45m", item2.formattedPosition)
    }
}

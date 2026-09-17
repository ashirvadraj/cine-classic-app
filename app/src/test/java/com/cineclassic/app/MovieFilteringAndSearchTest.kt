package com.cineclassic.app

import com.cineclassic.app.data.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieFilteringAndSearchTest {

    private val sampleMovies = listOf(
        Movie(
            id = "m1",
            title = "Zanjeer (1973 Original Classic)",
            year = 1973,
            language = "Hindi",
            genre = "Action / Crime / Drama",
            duration = "2h 25m",
            director = "Prakash Mehra",
            cast = listOf("Amitabh Bachchan", "Jaya Bhaduri", "Pran"),
            synopsis = "Angry young man classic",
            rating = "8.0/10",
            posterUrl = "http://example.com/p1.jpg",
            backdropUrl = "http://example.com/b1.jpg",
            videoUrl = "youtube:test1",
            quality = "1080p HD",
            fileSizeBytes = 1000000L
        ),
        Movie(
            id = "m2",
            title = "Charade",
            year = 1963,
            language = "English",
            genre = "Romantic Mystery / Thriller",
            duration = "1h 53m",
            director = "Stanley Donen",
            cast = listOf("Cary Grant", "Audrey Hepburn"),
            synopsis = "Romantic thriller in Paris",
            rating = "7.9/10",
            posterUrl = "http://example.com/p2.jpg",
            backdropUrl = "http://example.com/b2.jpg",
            videoUrl = "youtube:test2",
            quality = "1080p HD",
            fileSizeBytes = 1000000L
        ),
        Movie(
            id = "m3",
            title = "Awara",
            year = 1951,
            language = "Hindi",
            genre = "Drama / Romance",
            duration = "2h 48m",
            director = "Raj Kapoor",
            cast = listOf("Raj Kapoor", "Nargis"),
            synopsis = "A poor young man struggles",
            rating = "8.0/10",
            posterUrl = "http://example.com/p3.jpg",
            backdropUrl = "http://example.com/b3.jpg",
            videoUrl = "youtube:test3",
            quality = "1080p HD",
            fileSizeBytes = 1000000L
        )
    )

    @Test
    fun testLanguageFilterHindi() {
        val hindi = sampleMovies.filter { it.language.equals("Hindi", ignoreCase = true) }
        assertEquals(2, hindi.size)
        assertTrue(hindi.all { it.language == "Hindi" })
    }

    @Test
    fun testLanguageFilterEnglish() {
        val english = sampleMovies.filter { it.language.equals("English", ignoreCase = true) }
        assertEquals(1, english.size)
        assertEquals("Charade", english.first().title)
    }

    @Test
    fun testSearchByActor() {
        val query = "amitabh"
        val results = sampleMovies.filter { movie ->
            movie.title.contains(query, ignoreCase = true) ||
            movie.director.contains(query, ignoreCase = true) ||
            movie.cast.any { it.contains(query, ignoreCase = true) }
        }
        assertEquals(1, results.size)
        assertEquals("m1", results.first().id)
    }

    @Test
    fun testSearchByDirector() {
        val query = "raj kapoor"
        val results = sampleMovies.filter { movie ->
            movie.title.contains(query, ignoreCase = true) ||
            movie.director.contains(query, ignoreCase = true) ||
            movie.cast.any { it.contains(query, ignoreCase = true) }
        }
        assertEquals(1, results.size)
        assertEquals("Awara", results.first().title)
    }

    @Test
    fun testSearchByGenre() {
        val query = "mystery"
        val results = sampleMovies.filter { movie ->
            movie.genre.contains(query, ignoreCase = true)
        }
        assertEquals(1, results.size)
        assertEquals("Charade", results.first().title)
    }
}
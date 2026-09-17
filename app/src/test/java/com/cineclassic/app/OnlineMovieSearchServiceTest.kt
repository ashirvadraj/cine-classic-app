package com.cineclassic.app

import com.cineclassic.app.data.OnlineMovieSearchService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineMovieSearchServiceTest {

    @Test
    fun testUnescapeHtmlEntities() {
        val raw = "Tom &amp; Jerry &#39;Special&#39; &quot;Classic&quot; &lt;HD&gt; \\u0026 More"
        val unescaped = OnlineMovieSearchService.unescapeHtml(raw)
        assertEquals("Tom & Jerry 'Special' \"Classic\" <HD> & More", unescaped)
    }

    @Test
    fun testBlacklistKeywordsFilter() {
        assertTrue(OnlineMovieSearchService.isBlacklisted("Zanjeer Movie Review by Critics"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Don Official HD Trailer"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Sholay Unknown Facts and Trivia"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Best Fight Scene in 4K"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Old Bollywood Jukebox Songs"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Movie Ending Explained and Reaction"))

        // Legitimate full movies should pass
        assertFalse(OnlineMovieSearchService.isBlacklisted("Zanjeer 1973 Full Movie HD"))
        assertFalse(OnlineMovieSearchService.isBlacklisted("Mother India Full Feature Film"))
        assertFalse(OnlineMovieSearchService.isBlacklisted("Charade Cary Grant Audrey Hepburn 1080p"))
    }

    @Test
    fun testDurationParsing() {
        assertEquals(135, OnlineMovieSearchService.parseDurationMinutes("2:15:30"))
        assertEquals(75, OnlineMovieSearchService.parseDurationMinutes("1:15:00"))
        assertEquals(45, OnlineMovieSearchService.parseDurationMinutes("45:20"))
        assertEquals(15, OnlineMovieSearchService.parseDurationMinutes("15:00"))
        assertEquals(0, OnlineMovieSearchService.parseDurationMinutes(""))
        assertEquals(0, OnlineMovieSearchService.parseDurationMinutes("invalid"))
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("2h 15m", OnlineMovieSearchService.formatDurationDisplay("2:15:30"))
        assertEquals("45 min", OnlineMovieSearchService.formatDurationDisplay("45:20"))
        assertEquals("invalid", OnlineMovieSearchService.formatDurationDisplay("invalid"))
    }

    @Test
    fun testYearExtraction() {
        assertEquals(1973, OnlineMovieSearchService.extractYear("Zanjeer (1973) Full HD Movie"))
        assertEquals(2013, OnlineMovieSearchService.extractYear("Zanjeer 2013 Ram Charan Action"))
        assertEquals(1951, OnlineMovieSearchService.extractYear("Awara (1951) Raj Kapoor Classic"))
        assertEquals(1975, OnlineMovieSearchService.extractYear("Undated Classic Cinema"))
    }

    @Test
    fun testExtractYtInitialDataBalancedBraces() {
        val sampleHtml = "<html><script>var ytInitialData = {\"contents\":{\"twoColumnSearchResultsRenderer\":{\"primaryContents\":{\"sectionListRenderer\":{\"contents\":[]}}}}};</script></html>"
        val extracted = OnlineMovieSearchService.extractYtInitialData(sampleHtml)
        assertNotNull(extracted)
        assertTrue(extracted!!.startsWith("{\"contents\":"))
        assertTrue(extracted.endsWith("}}}}}"))
    }

    @Test
    fun testExtractYtInitialDataWithNewlinesAndEscapes() {
        val multilineHtml = "<html><script>window[\"ytInitialData\"] = {\"title\": \"A \\\"Classic\\\" Movie with {curly} braces\", \"id\": \"12345\"};</script></html>"
        val extracted = OnlineMovieSearchService.extractYtInitialData(multilineHtml)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("A \\\"Classic\\\" Movie with {curly} braces"))
    }

    @Test
    fun testExtractYtInitialDataMissingReturnsNull() {
        val noDataHtml = "<html><head><title>No Data</title></head></html>"
        val extracted = OnlineMovieSearchService.extractYtInitialData(noDataHtml)
        assertNull(extracted)
    }
}
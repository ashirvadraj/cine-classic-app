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
        assertEquals(2013, OnlineMovieSearchService.extractYear("The Lunchbox Irrfan Khan"))
        assertEquals(1995, OnlineMovieSearchService.extractYear("Dilwale Dulhania Le Jayenge"))
        assertEquals(2004, OnlineMovieSearchService.extractYear("Veer Zaara"))
        assertEquals(1975, OnlineMovieSearchService.extractYear("Sholay"))
        assertEquals(0, OnlineMovieSearchService.extractYear("Undated Classic Cinema"))
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

    @Test
    fun testSearchQueryNormalization() {
        assertEquals("zanjeer 1973 movie", OnlineMovieSearchService.normalizeSearchQuery("zanzeer 1973 mvie"))
        assertEquals("dilwale dulhania le jayenge", OnlineMovieSearchService.normalizeSearchQuery("dilwale dulhaniya le jyege"))
        assertEquals("veer zaara", OnlineMovieSearchService.normalizeSearchQuery("veer zara"))
        assertEquals("dilwale dulhania le jayenge", OnlineMovieSearchService.normalizeSearchQuery("DDLJ"))
    }

    @Test
    fun testExpandedBlacklistKeywords() {
        assertTrue(OnlineMovieSearchService.isBlacklisted("Dilwale Dulhania Le Jayenge Explanation & Analysis"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Veer Zaara Review & Facts"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Veer Zaara Facts & Ditels"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Veer Zaara Movie Story Explained"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Veer Zaara Full Movie All Songs Jukebox"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Shiva Hero Veer Zara Shorts"))
        assertTrue(OnlineMovieSearchService.isBlacklisted("Zanjeer 1973 Full Story & Breakdown"))
    }

    @Test
    fun testChannelBlacklist() {
        assertTrue(OnlineMovieSearchService.isChannelBlacklisted("TS MUVIES REVIEWS"))
        assertTrue(OnlineMovieSearchService.isChannelBlacklisted("Old Is Gold Movie Explainer"))
        assertTrue(OnlineMovieSearchService.isChannelBlacklisted("Best Of Bollywood's Music"))
        assertTrue(OnlineMovieSearchService.isChannelBlacklisted("Film Ki Factory"))
        assertTrue(OnlineMovieSearchService.isChannelBlacklisted("Subhajit Textile"))
        assertFalse(OnlineMovieSearchService.isChannelBlacklisted("Goldmines Bollywood"))
        assertFalse(OnlineMovieSearchService.isChannelBlacklisted("Shemaroo Movies"))
    }

    @Test
    fun testSnippetBlacklist() {
        assertTrue(OnlineMovieSearchService.isSnippetBlacklisted("Note:- This is not a full movie this is just a review"))
        assertTrue(OnlineMovieSearchService.isSnippetBlacklisted("#VeerZaaraReview #SRKMovies #BollywoodReview"))
        assertTrue(OnlineMovieSearchService.isSnippetBlacklisted("In today's video we revisit the classic movie"))
        assertFalse(OnlineMovieSearchService.isSnippetBlacklisted("Watch complete restored print in high definition ad-free"))
    }

    @Test
    fun testMatchesQueryTokens() {
        // Query 'veer zaara' should match 'Veer Zaara 2004 Full Movie'
        assertTrue(OnlineMovieSearchService.matchesQueryTokens("Veer Zaara 2004 Full Movie", "veer zara"))
        assertTrue(OnlineMovieSearchService.matchesQueryTokens("Veer-Zaara (2004)", "veer zara"))

        // Query 'veer zaara' should NOT match Salman Khan's 'Veer (2010)'
        assertFalse(OnlineMovieSearchService.matchesQueryTokens("Veer (2010) Salman Khan Full Hindi Movie", "veer zara"))

        // Query 'dilwale dulhania le jayenge' should NOT match Ajay Devgn's 'Dilwale (1994)'
        assertFalse(OnlineMovieSearchService.matchesQueryTokens("Dilwale (HD) (1994) Full Hindi Movie", "dilwale dulhaniya le jyege"))
    }
}
package com.cineclassic.app

import com.cineclassic.app.data.YouTubeStreamResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeStreamResolverTest {

    @Test
    fun testExtractVideoId() {
        assertEquals("2MizqUuFOkA", YouTubeStreamResolver.extractVideoId("youtube:2MizqUuFOkA"))
        assertEquals("2MizqUuFOkA", YouTubeStreamResolver.extractVideoId("https://www.youtube.com/watch?v=2MizqUuFOkA"))
        assertEquals("2MizqUuFOkA", YouTubeStreamResolver.extractVideoId("https://www.youtube.com/watch?v=2MizqUuFOkA&feature=shared"))
        assertEquals("2MizqUuFOkA", YouTubeStreamResolver.extractVideoId("https://youtu.be/2MizqUuFOkA?t=5"))
        assertEquals("2MizqUuFOkA", YouTubeStreamResolver.extractVideoId("https://www.youtube.com/embed/2MizqUuFOkA"))
        assertEquals("2MizqUuFOkA", YouTubeStreamResolver.extractVideoId("  2MizqUuFOkA  "))
    }
}

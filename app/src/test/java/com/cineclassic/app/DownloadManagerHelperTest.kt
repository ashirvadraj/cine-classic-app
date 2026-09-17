package com.cineclassic.app

import com.cineclassic.app.data.DownloadManagerHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadManagerHelperTest {

    private fun calculatePercent(downloaded: Long, total: Long): Int {
        if (total <= 0L) return 0
        return ((downloaded * 100) / total).toInt().coerceIn(0, 100)
    }

    @Test
    fun testDownloadProgressPercentCalculations() {
        val percent = calculatePercent(450L, 1000L)
        assertEquals(45, percent)
    }

    @Test
    fun testZeroTotalBytesDoesNotDivideByZero() {
        val percent = calculatePercent(0L, 0L)
        assertEquals(0, percent)
    }

    @Test
    fun testCompletedProgressCapsAt100() {
        val percent = calculatePercent(1200L, 1000L)
        assertEquals(100, percent)
    }
}
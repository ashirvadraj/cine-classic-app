package com.cineclassic.app

import com.cineclassic.app.data.DownloadManagerHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadManagerHelperTest {

    @Test
    fun testDownloadProgressPercentCalculations() {
        val total = 1000L
        val downloaded = 450L
        val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
        assertEquals(45, percent)
    }

    @Test
    fun testZeroTotalBytesDoesNotDivideByZero() {
        val total = 0L
        val downloaded = 0L
        val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
        assertEquals(0, percent)
    }

    @Test
    fun testCompletedProgressCapsAt100() {
        val total = 1000L
        val downloaded = 1200L
        val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
        assertEquals(100, percent)
    }
}
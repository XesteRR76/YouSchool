package org.youschool.app

import org.junit.Assert.*
import org.junit.Test
import org.youschool.app.ui.util.LessonTimeFormatter

class LessonTimeFormatterTest {

    @Test
    fun formatsNullOrBlankAsNull() {
        assertNull(LessonTimeFormatter.formatLessonTimeRange(null, null))
        assertNull(LessonTimeFormatter.formatLessonTimeRange("", ""))
        assertNull(LessonTimeFormatter.formatLessonTimeRange("   ", "   ", timeRaw = "", bellRaw = ""))
    }

    @Test
    fun formatsIsoDateTimes() {
        val formatted = LessonTimeFormatter.formatLessonTimeRange(
            startRaw = "2026-11-16T08:30:00",
            endRaw = "2026-11-16T09:15:00"
        )
        assertEquals("08:30 – 09:15", formatted)
    }

    @Test
    fun formatsDirectHHmm() {
        val formatted = LessonTimeFormatter.formatLessonTimeRange(
            startRaw = "8:30",
            endRaw = "9:15"
        )
        assertEquals("08:30 – 09:15", formatted)
    }

    @Test
    fun formatsTimeRaw() {
        val formatted = LessonTimeFormatter.formatLessonTimeRange(
            startRaw = null,
            endRaw = null,
            timeRaw = "09:30-10:15"
        )
        assertEquals("09:30 – 10:15", formatted)
    }

    @Test
    fun formatsBellRaw() {
        val formatted = LessonTimeFormatter.formatLessonTimeRange(
            startRaw = null,
            endRaw = null,
            bellRaw = "10:30 – 11:15"
        )
        assertEquals("10:30 – 11:15", formatted)
    }

    @Test
    fun formatsSingleStartTimeIfEndMissing() {
        val formatted = LessonTimeFormatter.formatLessonTimeRange(
            startRaw = "2026-11-16T11:35:00",
            endRaw = null
        )
        assertEquals("11:35", formatted)
    }
}

package org.youschool.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LessonTimeFormatter {

    fun formatLessonTimeRange(
        startRaw: String?,
        endRaw: String?,
        timeRaw: String? = null,
        bellRaw: String? = null
    ): String? {
        if (!bellRaw.isNullOrBlank()) {
            return normalizeTimeRange(bellRaw)
        }
        if (!timeRaw.isNullOrBlank()) {
            return normalizeTimeRange(timeRaw)
        }
        val start = extractTimeHHmm(startRaw)
        val end = extractTimeHHmm(endRaw)
        return when {
            start != null && end != null -> "$start – $end"
            start != null -> start
            end != null -> end
            else -> null
        }
    }

    fun normalizeTimeRange(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val parts = trimmed.split(Regex("[-–—]"))
        return if (parts.size == 2) {
            val start = extractTimeHHmm(parts[0])
            val end = extractTimeHHmm(parts[1])
            if (start != null && end != null) {
                "$start – $end"
            } else {
                trimmed
            }
        } else {
            extractTimeHHmm(trimmed) ?: trimmed
        }
    }

    fun extractTimeHHmm(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()

        val candidate = if (trimmed.contains("T")) {
            trimmed.substringAfter("T")
        } else {
            trimmed
        }

        // Match HH:mm or H:mm e.g. "8:30" or "08:30"
        val timeRegex = Regex("""(?:\D|^)(\d{1,2}):(\d{2})(?::\d{2})?(?:\D|$)""")
        val match = timeRegex.find(candidate)
        if (match != null) {
            val (h, m) = match.destructured
            return String.format(Locale.US, "%02d:%s", h.toInt(), m)
        }

        // Try parsing epoch millis
        val epoch = trimmed.toLongOrNull()
        if (epoch != null && epoch > 100000000000L) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(epoch))
        }

        return null
    }
}

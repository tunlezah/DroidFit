package com.visceralfit.core.common

import kotlin.time.Duration

/**
 * Timer formatting. Kept in one place because the workout player, history list and
 * spoken cues must agree exactly — a mismatch between the digits on screen and the
 * words in the user's ears reads as a bug even when both are "right".
 */
object DurationFormat {

    /** `m:ss` under an hour, `h:mm:ss` at or above it. For the countdown display. */
    fun clock(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Spoken form, e.g. "twenty five minutes" -> "25 minutes", "1 minute 30 seconds".
     * Digits are left as numerals because every mainstream TTS engine reads them
     * correctly, and spelling them out breaks with the user's chosen locale.
     */
    fun spoken(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return when {
            minutes == 0L -> "$seconds ${plural(seconds, "second")}"
            seconds == 0L -> "$minutes ${plural(minutes, "minute")}"
            else -> "$minutes ${plural(minutes, "minute")} $seconds ${plural(seconds, "second")}"
        }
    }

    /** Compact history label, e.g. "32 min". */
    fun compact(duration: Duration): String = "${duration.inWholeMinutes} min"

    private fun plural(value: Long, noun: String): String = if (value == 1L) noun else "${noun}s"

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L
}

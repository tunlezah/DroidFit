package com.visceralfit.feature.workout.player

import com.visceralfit.core.testing.ProhibitedClaims
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The content gate for spoken coaching, the counterpart to `:app`'s catalogue validation test.
 *
 * A spoken cue is health content in the same way a safety note is, and it reaches the user in
 * the one situation where they cannot pause to reconsider it — mid-interval, out of breath. The
 * claims check was previously applied only to the exercise catalogue, so until phase 08 every
 * line the app says out loud was unchecked.
 */
class CueTextTest {

    @Test
    fun `no cue makes a claim the evidence base does not support`() {
        val offenders = CueText.ALL_FIXED.flatMap { text ->
            ProhibitedClaims.violationsIn(text).map { "\"$text\" contains \"$it\"" }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    /**
     * Spec §5: under 12 words for anything spoken during work.
     *
     * The safety warning is exempt and deliberately so — it is spoken once, at session start,
     * before any work has begun, and shortening it would mean dropping one of the three
     * symptoms REQ-006 names.
     */
    @Test
    fun `every in-session cue is short enough to finish inside an interval`() {
        val overLimit = (CueText.MOTIVATION + listOf(CueText.HALFWAY, CueText.COUNTDOWN, CueText.REST_COUNTDOWN))
            .mapNotNull { text ->
                val words = text.trim().split(Regex("\\s+")).size
                if (words > MAX_WORDS_IN_WORK) "\"$text\" is $words words" else null
            }
        assertEquals(emptyList<String>(), overLimit)
    }

    /** Spec §1 and REQ-006: all three symptoms, named. */
    @Test
    fun `the safety cue names every symptom the requirement lists`() {
        listOf("chest pain", "dizziness", "breathlessness").forEach { symptom ->
            assertTrue(
                "the safety cue omits \"$symptom\": ${CueText.SAFETY}",
                CueText.SAFETY.contains(symptom, ignoreCase = true),
            )
        }
    }

    /**
     * Spec §5: durations are spoken through `DurationFormat`, never assembled by hand, so
     * pluralisation is right in one place rather than in every call site.
     */
    @Test
    fun `remaining time is pluralised`() {
        assertEquals("1 minute remaining.", CueText.remaining(1.minutes))
        assertEquals("15 minutes remaining.", CueText.remaining(15.minutes))
        assertEquals("30 seconds remaining.", CueText.remaining(30.seconds))
    }

    /** Spec §5: numerals, not spelled-out numbers, so the engine reads them in the user's locale. */
    @Test
    fun `spoken durations use numerals`() {
        assertTrue(CueText.remaining(10.minutes).contains("10"))
    }

    @Test
    fun `motivational lines cycle rather than repeating`() {
        val cycled = (0 until CueText.MOTIVATION.size).map(CueText::motivation)
        assertEquals(CueText.MOTIVATION, cycled)
        assertEquals("the cycle does not wrap", CueText.motivation(0), CueText.motivation(CueText.MOTIVATION.size))
    }

    private companion object {
        const val MAX_WORDS_IN_WORK = 12
    }
}

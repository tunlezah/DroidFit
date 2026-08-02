package com.visceralfit.feature.workout.player

import com.visceralfit.domain.coaching.CueTone
import com.visceralfit.domain.coaching.HapticCue
import com.visceralfit.domain.coaching.SpeechCue
import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.CoachingPreferences
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutBlock
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Every timing rule in `framework/09_coaching_and_tts_spec.md`, as a test.
 *
 * NO TEST HERE WAITS. The scheduler reads no clock, so a whole session is driven by handing it
 * `SessionState` values and integers — [Session.runFor] advances 200 ms at a time exactly as
 * the service's ticker does, and a 20-minute session runs in microseconds. If a test in this
 * file ever needs a `delay` or a test scheduler, the time source has stopped being injected and
 * the design has regressed.
 */
class CueSchedulerTest {

    // --- Segment start and boundary signals ---------------------------------------------

    @Test
    fun `each segment announces itself with the catalogue's spoken cue`() {
        val spoken = Session(plan(work(60.seconds, "climb"), work(60.seconds, "flat")))
            .runFor(2.minutes)
            .spokenTexts()

        assertTrue("expected both segments announced, got $spoken", spoken.containsAll(listOf("climb", "flat")))
    }

    @Test
    fun `a work segment starts with a rising tone and a long vibration`() {
        val first = Session(plan(work(60.seconds, "climb"))).runFor(1.seconds).first()

        assertEquals(CueTone.RISING, first.tone)
        assertEquals(HapticCue.LONG, first.haptic)
    }

    @Test
    fun `a rest segment starts with a falling tone and a short vibration`() {
        val emitted = Session(plan(work(10.seconds, "climb"), rest(20.seconds)))
            .runFor(15.seconds)
            .filter { it.tone != null }

        assertEquals(listOf(CueTone.RISING, CueTone.FALLING), emitted.map { it.tone })
        assertEquals(listOf(HapticCue.LONG, HapticCue.SHORT), emitted.map { it.haptic })
    }

    /** Spec §6: the two channels are independent so one can substitute for the other. */
    @Test
    fun `tones and haptics still fire with speech switched off`() {
        val emitted = Session(plan(work(60.seconds, "climb")), prefs = CoachingPreferences(speechEnabled = false))
            .runFor(1.seconds)

        assertEquals(CueTone.RISING, emitted.first().tone)
        assertTrue("speech was produced with speechEnabled off", emitted.all { it.speech == null })
    }

    @Test
    fun `an unavailable engine still tones and vibrates`() {
        val emitted = Session(plan(work(60.seconds, "climb")), speechAvailable = false).runFor(1.seconds)

        assertEquals(CueTone.RISING, emitted.first().tone)
        assertTrue("speech was produced with no engine", emitted.all { it.speech == null })
    }

    @Test
    fun `tones are suppressed when the user turns them off, and haptics are not`() {
        val emitted = Session(plan(work(60.seconds, "climb")), prefs = CoachingPreferences(cueTones = false))
            .runFor(1.seconds)

        assertNull(emitted.first().tone)
        assertEquals(HapticCue.LONG, emitted.first().haptic)
    }

    // --- Countdown ---------------------------------------------------------------------

    @Test
    fun `a work segment counts down its last three seconds as one utterance`() {
        val session = Session(plan(work(30.seconds, "climb"), work(30.seconds, "flat")))
        val spoken = session.runFor(30.seconds).spokenTexts()

        assertTrue("expected one countdown utterance in $spoken", CueText.COUNTDOWN in spoken)
        assertEquals(
            "the countdown must be one utterance, not three",
            1,
            spoken.count { it == CueText.COUNTDOWN },
        )
    }

    @Test
    fun `a countdown that would start late is dropped rather than spoken after the boundary`() {
        val session = Session(plan(work(30.seconds, "climb"), work(30.seconds, "flat")))
        // One tick at 28 s, then the process is descheduled until 29.9 s: the countdown is
        // 1.9 s late by the time it could start, which is past the boundary it counts to.
        session.tickAt(0)
        session.tickAt(28_000)
        val late = session.tickAt(29_900)

        assertNull("a countdown 1.9 s late was spoken anyway", late.speech)
    }

    @Test
    fun `the countdown is silent when the user turns it off`() {
        val spoken = Session(
            plan(work(30.seconds, "climb"), work(30.seconds, "flat")),
            prefs = CoachingPreferences(announceCountdown = false),
        ).runFor(30.seconds).spokenTexts()

        assertTrue("a countdown was spoken with the flag off: $spoken", CueText.COUNTDOWN !in spoken)
    }

    // --- Next exercise -----------------------------------------------------------------

    @Test
    fun `the next exercise is announced five seconds ahead`() {
        val session = Session(plan(work(30.seconds, "climb"), work(30.seconds, "flat")))
        session.runFor(24.seconds)
        val announcement = session.runFor(2.seconds).spokenTexts()

        assertEquals(listOf("Next up: flat."), announcement)
    }

    /** Spec §2: repeating the exercise name every round is noise; the round number is not. */
    @Test
    fun `repeated HIIT rounds announce the round instead of the exercise name`() {
        val plan = plan(
            work(30.seconds, "climb", round = 1, total = 3),
            work(30.seconds, "climb", round = 2, total = 3),
        )
        val session = Session(plan)
        session.runFor(24.seconds)
        val announcement = session.runFor(2.seconds).spokenTexts()

        assertEquals(listOf("Round 2 of 3."), announcement)
    }

    /** Spec §2: in a segment this short the cue would overlap the segment-start cue. */
    @Test
    fun `no next-exercise cue in a segment shorter than six seconds`() {
        val spoken = Session(plan(work(4.seconds, "climb"), work(30.seconds, "flat")))
            .runFor(4.seconds)
            .spokenTexts()

        assertTrue("a 4-second segment announced the next one: $spoken", spoken.none { it.startsWith("Next up") })
    }

    // --- The combined rest countdown ---------------------------------------------------

    /** Spec §2: two utterances inside the last three seconds of rest collide. */
    @Test
    fun `the rest countdown and the next exercise combine into one utterance`() {
        val spoken = Session(plan(rest(20.seconds), work(30.seconds, "flat")))
            .runFor(20.seconds)
            .spokenTexts()

        assertTrue("expected the combined form, got $spoken", "flat in three, two, one." in spoken)
        assertTrue(
            "the next-exercise cue was also spoken separately: $spoken",
            spoken.none { it.startsWith("Next up") },
        )
        assertTrue("the bare rest countdown was spoken too: $spoken", CueText.REST_COUNTDOWN !in spoken)
    }

    @Test
    fun `rest counts down on its own when the next exercise cue is switched off`() {
        val spoken = Session(
            plan(rest(20.seconds), work(30.seconds, "flat")),
            prefs = CoachingPreferences(announceNextExercise = false),
        ).runFor(20.seconds).spokenTexts()

        assertTrue("expected the bare rest countdown, got $spoken", CueText.REST_COUNTDOWN in spoken)
    }

    // --- Halfway -----------------------------------------------------------------------

    /**
     * Spec §4, and the rule most easily got wrong: halfway is halfway through the *work*.
     *
     * The session below is paused for five minutes at the two-minute mark. On wall-clock time
     * halfway would land at 10 minutes; on active elapsed time it lands at 15.
     */
    @Test
    fun `halfway fires on active elapsed time across a paused session`() {
        val session = Session(plan(work(10.minutes, "climb"), work(10.minutes, "flat")))
        session.runFor(2.minutes)
        session.pauseFor(5.minutes)
        val beforeHalfway = session.runFor(7.minutes).spokenTexts()
        val afterHalfway = session.runFor(2.minutes).spokenTexts()

        assertTrue("halfway fired on wall-clock time: $beforeHalfway", CueText.HALFWAY !in beforeHalfway)
        assertTrue("halfway never fired: $afterHalfway", CueText.HALFWAY in afterHalfway)
    }

    @Test
    fun `halfway fires exactly once`() {
        val spoken = Session(plan(work(2.minutes, "climb"), work(2.minutes, "flat")))
            .runFor(4.minutes)
            .spokenTexts()

        assertEquals(1, spoken.count { it == CueText.HALFWAY })
    }

    // --- Remaining time and motivation -------------------------------------------------

    @Test
    fun `remaining time is announced at five-minute marks and at one minute left`() {
        val spoken = Session(
            plan(work(20.minutes, "climb")),
            prefs = CoachingPreferences(announceRemainingTime = true),
        ).runFor(20.minutes).spokenTexts()

        val remaining = spoken.filter { it.endsWith("remaining.") }
        assertEquals(
            listOf(
                "15 minutes remaining.",
                "10 minutes remaining.",
                "5 minutes remaining.",
                "1 minute remaining.",
            ),
            remaining,
        )
    }

    @Test
    fun `motivational prompts are off by default and never repeat inside ninety seconds`() {
        val plan = plan(work(60.seconds, "a"), work(60.seconds, "b"), work(60.seconds, "c"))
        val silent = Session(plan).runFor(3.minutes).spokenTexts()
        assertTrue("a prompt was spoken with the flag off: $silent", silent.none { it in CueText.MOTIVATION })

        val spoken = Session(plan, prefs = CoachingPreferences(motivationalPrompts = true))
            .runFor(3.minutes)
            .spokenTexts()
        val prompts = spoken.filter { it in CueText.MOTIVATION }
        // Three 60-second segments span 180 s, so a 90-second floor permits at most two.
        assertTrue("expected at most two prompts in three minutes, got $prompts", prompts.size <= 2)
        assertTrue("no prompt at all was spoken", prompts.isNotEmpty())
        assertEquals("the same line was repeated", prompts.size, prompts.distinct().size)
    }

    // --- Full instructions -------------------------------------------------------------

    @Test
    fun `full instructions follow the short cue rather than landing on top of it`() {
        val session = Session(
            plan(work(2.minutes, "climb")),
            prefs = CoachingPreferences(speakFullInstructions = true),
        )
        val emitted = session.runFor(30.seconds)
        val order = emitted.spokenTexts()

        assertEquals("climb", order.first())
        assertTrue("the how-to was never read: $order", HOW_TO in order)
        val shortCueAt = emitted.indexOfFirst { it.speech?.text == "climb" }
        val instructionsAt = emitted.indexOfFirst { it.speech?.text == HOW_TO }
        val gapMillis = (instructionsAt - shortCueAt) * TICK_MILLIS
        assertTrue("only ${gapMillis}ms between the two utterances", gapMillis >= MIN_GAP_MILLIS)
    }

    @Test
    fun `full instructions are informational, so a transition cue can interrupt them`() {
        val cue = Session(
            plan(work(2.minutes, "climb")),
            prefs = CoachingPreferences(speakFullInstructions = true),
        ).runFor(30.seconds).mapNotNull { it.speech }.first { it.text == HOW_TO }

        assertEquals(SpeechCue.Priority.INFORMATIONAL, cue.priority)
    }

    // --- Contention and pausing --------------------------------------------------------

    /** Spec §4: never two utterances inside 1.5 seconds. */
    @Test
    fun `no two utterances are emitted inside the minimum gap`() {
        val plan = plan(
            work(8.seconds, "a"),
            rest(8.seconds),
            work(8.seconds, "b"),
            rest(8.seconds),
            work(8.seconds, "c"),
        )
        val ticks = Session(plan, prefs = CoachingPreferences(announceRemainingTime = true))
            .runForWithTimes(40.seconds)
            .filter { it.second.speech != null }
            .map { it.first }

        ticks.zipWithNext { earlier, later ->
            assertTrue(
                "two utterances ${later - earlier}ms apart",
                later - earlier >= MIN_GAP_MILLIS,
            )
        }
    }

    /**
     * The safety warning is CRITICAL, so it goes first and the segment-start cue waits for the
     * gap rather than colliding with it.
     */
    @Test
    fun `the safety warning precedes the first segment cue when an exercise is cardiac-flagged`() {
        val plan = plan(work(60.seconds, "burpees", caution = setOf("cardiac_caution")))
        val spoken = Session(plan).runFor(5.seconds).spokenTexts()

        assertEquals(listOf(CueText.SAFETY, "burpees"), spoken)
    }

    @Test
    fun `no safety warning when nothing in the plan is cardiac-flagged`() {
        val spoken = Session(plan(work(60.seconds, "climb"))).runFor(5.seconds).spokenTexts()

        assertTrue("a safety warning was spoken for a session that needs none", CueText.SAFETY !in spoken)
    }

    @Test
    fun `nothing is spoken while paused, and nothing is replayed on resume`() {
        val session = Session(plan(work(60.seconds, "climb"), work(60.seconds, "flat")))
        session.runFor(10.seconds)
        val paused = session.pauseFor(2.minutes)
        assertTrue("cues were emitted while paused", paused.all { it.isEmpty })

        // The second segment's own cue still fires when the session reaches it; what must not
        // happen is a burst of the cues that came due during the pause.
        val resumed = session.runFor(5.seconds).spokenTexts()
        assertTrue("a cue from before the pause was replayed: $resumed", resumed.none { it == "climb" })
    }

    @Test
    fun `a second session announces its own first segment`() {
        val scheduler = CueScheduler()
        val first = Session(plan(work(60.seconds, "climb")), scheduler = scheduler)
        first.runFor(10.seconds)

        val second = Session(plan(work(60.seconds, "flat")), id = "w_second", scheduler = scheduler)
        val spoken = second.runFor(2.seconds).spokenTexts()

        assertEquals(listOf("flat"), spoken)
    }

    @Test
    fun `a finished session says nothing`() {
        val session = Session(plan(work(10.seconds, "climb")))
        val afterEnd = session.runFor(30.seconds).drop(TICKS_PER_SECOND * 11)

        assertTrue("cues continued after the session ended", afterEnd.all { it.isEmpty })
    }

    // --- Fixtures ---------------------------------------------------------------------

    /**
     * A session being driven tick by tick, holding the same state the service holds.
     *
     * Wraps [SessionCoordinator] rather than fabricating [SessionState] values by hand, so the
     * elapsed times, segment rollovers and pause accounting the scheduler reads are the real
     * ones. A hand-built state would let a test pass against arithmetic the app does not do.
     */
    private class Session(
        workout: Workout,
        id: String? = null,
        private val prefs: CoachingPreferences = CoachingPreferences(),
        private val speechAvailable: Boolean = true,
        private val scheduler: CueScheduler = CueScheduler(),
    ) {
        private val coordinator = SessionCoordinator()
        private var nowMillis = START_MILLIS

        init {
            coordinator.begin(
                workout = id?.let { workout.copy(id = it) } ?: workout,
                startedAt = STARTED_AT,
                nowMillis = nowMillis,
            )
        }

        fun tickAt(offsetMillis: Long): CueBatch {
            nowMillis = START_MILLIS + offsetMillis
            coordinator.tick(nowMillis)
            val state = coordinator.state.value ?: return CueBatch.NONE
            return scheduler.onTick(state, nowMillis, prefs, speechAvailable)
        }

        fun runFor(duration: Duration): List<CueBatch> = runForWithTimes(duration).map { it.second }

        /** Each emission with the monotonic reading it was produced at, for gap assertions. */
        fun runForWithTimes(duration: Duration): List<Pair<Long, CueBatch>> {
            val ticks = duration.inWholeMilliseconds / TICK_MILLIS
            return (1..ticks).map {
                nowMillis += TICK_MILLIS
                coordinator.tick(nowMillis)
                val state = coordinator.state.value
                val batch = state?.let { current ->
                    scheduler.onTick(current, nowMillis, prefs, speechAvailable)
                } ?: CueBatch.NONE
                nowMillis to batch
            }
        }

        fun pauseFor(duration: Duration): List<CueBatch> {
            coordinator.pause(nowMillis)
            val emitted = runFor(duration)
            coordinator.resume(nowMillis)
            return emitted
        }
    }

    private companion object {
        const val START_MILLIS = 5_000_000L
        const val TICK_MILLIS = 200L
        const val TICKS_PER_SECOND = 5
        const val MIN_GAP_MILLIS = 1_500L
        val STARTED_AT: Instant = Instant.fromEpochMilliseconds(1_700_000_000_000L)

        /** [fixture]'s two how-to steps, joined the way [CueText.fullInstructions] joins them. */
        const val HOW_TO = "Set the position first. Then move slowly."

        fun List<CueBatch>.spokenTexts(): List<String> = mapNotNull { it.speech?.text }

        fun plan(vararg segments: Segment): Workout = Workout(
            id = "w_cues",
            title = "Cue test session",
            style = WorkoutStyle.MIXED,
            modalities = setOf(Modality.BODYWEIGHT),
            level = ExperienceLevel.INTERMEDIATE,
            blocks = listOf(WorkoutBlock(BlockKind.MAIN, segments.toList())),
            requestedDuration = segments.fold(Duration.ZERO) { sum, s -> sum + s.duration },
            generationSeed = 1L,
        )

        fun work(
            duration: Duration,
            exerciseId: String,
            round: Int? = null,
            total: Int? = null,
            caution: Set<String> = emptySet(),
        ) = Segment(
            kind = SegmentKind.WORK,
            duration = duration,
            exercise = fixture(exerciseId, caution),
            intensity = IntensityTarget.ZONE_2,
            roundIndex = round,
            roundTotal = total,
        )

        fun rest(duration: Duration) = Segment(
            kind = SegmentKind.REST,
            duration = duration,
            exercise = null,
            intensity = IntensityTarget.RECOVERY,
        )

        /**
         * The spoken cue is the bare id, so an assertion reads as the exercise it is about
         * rather than as a sentence that has to be kept in step with the fixture.
         */
        fun fixture(id: String, caution: Set<String> = emptySet()) = Exercise(
            id = id,
            name = id,
            modality = Modality.BODYWEIGHT,
            difficulty = ExperienceLevel.BEGINNER,
            metValue = 2.8,
            howTo = listOf("Set the position first.", "Then move slowly."),
            spokenInstruction = id,
            safetyNotes = listOf("A test fixture note."),
            commonMistakes = listOf("A test fixture mistake."),
            musclesWorked = listOf("Test"),
            illustrationId = id,
            cautionTags = caution,
        )
    }
}

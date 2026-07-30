package com.visceralfit.feature.workout.player

import com.visceralfit.domain.model.BlockKind
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The session clock.
 *
 * Every method takes the monotonic reading as a parameter, so these are plain arithmetic
 * tests with no scheduler, no Robolectric and no waiting. That is the whole reason the
 * coordinator was written that way: a timer you have to wait for is a timer nobody tests at
 * the boundaries, and the boundaries are where a session silently loses a minute.
 */
class SessionCoordinatorTest {

    private val coordinator = SessionCoordinator()

    @Test
    fun `advances through a segment without crossing its boundary`() {
        begin()
        coordinator.tick(START + 10_000)
        val state = requireNotNull(coordinator.state.value)
        assertEquals(0, state.segmentIndex)
        assertEquals(10.seconds, state.elapsedInSegment)
        assertEquals(10.seconds, state.activeElapsed)
        assertEquals(50.seconds, state.remainingInSegment)
    }

    @Test
    fun `crosses a boundary exactly on the second`() {
        begin()
        coordinator.tick(START + 60_000)
        val state = requireNotNull(coordinator.state.value)
        assertEquals(1, state.segmentIndex)
        assertEquals(kotlin.time.Duration.ZERO, state.elapsedInSegment)
    }

    /**
     * The case a naive implementation gets wrong. If the process is descheduled for longer
     * than a segment, the overflow has to roll into the following segments — dropping it
     * shortens the session by however long the stall was.
     */
    @Test
    fun `a single late tick rolls through several segments`() {
        begin()
        coordinator.tick(START + 150_000)
        val state = requireNotNull(coordinator.state.value)
        assertEquals(2, state.segmentIndex)
        assertEquals(30.seconds, state.elapsedInSegment)
        assertEquals(150.seconds, state.activeElapsed)
    }

    /** Many small ticks and one big one must land in the same place — no accumulated drift. */
    @Test
    fun `two hundred small ticks agree with one large tick`() {
        begin()
        repeat(TICK_COUNT) { index -> coordinator.tick(START + (index + 1L) * TICK_MILLIS) }
        val ticked = requireNotNull(coordinator.state.value)

        val single = SessionCoordinator()
        single.begin(workout(), STARTED_AT, START)
        single.tick(START + TICK_COUNT * TICK_MILLIS)
        val jumped = requireNotNull(single.state.value)

        assertEquals(jumped.segmentIndex, ticked.segmentIndex)
        assertEquals(jumped.elapsedInSegment, ticked.elapsedInSegment)
        assertEquals(jumped.activeElapsed, ticked.activeElapsed)
    }

    @Test
    fun `paused time does not count toward the session`() {
        begin()
        coordinator.tick(START + 10_000)
        coordinator.pause(START + 10_000)
        coordinator.tick(START + 300_000)
        val paused = requireNotNull(coordinator.state.value)
        assertEquals(10.seconds, paused.activeElapsed)
        assertEquals(10.seconds, paused.elapsedInSegment)

        coordinator.resume(START + 300_000)
        coordinator.tick(START + 310_000)
        val resumed = requireNotNull(coordinator.state.value)
        assertEquals(20.seconds, resumed.activeElapsed)
    }

    /**
     * Pausing must not discard the fraction of a second already worked: the clock is
     * advanced before the transition is applied.
     */
    @Test
    fun `pausing credits the time worked before the pause`() {
        begin()
        coordinator.pause(START + 5_000)
        assertEquals(5.seconds, requireNotNull(coordinator.state.value).activeElapsed)
    }

    @Test
    fun `skipping moves on without crediting the skipped time`() {
        begin()
        coordinator.tick(START + 10_000)
        coordinator.skip(START + 10_000)
        val state = requireNotNull(coordinator.state.value)
        assertEquals(1, state.segmentIndex)
        assertEquals(1, state.skippedSegments)
        // 10 s worked out of a 180 s plan, not the 60 s the first segment was worth.
        assertEquals(10.seconds, state.activeElapsed)
        assertEquals(10f / TOTAL_SECONDS, state.completionRatio, TOLERANCE)
    }

    @Test
    fun `the session finishes when the last segment runs out`() {
        begin()
        coordinator.tick(START + TOTAL_SECONDS * 1_000L)
        val state = requireNotNull(coordinator.state.value)
        assertTrue("expected the session to be finished, was $state", state.isFinished)
        assertEquals(1f, state.completionRatio, TOLERANCE)
        assertFalse(coordinator.isActive)
    }

    @Test
    fun `skipping past the last segment finishes the session`() {
        begin()
        repeat(3) { coordinator.skip(START) }
        assertTrue(requireNotNull(coordinator.state.value).isFinished)
    }

    /** A finished session is immutable: a late tick from the service cannot restart it. */
    @Test
    fun `a finished session ignores further ticks`() {
        begin()
        coordinator.finish(START + 5_000)
        val finished = requireNotNull(coordinator.state.value)
        coordinator.tick(START + 500_000)
        assertEquals(finished, requireNotNull(coordinator.state.value))
    }

    @Test
    fun `a clock that goes backwards does not rewind the session`() {
        begin()
        coordinator.tick(START + 30_000)
        coordinator.tick(START + 10_000)
        assertEquals(30.seconds, requireNotNull(coordinator.state.value).activeElapsed)
    }

    @Test
    fun `remaining total counts the rest of the plan`() {
        begin()
        coordinator.tick(START + 30_000)
        val state = requireNotNull(coordinator.state.value)
        assertEquals((TOTAL_SECONDS - 30).seconds, state.remainingTotal)
        assertEquals("second", state.nextSegment?.exercise?.id)
    }

    private fun begin() = coordinator.begin(workout(), STARTED_AT, START)

    private fun workout(): Workout {
        val segments = listOf("first", "second", "third").map { id ->
            Segment(
                kind = SegmentKind.WORK,
                duration = 60.seconds,
                exercise = exercise(id),
                intensity = IntensityTarget.ZONE_2,
            )
        }
        return Workout(
            id = "w_test",
            title = "Test session",
            style = WorkoutStyle.ZONE_2,
            modalities = setOf(Modality.FLOOR_PILATES),
            level = ExperienceLevel.BEGINNER,
            blocks = listOf(WorkoutBlock(BlockKind.MAIN, segments)),
            requestedDuration = TOTAL_SECONDS.seconds,
            generationSeed = 1L,
        )
    }

    private fun exercise(id: String) = Exercise(
        id = id,
        name = id,
        modality = Modality.FLOOR_PILATES,
        difficulty = ExperienceLevel.BEGINNER,
        metValue = 2.8,
        howTo = listOf("A test fixture step."),
        spokenInstruction = id,
        safetyNotes = listOf("A test fixture note."),
        commonMistakes = listOf("A test fixture mistake."),
        musclesWorked = listOf("Test"),
        illustrationId = id,
    )

    private companion object {
        /** Arbitrary, and deliberately not zero: the coordinator must not assume an origin. */
        const val START = 5_000_000L
        val STARTED_AT: Instant = Instant.fromEpochMilliseconds(1_700_000_000_000L)
        const val TOTAL_SECONDS = 180
        const val TICK_MILLIS = 200L
        const val TICK_COUNT = 200
        const val TOLERANCE = 0.0001f
    }
}

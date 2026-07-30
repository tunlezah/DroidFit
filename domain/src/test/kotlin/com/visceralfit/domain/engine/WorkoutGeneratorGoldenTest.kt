package com.visceralfit.domain.engine

import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * The worked example from `framework/07_workout_engine_spec.md` §9, asserted row for row.
 *
 * This is the most valuable test in the project: it pins every constant in the algorithm
 * at once, and an accidental change to any of them breaks it with a readable diff rather
 * than shifting a session length by a few seconds where nobody notices.
 *
 * The structure — how many segments, of what kind, how long, at what intensity — is fixed
 * by the specification and asserted literally here. Which *exercise* fills each slot
 * depends on the seeded shuffle, and is pinned separately by
 * [WorkoutGeneratorDeterminismTest]'s golden file.
 */
class WorkoutGeneratorGoldenTest {

    private val request = WorkoutRequest(
        duration = 20.minutes,
        style = WorkoutStyle.MIXED,
        modalities = setOf(Modality.SPIN_BIKE, Modality.FLOOR_PILATES),
        level = ExperienceLevel.INTERMEDIATE,
        seed = 42L,
    )

    private val workout = DefaultWorkoutGenerator(
        CatalogueFixture.forModalities(Modality.SPIN_BIKE, Modality.FLOOR_PILATES),
    ).generate(request).getOrThrow()

    @Test
    fun `matches the specification's worked example row for row`() {
        val actual = workout.blocks.flatMap { block ->
            block.segments.map { segment ->
                Row(block.kind, segment.kind, segment.duration.inWholeSeconds.toInt(), segment.intensity)
            }
        }
        assertEquals(SPEC_SECTION_9, actual)
    }

    @Test
    fun `totals exactly the requested twenty minutes`() {
        assertEquals(20.minutes, workout.actualDuration)
        assertEquals(TOTAL_SECONDS, workout.actualDuration.inWholeSeconds)
    }

    /**
     * The specification's budget arithmetic: 180 s warm-up, 900 s main, 120 s cool-down.
     * Asserted separately from the row list so a budget-split regression names itself
     * rather than showing up as a diff in the middle of thirteen rows.
     */
    @Test
    fun `splits the budget as the specification computes it`() {
        val byKind = workout.blocks.associate { it.kind to it.duration.inWholeSeconds }
        assertEquals(mapOf(BlockKind.WARM_UP to 180L, BlockKind.MAIN to 900L, BlockKind.COOL_DOWN to 120L), byKind)
    }

    /** Three surges, and every one of them inside the main block rather than at its edges. */
    @Test
    fun `places three threshold surges away from the block's edges`() {
        val main = workout.blocks.first { it.kind == BlockKind.MAIN }.segments
        val surges = main.filter { it.intensity == IntensityTarget.THRESHOLD }
        assertEquals(3, surges.size)
        assertEquals(listOf(1, 2, 3), surges.map { it.roundIndex })
        assertEquals(listOf(3, 3, 3), surges.map { it.roundTotal })

        var elapsed = 0
        val mainSeconds = main.sumOf { it.duration.inWholeSeconds.toInt() }
        main.forEach { segment ->
            val length = segment.duration.inWholeSeconds.toInt()
            if (segment.intensity == IntensityTarget.THRESHOLD) {
                assertEquals("a surge started in the first 90 s", true, elapsed >= 90)
                assertEquals("a surge ended in the last 60 s", true, mainSeconds - (elapsed + length) >= 60)
            }
            elapsed += length
        }
    }

    /** Both machine changes are paid for out of the block that contains them. */
    @Test
    fun `charges each transition to its own block`() {
        val transitions = workout.segments.filter { it.kind == SegmentKind.TRANSITION }
        assertEquals(2, transitions.size)
        assertEquals(listOf(20L, 20L), transitions.map { it.duration.inWholeSeconds })
    }

    /** The user is already on the bike when the main block starts, and comes off it last. */
    @Test
    fun `ends the warm-up and starts the cool-down on the machine`() {
        val warmUp = workout.blocks.first { it.kind == BlockKind.WARM_UP }.segments
        val coolDown = workout.blocks.first { it.kind == BlockKind.COOL_DOWN }.segments
        assertEquals(Modality.SPIN_BIKE, warmUp.last().exercise?.modality)
        assertEquals(Modality.SPIN_BIKE, coolDown.first().exercise?.modality)
        assertEquals(Modality.FLOOR_PILATES, coolDown.last().exercise?.modality)
    }

    @Test
    fun `is titled as what it is`() {
        assertEquals("Mixed — 20 min", workout.title)
        assertEquals(WorkoutStyle.MIXED, workout.style)
        assertEquals(emptyList<String>(), workout.buildNotes)
    }

    private data class Row(
        val block: BlockKind,
        val kind: SegmentKind,
        val seconds: Int,
        val intensity: IntensityTarget,
    )

    private companion object {
        const val TOTAL_SECONDS = 1200L

        /**
         * `framework/07_workout_engine_spec.md` §9, transcribed. Do not edit this to match
         * the implementation: if they disagree, one of them is wrong and the specification
         * is the place to start.
         */
        val SPEC_SECTION_9 = listOf(
            Row(BlockKind.WARM_UP, SegmentKind.WORK, 90, IntensityTarget.RECOVERY),
            Row(BlockKind.WARM_UP, SegmentKind.TRANSITION, 20, IntensityTarget.RECOVERY),
            Row(BlockKind.WARM_UP, SegmentKind.WORK, 70, IntensityTarget.ZONE_2),
            Row(BlockKind.MAIN, SegmentKind.WORK, 180, IntensityTarget.ZONE_2),
            Row(BlockKind.MAIN, SegmentKind.WORK, 60, IntensityTarget.THRESHOLD),
            Row(BlockKind.MAIN, SegmentKind.WORK, 180, IntensityTarget.ZONE_2),
            Row(BlockKind.MAIN, SegmentKind.WORK, 60, IntensityTarget.THRESHOLD),
            Row(BlockKind.MAIN, SegmentKind.WORK, 180, IntensityTarget.ZONE_2),
            Row(BlockKind.MAIN, SegmentKind.WORK, 60, IntensityTarget.THRESHOLD),
            Row(BlockKind.MAIN, SegmentKind.WORK, 180, IntensityTarget.ZONE_2),
            Row(BlockKind.COOL_DOWN, SegmentKind.WORK, 60, IntensityTarget.RECOVERY),
            Row(BlockKind.COOL_DOWN, SegmentKind.TRANSITION, 20, IntensityTarget.RECOVERY),
            Row(BlockKind.COOL_DOWN, SegmentKind.WORK, 40, IntensityTarget.RECOVERY),
        )
    }
}

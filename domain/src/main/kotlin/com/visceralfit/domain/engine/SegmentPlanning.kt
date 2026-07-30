package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.model.SegmentKind
import kotlin.time.Duration.Companion.seconds

/**
 * A segment while it is still being laid out, before it becomes an immutable [Segment].
 *
 * Durations are whole seconds during planning so the arithmetic is exact: every block
 * fills its budget to the second, which is what makes the ±30 s duration invariant hold
 * by construction rather than by luck.
 */
internal data class PlannedSegment(
    val kind: SegmentKind,
    val seconds: Int,
    val exercise: Exercise?,
    val intensity: IntensityTarget,
    val roundIndex: Int? = null,
    val roundTotal: Int? = null,
) {
    fun toSegment(): Segment = Segment(
        kind = kind,
        duration = seconds.seconds,
        exercise = exercise,
        intensity = intensity,
        roundIndex = roundIndex,
        roundTotal = roundTotal,
    )
}

/** Layout arithmetic shared by every block builder. */
internal object SegmentPlanning {

    /**
     * Splits [totalSeconds] into [count] parts, giving any remainder to the last, per
     * spec §4.1. Exact by construction: the parts always sum to [totalSeconds].
     */
    fun evenSplit(totalSeconds: Int, count: Int): List<Int> {
        require(count > 0) { "cannot split $totalSeconds seconds into $count segments" }
        val base = totalSeconds / count
        return List(count) { index -> if (index == count - 1) totalSeconds - base * (count - 1) else base }
    }

    /**
     * Inserts a transition before every segment whose modality differs from the one
     * before it, charging the transition to the segment that follows it so the block's
     * total is unchanged (spec §4 note; this is why the worked example's second warm-up
     * segment is 70 s rather than 90 s).
     *
     * A transition is skipped when paying for it would leave the following segment under
     * [SessionConstants.MIN_SEGMENT_SECONDS]: a 5-second effort announced by a 20-second
     * changeover is worse than changing position on the clock.
     */
    fun withTransitions(planned: List<PlannedSegment>): List<PlannedSegment> {
        if (planned.size < 2) return planned
        val result = mutableListOf(planned.first())
        for (index in 1 until planned.size) {
            val previous = planned[index - 1]
            val current = planned[index]
            val changesModality = previous.exercise != null &&
                current.exercise != null &&
                previous.exercise.modality != current.exercise.modality
            val affordable = current.seconds - SessionConstants.TRANSITION_SECONDS >=
                SessionConstants.MIN_SEGMENT_SECONDS
            if (changesModality && affordable) {
                result += transition()
                result += current.copy(seconds = current.seconds - SessionConstants.TRANSITION_SECONDS)
            } else {
                result += current
            }
        }
        return result
    }

    /**
     * Inserts a transition immediately after the first segment, charged to the second.
     * Used by the cool-down, where the spin-down comes off the machine before floor work
     * (spec §4.6) — the transition follows the first segment rather than preceding a
     * modality change, so it cannot be expressed by [withTransitions].
     */
    fun withTransitionAfterFirst(planned: List<PlannedSegment>): List<PlannedSegment> {
        if (planned.size < 2) return planned
        val second = planned[1]
        val affordable = second.seconds - SessionConstants.TRANSITION_SECONDS >=
            SessionConstants.MIN_SEGMENT_SECONDS
        if (!affordable) return planned
        return buildList {
            add(planned.first())
            add(transition())
            add(second.copy(seconds = second.seconds - SessionConstants.TRANSITION_SECONDS))
            addAll(planned.drop(2))
        }
    }

    private fun transition(): PlannedSegment = PlannedSegment(
        kind = SegmentKind.TRANSITION,
        seconds = SessionConstants.TRANSITION_SECONDS,
        exercise = null,
        intensity = IntensityTarget.RECOVERY,
    )

    /**
     * The intensity a work segment should carry given the style's intended target and
     * whatever the pool fallback chain capped it to.
     *
     * Capping is not a detail to be swallowed: the caller uses the same [cappedAt] to
     * downgrade the workout title, so a user never sees a session labelled as harder
     * than it was actually built (spec §3).
     */
    fun capped(intended: IntensityTarget, cappedAt: IntensityAnchor?): IntensityTarget =
        when (cappedAt) {
            null -> intended
            IntensityAnchor.VIGOROUS -> IntensityTarget.VIGOROUS
            IntensityAnchor.THRESHOLD -> IntensityTarget.THRESHOLD
            IntensityAnchor.ZONE_2 -> IntensityTarget.ZONE_2
            IntensityAnchor.RECOVERY, IntensityAnchor.REST -> IntensityTarget.RECOVERY
        }
}

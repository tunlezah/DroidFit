package com.visceralfit.feature.workout.player

import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * The authoritative state of a running session (ADR-0008).
 *
 * Held in a singleton written by [WorkoutService] and read by
 * [WorkoutPlayerViewModel], so rotation, backgrounding and a screen-off cannot
 * desynchronise the clock — the activity and the ViewModel can both be destroyed and
 * recreated around it.
 *
 * NO CLOCK IS READ IN HERE. Every method that advances time takes the current monotonic
 * reading as a parameter, which makes the whole state machine testable with plain integers
 * and no test scheduler. It also means the elapsed time is computed from *timestamps*
 * rather than accumulated from a tick count, so a delayed or dropped tick cannot make the
 * session drift: a 45-minute session that ticked 2,690 times instead of 2,700 is still
 * 45 minutes.
 */
@Singleton
class SessionCoordinator @Inject constructor() {

    private val internalState = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = internalState.asStateFlow()

    /** True while a session is loaded and unfinished, i.e. while the service should run. */
    val isActive: Boolean get() = internalState.value?.let { !it.isFinished } == true

    /**
     * Loads [workout] and starts it running immediately.
     *
     * Starting on load rather than waiting for a play tap is deliberate: the user has
     * already tapped Start on the Train screen, and a second tap to actually begin is the
     * kind of ceremony that gets noticed on a bike.
     */
    fun begin(workout: Workout, startedAt: Instant, nowMillis: Long) {
        internalState.value = SessionState(
            workout = workout,
            startedAt = startedAt,
            lastTickMillis = nowMillis,
        )
    }

    fun pause(nowMillis: Long) = mutate(nowMillis) { it.copy(isPaused = true) }

    fun resume(nowMillis: Long) = mutate(nowMillis) { it.copy(isPaused = false) }

    fun togglePause(nowMillis: Long) = mutate(nowMillis) { it.copy(isPaused = !it.isPaused) }

    /**
     * Ends the current segment now and moves to the next.
     *
     * The skipped time is *not* credited to the session: [SessionState.activeElapsed] only
     * ever counts real elapsed time, so skipping a 4-minute interval lowers the completion
     * ratio by 4 minutes' worth. That is the honest accounting — a skipped interval was not
     * performed (`framework/10_screen_specs.md` §4).
     */
    fun skip(nowMillis: Long) = mutate(nowMillis) { current ->
        current.copy(
            segmentIndex = current.segmentIndex + 1,
            elapsedInSegment = Duration.ZERO,
            skippedSegments = current.skippedSegments + 1,
        ).finishIfPastEnd()
    }

    /** Advances the clock to [nowMillis]. Called by the service roughly every 200 ms. */
    fun tick(nowMillis: Long) = mutate(nowMillis) { it }

    /** Ends the session where it stands, keeping whatever was completed. */
    fun finish(nowMillis: Long) = mutate(nowMillis) { it.copy(isFinished = true) }

    /** Clears the session entirely, e.g. once its summary has been recorded. */
    fun clear() {
        internalState.value = null
    }

    /**
     * Applies [transform] with the clock advanced to [nowMillis] first, so a transition and
     * the time that elapsed before it are never accounted in the wrong order — pausing
     * must not discard the second that had already been worked.
     */
    private fun mutate(nowMillis: Long, transform: (SessionState) -> SessionState) {
        internalState.update { current ->
            if (current == null || current.isFinished) return@update current
            transform(current.advancedTo(nowMillis))
        }
    }
}

/**
 * A session in progress.
 *
 * [activeElapsed] excludes paused time, so it reflects work done rather than wall-clock —
 * which is what the completion ratio and the energy estimate both need.
 */
data class SessionState(
    val workout: Workout,
    val startedAt: Instant,
    val segmentIndex: Int = 0,
    val elapsedInSegment: Duration = Duration.ZERO,
    val activeElapsed: Duration = Duration.ZERO,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val skippedSegments: Int = 0,
    /** Monotonic reading at the last advance. Not wall-clock; never persisted. */
    val lastTickMillis: Long = 0L,
) {
    private val segments: List<Segment> get() = workout.segments

    val currentSegment: Segment? get() = segments.getOrNull(segmentIndex)

    val nextSegment: Segment? get() = segments.getOrNull(segmentIndex + 1)

    val remainingInSegment: Duration
        get() = ((currentSegment?.duration ?: Duration.ZERO) - elapsedInSegment).coerceAtLeast(Duration.ZERO)

    /** Planned time still to come, including the unfinished part of the current segment. */
    val remainingTotal: Duration
        get() = remainingInSegment + segments.drop(segmentIndex + 1).fold(Duration.ZERO) { sum, s -> sum + s.duration }

    val segmentNumber: Int get() = (segmentIndex + 1).coerceAtMost(segments.size)

    val segmentCount: Int get() = segments.size

    /**
     * Fraction of the planned session actually performed. Skipping lowers it, because
     * [activeElapsed] counts only time really spent moving.
     */
    val completionRatio: Float
        get() {
            val planned = workout.actualDuration.inWholeMilliseconds
            if (planned <= 0L) return 0f
            return (activeElapsed.inWholeMilliseconds.toFloat() / planned).coerceIn(0f, 1f)
        }

    val block get() = workout.blocks.firstOrNull { segment -> currentSegment in segment.segments }

    /**
     * The same state with time advanced to [nowMillis], rolling through as many segment
     * boundaries as the elapsed interval covers.
     *
     * The loop matters: if the process is descheduled for eight seconds during a run of
     * 30-second intervals, one advance can cross a boundary, and dropping the overflow
     * would quietly shorten the session.
     */
    fun advancedTo(nowMillis: Long): SessionState {
        val deltaMillis = (nowMillis - lastTickMillis).coerceAtLeast(0L)
        if (isPaused || isFinished) return copy(lastTickMillis = nowMillis)

        var remainingDelta = deltaMillis.milliseconds
        var state = copy(lastTickMillis = nowMillis, activeElapsed = activeElapsed + remainingDelta)
        while (remainingDelta > Duration.ZERO) {
            val segment = state.currentSegment ?: return state.copy(isFinished = true)
            val leftInSegment = segment.duration - state.elapsedInSegment
            if (remainingDelta < leftInSegment) {
                return state.copy(elapsedInSegment = state.elapsedInSegment + remainingDelta)
            }
            remainingDelta -= leftInSegment
            state = state.copy(segmentIndex = state.segmentIndex + 1, elapsedInSegment = Duration.ZERO)
        }
        return state.finishIfPastEnd()
    }

    internal fun finishIfPastEnd(): SessionState =
        if (segmentIndex >= segments.size) copy(isFinished = true, segmentIndex = segments.size) else this
}

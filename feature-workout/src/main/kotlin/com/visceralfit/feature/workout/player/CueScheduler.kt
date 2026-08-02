package com.visceralfit.feature.workout.player

import com.visceralfit.domain.coaching.CueTone
import com.visceralfit.domain.coaching.HapticCue
import com.visceralfit.domain.coaching.SpeechCue
import com.visceralfit.domain.model.CoachingPreferences
import com.visceralfit.domain.model.SegmentKind
import kotlin.time.Duration.Companion.seconds

/**
 * Decides what the app should say, tone and vibrate at one instant of a session
 * (`framework/09_coaching_and_tts_spec.md`, phase 08).
 *
 * NO CLOCK IS READ IN HERE, for the same reason [SessionCoordinator] reads none: every
 * decision is a function of the [SessionState] it is handed plus the monotonic reading passed
 * with it, so every timing rule in the spec is a test over plain integers with no scheduler,
 * no `delay` and no waiting. `CueSchedulerTest` runs a whole 20-minute session in a few
 * microseconds.
 *
 * ## Freshness rather than instants
 *
 * A tick lands every 200 ms and the process can be descheduled for seconds, so no cue can be
 * defined as "fires exactly at T". Instead each candidate knows how late *this* tick is for
 * it, and is dropped when that exceeds its own [SpeechCue.staleAfterMillis]. That is what the
 * spec means operationally by dropping a stale cue: a countdown that would start 0.8 s late
 * finishes after the boundary it was counting to, so it is not spoken at all — whereas a
 * segment-start cue naming the exercise you are already doing is still useful two seconds in,
 * and gets a wider window to say so.
 *
 * The same mechanism explains why a cue blocked by the minimum gap is retried on the next
 * tick rather than deferred to a queue: it either gets through inside its own freshness
 * window or it never gets spoken. There is no queue, so there is nothing that can arrive late.
 *
 * ## Priority
 *
 * At most one utterance is chosen per tick, from the ordered list in [candidates]. A
 * lower-priority cue never overtakes a higher-priority one that the gap is holding back —
 * that is the spec's "the lower priority one is dropped". Contention *within* the engine
 * (informational cues dropped while something is already speaking) is
 * `AndroidSpeechCoach`'s job and is not duplicated here.
 */
internal class CueScheduler {

    private var workoutId: String? = null
    private var lastUtteranceAtMillis: Long? = null
    private var safetySettled = false
    private var halfwayAnnounced = false
    private var startedSegment = NO_SEGMENT
    private var signalledSegment = NO_SEGMENT
    private var nextAnnouncedFor = NO_SEGMENT
    private var countdownFor = NO_SEGMENT
    private var instructionsFor = NO_SEGMENT
    private var motivationFor = NO_SEGMENT
    private var lastMotivationAtMillis: Long? = null
    private var motivationCount = 0
    private val retiredMilestones = mutableSetOf<Long>()

    /**
     * The coaching output for this tick, empty when there is nothing to do — which is the
     * overwhelmingly common case, since ticks arrive five times a second and a 20-minute
     * session speaks perhaps thirty times.
     *
     * [speechAvailable] is the engine's own answer, so a device with no voice data still gets
     * tones and haptics: they follow [CoachingPreferences.cueTones] and
     * [CoachingPreferences.hapticCues], which are independent of `speechEnabled` precisely so
     * this fallback works (spec §6).
     */
    fun onTick(
        state: SessionState,
        nowMillis: Long,
        prefs: CoachingPreferences,
        speechAvailable: Boolean,
    ): CueBatch {
        resetIfNewSession(state)
        // Nothing is announced while paused, and nothing is replayed on resume. A cue that
        // was due during the pause is about a moment that has passed.
        if (state.isPaused || state.isFinished) return CueBatch.NONE
        val batch = boundarySignal(state, prefs)
        if (!prefs.speechEnabled || !speechAvailable) return batch
        return batch.copy(speech = utteranceFor(state, nowMillis, prefs))
    }

    /**
     * Forgets everything when a different workout starts.
     *
     * Keyed on the workout id rather than exposed as a `reset()` the caller must remember to
     * invoke: the scheduler outlives a single session inside the service, and a forgotten
     * reset would show up as a second session that never announces its own first segment —
     * a bug that only appears on the *second* run and so survives every manual test.
     */
    private fun resetIfNewSession(state: SessionState) {
        if (workoutId == state.workout.id) return
        workoutId = state.workout.id
        lastUtteranceAtMillis = null
        safetySettled = false
        halfwayAnnounced = false
        startedSegment = NO_SEGMENT
        signalledSegment = NO_SEGMENT
        nextAnnouncedFor = NO_SEGMENT
        countdownFor = NO_SEGMENT
        instructionsFor = NO_SEGMENT
        motivationFor = NO_SEGMENT
        lastMotivationAtMillis = null
        motivationCount = 0
        retiredMilestones.clear()
    }

    /** The tone and vibration at a segment boundary, independent of speech (spec §6). */
    private fun boundarySignal(state: SessionState, prefs: CoachingPreferences): CueBatch {
        val segment = state.currentSegment ?: return CueBatch.NONE
        val late = state.elapsedInSegment.inWholeMilliseconds > BOUNDARY_TOLERANCE_MILLIS
        if (signalledSegment == state.segmentIndex || late) return CueBatch.NONE
        signalledSegment = state.segmentIndex
        val isWork = segment.kind == SegmentKind.WORK
        return CueBatch(
            tone = CueTone.forWork(isWork).takeIf { prefs.cueTones },
            haptic = HapticCue.forWork(isWork).takeIf { prefs.hapticCues },
        )
    }

    private fun utteranceFor(state: SessionState, nowMillis: Long, prefs: CoachingPreferences): SpeechCue? {
        val candidate = candidates(state, nowMillis, prefs).firstOrNull { it.isFresh } ?: return null
        val gap = lastUtteranceAtMillis?.let { nowMillis - it } ?: Long.MAX_VALUE
        if (gap < MIN_GAP_MILLIS) return null
        candidate.onEmitted()
        lastUtteranceAtMillis = nowMillis
        return candidate.cue
    }

    /**
     * Everything currently due, hardest-to-postpone first.
     *
     * The order is not the priority enum's order and must not be replaced by it. Both the
     * countdown and the segment-start cue are TRANSITION, and when a very short segment makes
     * them collide the countdown has to win: it is counting down to a boundary that is about
     * to happen, while the segment-start cue is describing something the user can see on
     * screen.
     */
    private fun candidates(
        state: SessionState,
        nowMillis: Long,
        prefs: CoachingPreferences,
    ): List<Candidate> = listOfNotNull(
        safetyCandidate(state),
        countdownCandidate(state, prefs),
        segmentStartCandidate(state),
        nextExerciseCandidate(state, prefs),
        fullInstructionsCandidate(state, prefs),
        halfwayCandidate(state, prefs),
        remainingCandidate(state, prefs),
        motivationCandidate(state, nowMillis, prefs),
    )

    /**
     * The stop-if-symptoms warning, once at session start, when any exercise in the plan
     * carries `cardiac_caution` (spec §1, REQ-006).
     *
     * CRITICAL, so it interrupts. It is also the one cue with no preference flag: a user who
     * has turned spoken coaching off hears nothing, but a user who has it on cannot opt out of
     * this specific sentence.
     */
    private fun safetyCandidate(state: SessionState): Candidate? {
        if (safetySettled || state.segmentIndex > 0) return null
        val needed = state.workout.segments.any { CARDIAC_CAUTION in (it.exercise?.cautionTags ?: emptySet()) }
        if (!needed) {
            safetySettled = true
            return null
        }
        val cue = SpeechCue(CueText.SAFETY, SpeechCue.Priority.CRITICAL, SAFETY_STALE_MILLIS)
        return Candidate(cue, state.elapsedInSegment.inWholeMilliseconds) { safetySettled = true }
    }

    private fun segmentStartCandidate(state: SessionState): Candidate? {
        val segment = state.currentSegment?.takeIf { startedSegment != state.segmentIndex } ?: return null
        val text = CueText.segmentStart(segment) ?: return null
        val cue = SpeechCue(text, SpeechCue.Priority.TRANSITION, SEGMENT_START_STALE_MILLIS)
        return Candidate(cue, state.elapsedInSegment.inWholeMilliseconds) { startedSegment = state.segmentIndex }
    }

    /**
     * The last three seconds of a segment.
     *
     * The narrow staleness window is the point of this cue: a countdown is a promise about
     * when a boundary arrives, so one that starts even most of a second late is a lie and is
     * dropped rather than spoken.
     */
    private fun countdownCandidate(state: SessionState, prefs: CoachingPreferences): Candidate? {
        val segment = state.currentSegment?.takeIf { countdownFor != state.segmentIndex } ?: return null
        val lateness = COUNTDOWN_LEAD_MILLIS - state.remainingInSegment.inWholeMilliseconds
        if (lateness < 0) return null
        val text = CueText.countdown(segment, state.nextSegment, prefs) ?: return null
        val cue = SpeechCue(text, SpeechCue.Priority.TRANSITION, COUNTDOWN_STALE_MILLIS)
        return Candidate(cue, lateness) {
            countdownFor = state.segmentIndex
            // The combined form carries the next-exercise announcement, so that must not
            // then also be made on its own.
            nextAnnouncedFor = state.segmentIndex
        }
    }

    /**
     * "Next up: seated climb", five seconds ahead — the most-praised competitor feature
     * (R-0007) and an explicit operator request, hence its own section in the spec.
     */
    private fun nextExerciseCandidate(state: SessionState, prefs: CoachingPreferences): Candidate? {
        val segment = state.currentSegment
            ?.takeIf { prefs.announceNextExercise && nextAnnouncedFor != state.segmentIndex }
            ?.takeIf { it.duration >= MIN_ANNOUNCE_SEGMENT }
            ?: return null
        val lateness = NEXT_LEAD_MILLIS - state.remainingInSegment.inWholeMilliseconds
        val next = state.nextSegment
            ?.takeIf { lateness >= 0 && !CueText.combinesIntoCountdown(segment, it, prefs) }
            ?: return null
        val text = CueText.nextUp(next, segment) ?: return null
        return Candidate(SpeechCue(text, SpeechCue.Priority.TRANSITION), lateness) {
            nextAnnouncedFor = state.segmentIndex
        }
    }

    /**
     * The full `how_to`, scheduled after the short cue has had time to finish rather than at
     * the same instant.
     *
     * INFORMATIONAL, so if the short cue is still being spoken when this comes due the engine
     * drops it — which is the correct outcome and better than two voices at once. It also
     * requires enough segment left to be worth starting: reading four steps into the last ten
     * seconds of an interval is noise.
     */
    private fun fullInstructionsCandidate(state: SessionState, prefs: CoachingPreferences): Candidate? {
        val segment = state.currentSegment
            ?.takeIf { prefs.speakFullInstructions && instructionsFor != state.segmentIndex }
            ?: return null
        val exercise = segment.exercise ?: return null
        val dueAtMillis = CueText.shortCueMillis(exercise) + MIN_GAP_MILLIS
        val lateness = state.elapsedInSegment.inWholeMilliseconds - dueAtMillis
        if (lateness < 0 || state.remainingInSegment < MIN_REMAINING_FOR_INSTRUCTIONS) return null
        val text = CueText.fullInstructions(exercise)
        val cue = SpeechCue(text, SpeechCue.Priority.INFORMATIONAL, INSTRUCTIONS_STALE_MILLIS)
        return Candidate(cue, lateness) { instructionsFor = state.segmentIndex }
    }

    /**
     * Halfway through the **work**, not through the wall clock (spec §4).
     *
     * Driven by [SessionState.activeElapsed], which excludes paused time, so a user who
     * stopped for five minutes to take a call still hears this at the true halfway point of
     * the session rather than five minutes early.
     */
    private fun halfwayCandidate(state: SessionState, prefs: CoachingPreferences): Candidate? {
        if (halfwayAnnounced || !prefs.announceHalfway) return null
        val halfway = state.workout.actualDuration / 2
        val lateness = state.activeElapsed.inWholeMilliseconds - halfway.inWholeMilliseconds
        if (lateness < 0) return null
        val cue = SpeechCue(CueText.HALFWAY, SpeechCue.Priority.INFORMATIONAL)
        return Candidate(cue, lateness) { halfwayAnnounced = true }
    }

    /**
     * "15 minutes remaining", every five minutes and once at one minute left. Off by default.
     *
     * Milestones at or above the session's own length are excluded, so a 20-minute session
     * does not open by announcing that 20 minutes remain. Milestones already well past — the
     * session was backgrounded, or several were crossed in one descheduled tick — are retired
     * silently rather than spoken in a burst.
     */
    private fun remainingCandidate(state: SessionState, prefs: CoachingPreferences): Candidate? {
        if (!prefs.announceRemainingTime) return null
        val remainingSeconds = state.remainingTotal.inWholeSeconds
        val totalSeconds = state.workout.actualDuration.inWholeSeconds
        val due = MILESTONE_SECONDS.filter {
            it !in retiredMilestones && it < totalSeconds && remainingSeconds <= it
        }
        due.dropLast(1).forEach { retiredMilestones += it }
        val milestone = due.lastOrNull() ?: return null
        val lateness = (milestone - remainingSeconds) * MILLIS_PER_SECOND
        val cue = SpeechCue(CueText.remaining(milestone.seconds), SpeechCue.Priority.INFORMATIONAL)
        return Candidate(cue, lateness) { retiredMilestones += milestone }
    }

    /**
     * Encouragement mid-way through a work segment: once per segment, and never more often
     * than every 90 seconds however short the intervals are.
     *
     * Mid-segment rather than at the start so it does not compete with the segment-start cue,
     * and only in segments long enough to have a middle worth speaking into.
     */
    private fun motivationCandidate(
        state: SessionState,
        nowMillis: Long,
        prefs: CoachingPreferences,
    ): Candidate? {
        val segment = state.currentSegment
            ?.takeIf { prefs.motivationalPrompts && motivationFor != state.segmentIndex }
            ?.takeIf { it.kind == SegmentKind.WORK && it.duration >= MIN_MOTIVATION_SEGMENT }
            ?: return null
        val sinceLast = lastMotivationAtMillis?.let { nowMillis - it } ?: Long.MAX_VALUE
        if (sinceLast < MOTIVATION_INTERVAL_MILLIS) return null
        val lateness = state.elapsedInSegment.inWholeMilliseconds - segment.duration.inWholeMilliseconds / 2
        if (lateness < 0) return null
        val cue = SpeechCue(CueText.motivation(motivationCount), SpeechCue.Priority.INFORMATIONAL)
        return Candidate(cue, lateness) {
            motivationFor = state.segmentIndex
            lastMotivationAtMillis = nowMillis
            motivationCount++
        }
    }

    /**
     * A cue that is due, how late this tick is for it, and what to record if it is spoken.
     *
     * The bookkeeping is a callback rather than done at the call site so that a candidate
     * which loses to a higher-priority one is not marked as spoken — it stays due, and gets
     * another chance next tick until it goes stale.
     */
    private class Candidate(
        val cue: SpeechCue,
        val latenessMillis: Long,
        val onEmitted: () -> Unit,
    ) {
        val isFresh: Boolean get() = latenessMillis <= cue.staleAfterMillis
    }

    private companion object {
        const val NO_SEGMENT = -1
        const val CARDIAC_CAUTION = "cardiac_caution"

        /** Spec §4: never two utterances inside 1.5 seconds. */
        const val MIN_GAP_MILLIS = 1_500L

        const val COUNTDOWN_LEAD_MILLIS = 3_000L
        const val NEXT_LEAD_MILLIS = 5_000L

        /**
         * A boundary tone is a signal that a boundary *just happened*, so it is worth firing
         * slightly late but not seconds late. One tick's worth of slack, five times over.
         */
        const val BOUNDARY_TOLERANCE_MILLIS = 1_000L

        /** Counting "three, two, one" from later than this finishes after the boundary. */
        const val COUNTDOWN_STALE_MILLIS = 700L

        /** Naming the exercise you are already doing stays useful for a few seconds. */
        const val SEGMENT_START_STALE_MILLIS = 3_000L

        /** The warning has to be heard, so it gets the widest window of any cue. */
        const val SAFETY_STALE_MILLIS = 5_000L

        const val INSTRUCTIONS_STALE_MILLIS = 5_000L

        const val MOTIVATION_INTERVAL_MILLIS = 90_000L
        const val MILLIS_PER_SECOND = 1_000L

        val MIN_ANNOUNCE_SEGMENT = 6.seconds
        val MIN_MOTIVATION_SEGMENT = 40.seconds
        val MIN_REMAINING_FOR_INSTRUCTIONS = 20.seconds

        /** Five-minute marks up to the longest session the app builds, plus one minute. */
        val MILESTONE_SECONDS: List<Long> =
            (120 downTo 5 step 5).map { it * 60L } + listOf(60L)
    }
}

/**
 * One instant's worth of coaching output.
 *
 * At most one utterance, because two at once is the failure mode the whole design avoids. The
 * tone and haptic are not utterances and are not subject to that limit or to the minimum gap:
 * they are a boundary marker, not speech.
 */
internal data class CueBatch(
    val speech: SpeechCue? = null,
    val tone: CueTone? = null,
    val haptic: HapticCue? = null,
) {
    val isEmpty: Boolean get() = speech == null && tone == null && haptic == null

    companion object {
        val NONE = CueBatch()
    }
}

package com.visceralfit.feature.workout.player

import com.visceralfit.core.common.DurationFormat
import com.visceralfit.domain.model.CoachingPreferences
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.model.SegmentKind
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Every word the app says out loud, in one reviewable place.
 *
 * WHY IT IS SEPARATE FROM [CueScheduler]: the scheduler decides *when* to speak and is
 * judged by timing tests; this decides *what* to say and is judged by reading it. The
 * motivational lines in particular are the most subjective content in the app, and they are
 * the one thing here that a reviewer should argue with rather than verify.
 *
 * Authoring rules, from `framework/09_coaching_and_tts_spec.md` §5:
 *  - Under 12 words for anything spoken during work.
 *  - Numerals, not spelled-out numbers — every mainstream engine reads "10" correctly and in
 *    the user's locale, while "ten" hard-codes English. The one exception is the countdown,
 *    where "three, two, one" is the phrase itself rather than a quantity.
 *  - No symbols, no abbreviations, no emoji. [DurationFormat.spoken] is the only correct way
 *    to speak a duration; it handles pluralisation.
 *  - Nothing here may make a claim the evidence base does not support. `CueTextTest` checks
 *    every string in this file against `ProhibitedClaims`.
 */
internal object CueText {

    /** Spoken once at session start when any exercise in the plan carries `cardiac_caution`. */
    const val SAFETY = "Stop if you feel chest pain, dizziness or unusual breathlessness."

    const val HALFWAY = "Halfway."

    /** A work segment ending. Rest endings use [restCountdown] instead. */
    const val COUNTDOWN = "Three. Two. One."

    const val REST_COUNTDOWN = "Back on in three, two, one."

    /**
     * What to say when a segment begins.
     *
     * For anything with an exercise this is the catalogue's own `spokenInstruction`, which is
     * authored against the same word limits and already names the movement and its single
     * most important cue. Rewriting it here would put the same content in two places, and the
     * catalogue's copy is the one under the validation gate.
     */
    fun segmentStart(segment: Segment): String? = segment.exercise?.spokenInstruction
        ?: when (segment.kind) {
            SegmentKind.REST -> "Rest."
            SegmentKind.TRANSITION -> "Change over."
            SegmentKind.ACTIVE_RECOVERY -> "Keep moving, easy."
            SegmentKind.WORK -> null
        }

    /**
     * The last three seconds of a segment.
     *
     * During a rest this absorbs the next-exercise announcement rather than letting two
     * utterances collide inside three seconds (spec §2): "Seated climb in three, two, one."
     */
    fun countdown(segment: Segment, next: Segment?, prefs: CoachingPreferences): String? = when {
        !segment.isRestLike -> COUNTDOWN.takeIf { prefs.announceCountdown }
        combinesIntoCountdown(segment, next, prefs) ->
            "${nextLabel(next!!, segment)!!.phrase} in three, two, one."

        prefs.announceRestCountdown -> REST_COUNTDOWN
        else -> null
    }

    /**
     * True when the rest countdown will carry the next-exercise announcement itself, so the
     * separate five-second cue must be suppressed.
     *
     * The [MIN_ANNOUNCE_SEGMENT] floor is spec §2's rule: in a segment shorter than six
     * seconds the announcement would overlap the segment-start cue, so it is skipped outright
     * rather than spoken over.
     */
    fun combinesIntoCountdown(segment: Segment, next: Segment?, prefs: CoachingPreferences): Boolean =
        segment.isRestLike &&
            segment.duration >= MIN_ANNOUNCE_SEGMENT &&
            prefs.announceRestCountdown &&
            prefs.announceNextExercise &&
            next != null &&
            nextLabel(next, segment) != null

    /** The standalone five-second announcement, or null when there is nothing worth naming. */
    fun nextUp(next: Segment, current: Segment): String? {
        val label = nextLabel(next, current) ?: return null
        return if (label.isRound) "${label.phrase}." else "Next up: ${label.phrase}."
    }

    /**
     * The `how_to` steps, read as one utterance.
     *
     * One utterance rather than several because the spec requires them to be interruptible
     * mid-sentence by the next transition cue, and `QUEUE_FLUSH` on a single utterance does
     * exactly that. Several utterances would leave the tail of the queue to be spoken after
     * the interruption, which is the failure this design exists to prevent.
     */
    fun fullInstructions(exercise: Exercise): String = exercise.howTo.joinToString(" ")

    /**
     * Roughly how long [exercise]'s short cue takes to speak, used to schedule the full
     * instructions after it rather than over it.
     *
     * An estimate, deliberately: the engine's real rate depends on the user's chosen voice and
     * `speechRate`, and asking the engine would mean waiting for a callback the scheduler
     * cannot block on. [MILLIS_PER_SPOKEN_WORD] is 150 words per minute, the figure the
     * catalogue's own word limits are derived from, so the two agree. Erring long is the safe
     * direction: too long and the instructions start a moment late, too short and they collide.
     */
    fun shortCueMillis(exercise: Exercise): Long =
        exercise.spokenInstruction.trim().split(WHITESPACE).size * MILLIS_PER_SPOKEN_WORD

    fun remaining(remaining: Duration): String = "${DurationFormat.spoken(remaining)} remaining."

    /**
     * Encouragement during a work segment, cycled in order so the same line never lands twice
     * in a row.
     *
     * These are about effort and technique, never about outcome. "Strong, hold this" is a fact
     * about what the user is doing; "burning fat" would be a claim about their body that this
     * app cannot make (`framework/02_evidence_base.md` §6). That is the whole editorial rule
     * for this list.
     */
    fun motivation(index: Int): String = MOTIVATION[index.mod(MOTIVATION.size)]

    val MOTIVATION = listOf(
        "Strong. Hold this.",
        "Stay with it.",
        "Good work. Keep the rhythm.",
        "Breathe steady.",
        "Nearly through this one.",
    )

    /** Every fixed string in this file, for the prohibited-claims check. */
    val ALL_FIXED: List<String> = MOTIVATION + listOf(
        SAFETY,
        HALFWAY,
        COUNTDOWN,
        REST_COUNTDOWN,
        "Rest.",
        "Change over.",
        "Keep moving, easy.",
    )

    /**
     * What is coming, as a bare noun phrase.
     *
     * When the next segment repeats the current exercise — HIIT rounds — the round number is
     * announced instead of the name. Hearing "next up: seated climb" six times in a row is
     * noise, and the round number is the information the user actually lacks (spec §2).
     */
    private fun nextLabel(next: Segment, current: Segment): NextLabel? {
        val exercise = next.exercise ?: return null
        val round = next.roundIndex
        val total = next.roundTotal
        val repeats = exercise.id == current.exercise?.id
        return when {
            repeats && round != null && total != null -> NextLabel("Round $round of $total", isRound = true)
            repeats -> null
            else -> NextLabel(exercise.name, isRound = false)
        }
    }

    private data class NextLabel(val phrase: String, val isRound: Boolean)

    private val Segment.isRestLike: Boolean
        get() = kind == SegmentKind.REST ||
            kind == SegmentKind.TRANSITION ||
            kind == SegmentKind.ACTIVE_RECOVERY

    private val WHITESPACE = Regex("\\s+")

    /** 150 words per minute, the rate the catalogue's spoken-cue word limits assume. */
    private const val MILLIS_PER_SPOKEN_WORD = 400L

    /** Spec §2: below this, the next-exercise cue would overlap the segment-start cue. */
    private val MIN_ANNOUNCE_SEGMENT = 6.seconds
}

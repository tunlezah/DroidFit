package com.visceralfit.domain.engine

import com.visceralfit.domain.model.WorkoutStyle
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The per-style structural constants and the warm-up / main / cool-down split.
 *
 * Every number here comes from `framework/07_workout_engine_spec.md` §2 and is
 * restated in `framework/data/workout_templates.json`, which
 * `scripts/check_framework_data.py` recomputes. If you change one, change all three.
 */
internal data class StyleBudget(
    val warmUpFraction: Double,
    val minWarmUpSeconds: Int,
    val maxWarmUpSeconds: Int,
    val minCoolDownSeconds: Int,
    val minMainSeconds: Int,
) {
    /** The shortest total that can be built in this style. */
    val minTotalSeconds: Int get() = minWarmUpSeconds + minMainSeconds + minCoolDownSeconds

    companion object {
        fun of(style: WorkoutStyle): StyleBudget = when (style) {
            WorkoutStyle.HIIT -> StyleBudget(
                warmUpFraction = 0.20,
                minWarmUpSeconds = 300,
                maxWarmUpSeconds = 600,
                minCoolDownSeconds = 180,
                minMainSeconds = 480,
            )

            WorkoutStyle.ZONE_2 -> StyleBudget(
                warmUpFraction = 0.12,
                minWarmUpSeconds = 180,
                maxWarmUpSeconds = 420,
                minCoolDownSeconds = 120,
                minMainSeconds = 240,
            )

            WorkoutStyle.MIXED -> StyleBudget(
                warmUpFraction = 0.15,
                minWarmUpSeconds = 180,
                maxWarmUpSeconds = 480,
                minCoolDownSeconds = 120,
                minMainSeconds = 300,
            )

            WorkoutStyle.RECOVERY -> StyleBudget(
                warmUpFraction = 0.15,
                minWarmUpSeconds = 120,
                maxWarmUpSeconds = 300,
                minCoolDownSeconds = 120,
                minMainSeconds = 180,
            )
        }
    }
}

/**
 * What the engine can build, exposed to the UI.
 *
 * REQ-024: the Train screen must disable a style the chosen duration cannot support and
 * say why, rather than letting the user pick it and then failing generation. That means
 * the screen needs the same minimums the generator enforces — from the same place, so the
 * two cannot disagree. [StyleBudget] itself stays internal because nothing outside the
 * engine should be reading fractions and clamps.
 */
object SessionLimits {

    /** The accepted request range (A-0003). */
    val minimumDuration: Duration = SessionConstants.MIN_TOTAL_SECONDS.seconds
    val maximumDuration: Duration = SessionConstants.MAX_TOTAL_SECONDS.seconds

    /** The shortest session that can honestly be built in [style]. */
    fun minimumFor(style: WorkoutStyle): Duration = StyleBudget.of(style).minTotalSeconds.seconds

    /** Styles that [duration] can support, in declaration order. */
    fun stylesFor(duration: Duration): Set<WorkoutStyle> = WorkoutStyle.entries
        .filterTo(linkedSetOf()) { duration in minimumFor(it)..maximumDuration }
}

/** The three block lengths, in whole seconds, that a session is built to fill exactly. */
internal data class BlockBudget(
    val warmUpSeconds: Int,
    val mainSeconds: Int,
    val coolDownSeconds: Int,
) {
    val totalSeconds: Int get() = warmUpSeconds + mainSeconds + coolDownSeconds
}

/**
 * Session-wide constants. Kept in one object so the engine has no bare literals and a
 * reviewer has one list to compare against the specification.
 */
internal object SessionConstants {
    /** Accepted request range (A-0003). Outside it, generation fails rather than clamping. */
    const val MIN_TOTAL_SECONDS = 180
    const val MAX_TOTAL_SECONDS = 7200

    const val COOL_DOWN_FRACTION = 0.10
    const val MAX_COOL_DOWN_SECONDS = 300

    /** Time budgeted for changing equipment or position, charged to the enclosing block. */
    const val TRANSITION_SECONDS = 20

    /** Warm-up and cool-down get one segment per this many seconds, within their bounds. */
    const val SECONDS_PER_RAMP_SEGMENT = 90
    const val MIN_WARM_UP_SEGMENTS = 2
    const val MAX_WARM_UP_SEGMENTS = 4
    const val MIN_COOL_DOWN_SEGMENTS = 2
    const val MAX_COOL_DOWN_SEGMENTS = 3

    /**
     * No segment shorter than this is ever emitted. A 4-second "segment" is a rounding
     * artefact, not a prescription: it cannot be cued, and it reads as a bug on screen.
     * Residue below this threshold is folded into the neighbouring segment instead
     * (D-0024).
     */
    const val MIN_SEGMENT_SECONDS = 20

    /** Pool boundaries from spec §3. */
    const val WARM_UP_MAX_MET = 4.0
    const val MOBILITY_MAX_MET = 2.5
    const val STRENGTH_MIN_MET = 2.5
    const val VIGOROUS_MIN_MET = 8.0
    const val THRESHOLD_MIN_MET = 6.0
    const val THRESHOLD_MAX_MET = 10.9
    const val STEADY_MIN_MET = 4.0
    const val STEADY_MAX_MET = 9.0

    val transition: Duration = TRANSITION_SECONDS.seconds

    /**
     * Splits [totalSeconds] into the three blocks. Order matters and is specified:
     * warm-up first, then cool-down, and the main block takes what is left.
     */
    fun split(totalSeconds: Int, budget: StyleBudget): BlockBudget {
        val warmUp = (totalSeconds * budget.warmUpFraction).roundToInt()
            .coerceIn(budget.minWarmUpSeconds, budget.maxWarmUpSeconds)
        val coolDown = (totalSeconds * COOL_DOWN_FRACTION).roundToInt()
            .coerceIn(budget.minCoolDownSeconds, MAX_COOL_DOWN_SECONDS)
        return BlockBudget(
            warmUpSeconds = warmUp,
            mainSeconds = totalSeconds - warmUp - coolDown,
            coolDownSeconds = coolDown,
        )
    }
}

/**
 * A HIIT interval template.
 *
 * [mainSecondsNeeded] is `rounds × work + (rounds − 1) × recovery`. The `(rounds − 1)`
 * is deliberate: there is no recovery after the final work interval because the
 * cool-down serves that purpose. This is the arithmetic the framework got wrong on
 * first authoring (KI-0010), so it is computed here rather than tabulated.
 */
internal data class IntervalTemplate(
    val id: String,
    val workSeconds: Int,
    val recoverySeconds: Int,
    val rounds: Int,
    val evidenceKey: String,
) {
    val mainSecondsNeeded: Int get() = secondsFor(rounds)

    fun secondsFor(roundCount: Int): Int =
        roundCount * workSeconds + (roundCount - 1) * recoverySeconds

    companion object {
        /**
         * Largest first, because the engine takes the first that fits. `4x4` and `5x3`
         * both need 1500 s; `4x4` is listed first so the tie resolves in favour of the
         * only template with direct trial evidence behind it (`helgerud2007`).
         */
        val ALL: List<IntervalTemplate> = listOf(
            IntervalTemplate("4x4", workSeconds = 240, recoverySeconds = 180, rounds = 4, evidenceKey = "helgerud2007"),
            IntervalTemplate("5x3", workSeconds = 180, recoverySeconds = 150, rounds = 5, evidenceKey = "derived"),
            IntervalTemplate("6x2", workSeconds = 120, recoverySeconds = 120, rounds = 6, evidenceKey = "derived"),
            IntervalTemplate("8x1", workSeconds = 60, recoverySeconds = 90, rounds = 8, evidenceKey = "derived"),
            IntervalTemplate("10x30s", workSeconds = 30, recoverySeconds = 60, rounds = 10, evidenceKey = "derived"),
            IntervalTemplate("6x30s", workSeconds = 30, recoverySeconds = 60, rounds = 6, evidenceKey = "derived"),
        )

        /**
         * Recovery segments may be lengthened by at most this much to absorb leftover
         * time before an extra active-recovery segment is added (spec §4.2).
         */
        const val MAX_RECOVERY_EXTENSION_SECONDS = 60

        /**
         * Ceiling on added rounds. The spec fixes the round count per template, which
         * leaves a 60-minute HIIT request with a 20-minute tail of easy spinning — the
         * duration invariant holds but the programming is wrong. Extra rounds of the
         * chosen template are added while they fit, up to this bound, which keeps a long
         * interval session an interval session (D-0025).
         */
        const val MAX_ROUNDS = 20
    }
}

/** Mixed-style structural parameters, spec §4.4. */
internal object MixedConstants {
    const val MIN_SURGES = 2
    const val MAX_SURGES = 5
    const val SECONDS_PER_SURGE = 300
    const val MIN_BASE_SECONDS_PER_SEGMENT = 120

    const val LONG_MAIN_SECONDS = 1800
    const val LONG_SURGE_SECONDS = 180
    const val MEDIUM_MAIN_SECONDS = 1200
    const val MEDIUM_SURGE_SECONDS = 120
    const val SHORT_SURGE_SECONDS = 60

    /** No surge may start inside the first this-many seconds of the main block. */
    const val EARLIEST_SURGE_START_SECONDS = 90

    /** No surge may end inside the last this-many seconds of the main block. */
    const val LATEST_SURGE_END_MARGIN_SECONDS = 60

    fun surgeSecondsFor(mainSeconds: Int): Int = when {
        mainSeconds >= LONG_MAIN_SECONDS -> LONG_SURGE_SECONDS
        mainSeconds >= MEDIUM_MAIN_SECONDS -> MEDIUM_SURGE_SECONDS
        else -> SHORT_SURGE_SECONDS
    }
}

/** Zone 2 and recovery structural parameters, spec §4.3 and §4.5. */
internal object ContinuousConstants {
    const val SECONDS_PER_STEADY_SEGMENT = 600
    const val MAX_STEADY_SEGMENTS = 3

    const val MIN_RECOVERY_SEGMENT_SECONDS = 60
    const val MAX_RECOVERY_SEGMENT_SECONDS = 120
}

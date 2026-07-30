package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.WorkoutStyle

/**
 * The outcome of building a main block: the segments, the style that was actually
 * built, and anything the user must be told about how it differs from the request.
 */
internal data class MainBlock(
    val segments: List<PlannedSegment>,
    val builtStyle: WorkoutStyle,
    val cappedAt: IntensityAnchor?,
    val notes: List<String>,
) {
    /** The machine the main block is performed on, if any. Drives the warm-up and spin-down. */
    val machineModality: Modality?
        get() = segments.asSequence()
            .mapNotNull { it.exercise?.modality }
            .firstOrNull { it.isMachineCardio }
}

/**
 * Builds the main block for each style, spec §4.2–4.5.
 *
 * Every builder fills its budget exactly. Nothing here reads a clock or a global RNG:
 * the only source of variation is [picker], which the generator seeds once per request.
 */
internal class MainBlockBuilder(
    private val pools: ExercisePools,
    private val picker: ExercisePicker,
) {

    fun build(style: WorkoutStyle, mainSeconds: Int): MainBlock = when {
        // A session with no machine available is a Pilates session, and must be built and
        // recorded as one (spec §7, REQ-004). Building interval or surge structures out of
        // mat work would produce a session presented as aerobic training that is not
        // aerobic training — the one substitution the evidence base explicitly forbids
        // (02_evidence_base.md §1.5). RECOVERY is the honest recording because it is
        // defined as work that counts toward weekly volume but contributes no vigorous
        // minutes, which is exactly what spec §7.3 requires of a Pilates session (D-0022).
        pools.isPilatesOnly -> recovery(mainSeconds).let { block ->
            if (style == WorkoutStyle.RECOVERY) block else block.copy(notes = block.notes + PILATES_ONLY_NOTE)
        }

        style == WorkoutStyle.HIIT -> intervals(mainSeconds)
        style == WorkoutStyle.ZONE_2 -> steady(mainSeconds, WorkoutStyle.ZONE_2, emptyList())
        style == WorkoutStyle.MIXED -> mixed(mainSeconds)
        else -> recovery(mainSeconds)
    }

    // --- HIIT, spec §4.2 ----------------------------------------------------------

    private fun intervals(mainSeconds: Int): MainBlock {
        val template = IntervalTemplate.ALL.firstOrNull { it.mainSecondsNeeded <= mainSeconds }
            ?: return steady(mainSeconds, WorkoutStyle.ZONE_2, listOf(NO_TEMPLATE_FITS_NOTE))
        val work = PoolFallbacks.vigorous(pools)
        if (work.isEmpty) return steady(mainSeconds, WorkoutStyle.ZONE_2, listOf(NO_MACHINE_NOTE))

        val rounds = roundsFor(template, mainSeconds)
        val workModality = dominantModality(work.exercises)
        val onModality = work.exercises.filter { it.modality == workModality }
        val rotation = picker.pickCycling(
            onModality,
            if (onModality.size >= ROTATION_THRESHOLD) ROTATION_MAX else 1,
        )
        // Active recovery between intervals wants the easiest thing available on the same
        // machine, then the easiest anywhere. Falling straight back to the work pool would
        // name a vigorous interval as the recovery — which is how a recovery segment ends up
        // labelled "hard interval" (D-0038).
        val recoveryExercise = picker.pickOrNull(
            pools.steady.filter { it.modality == workModality }
                .ifEmpty { pools.steady }
                .ifEmpty { pools.mobility }
                .ifEmpty { onModality },
        )

        val extensions = recoveryExtensions(template, rounds, mainSeconds)
        val segments = buildList {
            for (round in 1..rounds) {
                add(
                    PlannedSegment(
                        kind = SegmentKind.WORK,
                        seconds = template.workSeconds,
                        exercise = rotation[(round - 1) % rotation.size],
                        intensity = SegmentPlanning.capped(IntensityTarget.VIGOROUS, work.cappedAt),
                        roundIndex = round,
                        roundTotal = rounds,
                    ),
                )
                if (round < rounds) {
                    add(
                        PlannedSegment(
                            kind = SegmentKind.ACTIVE_RECOVERY,
                            seconds = template.recoverySeconds + extensions.perRecovery[round - 1],
                            exercise = recoveryExercise,
                            intensity = IntensityTarget.RECOVERY,
                            roundIndex = round,
                            roundTotal = rounds,
                        ),
                    )
                }
            }
            if (extensions.residue > 0) {
                add(
                    PlannedSegment(
                        kind = SegmentKind.ACTIVE_RECOVERY,
                        seconds = extensions.residue,
                        exercise = recoveryExercise,
                        intensity = IntensityTarget.RECOVERY,
                    ),
                )
            }
        }
        val notes = buildList {
            work.note?.let(::add)
            if (rounds != template.rounds) add("${template.id} extended to $rounds rounds to fill the session")
        }
        return MainBlock(segments, WorkoutStyle.HIIT, work.cappedAt, notes)
    }

    /**
     * The chosen template's own round count, extended while whole rounds still fit.
     *
     * See [IntervalTemplate.MAX_ROUNDS] for why this departs from the specification's
     * fixed round count.
     */
    private fun roundsFor(template: IntervalTemplate, mainSeconds: Int): Int {
        var rounds = template.rounds
        while (rounds < IntervalTemplate.MAX_ROUNDS && template.secondsFor(rounds + 1) <= mainSeconds) {
            rounds++
        }
        return rounds
    }

    /**
     * Spreads the leftover across the recovery segments, capped per segment, with
     * whatever will not fit becoming a trailing active-recovery segment.
     */
    private fun recoveryExtensions(template: IntervalTemplate, rounds: Int, mainSeconds: Int): Extensions {
        val recoveryCount = rounds - 1
        val leftover = mainSeconds - template.secondsFor(rounds)
        if (recoveryCount <= 0) return Extensions(emptyList(), leftover)

        val cap = IntervalTemplate.MAX_RECOVERY_EXTENSION_SECONDS
        val perSegment = minOf(cap, leftover / recoveryCount)
        val extensions = MutableList(recoveryCount) { perSegment }
        var residue = leftover - perSegment * recoveryCount
        if (perSegment < cap) {
            // The remainder of the integer division is always smaller than the segment
            // count, so one extra second each clears it exactly.
            repeat(minOf(residue, recoveryCount)) { index -> extensions[index]++ }
            residue -= minOf(residue, recoveryCount)
        }
        if (residue in 1 until SessionConstants.MIN_SEGMENT_SECONDS) {
            // Emitting a sub-20-second segment is worse than one recovery running
            // slightly over the cap (D-0024).
            extensions[extensions.lastIndex] += residue
            residue = 0
        }
        return Extensions(extensions, residue)
    }

    private data class Extensions(val perRecovery: List<Int>, val residue: Int)

    // --- ZONE_2, spec §4.3 --------------------------------------------------------

    private fun steady(mainSeconds: Int, builtStyle: WorkoutStyle, inheritedNotes: List<String>): MainBlock {
        val pool = PoolFallbacks.steady(pools)
        val count = (mainSeconds / ContinuousConstants.SECONDS_PER_STEADY_SEGMENT)
            .coerceIn(1, ContinuousConstants.MAX_STEADY_SEGMENTS)
        val exercises = picker.pickCycling(pool.exercises, count)
        if (exercises.isEmpty()) {
            throw GenerationFailure.InsufficientVariety(required = count, available = 0)
        }
        val planned = SegmentPlanning.evenSplit(mainSeconds, count).mapIndexed { index, seconds ->
            PlannedSegment(
                kind = SegmentKind.WORK,
                seconds = seconds,
                exercise = exercises[index],
                intensity = SegmentPlanning.capped(IntensityTarget.ZONE_2, pool.cappedAt),
            )
        }
        return MainBlock(
            segments = SegmentPlanning.withTransitions(planned),
            builtStyle = builtStyle,
            cappedAt = pool.cappedAt,
            notes = inheritedNotes + listOfNotNull(pool.note),
        )
    }

    // --- MIXED, spec §4.4 ---------------------------------------------------------

    private fun mixed(mainSeconds: Int): MainBlock {
        val layout = mixedLayout(mainSeconds)
            ?: return steady(mainSeconds, WorkoutStyle.ZONE_2, listOf(NOT_ENOUGH_ROOM_FOR_SURGES_NOTE))

        val base = PoolFallbacks.steady(pools)
        val surge = PoolFallbacks.threshold(pools)
        val baseExercise = picker.pickOrNull(base.exercises)
            ?: throw GenerationFailure.InsufficientVariety(required = 1, available = 0)
        // Prefer surging on the machine the base is already on: hopping machines mid-block
        // spends the surge on the changeover.
        val surgeExercise = picker.pickOrNull(
            surge.exercises.filter { it.modality == baseExercise.modality }.ifEmpty { surge.exercises },
        ) ?: baseExercise

        val baseSeconds = SegmentPlanning.evenSplit(layout.baseTotal, layout.surgeCount + 1)
        val planned = buildList {
            baseSeconds.forEachIndexed { index, seconds ->
                add(
                    PlannedSegment(
                        kind = SegmentKind.WORK,
                        seconds = seconds,
                        exercise = baseExercise,
                        intensity = SegmentPlanning.capped(IntensityTarget.ZONE_2, base.cappedAt),
                    ),
                )
                if (index < layout.surgeCount) {
                    add(
                        PlannedSegment(
                            kind = SegmentKind.WORK,
                            seconds = layout.surgeSeconds,
                            exercise = surgeExercise,
                            intensity = SegmentPlanning.capped(IntensityTarget.THRESHOLD, surge.cappedAt),
                            roundIndex = index + 1,
                            roundTotal = layout.surgeCount,
                        ),
                    )
                }
            }
        }
        return MainBlock(
            segments = SegmentPlanning.withTransitions(planned),
            builtStyle = WorkoutStyle.MIXED,
            cappedAt = surge.cappedAt ?: base.cappedAt,
            notes = listOfNotNull(base.note, surge.note),
        )
    }

    private data class MixedLayout(val surgeCount: Int, val surgeSeconds: Int, val baseTotal: Int)

    /**
     * Resolves the surge count downwards until the base segments are long enough, both
     * to satisfy the specification's per-segment minimum and to keep every surge out of
     * the first 90 s and last 60 s of the block. Null when fewer than two surges survive,
     * which is the signal to build a Zone 2 block instead and say so in the title.
     */
    private fun mixedLayout(mainSeconds: Int): MixedLayout? {
        val surgeSeconds = MixedConstants.surgeSecondsFor(mainSeconds)
        var surgeCount = (mainSeconds / MixedConstants.SECONDS_PER_SURGE)
            .coerceIn(MixedConstants.MIN_SURGES, MixedConstants.MAX_SURGES)
        while (surgeCount >= MixedConstants.MIN_SURGES) {
            val baseTotal = mainSeconds - surgeCount * surgeSeconds
            val perBase = if (baseTotal > 0) baseTotal / (surgeCount + 1) else 0
            val roomy = baseTotal >= surgeCount * MixedConstants.MIN_BASE_SECONDS_PER_SEGMENT &&
                perBase >= MixedConstants.EARLIEST_SURGE_START_SECONDS &&
                perBase >= MixedConstants.LATEST_SURGE_END_MARGIN_SECONDS
            if (roomy) return MixedLayout(surgeCount, surgeSeconds, baseTotal)
            surgeCount--
        }
        return null
    }

    // --- RECOVERY, spec §4.5 ------------------------------------------------------

    private fun recovery(mainSeconds: Int): MainBlock {
        val mobility = PoolFallbacks.mobility(pools)
        val strength = PoolFallbacks.strength(pools)
        val count = ceilDiv(mainSeconds, ContinuousConstants.MAX_RECOVERY_SEGMENT_SECONDS)
            .coerceAtLeast(1)
        val mobilityCount = ceilDiv(count, 2)
        val strengthCount = count - mobilityCount
        val mobilityPicks = picker.pickCycling(mobility.exercises, mobilityCount)
        val strengthPicks = when {
            strengthCount > 0 -> picker.pickCycling(strength.exercises, strengthCount)
            else -> emptyList()
        }
        if (mobilityPicks.isEmpty() && strengthPicks.isEmpty()) {
            throw GenerationFailure.InsufficientVariety(required = count, available = 0)
        }

        val order = alternate(mobilityPicks, strengthPicks)
        val planned = SegmentPlanning.evenSplit(mainSeconds, count).mapIndexed { index, seconds ->
            PlannedSegment(
                kind = SegmentKind.WORK,
                seconds = seconds,
                exercise = order[index % order.size],
                intensity = IntensityTarget.RECOVERY,
            )
        }
        return MainBlock(
            segments = SegmentPlanning.withTransitions(planned),
            builtStyle = WorkoutStyle.RECOVERY,
            cappedAt = null,
            notes = listOfNotNull(mobility.note, strength.note),
        )
    }

    /** Interleaves two lists, starting with [first] and appending whatever is left over. */
    private fun alternate(first: List<Exercise>, second: List<Exercise>): List<Exercise> = buildList {
        val limit = maxOf(first.size, second.size)
        for (index in 0 until limit) {
            first.getOrNull(index)?.let(::add)
            second.getOrNull(index)?.let(::add)
        }
    }

    /** The machine modality with the most entries, ties broken by id so it never varies. */
    private fun dominantModality(exercises: List<Exercise>): Modality =
        exercises.groupBy { it.modality }
            .entries
            .sortedWith(compareByDescending<Map.Entry<Modality, List<Exercise>>> { it.value.size }.thenBy { it.key.id })
            .first()
            .key

    private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor

    private companion object {
        const val ROTATION_THRESHOLD = 3
        const val ROTATION_MAX = 3

        const val NO_MACHINE_NOTE = "no machine cardio available; built as steady work"
        const val PILATES_ONLY_NOTE =
            "No machine was available, so this is a Pilates strength and mobility session. " +
                "It counts toward your weekly minutes but not toward vigorous minutes."
        const val NO_TEMPLATE_FITS_NOTE = "too short for any interval template; built as steady work"
        const val NOT_ENOUGH_ROOM_FOR_SURGES_NOTE = "too short to place surges safely; built as steady work"
    }
}

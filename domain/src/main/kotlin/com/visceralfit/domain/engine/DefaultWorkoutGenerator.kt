package com.visceralfit.domain.engine

import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.SegmentKind
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutBlock
import com.visceralfit.domain.model.WorkoutStyle
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * The reference implementation of [WorkoutGenerator], built to
 * `framework/07_workout_engine_spec.md`.
 *
 * PURE BY CONSTRUCTION. The catalogue is handed to the constructor rather than read
 * from a repository, because the contract's `generate` is not a suspending function and
 * because a generator that cannot reach a database is a generator whose entire
 * behaviour is reproducible from its inputs. `GenerateWorkout` is the use case that
 * loads the catalogue and delegates here.
 *
 * DETERMINISM (spec §6). The only randomness is a single [Random] created from
 * `request.seed` once per call. Nothing reads a clock. Every pool is sorted by exercise
 * id before it can influence a choice, `request.modalities` is sorted before it is
 * iterated, and no `hashCode()` participates in an ordering decision. Random draws
 * happen in a fixed order — main block, then warm-up, then cool-down — because the
 * warm-up's last segment has to know which machine the main block put the user on.
 */
class DefaultWorkoutGenerator(private val catalogue: List<Exercise>) : WorkoutGenerator {

    override fun generate(request: WorkoutRequest): Result<Workout> = runCatching { build(request) }

    private fun build(request: WorkoutRequest): Workout {
        val totalSeconds = request.duration.inWholeSeconds.toInt()
        val budget = StyleBudget.of(request.style)
        val blocks = blockBudgetFor(request, totalSeconds, budget)

        val eligible = eligibleFor(request)
        if (eligible.isEmpty()) {
            throw GenerationFailure.NoEligibleExercises(request.modalities, request.level)
        }

        val pools = ExercisePools.partition(eligible)
        val picker = ExercisePicker(Random(request.seed), request.recentExerciseIds)
        val ceiling = IntensityAnchor.ceilingOf(request.effortCeiling)
        val main = MainBlockBuilder(pools, picker, ceiling).build(request.style, blocks.mainSeconds)
        val warmUp = warmUpSegments(pools, picker, blocks.warmUpSeconds, main.machineModality)
        val coolDown = coolDownSegments(pools, picker, blocks.coolDownSeconds, main.machineModality)

        val workoutBlocks = listOf(
            WorkoutBlock(BlockKind.WARM_UP, warmUp.map { it.toSegment() }),
            WorkoutBlock(BlockKind.MAIN, main.segments.map { it.toSegment() }),
            WorkoutBlock(BlockKind.COOL_DOWN, coolDown.map { it.toSegment() }),
        )
        val usedModalities = workoutBlocks
            .flatMap { block -> block.segments.mapNotNull { it.exercise?.modality } }
            .toSortedSet(compareBy { it.id })

        return Workout(
            id = workoutId(request),
            title = SessionTitle.of(
                builtStyle = main.builtStyle,
                cappedAt = main.cappedAt,
                modalities = usedModalities,
                totalSeconds = totalSeconds,
                isStrengthOnly = pools.isStrengthOnly,
            ),
            style = main.builtStyle,
            modalities = usedModalities,
            level = request.level,
            blocks = workoutBlocks,
            requestedDuration = request.duration,
            generationSeed = request.seed,
            buildNotes = buildNotes(request, main),
        )
    }

    /**
     * The block split, or a failure naming the shortest total this style can be built at.
     *
     * Both the out-of-range case and the too-short-main case report the same minimum,
     * because it is what the UI needs in either case: which styles it may offer for the
     * duration the user chose (REQ-024). An over-long request reports it too — the type is
     * `DurationTooShort` for both ends of the range, which the specification fixes so the
     * UI does not need a second error path.
     */
    private fun blockBudgetFor(request: WorkoutRequest, totalSeconds: Int, budget: StyleBudget): BlockBudget {
        val inRange = totalSeconds >= SessionConstants.MIN_TOTAL_SECONDS &&
            totalSeconds <= SessionConstants.MAX_TOTAL_SECONDS
        val blocks = if (inRange) SessionConstants.split(totalSeconds, budget) else null
        if (blocks == null || blocks.mainSeconds < budget.minMainSeconds) {
            throw GenerationFailure.DurationTooShort(request.duration, budget.minTotalSeconds.seconds)
        }
        return blocks
    }

    private fun eligibleFor(request: WorkoutRequest): List<Exercise> {
        // Sorted so the eligible set never depends on the catalogue's arrival order, and
        // `modalities` is read as a sorted list rather than iterated as a Set (spec §6).
        val allowed = request.modalities.map { it.id }.sorted().toSet()
        return catalogue
            .filter { exercise ->
                exercise.modality.id in allowed &&
                    request.level.canAttempt(exercise.difficulty) &&
                    exercise.cautionTags.none { it in request.avoidTags }
            }
            .sortedBy { it.id }
    }

    // --- Warm-up, spec §4.1 -------------------------------------------------------

    private fun warmUpSegments(
        pools: ExercisePools,
        picker: ExercisePicker,
        seconds: Int,
        mainModality: Modality?,
    ): List<PlannedSegment> {
        val resolved = PoolFallbacks.warmUp(pools)
        val count = (seconds / SessionConstants.SECONDS_PER_RAMP_SEGMENT)
            .coerceIn(SessionConstants.MIN_WARM_UP_SEGMENTS, SessionConstants.MAX_WARM_UP_SEGMENTS)
        val exercises = warmUpExercises(resolved.exercises, picker, count, mainModality)
        val planned = SegmentPlanning.evenSplit(seconds, count).mapIndexed { index, segmentSeconds ->
            PlannedSegment(
                kind = SegmentKind.WORK,
                seconds = segmentSeconds,
                exercise = exercises[index],
                // The first segment eases in; the rest ramp to Zone 2.
                intensity = if (index == 0) IntensityTarget.RECOVERY else IntensityTarget.ZONE_2,
            )
        }
        return SegmentPlanning.withTransitions(planned)
    }

    /**
     * Ascending by MET so the warm-up ramps, with one exception the specification makes
     * explicit: when a machine is available the **final** segment is on it, so the user
     * is already on the machine when the main block starts. That constraint outranks
     * strict MET ordering, and the machine is chosen to match the one the main block uses
     * rather than at random (D-0022) — a transition onto the wrong machine would defeat
     * the point of the rule.
     */
    private fun warmUpExercises(
        pool: List<Exercise>,
        picker: ExercisePicker,
        count: Int,
        mainModality: Modality?,
    ): List<Exercise> {
        val machines = pool.filter { it.modality.isMachineCardio }
        val preferred = machines.filter { it.modality == mainModality }.ifEmpty { machines }
        val floor = pool.filter { !it.modality.isMachineCardio }
        return when {
            preferred.isNotEmpty() && floor.isNotEmpty() -> {
                val ramp = picker.pick(floor, count - 1).sortedBy { it.metValue }
                ramp + checkNotNull(picker.pickOrNull(preferred))
            }

            preferred.isNotEmpty() -> picker.pickCycling(preferred, count).sortedBy { it.metValue }

            else -> picker.pick(pool, count).sortedBy { it.metValue }
        }
    }

    // --- Cool-down, spec §4.6 -----------------------------------------------------

    private fun coolDownSegments(
        pools: ExercisePools,
        picker: ExercisePicker,
        seconds: Int,
        mainModality: Modality?,
    ): List<PlannedSegment> {
        val resolved = PoolFallbacks.mobility(pools)
        val count = (seconds / SessionConstants.SECONDS_PER_RAMP_SEGMENT)
            .coerceIn(SessionConstants.MIN_COOL_DOWN_SEGMENTS, SessionConstants.MAX_COOL_DOWN_SEGMENTS)
        val spinDown = mainModality?.let { picker.pickOrNull(spinDownPool(pools, it)) }
        val floorCount = if (spinDown == null) count else count - 1
        val floor = if (floorCount > 0) picker.pick(resolved.exercises, floorCount) else emptyList()
        val exercises = listOfNotNull(spinDown) + floor

        val planned = SegmentPlanning.evenSplit(seconds, count).mapIndexed { index, segmentSeconds ->
            PlannedSegment(
                kind = SegmentKind.WORK,
                seconds = segmentSeconds,
                exercise = exercises[index % exercises.size],
                intensity = IntensityTarget.RECOVERY,
            )
        }
        return if (spinDown == null) {
            SegmentPlanning.withTransitions(planned)
        } else {
            SegmentPlanning.withTransitionAfterFirst(planned)
        }
    }

    /** Easy work on the machine the main block used, for the spin-down. */
    private fun spinDownPool(pools: ExercisePools, modality: Modality): List<Exercise> {
        val onMachine = pools.aerobic.filter { it.modality == modality }
        return onMachine
            .filter { IntensityAnchor.of(it.modality, it.metValue).isAtMost(IntensityAnchor.ZONE_2) }
            .ifEmpty { onMachine.sortedBy { it.metValue }.take(1) }
    }

    // --- Provenance ---------------------------------------------------------------

    private fun buildNotes(request: WorkoutRequest, main: MainBlock): List<String> = buildList {
        val ceiling = IntensityAnchor.ceilingOf(request.effortCeiling)
        if (ceiling != null && main.cappedAt == ceiling) {
            add(
                "Held at ${ceiling.id.replace('_', ' ')} because that is your effort ceiling. " +
                    "You can change it in Settings.",
            )
        }
        if (main.builtStyle != request.style) {
            val asked = SessionTitle.label(request.style)
            val built = SessionTitle.label(main.builtStyle)
            add("You asked for $asked; this session is built as $built.")
        }
        addAll(main.notes)
    }

    /**
     * A stable id derived from the request, so the same request always names the same
     * workout and any session can be reproduced from its id and seed.
     *
     * FNV-1a rather than [Any.hashCode]: enum `hashCode` is identity-based and therefore
     * differs between JVM runs, which would make ids — and any golden file containing
     * one — non-reproducible.
     */
    private fun workoutId(request: WorkoutRequest): String {
        val key = buildString {
            append(request.duration.inWholeSeconds)
            append('|').append(request.style.id)
            append('|').append(request.modalities.map { it.id }.sorted().joinToString(","))
            append('|').append(request.level.id)
            append('|').append(request.avoidTags.sorted().joinToString(","))
            append('|').append(request.seed)
        }
        return "w_" + fnv1a(key).toString(HEX_RADIX)
    }

    private fun fnv1a(value: String): Long {
        var hash = FNV_OFFSET_BASIS
        for (char in value) {
            hash = hash xor char.code.toLong()
            hash = (hash * FNV_PRIME) and FNV_MASK
        }
        return hash
    }

    private companion object {
        const val HEX_RADIX = 16
        const val FNV_OFFSET_BASIS = 0x811C9DC5L
        const val FNV_PRIME = 0x01000193L
        const val FNV_MASK = 0xFFFFFFFFL
    }
}

/**
 * Session titles. Separate from the generator because honesty about what was built is a
 * product requirement (REQ-004, spec §7), not a formatting detail — the rules here are
 * the ones a reviewer needs to be able to check in one place.
 */
internal object SessionTitle {

    fun of(
        builtStyle: WorkoutStyle,
        cappedAt: IntensityAnchor?,
        modalities: Set<Modality>,
        totalSeconds: Int,
        isStrengthOnly: Boolean,
    ): String {
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val name = when {
            isStrengthOnly -> "${strengthName(modalities)}: strength and control"
            cappedAt == null -> label(builtStyle)
            else -> "${label(builtStyle)} (${capLabel(cappedAt)})"
        }
        return "$name — $minutes min"
    }

    fun label(style: WorkoutStyle): String = when (style) {
        WorkoutStyle.HIIT -> "Intervals"
        WorkoutStyle.ZONE_2 -> "Steady"
        WorkoutStyle.MIXED -> "Mixed"
        WorkoutStyle.RECOVERY -> "Recovery"
    }

    /**
     * A session that carried no aerobic work is named for what it actually was. It must never
     * be presented as an interval or fat-loss session: the evidence base does not support
     * reformer or mat work as a visceral-fat intervention comparable to aerobic work
     * (spec §7, REQ-004, `wang2021pilates`).
     *
     * The reformer is named as Pilates because that is what it is. Floor work is named as
     * floor work rather than as Pilates, because the category holds general bodyweight
     * movement and calling a set of dead bugs "Pilates" is a claim about a method (D-0039).
     */
    private fun strengthName(modalities: Set<Modality>): String = when {
        modalities == setOf(Modality.REFORMER_PILATES) -> "Reformer Pilates"
        modalities == setOf(Modality.BODYWEIGHT) -> "Floor and bodyweight"
        Modality.REFORMER_PILATES in modalities -> "Pilates and floor work"
        else -> "Floor and bodyweight"
    }

    private fun capLabel(cappedAt: IntensityAnchor): String = when (cappedAt) {
        IntensityAnchor.VIGOROUS -> "vigorous"
        IntensityAnchor.THRESHOLD -> "threshold"
        IntensityAnchor.ZONE_2 -> "steady"
        IntensityAnchor.RECOVERY, IntensityAnchor.REST -> "easy"
    }

    private const val SECONDS_PER_MINUTE = 60
}

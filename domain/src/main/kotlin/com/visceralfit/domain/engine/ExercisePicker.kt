package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import kotlin.random.Random

/**
 * Deterministic, variety-aware selection from a pool, spec §5.
 *
 * DETERMINISM IS THE WHOLE POINT OF THIS CLASS. Everything else in the engine derives
 * its reproducibility from here, so the implementation is deliberately literal:
 *
 *  - the pool is sorted by id before anything else touches it, so the caller's ordering
 *    cannot leak into the result;
 *  - the only randomness is the [Random] handed in, which the generator creates once
 *    per request from `request.seed`;
 *  - scores are grouped and the groups are visited in ascending numeric order, never in
 *    map-iteration order.
 *
 * A single [ExercisePicker] is created per `generate` call and consumes its [random] in
 * call order, so the sequence of picks is part of the reproducible output.
 */
internal class ExercisePicker(private val random: Random, private val recent: List<String>) {

    /**
     * [count] exercises from [pool], least-recently-used first, shuffled within
     * equal-recency groups.
     *
     * Repeats are emitted only when [pool] is smaller than [count]: the front of the
     * ordering is reused, which keeps "never repeat while an unused eligible one
     * remains" true. A pool of fewer than two exercises cannot honestly fill three or
     * more distinct slots, so that case fails rather than producing a session that is
     * the same movement over and over.
     */
    fun pick(pool: List<Exercise>, count: Int): List<Exercise> {
        require(count > 0) { "pick() asked for $count exercises" }
        if (pool.isEmpty()) throw GenerationFailure.InsufficientVariety(required = count, available = 0)
        if (pool.size < MIN_POOL_FOR_REPEATS && count >= REPEAT_TOLERANCE_THRESHOLD) {
            throw GenerationFailure.InsufficientVariety(required = count, available = pool.size)
        }

        val ordered = order(pool)
        return List(count) { index -> ordered[index % ordered.size] }
    }

    /** A single exercise, or null when [pool] is empty. Never throws. */
    fun pickOrNull(pool: List<Exercise>): Exercise? =
        if (pool.isEmpty()) null else order(pool).first()

    /**
     * [count] exercises, cycling the ordering when the pool is smaller, and never
     * failing for lack of variety.
     *
     * Used where repetition is the prescription rather than a content gap: the base
     * segments of a Mixed session and the segments of a continuous Zone 2 block are one
     * sustained effort that has been divided so the coach has boundaries to cue on
     * (spec §4.3). Failing those with `InsufficientVariety` would refuse to build a
     * perfectly good steady ride on a single-machine catalogue (D-0024).
     */
    fun pickCycling(pool: List<Exercise>, count: Int): List<Exercise> {
        require(count > 0) { "pickCycling() asked for $count exercises" }
        if (pool.isEmpty()) return emptyList()
        val ordered = order(pool)
        return List(count) { index -> ordered[index % ordered.size] }
    }

    /**
     * The full pool in selection order: ascending by recency score, shuffled within each
     * score group.
     */
    private fun order(pool: List<Exercise>): List<Exercise> {
        val byScore = pool.sortedBy { it.id }.groupBy { recencyScore(it.id) }
        return byScore.keys.sorted().flatMap { score -> byScore.getValue(score).shuffled(random) }
    }

    /**
     * Zero when unused recently, otherwise higher the more recently it was used, so the
     * most recent exercise sorts last and is picked only when nothing else is left.
     */
    private fun recencyScore(id: String): Int {
        val index = recent.indexOf(id)
        return if (index < 0) 0 else recent.size - index
    }

    private companion object {
        /** Below this, repeating to fill a block is not variety, it is one exercise. */
        const val MIN_POOL_FOR_REPEATS = 2

        /** One or two repeated slots is a prescription; three or more is a content gap. */
        const val REPEAT_TOLERANCE_THRESHOLD = 3
    }
}

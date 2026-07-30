package com.visceralfit.domain.usecase

import com.visceralfit.domain.engine.DefaultWorkoutGenerator
import com.visceralfit.domain.engine.WorkoutRequest
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Loads the catalogue and generates a session.
 *
 * WHY THE GENERATOR IS NOT INJECTED DIRECTLY: `WorkoutGenerator.generate` is specified
 * as a non-suspending pure function, so it cannot read a repository. This use case is
 * the seam — it does the suspending catalogue read, then hands an immutable list to a
 * fresh [DefaultWorkoutGenerator]. Keeping the split means every engine test is a plain
 * JVM test with a literal list of exercises and no test doubles at all.
 */
class GenerateWorkout @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {
    suspend operator fun invoke(request: WorkoutRequest): Result<Workout> {
        val catalogue = exerciseRepository.getForModalities(request.modalities)
        return DefaultWorkoutGenerator(catalogue).generate(request)
    }
}

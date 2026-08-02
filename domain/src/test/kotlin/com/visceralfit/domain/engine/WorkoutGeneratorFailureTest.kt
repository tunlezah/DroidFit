package com.visceralfit.domain.engine

import com.visceralfit.domain.model.Exercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.IntensityTarget
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.WorkoutStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The failure paths and the honesty requirements — the two places where "it compiled and
 * produced a workout" is not good enough.
 *
 * A generation failure must arrive as a named reason the UI can turn into an actionable
 * message, because the alternative that competitors ship is a dead Start button
 * (`framework/03_competitive_analysis.md` §Complaint themes). And a session that could not
 * be built as requested must never be *presented* as the thing that was requested — that
 * is REQ-004, and for a Pilates-only session it is the direct implementation of the
 * evidence base's constraint on how Pilates is described.
 */
class WorkoutGeneratorFailureTest {

    private val generator = DefaultWorkoutGenerator(CatalogueFixture.ALL)

    // --- DurationTooShort ---------------------------------------------------------

    @Test
    fun `a request below the accepted range fails with the style's minimum`() {
        val failure = generator.generate(request(2.minutes, WorkoutStyle.ZONE_2)).failure()
        assertTrue(failure is GenerationFailure.DurationTooShort)
        failure as GenerationFailure.DurationTooShort
        assertEquals(2.minutes, failure.requested)
        assertEquals(StyleBudget.of(WorkoutStyle.ZONE_2).minTotalSeconds.seconds, failure.minimum)
    }

    @Test
    fun `a request above the accepted range fails rather than being clamped`() {
        val failure = generator.generate(request(121.minutes, WorkoutStyle.MIXED)).failure()
        assertTrue("121 minutes should not be silently clamped to 120", failure is GenerationFailure.DurationTooShort)
    }

    /**
     * The consequence the UI has to surface (REQ-024): a five-minute request cannot be a
     * HIIT session, because the smallest interval template needs eight minutes of main
     * block on its own.
     */
    @Test
    fun `five minutes cannot be a HIIT session and says so`() {
        val failure = generator.generate(request(5.minutes, WorkoutStyle.HIIT)).failure()
        assertTrue(failure is GenerationFailure.DurationTooShort)
        failure as GenerationFailure.DurationTooShort
        assertEquals(16.minutes, failure.minimum)
    }

    /**
     * The other half of REQ-024: the minimum is per style, so a duration that cannot be an
     * interval session can still be a good recovery session. This is why the failure has to
     * name the style's minimum rather than a single global one — the UI uses it to decide
     * which styles to offer for the chosen duration.
     */
    @Test
    fun `a duration too short for intervals is still long enough for recovery`() {
        assertTrue(generator.generate(request(7.minutes, WorkoutStyle.RECOVERY)).isSuccess)
        val failure = generator.generate(request(7.minutes, WorkoutStyle.HIIT)).exceptionOrNull()
        assertTrue(failure is GenerationFailure.DurationTooShort)
    }

    // --- NoEligibleExercises ------------------------------------------------------

    @Test
    fun `no enabled modality fails with the modalities and level that were asked for`() {
        val failure = generator.generate(
            request(20.minutes, WorkoutStyle.MIXED).copy(modalities = emptySet()),
        ).failure()
        assertTrue(failure is GenerationFailure.NoEligibleExercises)
        failure as GenerationFailure.NoEligibleExercises
        assertEquals(emptySet<Modality>(), failure.modalities)
    }

    /** Spec §1: every exercise excluded by a caution tag is still a named failure. */
    @Test
    fun `a catalogue in which every exercise is avoided fails with no eligible exercises`() {
        val everythingTagged = listOf(
            exercise("bodyweight_tagged_one", 2.8, cautionTags = setOf("lower_back")),
            exercise("bodyweight_tagged_two", 3.8, cautionTags = setOf("lower_back", "knee")),
        )
        val failure = DefaultWorkoutGenerator(everythingTagged).generate(
            request(20.minutes, WorkoutStyle.RECOVERY).copy(
                modalities = setOf(Modality.BODYWEIGHT),
                avoidTags = setOf("lower_back"),
            ),
        ).failure()
        assertTrue(failure is GenerationFailure.NoEligibleExercises)
    }

    @Test
    fun `a level with no content beneath it fails rather than serving harder work`() {
        val advancedOnly = listOf(
            exercise("bodyweight_hard", 3.8, difficulty = ExperienceLevel.ADVANCED),
        )
        val failure = DefaultWorkoutGenerator(advancedOnly).generate(
            request(20.minutes, WorkoutStyle.RECOVERY).copy(
                modalities = setOf(Modality.BODYWEIGHT),
                level = ExperienceLevel.BEGINNER,
            ),
        ).failure()
        assertTrue(failure is GenerationFailure.NoEligibleExercises)
        assertEquals(ExperienceLevel.BEGINNER, (failure as GenerationFailure.NoEligibleExercises).level)
    }

    // --- InsufficientVariety ------------------------------------------------------

    /**
     * A single-exercise catalogue can fill a short session honestly, but not a warm-up
     * that wants three distinct movements. Repeating one movement three times is a content
     * gap, and the engine says so rather than shipping the repetition.
     */
    @Test
    fun `a one-exercise catalogue fails when a block needs three distinct movements`() {
        val single = listOf(exercise("bodyweight_only_one", 2.8))
        val request = request(THREE_SEGMENT_WARM_UP, WorkoutStyle.ZONE_2)
            .copy(modalities = setOf(Modality.BODYWEIGHT))
        val failure = DefaultWorkoutGenerator(single).generate(request).failure()
        assertTrue("expected InsufficientVariety, got $failure", failure is GenerationFailure.InsufficientVariety)
        failure as GenerationFailure.InsufficientVariety
        assertEquals(1, failure.available)
    }

    // --- Honesty ------------------------------------------------------------------

    /**
     * Spec §7 and REQ-004. Reformer work must never be titled as an interval or fat-loss
     * session — `wang2021pilates` found no waist-circumference effect — and the style
     * recorded must be what was built, not what was asked for.
     */
    @Test
    fun `a reformer-only interval request is built and recorded as strength work`() {
        val workout = generatorFor(Modality.REFORMER_PILATES)
            .generate(
                request(30.minutes, WorkoutStyle.HIIT).copy(modalities = setOf(Modality.REFORMER_PILATES)),
            ).getOrThrow()

        assertEquals("Reformer Pilates: strength and control — 30 min", workout.title)
        assertEquals(WorkoutStyle.RECOVERY, workout.style)
        FORBIDDEN_IN_STRENGTH_TITLES.forEach { forbidden ->
            assertTrue(
                "a strength session was titled \"${workout.title}\"",
                !workout.title.contains(forbidden, ignoreCase = true),
            )
        }
        assertTrue(
            "the substitution was not explained to the user: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("strength and mobility", ignoreCase = true) },
        )
        // §7.3: contributes zero vigorous minutes.
        assertTrue(
            "a reformer session prescribed vigorous work",
            workout.segments.none { it.intensity == IntensityTarget.VIGOROUS },
        )
    }

    /**
     * The other half of D-0039, and the reason the rename mattered. Bodyweight work is
     * aerobic-capable, so a user with **no equipment at all** gets a genuine interval
     * session rather than being told to do an easy mat class.
     *
     * Before the rename this same request produced "Floor Pilates: strength and control",
     * because the category was labelled Pilates and the Pilates constraint caught it.
     */
    @Test
    fun `a bodyweight-only interval request produces genuine intervals`() {
        val workout = generatorFor(Modality.BODYWEIGHT)
            .generate(
                request(30.minutes, WorkoutStyle.HIIT).copy(modalities = setOf(Modality.BODYWEIGHT)),
            ).getOrThrow()

        assertEquals("Intervals — 30 min", workout.title)
        assertEquals(WorkoutStyle.HIIT, workout.style)
        assertTrue(
            "a bodyweight interval session prescribed no vigorous work at all",
            workout.segments.any { it.intensity == IntensityTarget.VIGOROUS },
        )
        assertTrue(
            "the work intervals are not on bodyweight movements",
            workout.segments
                .filter { it.intensity == IntensityTarget.VIGOROUS }
                .all { it.exercise?.modality == Modality.BODYWEIGHT },
        )
        // Nothing was *substituted*. A note about extending the template to fill the
        // duration is a description of the plan, not an apology for it.
        assertTrue(
            "an interval session that needed no substitution explained one: ${workout.buildNotes}",
            workout.buildNotes.none { it.contains("built as") || it.contains("capped") },
        )
    }

    /**
     * General core work is not a Pilates method claim (D-0039).
     *
     * The catalogue here is the bodyweight modality's low-intensity content only —
     * everything anchored at recovery — which is the second way a session becomes
     * strength-only: the modality *could* carry aerobic work, but nothing available on it
     * does. A set of dead bugs and planks is still not Pilates.
     */
    @Test
    fun `a low-intensity bodyweight session is strength work, and is not called Pilates`() {
        val coreOnly = CatalogueFixture.forModalities(Modality.BODYWEIGHT)
            .filter { it.metValue <= MAT_MAX_MET }
        val workout = DefaultWorkoutGenerator(coreOnly)
            .generate(
                request(20.minutes, WorkoutStyle.RECOVERY).copy(modalities = setOf(Modality.BODYWEIGHT)),
            ).getOrThrow()
        assertEquals("Bodyweight: strength and control — 20 min", workout.title)
        assertTrue(
            "general core work was called Pilates",
            !workout.title.contains("Pilates", ignoreCase = true),
        )
    }

    /**
     * The other side of D-0042: a mat session **is** Pilates and is named as such, and being
     * named Pilates is what puts it under the `wang2021pilates` constraint. Mat Pilates is
     * not aerobic-capable, so this is strength work however the request was phrased.
     */
    @Test
    fun `a mat Pilates interval request is built and recorded as Pilates strength work`() {
        val workout = generatorFor(Modality.MAT_PILATES)
            .generate(
                request(30.minutes, WorkoutStyle.HIIT).copy(modalities = setOf(Modality.MAT_PILATES)),
            ).getOrThrow()

        assertEquals("Mat Pilates: strength and control — 30 min", workout.title)
        assertEquals(WorkoutStyle.RECOVERY, workout.style)
        FORBIDDEN_IN_STRENGTH_TITLES.forEach { forbidden ->
            assertTrue(
                "a strength session was titled \"${workout.title}\"",
                !workout.title.contains(forbidden, ignoreCase = true),
            )
        }
        assertTrue(
            "a mat Pilates session prescribed vigorous work",
            workout.segments.none { it.intensity == IntensityTarget.VIGOROUS },
        )
    }

    /** A session spanning both Pilates modalities is named for the method, not for one apparatus. */
    @Test
    fun `a session across both Pilates modalities is titled Pilates`() {
        val bothPilates = setOf(Modality.MAT_PILATES, Modality.REFORMER_PILATES)
        val workout = DefaultWorkoutGenerator(CatalogueFixture.forModalities(*bothPilates.toTypedArray()))
            .generate(request(30.minutes, WorkoutStyle.MIXED).copy(modalities = bothPilates))
            .getOrThrow()

        assertEquals("Pilates: strength and control — 30 min", workout.title)
    }

    /**
     * Spec §3: when a fallback caps the intensity, the title must say so. The case is
     * reached by excluding the only vigorous-anchored elliptical work a beginner can do,
     * which leaves the steady pool as the hardest thing available.
     */
    @Test
    fun `capping the intensity downgrades the title and explains why`() {
        val workout = DefaultWorkoutGenerator(CatalogueFixture.forModalities(Modality.ELLIPTICAL))
            .generate(
                request(20.minutes, WorkoutStyle.HIIT).copy(
                    modalities = setOf(Modality.ELLIPTICAL),
                    level = ExperienceLevel.BEGINNER,
                    avoidTags = setOf("cardiac_caution"),
                ),
            ).getOrThrow()

        assertEquals("Intervals (steady) — 20 min", workout.title)
        assertTrue(
            "the cap was not explained: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("capped", ignoreCase = true) },
        )
        assertTrue(
            "a capped session still prescribed vigorous work",
            workout.segments.none { it.intensity == IntensityTarget.VIGOROUS },
        )
    }

    /** A style substitution is a bigger change than a cap, and gets said out loud too. */
    @Test
    fun `a style the duration cannot hold is substituted only with an explanation`() {
        val workout = generator.generate(request(10.minutes, WorkoutStyle.MIXED)).getOrThrow()
        assertEquals(WorkoutStyle.ZONE_2, workout.style)
        assertEquals("Steady — 10 min", workout.title)
        assertTrue(
            "the substitution was not explained: ${workout.buildNotes}",
            workout.buildNotes.any { it.contains("built as", ignoreCase = true) },
        )
    }

    @Test
    fun `generation never throws, whatever it is handed`() {
        val hostile = listOf(
            request(Duration.ZERO, WorkoutStyle.HIIT),
            request((-5).minutes, WorkoutStyle.MIXED),
            request(20.minutes, WorkoutStyle.HIIT).copy(modalities = emptySet()),
            request(20.minutes, WorkoutStyle.HIIT).copy(avoidTags = ALL_CAUTION_TAGS),
            request(20.minutes, WorkoutStyle.HIIT).copy(seed = Long.MIN_VALUE),
        )
        hostile.forEach { request ->
            val result = DefaultWorkoutGenerator(CatalogueFixture.ALL).generate(request)
            result.onFailure { assertTrue("$request threw $it", it is GenerationFailure) }
        }
        assertTrue(DefaultWorkoutGenerator(emptyList()).generate(request(20.minutes, WorkoutStyle.MIXED)).isFailure)
    }

    private fun generatorFor(vararg modalities: Modality) =
        DefaultWorkoutGenerator(CatalogueFixture.forModalities(*modalities))

    private fun request(duration: Duration, style: WorkoutStyle) = WorkoutRequest(
        duration = duration,
        style = style,
        modalities = Modality.entries.toSet(),
        level = ExperienceLevel.ADVANCED,
        seed = 7L,
    )

    private fun exercise(
        id: String,
        metValue: Double,
        difficulty: ExperienceLevel = ExperienceLevel.BEGINNER,
        cautionTags: Set<String> = emptySet(),
    ) = Exercise(
        id = id,
        name = id,
        modality = Modality.BODYWEIGHT,
        difficulty = difficulty,
        metValue = metValue,
        howTo = listOf("A test fixture step."),
        spokenInstruction = id,
        safetyNotes = listOf("A test fixture note."),
        commonMistakes = listOf("A test fixture mistake."),
        musclesWorked = listOf("Test"),
        illustrationId = id,
        cautionTags = cautionTags,
    )

    private fun <T> Result<T>.failure(): Throwable =
        checkNotNull(exceptionOrNull()) { "expected a failure but got ${getOrNull()}" }

    private companion object {
        /**
         * Long enough that the warm-up wants three segments: Zone 2 takes 12% of the total,
         * and three segments need at least 270 s of warm-up.
         */
        val THREE_SEGMENT_WARM_UP = 2_250.seconds

        val FORBIDDEN_IN_STRENGTH_TITLES = listOf("HIIT", "Intervals", "fat", "burn")

        /**
         * Mat and mobility work only. Everything at or below this is recovery-anchored, so
         * nothing in the resulting catalogue can carry aerobic work — which is the condition
         * `isStrengthOnly` is really about.
         */
        const val MAT_MAX_MET = 3.5

        val ALL_CAUTION_TAGS = setOf(
            "lower_back", "neck", "shoulder", "wrist", "knee",
            "hip", "ankle", "pregnancy", "cardiac_caution", "balance",
        )
    }
}

package com.visceralfit.app

import com.visceralfit.data.seed.ExerciseCatalogue
import com.visceralfit.data.seed.SeedExercise
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.Modality
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The quality gate for the exercise catalogue (KI-0006,
 * `framework/08_exercise_library_spec.md` §5).
 *
 * The catalogue is health guidance, not decoration: a blank safety note or a missing
 * stop-if-symptoms warning on a vigorous interval has a physical consequence. Before
 * this test the asset was only validated at runtime, where a malformed field was logged
 * and the app carried on with an empty exercise list.
 *
 * The asset is wired in as a test resource by `app/build.gradle.kts`, so this runs as a
 * plain JVM test with no device and no Robolectric.
 */
class ExerciseCatalogueValidationTest {

    private val catalogue: ExerciseCatalogue = run {
        val raw = checkNotNull(javaClass.getResourceAsStream("/exercises_seed.json")) {
            "exercises_seed.json is not on the test classpath — check the test resource wiring"
        }.bufferedReader().use { it.readText() }
        // Exactly the parser the app uses, including ignoreUnknownKeys = false, so a
        // typo'd field name fails here rather than at first launch.
        Json { ignoreUnknownKeys = false }.decodeFromString(ExerciseCatalogue.serializer(), raw)
    }

    private val exercises: List<SeedExercise> get() = catalogue.exercises

    @Test
    fun `ids are unique, snake case and modality prefixed`() {
        val duplicates = exercises.map { it.id }.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertEquals("duplicate ids", emptyMap<String, Int>(), duplicates)
        val malformed = exercises.map { it.id }.filterNot { ID_PATTERN.matches(it) }
        assertEquals("ids must match $ID_PATTERN", emptyList<String>(), malformed)
    }

    @Test
    fun `every modality and difficulty resolves to a known enum id`() {
        val unknownModalities = exercises.filter { Modality.fromId(it.modality) == null }.map { it.id }
        assertEquals(emptyList<String>(), unknownModalities)
        val unknownLevels = exercises.filter { ExperienceLevel.fromId(it.difficulty) == null }.map { it.id }
        assertEquals(emptyList<String>(), unknownLevels)
    }

    @Test
    fun `every met value is positive and plausible`() {
        val implausible = exercises.filterNot { it.metValue > 0.0 && it.metValue <= MAX_PLAUSIBLE_MET }
        assertEquals(emptyList<SeedExercise>(), implausible)
    }

    @Test
    fun `instructions are complete enough to perform without a video`() {
        val tooFew = exercises.filter { it.howTo.size < MIN_HOW_TO_STEPS }.map { it.id }
        assertEquals("how_to needs at least $MIN_HOW_TO_STEPS steps", emptyList<String>(), tooFew)
        val tooMany = exercises.filter { it.howTo.size > MAX_HOW_TO_STEPS }.map { it.id }
        assertEquals("over $MAX_HOW_TO_STEPS steps is probably two exercises", emptyList<String>(), tooMany)
        val blank = exercises.filter { exercise -> exercise.howTo.any { it.isBlank() } }.map { it.id }
        assertEquals(emptyList<String>(), blank)
    }

    /**
     * A spoken cue must finish inside the shortest interval its exercise can appear in
     * (REQ-044). Anything at 8.0 MET or above can land in a 30-second HIIT interval, so
     * it gets the tighter limit.
     */
    @Test
    fun `spoken cues fit the interval they can appear in`() {
        val overLimit = exercises.mapNotNull { exercise ->
            val limit = if (exercise.metValue >= VIGOROUS_MET) VIGOROUS_CUE_WORDS else STANDARD_CUE_WORDS
            val words = exercise.spokenInstruction.trim().split(Regex("\\s+")).size
            if (words > limit) "${exercise.id} ($words words, limit $limit)" else null
        }
        assertEquals(emptyList<String>(), overLimit)
        val blank = exercises.filter { it.spokenInstruction.isBlank() }.map { it.id }
        assertEquals(emptyList<String>(), blank)
    }

    @Test
    fun `safety notes and common mistakes are present and specific`() {
        val missing = exercises
            .filter { it.safetyNotes.isEmpty() || it.commonMistakes.isEmpty() }
            .map { it.id }
        assertEquals(emptyList<String>(), missing)
        val blank = exercises
            .filter { e -> (e.safetyNotes + e.commonMistakes).any { it.isBlank() } }
            .map { it.id }
        assertEquals(emptyList<String>(), blank)
    }

    /** REQ-006. The one category where an omission has a physical consequence. */
    @Test
    fun `vigorous exercises carry a stop if symptoms note`() {
        val missing = exercises
            .filter { it.metValue >= VIGOROUS_MET }
            .filterNot { exercise -> exercise.safetyNotes.any { SYMPTOM_PATTERN.containsMatchIn(it) } }
            .map { it.id }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `muscles worked names between two and five movers`() {
        val wrong = exercises
            .filterNot { it.musclesWorked.size in MIN_MUSCLES..MAX_MUSCLES }
            .map { "${it.id} (${it.musclesWorked.size})" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `caution tags come from the closed set the settings UI enumerates`() {
        val invented = exercises.flatMap { it.cautionTags }.toSortedSet() - KNOWN_CAUTION_TAGS
        assertEquals(emptySet<String>(), invented)
    }

    /**
     * Parsed out of `references.md` rather than hard-coded, so adding a citation to the
     * catalogue without adding the paper fails.
     */
    @Test
    fun `every evidence key exists in references md`() {
        val known = REFERENCE_KEY_PATTERN
            .findAll(referencesText())
            .map { it.groupValues[1] }
            .toSet()
        assertTrue("references.md yielded no citation keys", known.isNotEmpty())
        val unknown = exercises.flatMap { it.evidenceKeys }.toSortedSet() - known
        assertEquals(emptySet<String>(), unknown)
    }

    /**
     * The claims the evidence does not support (`framework/08_exercise_library_spec.md`
     * §3). Spot reduction is the important one: no exercise targets abdominal fat, so no
     * exercise may say it does.
     */
    @Test
    fun `no prohibited health claim appears anywhere in the content`() {
        val offenders = exercises.mapNotNull { exercise ->
            val prose = (
                exercise.howTo + exercise.safetyNotes + exercise.commonMistakes +
                    listOf(exercise.spokenInstruction, exercise.name)
                ).joinToString(" ")
            PROHIBITED_CLAIMS.firstOrNull { it.containsMatchIn(prose) }?.let { "${exercise.id}: $it" }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    /** Time-based prescriptions and rep counts fight each other; the app is time-based. */
    @Test
    fun `how to steps do not prescribe rep or set counts`() {
        val offenders = exercises
            .filter { exercise -> exercise.howTo.any { REP_COUNT_PATTERN.containsMatchIn(it) } }
            .map { it.id }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `per modality minimum counts are met`() {
        MODALITY_MINIMUMS.forEach { (modality, minimum) ->
            val found = exercises.count { it.modality == modality.id }
            assertTrue("$modality has $found exercises, needs $minimum", found >= minimum)
        }
    }

    @Test
    fun `every modality has at least three exercises at every level`() {
        MODALITY_MINIMUMS.keys.forEach { modality ->
            ExperienceLevel.entries.forEach { level ->
                val found = exercises.count { it.modality == modality.id && it.difficulty == level.id }
                assertTrue(
                    "$modality has $found ${level.id} exercises, needs $MIN_PER_LEVEL",
                    found >= MIN_PER_LEVEL,
                )
            }
        }
    }

    /** A user with several exclusions must still get a workable session. */
    @Test
    fun `every modality has at least four exercises with no caution tags`() {
        MODALITY_MINIMUMS.keys.forEach { modality ->
            val found = exercises.count { it.modality == modality.id && it.cautionTags.isEmpty() }
            assertTrue("$modality has only $found untagged exercises", found >= MIN_UNTAGGED_PER_MODALITY)
        }
    }

    /** Without these the generator falls through its pool fallback chain on every session. */
    @Test
    fun `the generator's pools are all non empty by a safe margin`() {
        val mobility = exercises.count { it.metValue <= MOBILITY_MAX_MET }
        assertTrue("mobility pool is $mobility", mobility >= MIN_POOL_SIZE)
        val warmUp = exercises.count { it.metValue <= WARM_UP_MAX_MET && it.modality.endsWith("pilates") }
        assertTrue("warm-up pool is $warmUp", warmUp >= MIN_POOL_SIZE)
        val vigorous = exercises.count { exercise ->
            exercise.metValue >= VIGOROUS_MET && Modality.fromId(exercise.modality)?.isMachineCardio == true
        }
        assertTrue("vigorous pool is $vigorous", vigorous >= MIN_VIGOROUS_POOL_SIZE)
    }

    private fun referencesText(): String {
        val candidates = listOf(
            java.io.File("../framework/data/references.md"),
            java.io.File("framework/data/references.md"),
        )
        val file = candidates.firstOrNull { it.exists() }
        checkNotNull(file) { "references.md not found relative to ${java.io.File("").absolutePath}" }
        return file.readText()
    }

    private companion object {
        val ID_PATTERN = Regex("^(bodyweight|reformer|elliptical|spin_bike|warmup|cooldown)_[a-z0-9_]+$")
        val SYMPTOM_PATTERN = Regex("chest pain|dizz|breathless|symptom", RegexOption.IGNORE_CASE)
        val REFERENCE_KEY_PATTERN = Regex("^### `([a-z0-9]+)`", RegexOption.MULTILINE)
        val REP_COUNT_PATTERN = Regex(
            "\\b(repeat|reps?|sets?)\\b\\s*(of\\s*)?\\d|\\d+\\s*(reps?|times)\\b",
            RegexOption.IGNORE_CASE,
        )

        val PROHIBITED_CLAIMS = listOf(
            Regex("belly fat", RegexOption.IGNORE_CASE),
            Regex("visceral fat", RegexOption.IGNORE_CASE),
            Regex("spot[- ]reduc", RegexOption.IGNORE_CASE),
            Regex("\\bmelts?\\b", RegexOption.IGNORE_CASE),
            Regex("\\btorch(es|ing)?\\b", RegexOption.IGNORE_CASE),
            Regex("burns? (body )?fat", RegexOption.IGNORE_CASE),
            Regex("everyone can", RegexOption.IGNORE_CASE),
            Regex("(fixes|cures) your", RegexOption.IGNORE_CASE),
        )

        val KNOWN_CAUTION_TAGS = setOf(
            "lower_back", "neck", "shoulder", "wrist", "knee",
            "hip", "ankle", "pregnancy", "cardiac_caution", "balance",
        )

        val MODALITY_MINIMUMS = mapOf(
            Modality.BODYWEIGHT to 24,
            Modality.REFORMER_PILATES to 10,
            Modality.ELLIPTICAL to 8,
            Modality.SPIN_BIKE to 12,
        )

        const val MIN_PER_LEVEL = 3
        const val MIN_UNTAGGED_PER_MODALITY = 4
        const val MIN_POOL_SIZE = 6
        const val MIN_VIGOROUS_POOL_SIZE = 4
        const val MIN_HOW_TO_STEPS = 3
        const val MAX_HOW_TO_STEPS = 8
        const val MIN_MUSCLES = 2
        const val MAX_MUSCLES = 5
        const val MAX_PLAUSIBLE_MET = 20.0
        const val VIGOROUS_MET = 8.0
        const val MOBILITY_MAX_MET = 2.5
        const val WARM_UP_MAX_MET = 4.0
        const val VIGOROUS_CUE_WORDS = 14
        const val STANDARD_CUE_WORDS = 20
    }
}

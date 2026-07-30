package com.visceralfit.data.seed

import android.content.Context
import com.visceralfit.core.common.IoDispatcher
import com.visceralfit.core.database.dao.ExerciseDao
import com.visceralfit.core.database.entity.ExerciseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the bundled exercise catalogue into Room.
 *
 * WHY A JSON ASSET AND NOT A PREPACKAGED .db (ADR-0005): the JSON file is
 * reviewable in a pull request — a reviewer can see that a safety note changed. A
 * binary database cannot be diffed, which matters when the content is health
 * guidance rather than decoration.
 *
 * IDEMPOTENCE: the seeder runs on every cold start and is a no-op unless the
 * bundled [ExerciseCatalogue.version] is higher than the lowest `seed_version` in
 * the table. Rows are upserted by id, so a content correction reaches existing
 * installs on the next launch without touching the user's history.
 *
 * FAILURE POLICY: a malformed asset is a build defect, not a runtime condition. The
 * seeder rethrows so it is caught by the instrumentation test in phase 07 rather
 * than shipping an app with a silently empty exercise list.
 */
@Singleton
class ExerciseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ExerciseDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = Json {
        ignoreUnknownKeys = false
        prettyPrint = false
    }

    suspend fun seedIfNeeded(): SeedOutcome = withContext(ioDispatcher) {
        val catalogue = readCatalogue()
        val existingCount = dao.count()
        val lowestVersion = dao.lowestSeedVersion()

        val needsSeed = existingCount == 0 ||
            lowestVersion == null ||
            lowestVersion < catalogue.version
        if (!needsSeed) return@withContext SeedOutcome.AlreadyCurrent(existingCount)

        val entities = catalogue.exercises.map { it.toEntity(catalogue.version) }
        dao.upsertAll(entities)
        // Drop exercises that were withdrawn from the catalogue — for instance a
        // movement removed because the evidence for it did not hold up.
        dao.deleteMissingFrom(entities.map { it.id })
        SeedOutcome.Seeded(entities.size, catalogue.version)
    }

    private fun readCatalogue(): ExerciseCatalogue {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return json.decodeFromString(ExerciseCatalogue.serializer(), raw)
    }

    private companion object {
        const val ASSET_NAME = "exercises_seed.json"
    }
}

sealed interface SeedOutcome {
    data class Seeded(val count: Int, val version: Int) : SeedOutcome
    data class AlreadyCurrent(val count: Int) : SeedOutcome
}

/**
 * Wire format for `app/src/main/assets/exercises_seed.json`.
 *
 * `ignoreUnknownKeys = false` is deliberate: a typo'd field name in the catalogue
 * should fail loudly at seed time rather than silently produce an exercise with no
 * safety notes.
 */
@Serializable
data class ExerciseCatalogue(
    val version: Int,
    val exercises: List<SeedExercise>,
)

@Serializable
data class SeedExercise(
    val id: String,
    val name: String,
    val modality: String,
    val difficulty: String,
    @SerialName("met_value") val metValue: Double,
    @SerialName("how_to") val howTo: List<String>,
    @SerialName("spoken_instruction") val spokenInstruction: String,
    @SerialName("safety_notes") val safetyNotes: List<String>,
    @SerialName("common_mistakes") val commonMistakes: List<String>,
    @SerialName("muscles_worked") val musclesWorked: List<String>,
    @SerialName("illustration_id") val illustrationId: String,
    @SerialName("caution_tags") val cautionTags: List<String> = emptyList(),
    @SerialName("is_per_side") val isPerSide: Boolean = false,
    @SerialName("evidence_keys") val evidenceKeys: List<String> = emptyList(),
) {
    fun toEntity(seedVersion: Int): ExerciseEntity = ExerciseEntity(
        id = id,
        name = name,
        modality = modality,
        difficulty = difficulty,
        metValue = metValue,
        howTo = howTo,
        spokenInstruction = spokenInstruction,
        safetyNotes = safetyNotes,
        commonMistakes = commonMistakes,
        musclesWorked = musclesWorked,
        illustrationId = illustrationId,
        cautionTags = cautionTags,
        isPerSide = isPerSide,
        evidenceKeys = evidenceKeys,
        seedVersion = seedVersion,
    )
}

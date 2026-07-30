package com.visceralfit.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entities. The authoritative schema, including the reasoning behind each
 * index, is /framework/06_data_model.md — keep the two in step.
 *
 * Conventions enforced by review:
 *  - Enums are stored as their stable string `id`, never as an ordinal.
 *  - Instants are stored as epoch milliseconds (INTEGER) in UTC.
 *  - Sets and lists are stored as newline-delimited text via the converters in
 *    [com.visceralfit.core.database.Converters]; no JSON blobs in queryable columns.
 */
@Entity(
    tableName = "exercises",
    indices = [Index("modality"), Index("difficulty")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** [com.visceralfit.domain.model.Modality.id] */
    val modality: String,
    /** [com.visceralfit.domain.model.ExperienceLevel.id] */
    val difficulty: String,
    @ColumnInfo(name = "met_value") val metValue: Double,
    @ColumnInfo(name = "how_to") val howTo: List<String>,
    @ColumnInfo(name = "spoken_instruction") val spokenInstruction: String,
    @ColumnInfo(name = "safety_notes") val safetyNotes: List<String>,
    @ColumnInfo(name = "common_mistakes") val commonMistakes: List<String>,
    @ColumnInfo(name = "muscles_worked") val musclesWorked: List<String>,
    @ColumnInfo(name = "illustration_id") val illustrationId: String,
    @ColumnInfo(name = "caution_tags") val cautionTags: List<String>,
    @ColumnInfo(name = "is_per_side") val isPerSide: Boolean,
    @ColumnInfo(name = "evidence_keys") val evidenceKeys: List<String>,
    /**
     * Version of the seed bundle this row came from. The seeder replaces rows whose
     * version is older than the bundled catalogue, which is how content updates ship
     * without a destructive migration.
     */
    @ColumnInfo(name = "seed_version") val seedVersion: Int,
)

@Entity(
    tableName = "sessions",
    indices = [Index("started_at_epoch_ms"), Index("style")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    val title: String,
    /** [com.visceralfit.domain.model.WorkoutStyle.id] */
    val style: String,
    /** Modality ids, newline-delimited. */
    val modalities: List<String>,
    @ColumnInfo(name = "started_at_epoch_ms") val startedAtEpochMs: Long,
    @ColumnInfo(name = "completed_at_epoch_ms") val completedAtEpochMs: Long,
    /** Excludes paused time, so it reflects work done rather than wall-clock. */
    @ColumnInfo(name = "active_seconds") val activeSeconds: Long,
    /** Null when body mass was unknown at the time; never back-filled retroactively. */
    @ColumnInfo(name = "estimated_kcal") val estimatedKcal: Int?,
    @ColumnInfo(name = "perceived_exertion") val perceivedExertion: Int?,
    @ColumnInfo(name = "completion_ratio") val completionRatio: Float,
    val note: String?,
)

@Entity(
    tableName = "measurements",
    indices = [Index(value = ["kind", "recorded_on_epoch_day"], unique = true)],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.visceralfit.domain.model.MeasurementKind.id] */
    val kind: String,
    /** Local date as an epoch day, so "today" never shifts with the device timezone. */
    @ColumnInfo(name = "recorded_on_epoch_day") val recordedOnEpochDay: Long,
    val value: Double,
    val unit: String,
)

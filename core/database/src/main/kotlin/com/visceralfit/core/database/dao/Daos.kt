package com.visceralfit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.visceralfit.core.database.entity.ExerciseEntity
import com.visceralfit.core.database.entity.MeasurementEntity
import com.visceralfit.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE modality IN (:modalityIds)")
    suspend fun getForModalities(modalityIds: List<String>): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT MIN(seed_version) FROM exercises")
    suspend fun lowestSeedVersion(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE id NOT IN (:keepIds)")
    suspend fun deleteMissingFrom(keepIds: List<String>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY started_at_epoch_ms DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT * FROM sessions
        WHERE started_at_epoch_ms >= :fromEpochMs AND started_at_epoch_ms < :toEpochMs
        ORDER BY started_at_epoch_ms DESC
        """,
    )
    fun observeBetween(fromEpochMs: Long, toEpochMs: Long): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(active_seconds), 0) FROM sessions
        WHERE started_at_epoch_ms >= :fromEpochMs
          AND started_at_epoch_ms < :toEpochMs
          AND completion_ratio >= :minimumCompletion
        """,
    )
    fun observeActiveSecondsBetween(
        fromEpochMs: Long,
        toEpochMs: Long,
        minimumCompletion: Float,
    ): Flow<Long>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY recorded_on_epoch_day DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE kind = :kind ORDER BY recorded_on_epoch_day DESC LIMIT 1")
    suspend fun latestOfKind(kind: String): MeasurementEntity?

    /** Upsert so that re-recording the same measurement on the same day corrects it. */
    @Upsert
    suspend fun upsert(measurement: MeasurementEntity): Long

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: Long)
}

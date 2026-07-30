package com.visceralfit.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.visceralfit.core.database.dao.ExerciseDao
import com.visceralfit.core.database.dao.MeasurementDao
import com.visceralfit.core.database.dao.SessionDao
import com.visceralfit.core.database.entity.ExerciseEntity
import com.visceralfit.core.database.entity.MeasurementEntity
import com.visceralfit.core.database.entity.SessionEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The single Room database.
 *
 * MIGRATION POLICY (binding on the building agent):
 *  - `fallbackToDestructiveMigration` is never enabled. A user's training history is
 *    the one thing in this app that cannot be regenerated, and wiping it on an
 *    upgrade would be unrecoverable — there is no cloud copy by design.
 *  - Every schema change bumps [version] and adds a `Migration` with a test that
 *    opens the previous schema JSON and migrates it. See
 *    /framework/06_data_model.md §Migrations.
 *  - Exercise content changes are NOT schema changes: they ship as a higher
 *    `seed_version` and are applied by the seeder.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        SessionEntity::class,
        MeasurementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VisceralFitDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        const val NAME = "visceralfit.db"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): VisceralFitDatabase =
        Room.databaseBuilder(context, VisceralFitDatabase::class.java, VisceralFitDatabase.NAME)
            .build()

    @Provides
    fun exerciseDao(database: VisceralFitDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun sessionDao(database: VisceralFitDatabase): SessionDao = database.sessionDao()

    @Provides
    fun measurementDao(database: VisceralFitDatabase): MeasurementDao = database.measurementDao()
}

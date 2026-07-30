package com.visceralfit.app

import android.app.Application
import android.util.Log
import com.visceralfit.core.common.ApplicationScope
import com.visceralfit.data.seed.ExerciseSeeder
import com.visceralfit.data.seed.SeedOutcome
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VisceralFitApplication : Application() {

    @Inject
    lateinit var exerciseSeeder: ExerciseSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        seedExerciseCatalogue()
    }

    /**
     * Seeding runs off the main thread and is not awaited: the home screen renders
     * its loading state from the exercise Flow, so a slow first seed delays content
     * rather than the window. Blocking `onCreate` here would put file I/O and a
     * database write directly into cold-start time.
     */
    private fun seedExerciseCatalogue() {
        applicationScope.launch {
            runCatching { exerciseSeeder.seedIfNeeded() }
                .onSuccess { outcome ->
                    when (outcome) {
                        is SeedOutcome.Seeded ->
                            Log.i(TAG, "Seeded ${outcome.count} exercises at v${outcome.version}")
                        is SeedOutcome.AlreadyCurrent ->
                            Log.d(TAG, "Exercise catalogue current (${outcome.count} rows)")
                    }
                }
                .onFailure { error ->
                    // A malformed asset is a build defect. Log loudly; the phase-07
                    // instrumentation test asserts a non-empty catalogue so this
                    // cannot reach a release build unnoticed.
                    Log.e(TAG, "Exercise seeding failed — catalogue asset is invalid", error)
                }
        }
    }

    private companion object {
        const val TAG = "VisceralFit"
    }
}

package com.visceralfit.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.visceralfit.domain.model.BodyPreferences
import com.visceralfit.domain.model.CoachingPreferences
import com.visceralfit.domain.model.DisplayPreferences
import com.visceralfit.domain.model.ExperienceLevel
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.ThemePreference
import com.visceralfit.domain.model.UnitSystem
import com.visceralfit.domain.model.UserPreferences
import com.visceralfit.domain.model.WorkoutStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Reads and writes [UserPreferences].
 *
 * Every read is defensive: a key that is absent, or holds a value that no longer
 * maps to a known enum id, falls back to the model default rather than throwing.
 * A settings file written by a future version of the app must never brick a
 * downgrade, and an unknown modality id must never crash the workout screen.
 */
@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.preferencesStore

    fun observe(): Flow<UserPreferences> = store.data.map { it.toUserPreferences() }

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { mutable ->
            val updated = transform(mutable.toUserPreferences())
            mutable.write(updated)
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            enabledModalities = this[Keys.EnabledModalities]
                ?.mapNotNull(Modality::fromId)
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: defaults.enabledModalities,
            level = this[Keys.Level]?.let(ExperienceLevel::fromId) ?: defaults.level,
            defaultStyle = this[Keys.DefaultStyle]?.let(WorkoutStyle::fromId) ?: defaults.defaultStyle,
            defaultDuration = this[Keys.DefaultDurationSeconds]?.seconds ?: defaults.defaultDuration,
            weeklyMinutesGoal = this[Keys.WeeklyMinutesGoal] ?: defaults.weeklyMinutesGoal,
            safetyNoticeAcknowledged = this[Keys.SafetyAcknowledged] ?: defaults.safetyNoticeAcknowledged,
            coaching = CoachingPreferences(
                speechEnabled = this[Keys.SpeechEnabled] ?: defaults.coaching.speechEnabled,
                announceNextExercise = this[Keys.AnnounceNext] ?: defaults.coaching.announceNextExercise,
                announceCountdown = this[Keys.AnnounceCountdown] ?: defaults.coaching.announceCountdown,
                announceHalfway = this[Keys.AnnounceHalfway] ?: defaults.coaching.announceHalfway,
                announceRemainingTime = this[Keys.AnnounceRemaining] ?: defaults.coaching.announceRemainingTime,
                motivationalPrompts = this[Keys.Motivational] ?: defaults.coaching.motivationalPrompts,
                announceRestCountdown = this[Keys.AnnounceRest] ?: defaults.coaching.announceRestCountdown,
                speakFullInstructions = this[Keys.SpeakFullInstructions]
                    ?: defaults.coaching.speakFullInstructions,
                speechRate = this[Keys.SpeechRate]?.coerceRate() ?: defaults.coaching.speechRate,
                speechPitch = this[Keys.SpeechPitch]?.coerceRate() ?: defaults.coaching.speechPitch,
                cueTones = this[Keys.CueTones] ?: defaults.coaching.cueTones,
                hapticCues = this[Keys.HapticCues] ?: defaults.coaching.hapticCues,
            ),
            display = DisplayPreferences(
                keepScreenOn = this[Keys.KeepScreenOn] ?: defaults.display.keepScreenOn,
                theme = this[Keys.Theme]?.let { id -> ThemePreference.entries.firstOrNull { it.id == id } }
                    ?: defaults.display.theme,
                amoledDarkMode = this[Keys.Amoled] ?: defaults.display.amoledDarkMode,
                dynamicColour = this[Keys.DynamicColour] ?: defaults.display.dynamicColour,
                machineMode = this[Keys.MachineMode] ?: defaults.display.machineMode,
            ),
            body = BodyPreferences(
                availableEquipment = this[Keys.AvailableEquipment]
                    ?.mapNotNull(Modality::fromId)
                    ?.toSet()
                    ?: defaults.body.availableEquipment,
                bodyMassKg = this[Keys.BodyMassKg]?.toDouble()?.takeIf { it > 0.0 },
                ageYears = this[Keys.AgeYears]?.takeIf { it in MIN_AGE..MAX_AGE },
                units = this[Keys.Units]?.let { id -> UnitSystem.entries.firstOrNull { it.id == id } }
                    ?: defaults.body.units,
                avoidTags = this[Keys.AvoidTags] ?: defaults.body.avoidTags,
            ),
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(prefs: UserPreferences) {
        this[Keys.EnabledModalities] = prefs.enabledModalities.mapTo(mutableSetOf()) { it.id }
        this[Keys.Level] = prefs.level.id
        this[Keys.DefaultStyle] = prefs.defaultStyle.id
        this[Keys.DefaultDurationSeconds] = prefs.defaultDuration.inWholeSeconds.toInt()
        this[Keys.WeeklyMinutesGoal] = prefs.weeklyMinutesGoal
        this[Keys.SafetyAcknowledged] = prefs.safetyNoticeAcknowledged

        this[Keys.SpeechEnabled] = prefs.coaching.speechEnabled
        this[Keys.AnnounceNext] = prefs.coaching.announceNextExercise
        this[Keys.AnnounceCountdown] = prefs.coaching.announceCountdown
        this[Keys.AnnounceHalfway] = prefs.coaching.announceHalfway
        this[Keys.AnnounceRemaining] = prefs.coaching.announceRemainingTime
        this[Keys.Motivational] = prefs.coaching.motivationalPrompts
        this[Keys.AnnounceRest] = prefs.coaching.announceRestCountdown
        this[Keys.SpeakFullInstructions] = prefs.coaching.speakFullInstructions
        this[Keys.SpeechRate] = prefs.coaching.speechRate
        this[Keys.SpeechPitch] = prefs.coaching.speechPitch
        this[Keys.CueTones] = prefs.coaching.cueTones
        this[Keys.HapticCues] = prefs.coaching.hapticCues

        this[Keys.KeepScreenOn] = prefs.display.keepScreenOn
        this[Keys.Theme] = prefs.display.theme.id
        this[Keys.Amoled] = prefs.display.amoledDarkMode
        this[Keys.DynamicColour] = prefs.display.dynamicColour
        this[Keys.MachineMode] = prefs.display.machineMode

        this[Keys.AvailableEquipment] = prefs.body.availableEquipment.mapTo(mutableSetOf()) { it.id }
        prefs.body.bodyMassKg?.let { this[Keys.BodyMassKg] = it.toFloat() } ?: remove(Keys.BodyMassKg)
        prefs.body.ageYears?.let { this[Keys.AgeYears] = it } ?: remove(Keys.AgeYears)
        this[Keys.Units] = prefs.body.units.id
        this[Keys.AvoidTags] = prefs.body.avoidTags
    }

    private fun Float.coerceRate(): Float =
        coerceIn(CoachingPreferences.MIN_RATE, CoachingPreferences.MAX_RATE)

    /**
     * Key names are frozen: renaming one silently resets that setting for every
     * existing install. If a key must change meaning, add a new key and migrate.
     */
    private object Keys {
        val EnabledModalities = stringSetPreferencesKey("enabled_modalities")
        val Level = stringPreferencesKey("level")
        val DefaultStyle = stringPreferencesKey("default_style")
        val DefaultDurationSeconds = intPreferencesKey("default_duration_seconds")
        val WeeklyMinutesGoal = intPreferencesKey("weekly_minutes_goal")
        val SafetyAcknowledged = booleanPreferencesKey("safety_notice_acknowledged")

        val SpeechEnabled = booleanPreferencesKey("speech_enabled")
        val AnnounceNext = booleanPreferencesKey("announce_next_exercise")
        val AnnounceCountdown = booleanPreferencesKey("announce_countdown")
        val AnnounceHalfway = booleanPreferencesKey("announce_halfway")
        val AnnounceRemaining = booleanPreferencesKey("announce_remaining_time")
        val Motivational = booleanPreferencesKey("motivational_prompts")
        val AnnounceRest = booleanPreferencesKey("announce_rest_countdown")
        val SpeakFullInstructions = booleanPreferencesKey("speak_full_instructions")
        val SpeechRate = floatPreferencesKey("speech_rate")
        val SpeechPitch = floatPreferencesKey("speech_pitch")
        val CueTones = booleanPreferencesKey("cue_tones")
        val HapticCues = booleanPreferencesKey("haptic_cues")

        val KeepScreenOn = booleanPreferencesKey("keep_screen_on")
        val Theme = stringPreferencesKey("theme")
        val Amoled = booleanPreferencesKey("amoled_dark_mode")
        val DynamicColour = booleanPreferencesKey("dynamic_colour")
        val MachineMode = booleanPreferencesKey("machine_mode")

        val AvailableEquipment = stringSetPreferencesKey("available_equipment")
        val BodyMassKg = floatPreferencesKey("body_mass_kg")
        val AgeYears = intPreferencesKey("age_years")
        val Units = stringPreferencesKey("units")
        val AvoidTags = stringSetPreferencesKey("avoid_tags")
    }

    private companion object {
        const val MIN_AGE = 13
        const val MAX_AGE = 110
    }
}

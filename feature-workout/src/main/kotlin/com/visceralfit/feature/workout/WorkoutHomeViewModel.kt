package com.visceralfit.feature.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.engine.GenerationFailure
import com.visceralfit.domain.engine.SessionLimits
import com.visceralfit.domain.engine.WorkoutRequest
import com.visceralfit.domain.model.Modality
import com.visceralfit.domain.model.UserPreferences
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutStyle
import com.visceralfit.domain.repository.ExerciseRepository
import com.visceralfit.domain.repository.PreferencesRepository
import com.visceralfit.domain.usecase.GenerateWorkout
import com.visceralfit.feature.workout.player.SessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class WorkoutHomeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val generateWorkout: GenerateWorkout,
    private val sessionCoordinator: SessionCoordinator,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Selections the user has made on this screen but not yet saved as defaults. */
    private val selection = MutableStateFlow(Selection())

    private val generation = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)

    val state: StateFlow<WorkoutHomeUiState> = combine(
        preferencesRepository.observe(),
        exerciseRepository.observeAll(),
        selection,
        generation,
    ) { prefs, exercises, chosen, generationState ->
        val minutes = chosen.minutes ?: prefs.defaultDuration.inWholeMinutes.toInt()
        val available = SessionLimits.stylesFor(minutes.minutes)
        WorkoutHomeUiState(
            isLoading = false,
            selectedMinutes = minutes,
            // Never leave a disabled style selected: the Start button would then be
            // permanently unavailable with no obvious cause.
            selectedStyle = (chosen.style ?: prefs.defaultStyle).takeIf { it in available }
                ?: available.firstOrNull()
                ?: WorkoutStyle.RECOVERY,
            usableModalities = prefs.usableModalities(),
            availableStyles = available,
            minimumMinutesByStyle = WorkoutStyle.entries.associateWith {
                SessionLimits.minimumFor(it).inWholeMinutes.toInt()
            },
            exerciseCount = exercises.size,
            generation = generationState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = WorkoutHomeUiState(),
    )

    fun selectDuration(minutes: Int) = selection.update { it.copy(minutes = minutes) }

    fun selectStyle(style: WorkoutStyle) = selection.update { it.copy(style = style) }

    /**
     * Builds a session from the current selections.
     *
     * The seed is recorded on the workout, so any session a user reports can be regenerated
     * exactly from its id and seed.
     */
    fun start() {
        if (generation.value is GenerationUiState.Generating) return
        generation.value = GenerationUiState.Generating
        viewModelScope.launch {
            val prefs = preferencesRepository.observe().first()
            val current = state.value
            val request = WorkoutRequest(
                duration = current.selectedMinutes.minutes,
                style = current.selectedStyle,
                modalities = prefs.usableModalities(),
                level = prefs.level,
                avoidTags = prefs.body.avoidTags,
                seed = newSeed(),
                recentExerciseIds = recentExerciseIds(),
            )
            generation.value = generateWorkout(request).fold(
                onSuccess = { GenerationUiState.Ready(it) },
                onFailure = { GenerationUiState.Failed(GenerationMessage.of(it, prefs)) },
            )
        }
    }

    /**
     * Hands the generated plan to the session coordinator and clears this screen's state.
     *
     * The caller starts the foreground service — that needs a `Context`, which a ViewModel
     * must not hold, and the split keeps this class testable without Robolectric.
     * Returns false when there is nothing to begin, so the caller does not navigate into an
     * empty player.
     */
    fun beginSession(): Boolean {
        val ready = generation.value as? GenerationUiState.Ready ?: return false
        sessionCoordinator.begin(
            workout = ready.workout,
            startedAt = Clock.System.now(),
            nowMillis = SystemClock.elapsedRealtime(),
        )
        generation.value = GenerationUiState.Idle
        return true
    }

    /** Discards the generated plan without starting it. */
    fun consumeGeneratedWorkout() {
        generation.value = GenerationUiState.Idle
    }

    fun dismissFailure() {
        generation.value = GenerationUiState.Idle
    }

    /**
     * Exercise ids from recent sessions, de-prioritised so consecutive sessions do not
     * repeat the same movements (REQ-034).
     *
     * Empty for now, and deliberately so rather than approximated: `CompletedSession`
     * records the session's title, style and modalities but not which exercises it used,
     * so there is nothing honest to return. The generator treats an empty list as "nothing
     * to avoid", so variety still works within a session — just not across sessions.
     * Tracked as KI-0012; closing it needs a `session_exercises` table, which is a schema
     * change and therefore phase 09 work.
     */
    private fun recentExerciseIds(): List<String> = emptyList()

    /**
     * The one clock read in the whole generation path, and it is deliberately **here**
     * rather than inside the engine — that is what keeps the engine reproducible (spec §6).
     */
    private fun newSeed(): Long = System.currentTimeMillis()

    private data class Selection(
        val minutes: Int? = null,
        val style: WorkoutStyle? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** What the Start button is currently doing. */
sealed interface GenerationUiState {
    data object Idle : GenerationUiState

    data object Generating : GenerationUiState

    data class Ready(val workout: Workout) : GenerationUiState

    data class Failed(val message: GenerationMessage) : GenerationUiState
}

/**
 * A failure, translated into something the user can act on.
 *
 * Never a generic error. Every branch below names what went wrong *and* the specific fix,
 * because a dead Start button with no explanation is the single most common complaint in
 * the competitor review sample (`framework/03_competitive_analysis.md` §Complaint themes).
 */
data class GenerationMessage(val reason: String, val fix: String) {
    companion object {
        fun of(failure: Throwable, prefs: UserPreferences): GenerationMessage = when (failure) {
            is GenerationFailure.DurationTooShort -> forDuration(failure)
            is GenerationFailure.NoEligibleExercises -> forEligibility(failure, prefs)
            is GenerationFailure.InsufficientVariety -> GenerationMessage(
                reason = "There are not enough different movements available for a session this long.",
                fix = "Enable another exercise type in Settings, or choose a shorter session.",
            )

            else -> GenerationMessage(
                reason = "The session could not be built.",
                fix = "Try a different duration or session type. If it keeps happening, " +
                    "this is a bug worth reporting.",
            )
        }

        private fun forDuration(failure: GenerationFailure.DurationTooShort): GenerationMessage = when {
            failure.requested > SessionLimits.maximumDuration -> GenerationMessage(
                reason = "The longest session this app will build is " +
                    "${SessionLimits.maximumDuration.inWholeMinutes} minutes.",
                fix = "Choose a shorter duration.",
            )

            else -> GenerationMessage(
                reason = "This session type needs at least ${failure.minimum.inWholeMinutes} minutes.",
                fix = "Choose a longer duration, or pick a session type that fits " +
                    "${failure.requested.inWholeMinutes} minutes.",
            )
        }

        private fun forEligibility(
            failure: GenerationFailure.NoEligibleExercises,
            prefs: UserPreferences,
        ): GenerationMessage {
            val equipmentMissing = prefs.enabledModalities.any { it.requiresEquipment } &&
                prefs.usableModalities().isEmpty()
            return when {
                failure.modalities.isEmpty() && equipmentMissing -> GenerationMessage(
                    reason = "Every exercise type you have enabled needs equipment you have not marked as available.",
                    fix = "In Settings, mark the equipment you have access to, or enable floor Pilates.",
                )

                failure.modalities.isEmpty() -> GenerationMessage(
                    reason = "No exercise types are enabled.",
                    fix = "Enable at least one in Settings.",
                )

                prefs.body.avoidTags.isNotEmpty() -> GenerationMessage(
                    reason = "Nothing in the catalogue matches your experience level once your " +
                        "movements to avoid are excluded.",
                    fix = "Review the movements you are avoiding in Settings, or enable another exercise type.",
                )

                else -> GenerationMessage(
                    reason = "Nothing in the catalogue matches ${failure.level.id} level for " +
                        "the exercise types you have enabled.",
                    fix = "Enable another exercise type in Settings.",
                )
            }
        }
    }
}

data class WorkoutHomeUiState(
    val isLoading: Boolean = true,
    val selectedMinutes: Int = DEFAULT_MINUTES,
    val selectedStyle: WorkoutStyle = WorkoutStyle.MIXED,
    val usableModalities: Set<Modality> = emptySet(),
    /** Styles the selected duration can support (REQ-024). */
    val availableStyles: Set<WorkoutStyle> = WorkoutStyle.entries.toSet(),
    /** The minimum each style needs, so a disabled chip can state its own reason. */
    val minimumMinutesByStyle: Map<WorkoutStyle, Int> = emptyMap(),
    val exerciseCount: Int = 0,
    val generation: GenerationUiState = GenerationUiState.Idle,
) {
    /** Start is disabled only when generation genuinely cannot succeed. */
    val canStart: Boolean
        get() = usableModalities.isNotEmpty() &&
            exerciseCount > 0 &&
            selectedStyle in availableStyles &&
            generation !is GenerationUiState.Generating

    val startDisabledReason: String?
        get() = when {
            canStart -> null
            usableModalities.isEmpty() ->
                "Enable an exercise type in Settings, and mark the equipment you have."

            exerciseCount == 0 -> "The exercise catalogue has not loaded yet."
            selectedStyle !in availableStyles ->
                "${selectedStyle.displayName} needs at least " +
                    "${minimumMinutesByStyle[selectedStyle] ?: 0} minutes."

            else -> null
        }

    companion object {
        /** Presets from PRD REQ-012. Custom duration is a separate entry point. */
        val DURATION_PRESETS_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60, 90)
        const val DEFAULT_MINUTES = 20
    }
}

/** Plain-language style names. The enum ids are storage keys, not labels. */
val WorkoutStyle.displayName: String
    get() = when (this) {
        WorkoutStyle.HIIT -> "Intervals"
        WorkoutStyle.ZONE_2 -> "Steady"
        WorkoutStyle.MIXED -> "Mixed"
        WorkoutStyle.RECOVERY -> "Recovery"
    }

val Modality.displayName: String
    get() = when (this) {
        Modality.FLOOR_PILATES -> "Floor Pilates"
        Modality.REFORMER_PILATES -> "Reformer"
        Modality.ELLIPTICAL -> "Elliptical"
        Modality.SPIN_BIKE -> "Spin bike"
    }

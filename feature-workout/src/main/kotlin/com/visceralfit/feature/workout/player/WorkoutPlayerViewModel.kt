package com.visceralfit.feature.workout.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visceralfit.domain.coaching.SpeechCoach
import com.visceralfit.domain.coaching.SpeechState
import com.visceralfit.domain.model.BlockKind
import com.visceralfit.domain.model.CompletedSession
import com.visceralfit.domain.model.Workout
import com.visceralfit.domain.model.WorkoutBlock
import com.visceralfit.domain.repository.HistoryRepository
import com.visceralfit.domain.repository.PreferencesRepository
import com.visceralfit.domain.usecase.EstimateEnergyExpenditure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Observes the session the service is running, and records it when it ends.
 *
 * Holds no clock and no timer of its own (ADR-0008's compliance rule). Everything on
 * screen is derived from [SessionCoordinator.state], so an activity recreation is invisible
 * to the session.
 */
@HiltViewModel
class WorkoutPlayerViewModel @Inject constructor(
    private val coordinator: SessionCoordinator,
    private val historyRepository: HistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val estimateEnergy: EstimateEnergyExpenditure,
    speechCoach: SpeechCoach,
) : ViewModel() {

    private val summary = MutableStateFlow<SessionSummary?>(null)

    private val speechNoticeDismissed = MutableStateFlow(false)

    val state: StateFlow<PlayerUiState> = combine(
        coordinator.state,
        preferencesRepository.observe(),
        summary,
        speechCoach.state,
        speechNoticeDismissed,
    ) { session, prefs, recorded, speech, dismissed ->
        when {
            recorded != null -> PlayerUiState.Finished(recorded)
            session == null -> PlayerUiState.NoSession
            else -> PlayerUiState.Running(
                session = session,
                keepScreenOn = prefs.display.keepScreenOn,
                machineMode = prefs.display.machineMode,
                speechNotice = speechNotice(speech, prefs.coaching.speechEnabled, dismissed),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PlayerUiState.NoSession,
    )

    fun togglePause() = coordinator.togglePause(nowMillis())

    fun skip() = coordinator.skip(nowMillis())

    /**
     * Ends the session and records it, whether it ran to completion or was stopped early.
     *
     * An abandoned session is still recorded, with its real completion ratio. Discarding it
     * would make the history a record of good days only, and the recovery recommender reads
     * completion ratios to notice that the current prescription is too hard
     * (`framework/07_workout_engine_spec.md` §8) — exactly the signal a discarded session
     * carries.
     */
    fun finishAndRecord() {
        val session = coordinator.state.value ?: return
        coordinator.finish(nowMillis())
        val ended = coordinator.state.value ?: session
        viewModelScope.launch {
            val prefs = preferencesRepository.observe().first()
            val kilocalories = estimateEnergy(performedPortionOf(ended), prefs.body.bodyMassKg)
            val record = CompletedSession(
                id = 0L,
                workoutId = ended.workout.id,
                title = ended.workout.title,
                style = ended.workout.style,
                modalities = ended.workout.modalities,
                startedAt = ended.startedAt,
                completedAt = Clock.System.now(),
                activeDuration = ended.activeElapsed,
                estimatedKilocalories = kilocalories,
                perceivedExertion = null,
                completionRatio = ended.completionRatio,
                note = null,
            )
            historyRepository.record(record)
            summary.value = SessionSummary(
                title = record.title,
                plannedDuration = ended.workout.actualDuration,
                activeDuration = ended.activeElapsed,
                estimatedKilocalories = kilocalories,
                segmentsCompleted = ended.segmentIndex.coerceAtMost(ended.segmentCount),
                segmentCount = ended.segmentCount,
                skippedSegments = ended.skippedSegments,
                completionRatio = ended.completionRatio,
            )
            coordinator.clear()
        }
    }

    /** Hides the speech-unavailable notice for the rest of this session. */
    fun dismissSpeechNotice() {
        speechNoticeDismissed.value = true
    }

    /** Called once the summary has been seen, so the player can be navigated away from. */
    fun dismissSummary() {
        summary.value = null
    }

    /**
     * The part of the plan actually performed, for the energy estimate.
     *
     * Charging the whole plan when the user stopped a third of the way in would overstate
     * expenditure, and an energy figure a user reads as measured must never be inflated
     * (REQ-081, D-0005). Segments before the cursor count in full, the current one is
     * truncated to what was done, and the rest are dropped.
     *
     * The result is a synthetic [Workout] with one block: [EstimateEnergyExpenditure] sums
     * segments and does not care how they are grouped, so preserving the block structure
     * would be shape for its own sake.
     */
    private fun performedPortionOf(state: SessionState): Workout {
        val performed = state.workout.segments.mapIndexedNotNull { index, segment ->
            when {
                index < state.segmentIndex -> segment
                index == state.segmentIndex && state.elapsedInSegment > Duration.ZERO ->
                    segment.copy(duration = state.elapsedInSegment)

                else -> null
            }
        }
        return state.workout.copy(blocks = listOf(WorkoutBlock(BlockKind.MAIN, performed)))
    }

    private fun nowMillis(): Long = android.os.SystemClock.elapsedRealtime()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * The message for an unavailable speech engine, or null when there is nothing to say.
         *
         * Four reasons, four messages (spec §6). They are distinguished because the *actions*
         * they imply differ completely: missing voice data is something the user can fix in
         * Android settings and the message says so, while a failed init is not, and saying
         * "install voice data" to someone whose engine crashed sends them somewhere useless.
         *
         * `DISABLED_BY_USER` and `Initialising` both produce null. Telling a user that the
         * feature they just switched off is off would be nagging, and a notice during
         * initialisation would flash up on every cold start and then vanish.
         */
        fun speechNotice(state: SpeechState, speechEnabled: Boolean, dismissed: Boolean): String? = when {
            dismissed || !speechEnabled -> null
            state !is SpeechState.Unavailable -> null
            else -> when (state.reason) {
                SpeechState.Unavailable.Reason.NO_ENGINE_INSTALLED ->
                    "No text-to-speech engine found. Your workout will use tones and vibration instead."

                SpeechState.Unavailable.Reason.NO_VOICE_DATA_FOR_LOCALE ->
                    "No voice data installed for your language. You can add it in Android settings, " +
                        "or continue with tones and vibration."

                SpeechState.Unavailable.Reason.ENGINE_INIT_FAILED ->
                    "Spoken coaching is unavailable on this device right now."

                SpeechState.Unavailable.Reason.DISABLED_BY_USER -> null
            }
        }
    }
}

sealed interface PlayerUiState {
    /** No session is loaded — the player was opened directly, or the session already ended. */
    data object NoSession : PlayerUiState

    data class Running(
        val session: SessionState,
        val keepScreenOn: Boolean,
        val machineMode: Boolean,
        /**
         * Why spoken coaching is not working, or null when it is — or when the user turned it
         * off, which is not a fault to report.
         *
         * Non-blocking and dismissible by contract (spec §6): the session runs identically
         * without speech, so a notice that could stop a workout starting would be a defect
         * worse than the silence it describes.
         */
        val speechNotice: String? = null,
    ) : PlayerUiState

    data class Finished(val summary: SessionSummary) : PlayerUiState
}

/**
 * What a finished session came to.
 *
 * [estimatedKilocalories] is null when body mass is unknown, and the UI must render an
 * em dash rather than a zero (D-0005): a plausible-looking fake figure is worse than an
 * absent one.
 */
data class SessionSummary(
    val title: String,
    val plannedDuration: Duration,
    val activeDuration: Duration,
    val estimatedKilocalories: Int?,
    val segmentsCompleted: Int,
    val segmentCount: Int,
    val skippedSegments: Int,
    val completionRatio: Float,
) {
    val ranToCompletion: Boolean get() = completionRatio >= CompletedSession.COMPLETION_THRESHOLD
}

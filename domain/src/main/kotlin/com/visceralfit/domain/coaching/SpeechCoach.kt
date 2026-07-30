package com.visceralfit.domain.coaching

import kotlinx.coroutines.flow.Flow

/**
 * Port for spoken coaching. The Android TextToSpeech implementation lives in
 * `:core:speech`; the domain and feature layers only know this interface, which is
 * what makes the coaching rules unit-testable without a device.
 */
interface SpeechCoach {
    val state: Flow<SpeechState>

    /**
     * Queues [cue] for speaking. Cues are not guaranteed to be spoken: if the
     * engine is unavailable, or a higher-priority cue arrives while this one is
     * still queued, it is dropped. That is intentional — a stale "10 seconds left"
     * spoken 20 seconds late is worse than silence (see REQ-054).
     */
    fun speak(cue: SpeechCue)

    /** Drops anything queued and stops mid-utterance. Used on pause and on exit. */
    fun stop()

    suspend fun setRate(rate: Float)

    suspend fun setPitch(pitch: Float)
}

/**
 * Availability of spoken coaching. [Unavailable] is a first-class state, not an
 * error: many devices have no TTS voice data installed, and the app must stay
 * fully usable when that is the case (REQ-055).
 */
sealed interface SpeechState {
    data object Initialising : SpeechState

    data object Ready : SpeechState

    data class Unavailable(val reason: Reason) : SpeechState {
        enum class Reason {
            NO_ENGINE_INSTALLED,
            NO_VOICE_DATA_FOR_LOCALE,
            ENGINE_INIT_FAILED,
            DISABLED_BY_USER,
        }
    }
}

/**
 * A single utterance. [priority] decides what wins when cues collide, which
 * happens constantly at segment boundaries.
 */
data class SpeechCue(
    val text: String,
    val priority: Priority,
    /** Drop rather than speak late if this cue cannot start within this many ms. */
    val staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
) {
    enum class Priority {
        /** Safety and stop cues. Interrupt anything. */
        CRITICAL,

        /** "Next up: ..." and countdowns. Interrupt informational cues. */
        TRANSITION,

        /** Halfway, time remaining, motivational lines. Dropped under contention. */
        INFORMATIONAL,
    }

    companion object {
        const val DEFAULT_STALE_AFTER_MILLIS = 2_000L
    }
}

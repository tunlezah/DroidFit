package com.visceralfit.domain.coaching

/**
 * Port for the non-speech half of coaching: a tone and a vibration at each segment
 * boundary.
 *
 * WHY THIS IS SEPARATE FROM [SpeechCoach] AND NOT A FALLBACK INSIDE IT: tones and haptics
 * are not a degraded form of speech, they are their own channel, and `cueTones` and
 * `hapticCues` are deliberately independent of `speechEnabled`
 * (`framework/09_coaching_and_tts_spec.md` §6). A user on a spin bike with music in their
 * ears may want the vibration and nothing else; a user with no TTS voice data installed
 * gets the tones as the only signal they have. Folding this into the speech coach would
 * make the second case depend on the first case's failure.
 *
 * Both methods are fire-and-forget. Neither may block the caller: they are invoked from the
 * session ticker, and a boundary signal that arrives late is worse than one that is missed
 * (spec §0 — "a cue is only useful on time").
 */
interface CueFeedback {

    fun play(tone: CueTone)

    fun vibrate(haptic: HapticCue)

    /** Releases any platform resources held. Call from the owning service's `onDestroy`. */
    fun release()
}

/**
 * A boundary tone: rising into work, falling into rest (spec §6).
 *
 * The direction carries the meaning, which is why it is named for the direction rather than
 * for the segment kind — a user learns "up means go" in one session and never has to look at
 * the screen for a boundary again.
 */
enum class CueTone {
    RISING,
    FALLING,
    ;

    companion object {
        fun forWork(isWork: Boolean): CueTone = if (isWork) RISING else FALLING
    }
}

/** A boundary vibration: long into work, short into rest (spec §6). */
enum class HapticCue {
    LONG,
    SHORT,
    ;

    companion object {
        fun forWork(isWork: Boolean): HapticCue = if (isWork) LONG else SHORT
    }
}

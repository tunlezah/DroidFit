package com.visceralfit.feature.settings

import com.visceralfit.domain.model.CoachingPreferences

/**
 * The coaching switches, in the order they appear in Settings.
 *
 * WHY A TABLE AND NOT ELEVEN HAND-WRITTEN ROWS: `framework/09_coaching_and_tts_spec.md` §1 says
 * every cue type is independently switchable, and until phase 08 only five of the eleven flags
 * were surfaced at all — the other six existed in `CoachingPreferences`, did nothing, and were
 * unreachable. Six near-identical composables plus six lambdas threaded through the screen is
 * the shape that made it easy to stop at five. Adding a twelfth cue type is now one row here.
 *
 * [needsSpeech] is the interesting column. Tones and haptics are deliberately *not* gated by the
 * master speech switch, because they are what substitutes for speech when it is unavailable
 * (spec §6). Greying them out with speech off would break exactly the case they exist for.
 */
internal enum class CoachingToggle(
    val title: String,
    val subtitle: String?,
    val needsSpeech: Boolean,
    private val get: (CoachingPreferences) -> Boolean,
    private val set: (CoachingPreferences, Boolean) -> CoachingPreferences,
) {
    SPEECH(
        title = "Speak cues aloud",
        subtitle = "Uses your device's text-to-speech voice. Works offline once voice data is installed.",
        needsSpeech = false,
        get = { it.speechEnabled },
        set = { prefs, value -> prefs.copy(speechEnabled = value) },
    ),
    NEXT_EXERCISE(
        title = "Announce the next exercise",
        subtitle = "Names the upcoming movement five seconds before it starts.",
        needsSpeech = true,
        get = { it.announceNextExercise },
        set = { prefs, value -> prefs.copy(announceNextExercise = value) },
    ),
    COUNTDOWN(
        title = "Count down the last seconds",
        subtitle = "Three, two, one before a segment ends.",
        needsSpeech = true,
        get = { it.announceCountdown },
        set = { prefs, value -> prefs.copy(announceCountdown = value) },
    ),
    REST_COUNTDOWN(
        title = "Count down the end of rests",
        subtitle = "Names what is coming as the rest runs out, so you are ready for it.",
        needsSpeech = true,
        get = { it.announceRestCountdown },
        set = { prefs, value -> prefs.copy(announceRestCountdown = value) },
    ),
    HALFWAY(
        title = "Halfway reminder",
        subtitle = "Halfway through the work you have actually done, not through the clock.",
        needsSpeech = true,
        get = { it.announceHalfway },
        set = { prefs, value -> prefs.copy(announceHalfway = value) },
    ),
    REMAINING_TIME(
        title = "Announce time remaining",
        subtitle = "Every five minutes, and once at one minute left.",
        needsSpeech = true,
        get = { it.announceRemainingTime },
        set = { prefs, value -> prefs.copy(announceRemainingTime = value) },
    ),
    FULL_INSTRUCTIONS(
        title = "Read technique cues aloud",
        subtitle = "Reads the full how-to after the exercise name, not just the name.",
        needsSpeech = true,
        get = { it.speakFullInstructions },
        set = { prefs, value -> prefs.copy(speakFullInstructions = value) },
    ),
    MOTIVATION(
        title = "Encouragement during work",
        subtitle = "A short line mid-interval, at most once every 90 seconds.",
        needsSpeech = true,
        get = { it.motivationalPrompts },
        set = { prefs, value -> prefs.copy(motivationalPrompts = value) },
    ),
    TONES(
        title = "Tone at each change",
        subtitle = "A rising tone into work, falling into rest. Works with speech off.",
        needsSpeech = false,
        get = { it.cueTones },
        set = { prefs, value -> prefs.copy(cueTones = value) },
    ),
    HAPTICS(
        title = "Vibrate at each change",
        subtitle = "Long into work, short into rest. Works with speech off.",
        needsSpeech = false,
        get = { it.hapticCues },
        set = { prefs, value -> prefs.copy(hapticCues = value) },
    ),
    ;

    fun read(preferences: CoachingPreferences): Boolean = get(preferences)

    fun write(preferences: CoachingPreferences, value: Boolean): CoachingPreferences =
        set(preferences, value)
}

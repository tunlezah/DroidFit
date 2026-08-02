package com.visceralfit.core.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.visceralfit.domain.coaching.SpeechCoach
import com.visceralfit.domain.coaching.SpeechCue
import com.visceralfit.domain.coaching.SpeechState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SpeechCoach] backed by the platform TextToSpeech engine.
 *
 * Behaviour that is deliberate and must not be "fixed" without reading
 * /framework/09_coaching_and_tts_spec.md:
 *
 *  - Engine initialisation is asynchronous and can fail permanently (no engine
 *    installed, no voice data for the locale). Failure resolves to
 *    [SpeechState.Unavailable], which the UI renders as a dismissible notice. The
 *    workout still runs, with tones and haptics instead of speech.
 *
 *  - A cue that cannot be started before its [SpeechCue.staleAfterMillis] elapses is
 *    dropped rather than queued. Coaching cues are only useful on time; "ten
 *    seconds left" spoken after the interval ended actively misleads.
 *
 *  - [SpeechCue.Priority.CRITICAL] and TRANSITION cues flush the queue
 *    (QUEUE_FLUSH); INFORMATIONAL cues are dropped outright while anything is
 *    speaking, rather than queued behind it.
 *
 *  - The engine is created lazily on first [speak] and released in [shutdown]. It is
 *    never created in `init`, because constructing a TextToSpeech instance spins up
 *    an IPC binding that costs ~200 ms and would show up in cold-start time for
 *    users who have speech switched off.
 *
 *  - Audio focus is requested per utterance as `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` and
 *    abandoned the moment the utterance ends (spec §7). Never `AUDIOFOCUS_GAIN`, and never
 *    held between cues: holding focus pauses the user's music for the whole session, which is
 *    the single most annoying thing an app of this kind can do.
 */
@Singleton
class AndroidSpeechCoach @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechCoach {

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Initialising)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var engine: TextToSpeech? = null

    @Volatile
    private var isSpeaking: Boolean = false

    private var utteranceCounter: Long = 0

    /** Set when the init callback arrives before [engine] has been assigned. */
    @Volatile
    private var pendingInitStatus: Int? = null

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Cues route as navigation guidance, not as media.
     *
     * `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` is what makes a cue behave the way a satnav
     * instruction does: it follows the user to whatever they are actually listening on,
     * including Bluetooth headphones, and it ducks music rather than replacing it (spec §7).
     */
    private val cueAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /**
     * Stops mid-utterance when focus is lost, and does not resume.
     *
     * A phone call is the case that matters (spec §7). Resuming afterwards would speak a cue
     * about a segment that has since ended, which is precisely the stale cue this design drops
     * everywhere else.
     */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            stop()
        }
    }

    private val focusRequest: AudioFocusRequest = AudioFocusRequest
        .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(cueAttributes)
        .setOnAudioFocusChangeListener(focusListener)
        .build()

    override fun speak(cue: SpeechCue) {
        val tts = ensureEngine() ?: return
        if (_state.value !is SpeechState.Ready) return

        val shouldFlush = when (cue.priority) {
            SpeechCue.Priority.CRITICAL, SpeechCue.Priority.TRANSITION -> true
            SpeechCue.Priority.INFORMATIONAL -> false
        }
        if (!shouldFlush && isSpeaking) {
            // Drop instead of queueing: see the class doc.
            return
        }

        val queueMode = if (shouldFlush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val id = "cue-${utteranceCounter++}"
        isSpeaking = true
        requestFocus()
        tts.speak(cue.text, queueMode, EMPTY_PARAMS, id)
    }

    override fun stop() {
        engine?.stop()
        isSpeaking = false
        abandonFocus()
    }

    override suspend fun setRate(rate: Float) {
        engine?.setSpeechRate(rate)
    }

    override suspend fun setPitch(pitch: Float) {
        engine?.setPitch(pitch)
    }

    /** Releases the engine. Call from the owning service's `onDestroy`. */
    override fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isSpeaking = false
        abandonFocus()
        _state.value = SpeechState.Initialising
    }

    private fun requestFocus() {
        audioManager.requestAudioFocus(focusRequest)
    }

    /**
     * Hands focus straight back so the user's music unducks immediately.
     *
     * Called from every terminal path of an utterance — done, error, stop, shutdown — because
     * a single missed path leaves the music quiet for the rest of the session, and that is a
     * failure the user notices long before they work out which app caused it.
     */
    private fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun ensureEngine(): TextToSpeech? {
        engine?.let { return it }

        val created = TextToSpeech(context) { status -> onEngineInit(status) }
        created.setAudioAttributes(cueAttributes)
        created.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    abandonFocus()
                }

                @Deprecated("Required override; the int-arg overload is called instead.")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    abandonFocus()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
                    abandonFocus()
                }
            },
        )
        engine = created
        // The init callback is normally posted to the main looper and so arrives
        // after this assignment, but the platform does not promise that. If it
        // already fired, replay it now that `engine` is readable.
        pendingInitStatus?.let { status ->
            pendingInitStatus = null
            onEngineInit(status)
        }
        return created
    }

    private fun onEngineInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _state.value = SpeechState.Unavailable(SpeechState.Unavailable.Reason.ENGINE_INIT_FAILED)
            return
        }
        val tts = engine
        if (tts == null) {
            // Callback beat the field assignment; ensureEngine() will replay this.
            pendingInitStatus = status
            return
        }
        _state.value = when (tts.setLanguage(Locale.getDefault())) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED ->
                SpeechState.Unavailable(SpeechState.Unavailable.Reason.NO_VOICE_DATA_FOR_LOCALE)
            else -> SpeechState.Ready
        }
    }

    private companion object {
        /**
         * Stream routing comes from the engine's [AudioAttributes], not from a per-utterance
         * parameter, so this stays empty rather than carrying the deprecated
         * `KEY_PARAM_STREAM`.
         */
        val EMPTY_PARAMS = Bundle()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechModule {
    @Binds
    abstract fun bindSpeechCoach(impl: AndroidSpeechCoach): SpeechCoach
}

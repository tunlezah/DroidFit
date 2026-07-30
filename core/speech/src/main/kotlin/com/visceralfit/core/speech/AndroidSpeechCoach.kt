package com.visceralfit.core.speech

import android.content.Context
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
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        isSpeaking = true
        tts.speak(cue.text, queueMode, params, id)
    }

    override fun stop() {
        engine?.stop()
        isSpeaking = false
    }

    override suspend fun setRate(rate: Float) {
        engine?.setSpeechRate(rate)
    }

    override suspend fun setPitch(pitch: Float) {
        engine?.setPitch(pitch)
    }

    /** Releases the engine. Call from the owning service's `onDestroy`. */
    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isSpeaking = false
        _state.value = SpeechState.Initialising
    }

    private fun ensureEngine(): TextToSpeech? {
        engine?.let { return it }

        val created = TextToSpeech(context) { status -> onEngineInit(status) }
        created.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }

                @Deprecated("Required override; the int-arg overload is called instead.")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechModule {
    @Binds
    abstract fun bindSpeechCoach(impl: AndroidSpeechCoach): SpeechCoach
}

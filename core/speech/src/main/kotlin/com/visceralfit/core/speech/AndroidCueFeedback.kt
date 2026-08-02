package com.visceralfit.core.speech

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.visceralfit.core.common.DefaultDispatcher
import com.visceralfit.domain.coaching.CueFeedback
import com.visceralfit.domain.coaching.CueTone
import com.visceralfit.domain.coaching.HapticCue
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CueFeedback] on the platform's tone generator and vibrator.
 *
 * WHY THE TONES ARE TWO NOTES AND NOT ONE: the spec asks for a *rising* tone into work and a
 * *falling* tone into rest, and a direction cannot be expressed by a single beep.
 * `ToneGenerator`'s own named tones (`TONE_PROP_BEEP`, `TONE_PROP_ACK`) are all fixed
 * patterns, so the direction is built here from two DTMF tones of known pitch played in
 * sequence — low then high, or high then low. That is the difference between a signal a user
 * learns in one session and two beeps they have to think about.
 *
 * WHY IT IS FIRE-AND-FORGET: this is called from the session ticker, which must never block.
 * The two-note sequence needs a gap between the notes, so it runs on its own scope; a dropped
 * boundary tone is a far better outcome than a stalled clock.
 *
 * The generator is created lazily for the same reason the TTS engine is: it allocates an audio
 * track, and a user with tones switched off should not pay for one.
 */
@Singleton
class AndroidCueFeedback @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : CueFeedback {

    private val scope = CoroutineScope(defaultDispatcher + SupervisorJob())

    private var generator: ToneGenerator? = null

    override fun play(tone: CueTone) {
        val notes = when (tone) {
            CueTone.RISING -> listOf(LOW_NOTE, HIGH_NOTE)
            CueTone.FALLING -> listOf(HIGH_NOTE, LOW_NOTE)
        }
        scope.launch {
            val toneGenerator = ensureGenerator() ?: return@launch
            notes.forEachIndexed { index, note ->
                if (index > 0) delay(NOTE_GAP_MILLIS)
                toneGenerator.startTone(note, NOTE_MILLIS)
            }
        }
    }

    override fun vibrate(haptic: HapticCue) {
        val millis = when (haptic) {
            HapticCue.LONG -> LONG_BUZZ_MILLIS
            HapticCue.SHORT -> SHORT_BUZZ_MILLIS
        }
        vibrator()?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun release() {
        scope.cancel()
        generator?.release()
        generator = null
    }

    /**
     * Null when the platform refuses to allocate an audio track, which happens on some devices
     * when too many are already open. A missing boundary tone is not worth a crash.
     */
    private fun ensureGenerator(): ToneGenerator? {
        generator?.let { return it }
        return runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME) }
            .getOrNull()
            ?.also { generator = it }
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private companion object {
        /** DTMF 1 is 697 + 1209 Hz; DTMF D is 941 + 1633 Hz — audibly the higher of the two. */
        const val LOW_NOTE = ToneGenerator.TONE_DTMF_1
        const val HIGH_NOTE = ToneGenerator.TONE_DTMF_D

        const val NOTE_MILLIS = 110
        const val NOTE_GAP_MILLIS = 120L

        /**
         * Loud enough to hear over a spin bike, quiet enough not to startle. Percentage of the
         * music stream's volume, so a user who has turned their music down turns these down too.
         */
        const val TONE_VOLUME = 70

        const val LONG_BUZZ_MILLIS = 350L
        const val SHORT_BUZZ_MILLIS = 120L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CueFeedbackModule {
    @Binds
    abstract fun bindCueFeedback(impl: AndroidCueFeedback): CueFeedback
}

package com.visceralfit.feature.workout.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.visceralfit.core.common.DefaultDispatcher
import com.visceralfit.domain.coaching.CueFeedback
import com.visceralfit.domain.coaching.SpeechCoach
import com.visceralfit.domain.coaching.SpeechState
import com.visceralfit.domain.model.CoachingPreferences
import com.visceralfit.domain.model.Segment
import com.visceralfit.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps a running session alive and drives its clock (ADR-0008).
 *
 * WHY A SERVICE AT ALL: a session runs 5–90 minutes and the user will rotate the phone,
 * take a call, switch apps and let the screen go dark. A ViewModel survives rotation but
 * not process death, and a coroutine in a composable stops when the composable leaves the
 * composition. The clock has to live somewhere that outlives both.
 *
 * WHY `mediaPlayback` AND NOT `exercise`: the service's ongoing output is audio coaching,
 * and unlike most foreground-service types `mediaPlayback` has no six-hour cap — which a
 * 90-minute session plus a forgotten pause could otherwise approach.
 *
 * The service owns no session state of its own. [SessionCoordinator] holds it, and the
 * service's jobs are to exist (so the process is not killed), to advance the clock, to keep the
 * notification truthful, and to drive the coaching cues.
 *
 * WHY THE CUES ARE DRIVEN FROM HERE AND NOT FROM THE PLAYER SCREEN (phase 08): the screen can
 * be gone. A user on a spin bike puts the phone face-down, or switches to their music app, and
 * that is exactly when spoken coaching matters most — it is the only channel left. Cues driven
 * from a composable would stop at the moment they became the point. The scheduler itself is a
 * pure function of the session state ([CueScheduler]), so nothing about that decision makes it
 * harder to test.
 */
@AndroidEntryPoint
class WorkoutService : LifecycleService() {

    @Inject
    lateinit var coordinator: SessionCoordinator

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    lateinit var speechCoach: SpeechCoach

    @Inject
    lateinit var cueFeedback: CueFeedback

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val cueScheduler = CueScheduler()

    private var ticker: Job? = null

    /**
     * The coaching settings and the engine's availability, sampled onto fields the ticker can
     * read without suspending.
     *
     * The ticker runs every 200 ms and must not `first()` a flow on each pass. Both values
     * change rarely — a settings toggle, an engine that finishes initialising — so they are
     * collected once into fields and read from there.
     */
    @Volatile
    private var coaching: CoachingPreferences = CoachingPreferences()

    @Volatile
    private var speechAvailable: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        observeCoachingPreferences()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PAUSE -> {
                coordinator.togglePause(nowMillis())
                // Stop mid-utterance rather than finishing a sentence about a segment the
                // user has just paused. Nothing is replayed on resume.
                if (coordinator.state.value?.isPaused == true) speechCoach.stop()
            }
            ACTION_SKIP -> {
                coordinator.skip(nowMillis())
                speechCoach.stop()
            }
            ACTION_STOP -> {
                coordinator.finish(nowMillis())
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (!coordinator.isActive) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        startTicking()
        // START_NOT_STICKY: a session the system killed should not silently resume hours
        // later with a stale clock. The user restarts it, which is the honest behaviour.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        ticker = null
        // Spec §8: leaking the engine leaks its IPC binding. The service is the only owner
        // that knows when the session is really over, so it is the only place this can happen.
        speechCoach.shutdown()
        cueFeedback.release()
        super.onDestroy()
    }

    private fun observeCoachingPreferences() {
        lifecycleScope.launch {
            preferencesRepository.observe().collect { prefs ->
                coaching = prefs.coaching
                if (prefs.coaching.speechEnabled) {
                    speechCoach.setRate(prefs.coaching.speechRate)
                    speechCoach.setPitch(prefs.coaching.speechPitch)
                }
            }
        }
        lifecycleScope.launch {
            speechCoach.state.collect { speechAvailable = it is SpeechState.Ready }
        }
    }

    /**
     * Hands one tick's worth of decisions to the speech engine and the tone generator.
     *
     * Deliberately not conditional on anything: every rule about what to say, when, and
     * whether at all lives in [CueScheduler], where it is tested. A condition added here would
     * be a rule with no test.
     */
    private fun deliver(batch: CueBatch) {
        if (batch.isEmpty) return
        batch.tone?.let(cueFeedback::play)
        batch.haptic?.let(cueFeedback::vibrate)
        batch.speech?.let(speechCoach::speak)
    }

    /**
     * Advances the coordinator several times a second and refreshes the notification once a
     * second.
     *
     * The interval is far shorter than the one-second display resolution on purpose: the
     * coordinator computes elapsed time from timestamps, so a fine interval costs almost
     * nothing and keeps the countdown from visibly stuttering when a tick lands late.
     */
    private fun startTicking() {
        if (ticker?.isActive == true) return
        ticker = lifecycleScope.launch(defaultDispatcher) {
            var lastNotified = 0L
            while (isActive) {
                val now = nowMillis()
                coordinator.tick(now)
                val state = coordinator.state.value
                if (state == null || state.isFinished) {
                    stopSelf()
                    return@launch
                }
                deliver(cueScheduler.onTick(state, now, coaching, speechAvailable))
                if (now - lastNotified >= NOTIFICATION_REFRESH_MILLIS) {
                    lastNotified = now
                    notificationManager().notify(NOTIFICATION_ID, buildNotification())
                }
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val state = coordinator.state.value
        val title = state?.workout?.title ?: "Workout"
        val text = state?.let(::describe) ?: "Getting ready"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())
            .addAction(
                android.R.drawable.ic_media_pause,
                if (state?.isPaused == true) "Resume" else "Pause",
                command(ACTION_PAUSE),
            )
            .addAction(android.R.drawable.ic_media_next, "Skip", command(ACTION_SKIP))
            .addAction(android.R.drawable.ic_delete, "End", command(ACTION_STOP))
            .build()
    }

    private fun describe(state: SessionState): String {
        val remaining = state.remainingInSegment.inWholeSeconds
        val clock = "%d:%02d".format(remaining / SECONDS_PER_MINUTE, remaining % SECONDS_PER_MINUTE)
        val name = state.currentSegment?.label ?: "Finishing"
        return if (state.isPaused) "Paused · $name" else "$clock · $name"
    }

    private fun command(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, WorkoutService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Reopens the app rather than a specific destination. The player is the app's current
     * screen whenever this notification exists, so a plain launch intent lands there — and
     * it keeps the service from needing to know about navigation.
     */
    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Workout in progress",
            // LOW, not DEFAULT: this notification is a status display and a set of
            // controls. It must never make a sound or vibrate — the coaching cues do that,
            // and a session that pings every segment boundary would be unusable.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the current segment while a session is running."
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun nowMillis(): Long = android.os.SystemClock.elapsedRealtime()

    companion object {
        const val ACTION_START = "com.visceralfit.action.START"
        const val ACTION_PAUSE = "com.visceralfit.action.PAUSE"
        const val ACTION_SKIP = "com.visceralfit.action.SKIP"
        const val ACTION_STOP = "com.visceralfit.action.STOP"

        private const val CHANNEL_ID = "workout_in_progress"
        private const val NOTIFICATION_ID = 1001

        /**
         * Sampled well below the one-second display resolution so a late tick does not
         * show as a stuttering countdown. Elapsed time comes from timestamps, so the
         * interval affects smoothness only, never accuracy.
         */
        private const val TICK_INTERVAL_MILLIS = 200L
        private const val NOTIFICATION_REFRESH_MILLIS = 1_000L
        private const val SECONDS_PER_MINUTE = 60

        fun start(context: Context) = send(context, ACTION_START)

        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            val intent = Intent(context, WorkoutService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

/** What to call a segment on screen and in the notification. */
internal val Segment.label: String
    get() = exercise?.name ?: when (kind) {
        com.visceralfit.domain.model.SegmentKind.TRANSITION -> "Change over"
        com.visceralfit.domain.model.SegmentKind.REST -> "Rest"
        com.visceralfit.domain.model.SegmentKind.ACTIVE_RECOVERY -> "Easy"
        com.visceralfit.domain.model.SegmentKind.WORK -> "Work"
    }

/** `Service.START_NOT_STICKY`, re-exported so the constant is not read off an instance. */
private const val START_NOT_STICKY = Service.START_NOT_STICKY

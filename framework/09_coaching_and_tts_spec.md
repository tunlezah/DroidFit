# 09 — Coaching and text-to-speech specification

Everything the app says out loud. Implementation: `core:speech` (the engine) and
`feature-workout` (the cue scheduler, phase 08).

The design principle throughout: **a cue is only useful on time.** Speech that arrives late
is worse than silence, because the user acts on it.

---

## 1. Cue types

Each maps to one `CoachingPreferences` flag (ADR-0007). All independently switchable, all
gated by the master `speechEnabled`.

| Cue | Flag | Default | When | Priority | Example |
|---|---|---|---|---|---|
| Next exercise | `announceNextExercise` | **on** | 5 s before the next work segment | TRANSITION | "Next up: seated climb." |
| Segment start | always (if speech on) | — | At segment start | TRANSITION | "Seated climb. Add resistance and drive through the whole pedal stroke." |
| Countdown | `announceCountdown` | on | 3, 2, 1 before a segment ends | TRANSITION | "Three. Two. One." |
| Halfway | `announceHalfway` | on | 50% of total session elapsed | INFORMATIONAL | "Halfway." |
| Remaining time | `announceRemainingTime` | off | Every 5 min, and at 1 min left | INFORMATIONAL | "10 minutes remaining." |
| Motivational | `motivationalPrompts` | off | Once per work segment, max every 90 s | INFORMATIONAL | "Strong. Hold this." |
| Rest countdown | `announceRestCountdown` | on | 3 s before rest ends | TRANSITION | "Back on in three, two, one." |
| Full instructions | `speakFullInstructions` | off | At segment start, after the short cue | INFORMATIONAL | The `how_to` steps in order |
| Safety | always | — | On session start if any exercise carries `cardiac_caution` | **CRITICAL** | "Stop if you feel chest pain, dizziness or unusual breathlessness." |

## 2. The "announce next exercise" cue

The most-praised competitor feature (R-0007) and an explicit operator request, so it gets its
own rules.

- Fires **5 s before** the upcoming work segment starts — enough to change position or
  resistance, short enough to still be current.
- If the current segment is shorter than 6 s, the cue is skipped entirely rather than
  overlapping the segment-start cue.
- During rest, it is combined with the rest countdown into one utterance rather than two:
  "Seated climb in three, two, one." Two separate utterances in the last 3 seconds of rest
  collide.
- If the next segment uses the same exercise as the current one (HIIT rounds), announce the
  round instead: "Round 3 of 6." Repeating the exercise name every round is noise.

## 3. Priority and contention

`SpeechCue.Priority`, resolved in `AndroidSpeechCoach`:

| Priority | Behaviour when something is already speaking |
|---|---|
| `CRITICAL` | Interrupts (`QUEUE_FLUSH`) |
| `TRANSITION` | Interrupts (`QUEUE_FLUSH`) |
| `INFORMATIONAL` | **Dropped.** Not queued |

Informational cues are dropped rather than queued because a queue means "halfway" gets spoken
during the next interval, where it is simply wrong. The alternative — a stale cue arriving
late — is the failure mode this design exists to prevent.

Every cue also carries `staleAfterMillis` (default 2,000 ms). A cue that cannot *start*
within that window is discarded.

## 4. Timing rules

- **Never two utterances inside 1.5 s.** The scheduler enforces a minimum gap; the lower
  priority one is dropped.
- **Countdown cues are spoken as one utterance** ("Three. Two. One."), not three, so
  engine latency cannot stretch them past the boundary.
- **Full instructions are interruptible.** If `speakFullInstructions` is on and the next
  transition cue arrives, the instructions stop mid-sentence. That is correct.
- **The halfway cue fires on session elapsed time, not segment count.** A user who paused for
  five minutes should still hear it at the true halfway point of *work*.

## 5. Cue text authoring

- Short. Under 12 words for anything spoken during work.
- Numerals, not words: TTS reads "10" correctly and locale-appropriately; "ten" is a
  hard-coded English choice.
- Avoid symbols. "eight to nine out of ten", never "8–9/10". Avoid abbreviations: "seconds",
  not "s".
- No emoji, no punctuation TTS reads aloud.
- `DurationFormat.spoken()` is the only correct way to speak a duration. It handles
  pluralisation. Do not build a string by hand.

## 6. Unavailability — a first-class state

`SpeechState.Unavailable` with four distinguished reasons (A-0005):

| Reason | Cause | User-facing message |
|---|---|---|
| `NO_ENGINE_INSTALLED` | No TTS engine on the device | "No text-to-speech engine found. Your workout will use tones and vibration instead." |
| `NO_VOICE_DATA_FOR_LOCALE` | Engine present, no voice for the locale | "No voice data installed for your language. You can add it in Android settings, or continue with tones and vibration." |
| `ENGINE_INIT_FAILED` | Init returned an error | "Spoken coaching is unavailable on this device right now." |
| `DISABLED_BY_USER` | `speechEnabled` is false | No message — this is a normal setting |

Rules:

- The notice is **non-blocking and dismissible.** It never gates starting a workout.
- With speech unavailable, **tones and haptics substitute**: a rising tone plus a long
  vibration at each work-segment start, a falling tone plus a short vibration at each rest
  start. These follow `cueTones` and `hapticCues`, which are independent of `speechEnabled`
  precisely so this fallback works.
- **Test this path deliberately.** Disable TTS in system settings and run a full session.
  It is the most likely real-world degradation and the easiest to forget.

## 7. Audio focus

The user is probably playing music.

- Request `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, not `AUDIOFOCUS_GAIN`. Cues duck the music
  briefly and hand focus straight back.
- Speak on `STREAM_MUSIC` / `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` so cues route to whatever
  the user is listening on, including Bluetooth headphones.
- **Never hold focus between cues.** Holding it pauses the user's music for the whole
  session, which is the single most annoying thing an app of this kind can do.
- On a phone call, stop speaking and do not resume mid-utterance.

## 8. Engine lifecycle

`AndroidSpeechCoach` creates the `TextToSpeech` instance **lazily on first `speak`**, never in
`init` — constructing one binds an IPC service and costs roughly 200 ms, which would land in
cold-start time for users who have speech switched off.

`shutdown()` must be called from the workout service's `onDestroy`. Leaking an engine leaks
the binding.

The init callback can, in principle, fire before the constructor returns. The implementation
handles both orderings via `pendingInitStatus` — do not "simplify" that away.

## 9. Exit criteria for phase 08

- [ ] Cue scheduler in `feature-workout`, driven by the session clock, unit-tested with a
      virtual clock — no real waiting in tests.
- [ ] Test: every cue type fires at its specified time.
- [ ] Test: priority resolution — an informational cue arriving during speech is dropped, a
      transition cue interrupts.
- [ ] Test: stale cues are discarded.
- [ ] Test: minimum 1.5 s gap enforced.
- [ ] Test: the rest countdown and next-exercise cue combine into one utterance.
- [ ] Test: halfway fires on active elapsed time, not wall-clock, across a paused session.
- [ ] Manual test, recorded in the phase log: full session with TTS disabled at OS level.
- [ ] Manual test: full session with music playing — music must resume immediately after each
      cue and never stay ducked.
- [ ] Every cue string passes the prohibited-claims check from `02_evidence_base.md` §6.
- [ ] `project_memory` updated.

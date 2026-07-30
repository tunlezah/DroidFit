# Phase 08 — Coaching

## Objective
Implement the cue scheduler so the app coaches out loud, correctly timed, with a working fallback
when speech is unavailable.

## Read first
- `framework/09_coaching_and_tts_spec.md` — all of it
- `core/speech/src/main/kotlin/**/AndroidSpeechCoach.kt` — built; understand its contention rules
- `domain/coaching/SpeechCoach.kt`

## Agents
**UX Research** (leads — timing is a UX problem). Accessibility. Fitness Science (cue content).

## Files you may touch
- `feature-workout/**` — the scheduler
- `core/speech/**`
- `project_memory/*`

## Work

### 1. The scheduler
In `feature-workout`, driven by the **session clock from the service**, not by a local timer.

**It must be testable with a virtual clock.** If a test needs a real `delay`, the time source is
not injected and the design is wrong. Every timing rule in the spec becomes a test.

### 2. Implement each cue type
The table in `09_coaching_and_tts_spec.md` §1. The non-obvious ones:

- **Next exercise** (§2) fires 5 s ahead; is skipped entirely if the current segment is under 6 s;
  **combines with the rest countdown into one utterance** rather than two colliding ones; and
  announces the round instead of the name when the exercise is unchanged across HIIT rounds.
- **Halfway** fires on **active elapsed time**, not wall-clock. A user who paused for five
  minutes should hear it at the true halfway point of work.
- **Countdown** is one utterance ("Three. Two. One."), not three, so engine latency cannot stretch
  it past the boundary.
- **Full instructions** are interruptible mid-sentence by a transition cue. That is correct.

### 3. Contention and staleness
Priority resolution is already in `AndroidSpeechCoach`: informational cues are **dropped**, not
queued. Enforce the 1.5 s minimum gap in the scheduler, and honour `staleAfterMillis`.

The reasoning matters: a queued "halfway" gets spoken during the next interval where it is simply
wrong. Dropping is the correct behaviour, not a limitation.

### 4. Tones and haptics
`cueTones` and `hapticCues` are independent of `speechEnabled` precisely so they can substitute
when speech is unavailable. A rising tone plus a long vibration at work start; a falling tone plus
a short vibration at rest start.

### 5. Audio focus
Spec §7. `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, never `AUDIOFOCUS_GAIN`. **Never hold focus between
cues** — holding it pauses the user's music for the whole session, which is the single most
annoying thing an app of this kind can do.

### 6. The unavailability path
Four reasons, four messages (spec §6). Non-blocking, dismissible, never gates starting a workout.

**Test it by actually disabling TTS in system settings and running a full session.** This is the
most likely real-world degradation (A-0005) and the easiest to forget.

## Exit criteria
- [ ] Scheduler driven by the service clock, unit-tested with a virtual clock — no real waiting in
      any test.
- [ ] Test per cue type: fires at its specified time.
- [ ] Test: informational cue during speech is dropped; transition cue interrupts.
- [ ] Test: stale cues discarded.
- [ ] Test: 1.5 s minimum gap enforced.
- [ ] Test: rest countdown and next-exercise combine into one utterance.
- [ ] Test: halfway fires on active elapsed time across a paused session.
- [ ] Test: HIIT rounds announce the round, not the repeated exercise name.
- [ ] Tones and haptics work with speech off.
- [ ] Every cue string passes the prohibited-claims check.
- [ ] **Manual:** full session with TTS disabled at OS level — recorded.
- [ ] **Manual:** full session with music playing; music resumes immediately after each cue and
      never stays ducked — recorded.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `decisions.md` — phase-log row; any timing constant you had to choose, the motivational cue
  wording (it is the most subjective content in the app).
- `assumptions.md` — A-0005 status.
- `known_issues.md` — any device-specific TTS behaviour you hit.

## Do not
- Sleep or use a real `delay` in a test.
- Queue informational cues.
- Hold audio focus between cues.
- Let the unavailability notice block starting a workout.
- Report the TTS-disabled manual test as passing without running it.

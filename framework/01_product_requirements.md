# 01 — Product requirements

Numbered requirements. Every phase, commit and test traces to one of these. If you are
about to build something that traces to nothing here, stop: either it is out of scope
(record it in `project_memory/future_features.md`) or a requirement is missing (add it
here with an entry in `project_memory/decisions.md`).

Notation: **MUST** = release blocker. **SHOULD** = expected, may be deferred with a
`technical_debt.md` entry. **MAY** = optional.

---

## Traceability to the original request

| Operator's words (UF-0001, UF-0002) | Requirements |
|---|---|
| "focus on excercise that will reduce visceral fat" | REQ-001..004, REQ-030..036 |
| "floor, reformer Pilates, elliptical or spin bike … allow selection of any of these (or removal)" | REQ-010, REQ-011 |
| "customisable time period" | REQ-012, REQ-013 |
| "heavily slanted towards a Motorola edge 60 … Android 15" | REQ-070..076 |
| "option to announce the exercise coming up" | REQ-051 |
| "research what are evidence backed exercises and pull them in locally" | REQ-001..004, REQ-040..047, REQ-060 |
| "describe both verbally and written how to perform that exercise properly" | REQ-041, REQ-044, REQ-053 |
| "best features and highest rated features from other apps" | REQ-020..023 |
| "keeping the screen on" | REQ-070 |
| "GitHub action that fully compile and outputs a working APK that can be sideloaded" | REQ-110..114 |
| "project memory directory structure is updated" | REQ-120..122 |

---

## 1. Evidence and claims

- **REQ-001 (MUST)** Every programming rule — style, intensity, duration, structure,
  progression — traces to a citation key in `framework/data/references.md`.
- **REQ-002 (MUST)** The app surfaces the WHO 2020 weekly activity range (150–300 min
  moderate, or 75–150 min vigorous) as the reference frame for its weekly goal, rather
  than inventing a target.
- **REQ-003 (MUST)** The app never estimates visceral fat, never implies spot reduction,
  and never claims a specific movement targets abdominal fat. Waist measurements are
  presented as a trend only, with that limit stated on screen.
- **REQ-004 (MUST)** Pilates content is positioned per R-0003: real body-composition and
  strength benefit, **no** demonstrated waist-circumference effect. No Pilates-only
  session is presented as a visceral-fat intervention comparable to vigorous aerobic work.
- **REQ-005 (MUST)** A safety notice is shown before the first session and must be
  acknowledged. It covers: stop on chest pain, dizziness or unusual breathlessness; consult
  a clinician if you have a condition or are new to vigorous exercise; this app is not
  medical advice.
- **REQ-006 (MUST)** Every exercise carries safety notes, and every vigorous exercise
  carries a stop-if-symptoms note.

## 2. Exercise types and duration

- **REQ-010 (MUST)** The user can independently enable or disable each of: floor Pilates,
  reformer Pilates, elliptical, spin bike. Adding a fifth modality must require no change
  outside `Modality`, the content asset and the MET table.
- **REQ-011 (MUST)** The last enabled modality cannot be disabled — an empty selection
  makes generation impossible. The UI explains the refusal rather than silently ignoring
  the tap.
- **REQ-012 (MUST)** Duration presets: 5, 10, 15, 20, 30, 45, 60, 90 minutes, plus a
  custom value.
- **REQ-013 (MUST)** Custom duration accepts 3–120 minutes in 1-minute steps (A-0003).
  Values outside the range are rejected with an explanation, not clamped silently.
- **REQ-014 (MUST)** Equipment availability is tracked separately from modality
  preference. An enabled modality whose equipment is unavailable is excluded from
  generation, and the UI says so (D-0004).
- **REQ-015 (MUST)** Three experience levels: beginner, intermediate, advanced. The app
  never auto-promotes a user to a higher level; escalation is an explicit user action
  (A-0007).

## 3. Competitive and UX requirements

Derived from R-0007. These are UX requirements, never programming requirements.

- **REQ-020 (MUST)** No account, no login, no subscription, no network. These structurally
  eliminate the three largest complaint categories in the competitor sample.
- **REQ-021 (MUST)** The countdown is legible from ~1 m at arm's length in the default
  layout, and from ~2 m in machine mode.
- **REQ-022 (MUST)** Work, rest, active-recovery and transition states are visually
  distinct by colour **and** by at least one non-colour signal (label, ring weight or
  icon).
- **REQ-023 (SHOULD)** Session configurations can be saved and re-run without
  reconfiguring.
- **REQ-024 (MUST)** No disabled control without a visible reason. A dead button with no
  explanation is a defect.

## 4. Workout engine

Specified in full in `framework/07_workout_engine_spec.md`.

- **REQ-030 (MUST)** A generated `Workout` is immutable. Edits produce a new instance.
- **REQ-031 (MUST)** Generation is deterministic: the same request and seed produce an
  identical plan.
- **REQ-032 (MUST)** Generated duration is within ±30 s of the request.
- **REQ-033 (MUST)** Every session has a warm-up, a main block and a cool-down, with
  style-specific minimum warm-up durations.
- **REQ-034 (MUST)** Exercises used in recent sessions are de-prioritised, so consecutive
  sessions differ.
- **REQ-035 (MUST)** No segment may use an exercise outside the requested modalities,
  above the requested level, or carrying a tag in the user's avoid list.
- **REQ-036 (MUST)** Four styles: HIIT, Zone 2, Mixed, Recovery, each with its documented
  intensity architecture.
- **REQ-037 (MUST)** Generation failure is reported to the user with the specific reason
  and a suggested fix. It is never a silent no-op or a crash.

## 5. Exercise content

Specified in `framework/08_exercise_library_spec.md`.

- **REQ-040 (MUST)** Every exercise has: name, modality, difficulty, MET value, ordered
  how-to steps, a one-line spoken cue, safety notes, common mistakes, muscles worked, an
  illustration id, and evidence keys.
- **REQ-041 (MUST)** Written instructions are complete enough to perform the movement
  correctly with no video.
- **REQ-042 (MUST)** MET values come from the 2024 Adult Compendium, with the code
  recorded. Any approximation is documented as an assumption.
- **REQ-043 (MUST)** Content is bundled in the APK. Nothing is downloaded, ever.
- **REQ-044 (MUST)** The spoken cue is short enough to finish inside the shortest interval
  the exercise can appear in.
- **REQ-045 (MUST)** Contraindication tags allow a user to exclude movements (for example
  `lower_back`, `knee`, `shoulder`, `neck`, `wrist`, `hip`, `cardiac_caution`).
- **REQ-046 (MUST)** Each exercise has an illustration or the neutral placeholder. A
  missing drawing never blocks a workout.
- **REQ-047 (MUST)** Content is updatable by shipping a higher seed version, with no
  schema migration and no loss of user history.

## 6. Coaching

Specified in `framework/09_coaching_and_tts_spec.md`.

- **REQ-050 (MUST)** Independently switchable: announce next exercise, countdown, halfway
  reminder, remaining time, motivational prompts, rest countdown. Plus a master speech
  toggle, and a separate toggle for reading full technique cues aloud (ADR-0007).
- **REQ-051 (MUST)** "Announce the next exercise" names the upcoming movement **before**
  it starts, with enough lead time to act on it. Default on.
- **REQ-052 (MUST)** Cue priority: critical cues interrupt; transition cues interrupt
  informational ones; informational cues are dropped under contention.
- **REQ-053 (MUST)** Technique cues can be read aloud in full, on request.
- **REQ-054 (MUST)** A cue that cannot be spoken on time is dropped, not spoken late.
- **REQ-055 (MUST)** With no TTS engine or no voice data, the app stays fully usable:
  a non-blocking notice, and tones plus haptics in place of speech.
- **REQ-056 (SHOULD)** Speech rate and pitch are adjustable.
- **REQ-057 (MUST)** Coaching audio does not permanently duck or steal focus from the
  user's music; cues mix over it.

## 7. Display, device and platform

Specified in `framework/15_device_targets_motorola_edge_60.md`.

- **REQ-070 (MUST)** Keep-screen-on during workouts, user-toggleable, taking effect
  immediately when changed mid-session.
- **REQ-071 (MUST)** Material 3, dynamic colour supported, with an AMOLED true-black dark
  mode.
- **REQ-072 (MUST)** Edge-to-edge, correct inset handling on a curved-edge display.
- **REQ-073 (MUST)** Portrait and landscape both work. Rotation loses no session state.
- **REQ-074 (MUST)** Session state survives activity recreation and process death
  (ADR-0008).
- **REQ-075 (SHOULD)** Machine mode: oversized timer and controls for a phone in a cradle.
- **REQ-076 (SHOULD)** Smooth at the panel's 120 Hz; no dropped frames during the timer.

## 8. Tracking and progress

- **REQ-080 (MUST)** Workout history with date, duration, style, modalities.
- **REQ-081 (MUST)** Estimated energy expenditure, labelled "estimated", shown as `—`
  when body mass is unknown. Never a fabricated figure (D-0005).
- **REQ-082 (MUST)** Weekly and monthly minute totals against the user's goal.
- **REQ-083 (MUST)** Streaks, counting only sessions past the completion threshold.
- **REQ-084 (SHOULD)** Calendar view.
- **REQ-085 (SHOULD)** Optional body mass, waist, hip, chest and thigh measurements.
- **REQ-086 (SHOULD)** Progress photos in app-private storage, excluded from backup, with
  an optional biometric gate. Never written to MediaStore or any shared location.
- **REQ-087 (SHOULD)** Personal bests, achievements, favourites, custom workouts, an
  interval builder.
- **REQ-088 (SHOULD)** Recovery recommendation based on recent training load.
- **REQ-089 (SHOULD)** Local backup and restore, user-initiated, to a location the user
  chooses.
- **REQ-090 (MUST)** No visceral fat estimate, no body fat percentage estimate, no
  bioimpedance-style derived figure.

## 9. Architecture, privacy, quality

- **REQ-100 (MUST)** Kotlin, Compose, Material 3, MVVM, Clean Architecture, repository
  pattern, Hilt, Room, DataStore, coroutines, Flow, Navigation Compose (ADR-0001).
- **REQ-101 (MUST)** No `INTERNET` permission (D-0009).
- **REQ-102 (MUST)** No analytics, no crash reporting to a third party, no advertising id.
- **REQ-103 (MUST)** No user data leaves the device except through an explicit,
  user-initiated export.
- **REQ-104 (MUST)** Future integrations are architected behind interfaces but not
  required (ADR-0006).
- **REQ-105 (MUST)** `domain` has no Android dependency.
- **REQ-106 (MUST)** No destructive database migration, ever.

## 10. Build and delivery

Specified in `framework/14_ci_cd_and_release.md`.

- **REQ-110 (MUST)** GitHub Actions builds the app on every push and pull request.
- **REQ-111 (MUST)** CI runs unit tests and static analysis (detekt including ktlint
  rules) and fails on either.
- **REQ-112 (MUST)** CI produces a debug-signed APK, uploaded as an artifact, installable
  by sideload with no store involvement (ADR-0002).
- **REQ-113 (MUST)** CI verifies the artefact is a real APK (contains a manifest and dex),
  not merely that a file exists.
- **REQ-114 (SHOULD)** Instrumentation tests run on an emulator on the default branch and
  on manual dispatch.

## 11. Process

- **REQ-120 (MUST)** `/project_memory`'s eight files are updated in the same commit as the
  work they describe.
- **REQ-121 (MUST)** Project memory is append-only. Superseding an entry means adding a
  new one, not editing the old one.
- **REQ-122 (MUST)** No phase is complete until its exit criteria pass **and** its memory
  entries exist.
- **REQ-123 (MUST)** Every deviation from this document is recorded in
  `project_memory/decisions.md` before the deviating code is committed.

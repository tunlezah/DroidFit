# Known issues

Bugs, gaps and blockers that are **not yet fixed**. Template:
`_templates/entry_templates.md`. Numbering: `KI-NNNN`.

Distinct from `technical_debt.md`: debt is a deliberate trade-off, an issue is something
wrong. If you are unsure which file an entry belongs in, ask "did we choose this?" — if
yes it is debt, if no it is an issue.

**Close entries, do not delete them.** Set `Status: fixed in <commit>` so the history of
what went wrong survives.

---

### KI-0001 — Workout generation is not implemented
- **Date:** 2026-07-30
- **Severity:** blocker
- **Area:** `domain/engine`
- **Symptom:** The Start button on the Train screen is disabled and labelled
  "workout engine arrives in phase 06". No workout can be performed.
- **Reproduction:** Launch the app, open Train, observe the disabled button.
- **Cause:** By design — `WorkoutGenerator` is an interface with no implementation.
  The algorithm is fully specified in `framework/07_workout_engine_spec.md`.
- **Workaround:** None. This is the app's core function.
- **Status:** **fixed in phase 06.** `DefaultWorkoutGenerator` implements the specification;
  `GenerateWorkout` loads the catalogue and delegates to it, and the Start button generates a real
  session and shows the plan. Verified by a golden-file test that reproduces engine spec §9 row for
  row *including its exercise choices*, plus determinism, duration-fit, structure and eligibility
  invariants asserted across 1,680 requests. Three programming faults the invariants did **not**
  catch were found by reading the output; see D-0027.

### KI-0002 — Weekly minutes always reads zero on the Progress screen
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** `data/repository`, `feature-progress`
- **Symptom:** The "This week" card shows `0 of 150 minutes` regardless of completed
  sessions, so the progress bar never moves.
- **Reproduction:** Record any session, open Progress, observe 0.
- **Cause:** `DefaultHistoryRepository.observeWeeklyLoad` returns `flowOf(emptyList())`
  (see TD-0007), and `ProgressViewModel` hard-codes `minutesThisWeek = 0`.
- **Workaround:** History lists individual sessions correctly.
- **Status:** **fixed in phase 09.** `observeWeeklyLoad` returns one summary per ISO week, most
  recent first, bounded by local midnights. The Progress screen shows real minutes, the completed-
  session count, vigorous minutes and — when there is something to say — rest-day advice.
  `WeeklyLoadTest` covers it, and its first assertion is simply that the function is not a constant,
  because that is the shape the bug had.

### KI-0003 — 12 of 14 seeded exercises have no illustration
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `core:designsystem/illustration`
- **Symptom:** Most exercises render the neutral placeholder figure instead of a diagram.
- **Reproduction:** Any exercise whose `illustrationId` is not `pilates_dead_bug` or
  `spin_seated`.
- **Cause:** Only two drawings have been authored. The harness, the coordinate helpers
  and the fallback all work.
- **Workaround:** The placeholder is deliberately plain so it reads as "no diagram"
  rather than as a wrong diagram, and written plus spoken cues are complete for every
  exercise.
- **Status:** open — phase 03

### KI-0004 — Exercise catalogue is 14 exercises, not the ~60 the engine needs
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** `app/src/main/assets/exercises_seed.json`
- **Symptom:** Reformer has 2 exercises and elliptical has 2. The generator's variety
  invariant (no exercise repeated within a session unless the structure requires it) will
  fail for single-modality sessions longer than a few minutes.
- **Reproduction:** Count entries per modality in the seed asset.
- **Cause:** The seed is a worked example demonstrating the schema and the authoring
  standard, not the finished catalogue.
- **Workaround:** Multi-modality sessions have more to draw on.
- **Status:** **fixed in phase 02.** 65 exercises: floor Pilates 28, spin bike 13, reformer 13,
  elliptical 11 — above every minimum in `framework/08_exercise_library_spec.md` §1, including the
  binding one, four untagged exercises per modality. The counts are now asserted by
  `ExerciseCatalogueValidationTest` and `check_framework_data.py` rather than counted by hand
  (D-0019).

### KI-0005 — No onboarding flow, so the safety notice is never shown
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** `app`, `feature-workout`
- **Symptom:** `UserPreferences.safetyNoticeAcknowledged` exists and defaults to false,
  but nothing reads it, so no safety notice is ever presented.
- **Reproduction:** Fresh install; no notice appears.
- **Cause:** The onboarding flow is phase 07 work.
- **Workaround:** None.
- **Status:** **fixed in phase 07.** `SafetyNoticeScreen` gates the whole shell until
  `safetyNoticeAcknowledged` is set (D-0029). Covers all three points REQ-005 enumerates: stop on
  chest pain, dizziness or unusual breathlessness; consult a clinician if you have a condition or
  are new to vigorous exercise; this app is not medical advice. A-0007 remains an open assumption —
  the notice is its mitigation, not its answer.

### KI-0006 — Nothing verifies the seed asset at build time
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `data/seed`, build
- **Symptom:** A malformed or incomplete `exercises_seed.json` is only detected at
  runtime, where it is logged and the catalogue silently stays empty.
- **Reproduction:** Break a field name in the asset and launch; the app runs with no
  exercises.
- **Cause:** The validation test described in
  `framework/08_exercise_library_spec.md` §Validation has not been written.
- **Workaround:** `ignoreUnknownKeys = false` at least makes the failure loud in logcat.
- **Status:** **fixed in phase 02.** `app/src/test/.../ExerciseCatalogueValidationTest` is a plain
  JVM test over the shipped asset — wired onto the unit-test classpath by `app/build.gradle.kts`
  rather than copied, so it validates the real file. Seventeen assertions covering §5's list:
  parse with `ignoreUnknownKeys = false`, id shape and uniqueness, enum resolution, MET plausibility,
  instruction completeness, the two-tier spoken-cue word limit, stop-if-symptoms notes on vigorous
  work, the closed caution-tag set, evidence keys parsed out of `references.md`, prohibited-claim
  regexes, no rep counts, and every count and balance minimum.

### KI-0007 — Room migration test infrastructure does not exist yet
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `core:database`
- **Symptom:** The database is at version 1 with schema export enabled, but there is no
  `MigrationTestHelper` harness, so the first migration will be written without a
  safety net.
- **Reproduction:** N/A — absence of tests.
- **Cause:** No migration exists yet, so there is nothing to test.
- **Workaround:** None needed until the schema changes.
- **Status:** open — must be built **before** the first schema change, not after. See
  `framework/06_data_model.md` §Migrations.
- **Update 2026-07-30 (phase 06):** phase 06's prompt asks for the harness to be built in this
  phase. It was **not**, and deliberately: the phase turned out to need no schema change at all.
  The intensity anchor — the one thing that looked like it needed a new column — is a property of
  the `(modality, MET)` pair rather than of an exercise, so it became a lookup table in `:domain`
  instead (D-0020). Building an instrumented `MigrationTestHelper` harness here would have been
  unverifiable in this environment (no emulator; see KI-0008) and would have tested nothing, since
  there is still no migration. The constraint stands unchanged: the harness must exist before the
  first schema change, which phase 09's `session_exercises` table (KI-0012) will be.

### KI-0008 — Instrumentation tests have never been executed
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** CI, `app/src/androidTest`
- **Symptom:** The emulator job in `android-ci.yml` is written but unverified; there are
  no instrumentation tests to run yet, and `VisceralFitTestRunner` has never started.
- **Reproduction:** N/A.
- **Cause:** No emulator was available in the authoring environment. The unit-test,
  detekt and APK jobs were verified locally; the emulator job was not.
- **Workaround:** The job is gated to the default branch and manual dispatch, so if it is
  misconfigured it cannot block a PR.
- **Update 2026-07-30:** the job **did** start on run 30533162151 (this branch is the repository's
  default, so the gate matched). It got through KVM setup and ran `connectedDebugAndroidTest` for
  roughly 25 minutes before being **cancelled** by the next push — `cancel-in-progress: true` in the
  workflow's concurrency group. So the setup steps (checkout, JDK, Gradle, KVM) are confirmed
  working; whether the emulator boots and `connectedDebugAndroidTest` completes is **still
  unverified**.
- **Status:** open. To settle it: trigger the workflow manually (`workflow_dispatch`) and let it run
  without pushing to the branch. Expect it to pass trivially at present — there are no
  instrumentation tests yet — which is exactly what makes it a clean check of the emulator setup
  before phase 06 depends on it.

### KI-0009 — CI's unit-test step originally skipped the entire :domain test suite
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** CI, root `build.gradle.kts`
- **Symptom:** `./gradlew testDebugUnitTest` reported success while running **zero** domain tests.
- **Reproduction:** was `./gradlew testDebugUnitTest` and inspect which `:test` tasks executed —
  `:domain:test` was absent.
- **Cause:** `:domain` is a pure Kotlin/JVM module, so its test task is `test`, not
  `testDebugUnitTest`. Gradle runs a named task only in projects that have it, and reports success
  when some projects do — so the gap is completely invisible in the log.
- **Why this mattered more than it looks:** `:domain` is where the workout engine and all
  programming rules live, and phase 06's property and golden-file tests are the most important
  tests in the project. They would have been written, passed locally, and then never run in CI.
- **Fix:** `qualityCheck` now enumerates the correct test task per module type, and the workflow
  calls `qualityCheck` rather than `testDebugUnitTest`. Verified by checking `:domain:test` appears
  in the executed task list.
- **Status:** fixed in the framework commit. **Recorded rather than silently corrected** because
  the same trap catches any future pure-JVM module — if you add one, add it to the aggregate.

### KI-0010 — Four of six HIIT interval durations in the engine spec were arithmetically wrong
- **Date:** 2026-07-30
- **Severity:** major (in the specification, not the code)
- **Area:** `framework/07_workout_engine_spec.md` §4.2, `framework/data/workout_templates.json`
- **Symptom:** The `main_seconds_needed` column stated 1470 / 1080 / 870 / 510 for the `5x3`,
  `8x1`, `10x30s` and `6x30s` templates; the correct values are 1500 / 1110 / 840 / 480.
- **Reproduction:** `rounds × work + (rounds − 1) × recovery` for each row.
- **Cause:** Hand-computed while authoring, with the `(rounds − 1)` on the recovery term applied
  inconsistently.
- **Why it mattered:** the spec is written to be implemented literally, and the phase-06 exit
  criteria tell the implementer to use the numbers given. They would have produced HIIT sessions
  30–60 s off the requested duration and then failed the ±30 s duration-fit property test, with the
  failure appearing to be in the implementation rather than in the spec. A `4x4`/`5x3` tie at
  1500 s also emerged, which the spec now resolves explicitly in favour of the evidence-backed
  `4x4`.
- **Fix:** values corrected in both files; the spec now shows the arithmetic inline rather than
  just the result. `scripts/check_framework_data.py` added, which recomputes every template and
  also checks template ordering, the style budget sums, MET-table coverage and citation keys —
  24 checks, run in CI.
- **Status:** fixed. Recorded because it is the clearest evidence in this project that **a
  specification needs tests too**: four wrong numbers sat in a document that reads as
  authoritative, and only recomputing them found it.

### KI-0011 — The seed catalogue violated the authoring standard the same document defines
- **Date:** 2026-07-30
- **Severity:** major (specification/content inconsistency)
- **Area:** `app/src/main/assets/exercises_seed.json`,
  `framework/08_exercise_library_spec.md`
- **Symptom:** The 14 worked examples — presented to the building agent as "match their depth" —
  broke two of the spec's own rules:
  1. Eleven of fourteen `spoken_instruction` values exceeded the stated 14-word limit (up to 21
     words).
  2. Three of the four exercises at `met_value >= 8.0` had **no stop-if-symptoms safety note**,
     which REQ-006 requires: `spin_bike_seated_flat` (9.0), `spin_bike_seated_climb` (10.8),
     `spin_bike_sprint` (12.5).
- **Cause:** The rules and the examples were authored separately and never cross-checked. The
  14-word limit was also derived loosely: at a typical TTS rate of ~150 wpm it corresponds to
  ~5.5 s, which is right for a 30 s HIIT interval but needlessly tight for a movement that only
  ever appears in a multi-minute block.
- **Why it mattered:** the seed is the *exemplar*. An agent told to match examples that contradict
  the rules will follow the examples, and would have propagated both faults across ~40 more
  exercises — including the missing safety notes on vigorous work, which is the one category where
  the omission has a physical consequence.
- **Fix:** two-tier word limit, which is what the reasoning actually supports — **≤14 words when
  `met_value >= 8.0`** (30 s intervals), **≤20 words otherwise**. Over-long cues trimmed, the three
  missing safety notes added, catalogue version bumped to 2. Both rules are now enforced by
  `scripts/check_framework_data.py` (26 checks) rather than only stated in prose.
- **Status:** fixed. Recorded because the general lesson applies to every phase: **an exemplar that
  contradicts its own rules is followed in preference to the rules.** Phase 02 must run the data
  check after each authoring batch, not once at the end.

### KI-0012 — Exercise variety is not carried across sessions
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `feature-workout`, `core:database`
- **Symptom:** Two sessions generated back to back can use the same movements. The generator's
  `recentExerciseIds` parameter works and is tested, but the app always passes an empty list.
- **Reproduction:** Generate a 20-minute mixed session twice with the same settings; the base and
  surge exercises are drawn from the same pool with no memory between them.
- **Cause:** `CompletedSession` records a session's title, style and modalities, but not which
  exercises it used. There is nothing honest to pass, so `WorkoutHomeViewModel.recentExerciseIds()`
  returns an empty list rather than approximating from the title.
- **Workaround:** Variety within a session is unaffected — that is the invariant that matters most,
  and it is tested.
- **Status:** open — phase 09. Needs a `session_exercises` join table, which is a schema change and
  therefore needs KI-0007's migration harness first.

### KI-0013 — The engine will build a 20-round interval session if asked
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `domain/engine`
- **Symptom:** A 120-minute HIIT request produces a structurally valid session of up to 20 four-
  minute intervals at 85–95% HRmax. Every invariant passes. No competent coach would prescribe it.
- **Reproduction:** generate 120 minutes, HIIT, any modality set.
- **Cause:** The PRD accepts 3–120 minutes for every style (A-0003), and nothing rejects a
  duration that is *possible* but not *advisable*. D-0026 made the output coherent; it did not make
  it sensible. Before that change the same request produced 77 minutes of active recovery, which
  was worse.
- **Workaround:** none needed today — nothing in the UI suggests a two-hour interval session, and
  the duration presets stop at 90 minutes.
- **Status:** open. The right fix is in the recovery recommender (engine spec §8), which should warn
  before generating rather than have the engine silently refuse: an upper bound on *vigorous* minutes
  per session, surfaced as advice with a reason. Phase 09.

### KI-0014 — Three engine faults passed every invariant test
- **Date:** 2026-07-30
- **Severity:** major (process, not code — the faults themselves are fixed)
- **Area:** `domain/engine`, testing strategy
- **Symptom:** The first plan the engine generated was structurally perfect and prescriptively
  wrong in three places: a vigorous 8.8 MET interval used as a Zone 2 base, the easy flat road
  eligible as a HIIT work interval, and a static chest-opener stretch as the first warm-up segment.
  Determinism, duration fit, structure and eligibility all passed.
- **Cause:** Every invariant in engine spec §1 is a *structural* property. None of them asks whether
  the session is good programming, because that question cannot be phrased as an assertion over
  durations and set membership.
- **Fix:** the faults are fixed (D-0027) and the pool rules now use the Compendium intensity anchor.
- **Why it stays open as an issue:** the *gap* is not fixed. There is still no test that would catch
  a fourth fault of the same kind. The golden file helps — it is the one artefact a human reads —
  but it pins one request out of a very large space.
- **Update 2026-07-30:** suggestion (1) is **done**. `WorkoutGeneratorInvariantTest` now asserts
  that no segment prescribes an exercise more than one intensity band from its Compendium anchor,
  across the full request matrix. It found a fourth fault of the same kind on its first run — a
  recovery session prescribing slow mountain climbers (7.0 MET, threshold) as easy work — which is
  fixed in D-0038. That is the strongest possible argument for the check: it was written to cover
  faults already found, and immediately caught one that nothing else had.
- **Status:** open, narrowed. What remains is suggestion (2): the Fitness Science sign-off in phase
  06's exit criteria — a human reading generated sessions at each style and several durations.
  **That has not been done, and it is an exit criterion this phase has not met.** The mechanical
  check covers intensity mismatches; it cannot tell whether a session is well *programmed*.

### KI-0020 — Two authored floor exercises are unreachable by the generator
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `domain/model/Modality`, `app/src/main/assets/exercises_seed.json`
- **Symptom:** `floor_pilates_mountain_climber_slow` (7.0 MET) and `floor_pilates_star_jumps` (7.5)
  can never be selected. They are authored, validated and shipped, and no session will ever use
  them.
- **Cause:** They are not machine cardio, so they are outside the cardio, vigorous, threshold and
  steady pools. They are above the mobility ceiling, so they are outside that pool. And D-0038 now
  excludes them from the recovery block's strength pool, because a recovery block prescribes
  everything at RECOVERY intensity and these are threshold and vigorous work.
- **The real cause underneath:** `Modality.FLOOR_PILATES` conflates two different things — mat
  Pilates, which is strength and control, and bodyweight cardio, which is aerobic work. Star jumps
  at 7.5 MET are aerobic exercise that `chen2024nma` supports; calling them Pilates is what makes
  them unusable, because D-0023 requires Pilates-only sessions to be built as recovery.
- **Two candidate fixes, and this is an operator question rather than a guess:**
  1. Add a fifth modality (bodyweight cardio). Then a user with no machine can be given genuine
     intervals, and the Pilates honesty rule still holds because Pilates stays Pilates.
  2. Withdraw the two exercises. Cheaper, and loses the only route to vigorous work for a user with
     no equipment at all.
- **Recommendation:** option 1, because "no equipment" is a common case and currently gets recovery
  sessions only. It is not a small change — `Modality` is documented as the single extension point
  and adding one requires MET values, 12 exercises and a decision entry.
- **Status:** **fixed 2026-07-31 (D-0039), and by a better route than either option offered.** The
  operator's answer was that no fifth category was needed: the existing one had simply been named
  wrongly. `FLOOR_PILATES` is now `BODYWEIGHT`, aerobic capability is a property of the modality
  (false only for the reformer), and seven bodyweight cardio exercises were added across all three
  levels. The two stranded movements are reachable, and a user with no equipment at all now gets
  genuine interval sessions instead of recovery only.
- **Lesson worth keeping:** the analysis in this entry framed the choice as "add a category or delete
  the content", and both were worse than the option it did not consider — that the *existing* name
  was wrong. A category name that encodes an evidence constraint is doing two jobs, and it was the
  wrong name for one of them.

### KI-0015 — The safety notice cannot be read again from Settings
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `feature-settings`, `app/ui`
- **Symptom:** The notice's own copy says "You can read this notice again at any time from
  Settings". There is no such entry in Settings.
- **Reproduction:** acknowledge the notice, open Settings, look for it.
- **Cause:** The notice lives in `app/ui` and Settings is a feature module, so re-showing it needs
  either a route or the screen moved somewhere both can reach.
- **Why it is recorded rather than fixed by deleting the sentence:** the sentence describes the
  behaviour the app *should* have. A user who wants to re-read a medical safety notice should be
  able to. Deleting the promise would be the wrong repair.
- **Status:** open. Fix with the rest of the Settings work; a `notice` route in the app graph that
  Settings navigates to is enough.

### KI-0016 — POST_NOTIFICATIONS is never requested, so the session notification may not appear
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** `app`, `feature-workout/player`
- **Symptom:** On Android 13 and above the workout notification — which carries the pause, skip and
  end controls — will not be shown unless the user has separately granted notifications. Nothing
  asks.
- **Reproduction:** fresh install on API 33+, start a session, background the app. The service runs
  and the clock stays correct, but there is no notification and therefore no lock-screen controls.
- **Cause:** The permission is declared in the manifest but never requested at runtime. Foreground
  services still start without it; only the notification is suppressed.
- **Impact:** the session itself is unaffected — this is the one saving grace. The clock, the
  service and the recording all work; the user simply has to reopen the app to control the session.
- **Status:** open. Needs a rationale-then-request flow at the point the first session starts, which
  is where the permission's purpose is obvious, rather than at launch.

### KI-0017 — The player has never run on a device or emulator
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** `feature-workout/player`, CI
- **Symptom:** Everything about the player that can be tested on the JVM is tested — the clock has
  twelve tests covering boundary crossing, stalls, pause accounting, skips, backwards clocks and
  completion. Nothing about it has been *seen*: not the foreground service starting, not the
  notification, not the countdown rendering, not `KeepScreenOn`, not rotation mid-session.
- **Cause:** No emulator in the authoring environment (the same cause as KI-0008), and the
  instrumentation CI job is gated to the default branch.
- **What this means concretely:** the parts that could be wrong and would not show up are the
  service lifecycle (does `startForeground` succeed with the declared type?), the manifest merge of
  the service declaration, and whether the session survives an activity recreation in practice
  rather than in principle.
- **Status:** open, and it should be closed before this is called done. Cheapest route: dispatch the
  existing `instrumentation` workflow job manually against this branch, then add an instrumentation
  test that starts a session, rotates, and asserts the remaining time did not jump.

### KI-0018 — No spoken coaching, and no landscape layout
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `feature-workout/player`, `core:speech`
- **Symptom:** The player is silent. `SpeechCoach` and its Android implementation exist and are
  complete, but nothing calls them, so a user on a bike must look at the phone to know a segment
  changed. Landscape (REQ-073) renders the portrait layout in a scrolling column.
- **Cause:** Both are phase 08 and phase 13 work respectively. Recorded here because the player is
  usable without them and it would be easy to mistake "the player is done" for "phase 07 is done".
- **Status:** open — phase 08 (cues) and phase 13 (landscape). Machine mode *is* implemented: the
  countdown scales to 148 sp and the technique block is dropped.

### KI-0019 — Week boundaries are fixed when the flow is collected
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `data/repository`
- **Symptom:** A Progress screen left open across midnight on a Sunday keeps showing the previous
  week until the flow is re-collected — which happens on any navigation away and back, so it needs
  the screen to be genuinely left open across the boundary.
- **Cause:** `observeWeeklyLoad` reads the clock once, at collection, to compute the week start and
  the query range.
- **Why it is recorded rather than fixed:** the fix is a flow that re-emits on a date change, which
  means either a ticking clock or a broadcast receiver for `ACTION_DATE_CHANGED`. Both are real
  machinery for a case that resolves itself the moment the user touches the app.
- **Status:** open, low priority. Fix with `ACTION_DATE_CHANGED` if it ever matters; do not add a
  polling clock.

### KI-0021 — The waist screen does not say where to measure
- **Date:** 2026-07-30
- **Severity:** minor
- **Area:** `feature-progress`
- **Symptom:** Nothing on screen tells the user which anatomical site to measure at, so the
  same person can silently switch sites between measurements and read the difference as a
  trend.
- **Why it matters more than it looks:** the app's stated position is that waist is a *trend*
  indicator with roughly ±1 cm of self-measurement error (A-0008). Switching between the navel
  and the WHO site (midpoint between lowest rib and iliac crest) moves the reading by several
  centimetres — far more than the 1 cm threshold the app uses to decide whether to call
  something a change. So a site switch manufactures a trend that is not there, which is the
  exact failure the threshold exists to prevent.
- **What the operator does today (UF-0006):** measures manually at the navel. That is a valid
  site; consistency matters far more than which one.
- **Fix:** state the site next to the input, and record it with the measurement so a future
  change of site is visible rather than invisible. Recording it is the important half.
- **Status:** open. Phase 10 owns the measurement UI.

### KI-0022 — The reformer's evidence constraint is now enforced in one place, and untested at the boundary
- **Date:** 2026-07-31
- **Severity:** minor
- **Area:** `domain/model/Modality`, `domain/engine`
- **Symptom:** `Modality.supportsAerobicWork` returning false for `REFORMER_PILATES` is the single
  thing standing between the app and presenting reformer work as cardio (REQ-004). Nothing asserts
  that a *newly added* modality gets a deliberate answer for it.
- **Why it is worth an entry:** the property defaults to true — it is written as
  `this != REFORMER_PILATES`. A fifth modality added tomorrow is aerobic-capable by default and
  silently. For a modality like rowing that is correct; for mat Pilates it would be a health-claim
  regression, and no test would fail.
- **Fix:** invert the default so the property is an explicit per-entry constructor argument rather
  than a derived expression, which makes a new modality unable to compile without answering the
  question. `Modality`'s own documentation already lists what adding one requires; this belongs on
  that list.
- **Status:** open, small. Do it the next time `Modality` is touched.

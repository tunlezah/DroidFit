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
- **Status:** open — phase 06

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
- **Status:** open — phase 09. **Must not ship in this state**: a number that is wrong is
  worse than a number that is absent.

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
- **Status:** open — phase 02. Target counts are in
  `framework/08_exercise_library_spec.md`.

### KI-0005 — No onboarding flow, so the safety notice is never shown
- **Date:** 2026-07-30
- **Severity:** major
- **Area:** `app`, `feature-workout`
- **Symptom:** `UserPreferences.safetyNoticeAcknowledged` exists and defaults to false,
  but nothing reads it, so no safety notice is ever presented.
- **Reproduction:** Fresh install; no notice appears.
- **Cause:** The onboarding flow is phase 07 work.
- **Workaround:** None.
- **Status:** open — phase 07. This is the mitigation for A-0007 and is a release
  blocker.

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
- **Status:** open — phase 02

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
- **Status:** open — verify on the first `workflow_dispatch` run.

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

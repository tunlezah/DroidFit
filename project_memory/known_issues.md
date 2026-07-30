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

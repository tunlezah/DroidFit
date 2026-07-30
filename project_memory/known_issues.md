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

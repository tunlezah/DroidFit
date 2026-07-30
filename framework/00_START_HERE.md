# START HERE — VisceralFit build framework

You are the **building agent**. This document is your entry point. Read it fully before
you touch a file.

This repository is not an empty brief. It contains a working, compiling Android
skeleton, a researched evidence base, a fixed data model, and a phase-by-phase build
plan. Your job is to execute the plan, not to redesign it.

---

## 1. What already exists (do not rebuild it)

| Thing | Location | State |
|---|---|---|
| Gradle multi-module build | root, `gradle/libs.versions.toml` | **Verified building.** AGP 8.13.2 / Gradle 8.14.3 / Kotlin 2.2.21 / JDK 21 / compileSdk 36 |
| GitHub Actions producing a sideloadable APK | `.github/workflows/android-ci.yml` | Working |
| Domain models, repository ports | `domain/` | Complete for v1 scope |
| Room schema + DAOs | `core/database/` | Complete for v1 scope |
| Preferences (DataStore) | `core/datastore/` | Complete, all v1 settings |
| Theme, type scale, illustration harness | `core/designsystem/` | Complete; artwork needs adding |
| TTS coach | `core/speech/` | Complete |
| Repository implementations, seeder | `data/` | Complete except `observeWeeklyLoad` |
| Four screens wired end to end | `feature-*/` | Settings is fully functional; the other three are honest partial states |
| Exercise catalogue | `app/src/main/assets/exercises_seed.json` | 14 seeded; target is ~60 |
| Evidence base with citations | `framework/02_evidence_base.md` | Researched, cited |

**The single largest missing piece is the workout generator** (`domain/engine/`), which
is specified in full in `framework/07_workout_engine_spec.md`.

---

## 2. Non-negotiable rules

These are the constraints that make the product correct rather than merely working.
Breaking one is a defect regardless of whether tests pass.

1. **No unsupported health claims.** The app never estimates visceral fat, never
   claims spot reduction, and never implies a specific exercise targets belly fat.
   Every programming claim traces to a citation in `framework/data/references.md`.
   See `framework/02_evidence_base.md` §Claims we do not make.
2. **Offline forever.** The app has no `INTERNET` permission. If you find yourself
   wanting one, you have misunderstood a requirement — stop and record the question
   in `project_memory/known_issues.md` instead.
3. **No third-party media assets.** Illustrations are drawn in Compose. No stock
   images, no bundled fonts, no downloaded content. (ADR-0003, ADR-0011.)
4. **Never destroy user data.** No `fallbackToDestructiveMigration`. Every schema
   change ships a tested migration.
5. **Never fabricate a number.** If body mass is unknown, the energy estimate is
   `null` and the UI shows `—`. A plausible-looking fake figure is worse than an
   absent one.
6. **The version catalogue is the only place versions live.** Never inline a version
   in a module build file.
7. **Update project memory in the same commit as the work.** See §4. A commit that
   changes behaviour without touching `project_memory/` will be rejected at review.
8. **`domain/` stays Android-free.** If you need `android.*` in a domain file, the
   logic belongs in `data/` or a feature module instead.

---

## 3. How to execute

Work through `framework/prompts/` in order. Each phase file contains:

- its **objective** and the **files it may touch**,
- the **specification** it implements (pointing at the relevant reference doc),
- **exit criteria** that are literally checkable (a command to run, an assertion to
  make), and
- the **project memory updates** the phase must make.

Do not start phase N+1 until phase N's exit criteria pass. Do not batch phases into
one commit.

```
framework/prompts/
  phase_00_orientation.md        Read, verify the build, record baseline
  phase_01_evidence_lock.md      Validate the evidence base, expand references
  phase_02_content_authoring.md  Grow the exercise catalogue to ~60
  phase_03_illustrations.md       Draw the vector artwork
  phase_04_design_system.md       Complete components and machine mode
  phase_05_navigation.md          Type-safe routes, deep links, back handling
  phase_06_workout_engine.md      THE BIG ONE — generator + property tests
  phase_07_workout_player.md      Timer, foreground service, keep-screen-on
  phase_08_coaching.md            Cue scheduling, TTS integration, fallbacks
  phase_09_tracking.md            History, streaks, weekly load, recovery advice
  phase_10_progress_measures.md   Measurements, photos, goals, backup/restore
  phase_11_accessibility.md       TalkBack, contrast, touch targets, large text
  phase_12_performance.md         Startup, frame timing, battery
  phase_13_hardening.md           Rotation, dark mode, offline, edge cases
  phase_14_release.md             Version bump, APK verification, documentation
```

---

## 4. Project memory protocol (mandatory, every phase)

`/project_memory/` is the project's durable state. It exists because you will lose
your context window, and the next agent — or the next you — must be able to pick up
without re-deriving anything.

**At the start of every phase**, read all eight files. They are short by design.

**At the end of every phase**, before you commit, update every file that has anything
to record. The rules for which file gets what:

| File | Records |
|---|---|
| `decisions.md` | Any choice with more than one defensible answer, plus the reason and what would reverse it |
| `architecture_decisions.md` | Structural decisions, numbered ADR-NNNN, in the standard ADR shape |
| `assumptions.md` | Anything you took as true without confirming, and how to confirm it |
| `known_issues.md` | Bugs, gaps and blockers you did not fix, with reproduction steps |
| `technical_debt.md` | Shortcuts taken deliberately, with the condition that should trigger repayment |
| `research_summary.md` | What research established, with citations. Append, never rewrite |
| `future_features.md` | Things deliberately out of scope, with enough detail to pick up later |
| `user_feedback.md` | Anything the human operator told you, verbatim, plus how you acted on it |

**Rules:**

- **Append, do not rewrite.** Each entry is dated and numbered. Superseding an old
  entry means adding a new one that says so, not editing the old one.
- **A phase is not complete until its memory entries exist.** This is part of the
  exit criteria, not paperwork afterwards.
- **Empty is a valid state, silence is not.** If a phase produced no new assumptions,
  add a line to the phase log in `decisions.md` saying so.
- **Every entry answers "why", not just "what".** "Chose Room" is useless. "Chose
  Room over SQLDelight because the schema-export-and-diff workflow is what keeps
  migrations reviewable, and no multiplatform target is planned" is useful.

Templates are in `project_memory/_templates/entry_templates.md`.

---

## 5. Commit and branch discipline

- Branch: `claude/android-fitness-app-framework-7ox2y0` unless told otherwise.
- One phase per commit series; each commit compiles and passes `./gradlew qualityCheck`.
- Commit message form:
  ```
  phase 06: implement deterministic workout generator

  <what changed and why, 2-5 lines>

  Project memory: decisions.md D-0021..D-0023, technical_debt.md TD-0004
  ```
- Never force-push a branch someone else may have pulled.

---

## 6. Reference documents

| Doc | Read it when |
|---|---|
| `01_product_requirements.md` | Always. Numbered REQ-NNN requirements; every phase traces to them |
| `02_evidence_base.md` | Any programming, intensity, duration or claim decision |
| `03_competitive_analysis.md` | Any feature-shape or UX-priority question |
| `04_ux_research_and_design_system.md` | Any visual or interaction decision |
| `05_architecture.md` | Any question about where code belongs |
| `06_data_model.md` | Any persistence change |
| `07_workout_engine_spec.md` | Phase 06, and anything that generates a session |
| `08_exercise_library_spec.md` | Authoring or editing exercise content |
| `09_coaching_and_tts_spec.md` | Anything the app says out loud |
| `10_screen_specs.md` | Building or changing a screen |
| `11_illustration_spec.md` | Drawing artwork |
| `12_accessibility_spec.md` | Every UI phase; it is not a final pass |
| `13_testing_strategy.md` | Writing any test |
| `14_ci_cd_and_release.md` | Anything touching the build or the APK |
| `15_device_targets_motorola_edge_60.md` | Device-specific behaviour |
| `16_glossary.md` | Any term you are unsure about |
| `17_definition_of_done.md` | Before claiming any phase, or the project, is done |

---

## 7. When you are unsure

In priority order:

1. Check the relevant reference doc — the answer is probably already written down.
2. Check `project_memory/decisions.md` — it may have been decided already.
3. If it is a genuine gap, **write it down** in `project_memory/known_issues.md` with
   the options and your recommendation, pick the safest option, note the assumption in
   `assumptions.md`, and continue. Do not stall the build.
4. Only stop and ask the human when proceeding either way would be unsafe (a health
   claim, a data-loss risk, or a privacy change).

Never guess silently. A recorded wrong assumption is recoverable; an unrecorded one
is not.

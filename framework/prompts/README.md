# Build prompts

The executable plan. Work through these **in order**. Each file is self-contained enough to be
handed to a fresh agent with no other context beyond `framework/00_START_HERE.md`.

## Structure of every phase file

| Section | Purpose |
|---|---|
| **Objective** | One sentence. What exists at the end that did not exist at the start |
| **Read first** | The specific documents to load. Do not skip |
| **Agents** | Which briefs from `framework/agents/` apply |
| **Files you may touch** | The boundary. Touching something outside this list needs a reason recorded in `decisions.md` |
| **Work** | Numbered steps |
| **Exit criteria** | Checkable conditions. Commands to run, assertions to make |
| **Project memory updates** | Mandatory. Part of the exit criteria, not paperwork afterwards |

## Rules

1. **In order.** Later phases depend on earlier ones. Phase 06 in particular is the pivot —
   almost nothing after it works without it.
2. **Do not start N+1 until N's exit criteria pass.** All of them, including the memory
   updates.
3. **One phase per commit series.** Every commit compiles and passes
   `./gradlew qualityCheck`.
4. **Do not batch phases.** The point of the boundaries is that a failure is attributable.
5. **If a phase is blocked**, do everything in it that is not blocked, record the blocker in
   `known_issues.md` with what you tried, and move on. Do not stall the whole build on one
   item.
6. **Report honestly.** If a manual test could not be run because no device was available, say
   so. An unrun test reported as passing poisons every later decision.

## The phases

| # | File | Objective | Blocks release? |
|---|---|---|---|
| 00 | `phase_00_orientation.md` | Verify the build, read everything, surface open assumptions | — |
| 01 | `phase_01_evidence_lock.md` | Validate the evidence base; extend references | — |
| 02 | `phase_02_content_authoring.md` | Grow the catalogue to ~54 exercises + validation test | **Yes** (KI-0004) |
| 03 | `phase_03_illustrations.md` | Draw every exercise illustration | No |
| 04 | `phase_04_design_system.md` | Complete the component inventory, machine mode | No |
| 05 | `phase_05_navigation.md` | Type-safe routes, custom duration, back handling | No |
| 06 | `phase_06_workout_engine.md` | **The generator.** Property tests, golden file | **Yes** (KI-0001) |
| 07 | `phase_07_workout_player.md` | Timer service, player UI, onboarding, safety notice | **Yes** (KI-0005) |
| 08 | `phase_08_coaching.md` | Cue scheduler, TTS integration, fallbacks | No |
| 09 | `phase_09_tracking.md` | History, streaks, weekly load, recovery advice | **Yes** (KI-0002) |
| 10 | `phase_10_progress_measures.md` | Measurements, photos, goals, backup/restore | No |
| 11 | `phase_11_accessibility.md` | Full accessibility audit and automated checks | No |
| 12 | `phase_12_performance.md` | Startup, frame timing, battery, size gate | No |
| 13 | `phase_13_hardening.md` | Rotation, dark mode, offline, edge cases | No |
| 14 | `phase_14_release.md` | Version, verification, final documentation | — |

## Progress tracking

The phase log table at the top of `project_memory/decisions.md` is the record. Add a row when a
phase completes — including "no new assumptions" if that is the case. Silence is not a valid
state; an explicit "nothing to record" is.

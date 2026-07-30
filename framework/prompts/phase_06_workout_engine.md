# Phase 06 — Workout engine

## Objective
Implement `WorkoutGenerator`. This is the phase that turns the skeleton into an app. Closes
**KI-0001** (the primary release blocker).

## Read first
- `framework/07_workout_engine_spec.md` — **all of it, carefully.** It is written to remove
  judgement calls. Where it gives a number, use that number
- `framework/02_evidence_base.md` §3 (style prescriptions) and §1.5 (the Pilates constraint)
- `framework/13_testing_strategy.md` §2
- `domain/src/main/kotlin/com/visceralfit/domain/engine/WorkoutGenerator.kt`

## Agents
**Android Architecture** and **QA** (co-lead). **Fitness Science** (must sign off that the
generated sessions match the prescriptions).

## Files you may touch
- `domain/src/main/kotlin/**` and `domain/src/test/kotlin/**`
- `feature-workout/**` — only to wire the Start button
- `project_memory/*`

## Work

### 1. Build the migration test harness first
`KI-0007`. The engine phase is the last comfortable moment to build it, because phase 09 adds
tables. Set up `MigrationTestHelper` in `core:database`'s `androidTest` with a passing
version-1-to-itself sanity test, so the harness is proven before there is a real migration
depending on it.

### 2. Implement, in this order
1. Duration budget and the split (spec §2).
2. Pool partitioning and the fallback chain (§3).
3. Exercise selection — `pick()` (§5). Get determinism right **here**; everything else depends on
   it.
4. Warm-up construction (§4.1).
5. Each main-block style: ZONE_2 (simplest), then MIXED, then HIIT, then RECOVERY (§4.2–4.5).
6. Cool-down (§4.6).
7. Failure paths (§1).
8. Pilates-only handling and title honesty (§7).

Write the test for each piece as you build it, not afterwards.

### 3. Determinism is the hard part
Work through the §6 checklist explicitly. The specific traps:
- Only one `Random(request.seed)`, created once per `generate` call.
- No clock read anywhere in `domain/engine`.
- Every `Set` sorted before it influences output. `request.modalities` is a `Set` — sort it.
- No `hashCode()` in an ordering decision.

**Prove it with a golden file, not by comparing two in-process calls.** Two calls in one JVM
share `HashSet` iteration order, so that test passes while the bug is present. A checked-in
expected output catches it.

### 4. The worked example is your golden file
Spec §9 gives a complete expected plan for a specific request. Implement until it matches
**exactly**, segment for segment. It pins every constant in the algorithm, and any accidental
change later breaks it with a readable diff. This is the single most valuable test in the project.

### 5. Wire the Start button
Replace the disabled placeholder. On failure, show the specific `GenerationFailure` reason and a
suggested fix — never a generic error.

## Exit criteria
- [ ] `WorkoutGenerator` implemented in `domain/engine`. No Android imports.
- [ ] Golden-file test matching spec §9 exactly.
- [ ] Property test: determinism across 1,000 seeds.
- [ ] Property test: duration fit ±30 s across all styles × all presets × custom boundaries
      (3, 4, 119, 120 min).
- [ ] Property test: structure invariant across all styles and durations.
- [ ] Property test: eligibility invariant, including the all-tags-avoided case.
- [ ] Unit test per `GenerationFailure` case.
- [ ] Test: Pilates-only request produces an honestly titled session with the built style
      recorded.
- [ ] Test: intensity capping downgrades the title.
- [ ] Every item in spec §6 verified by inspection and by test.
- [ ] `MigrationTestHelper` harness in place (KI-0007).
- [ ] Start button generates a real workout; failures show the reason and a fix.
- [ ] Fitness Science sign-off: generated sessions at each style and several durations match the
      §3 prescriptions in `02_evidence_base.md`.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `known_issues.md` — close KI-0001 and KI-0007.
- `decisions.md` — phase-log row, plus a `D-` entry for **every constant you had to choose that
  the spec did not fix**. There will be some; record them rather than leaving them as unexplained
  magic numbers.
- `assumptions.md` — anything you had to guess about the prescriptions.
- `technical_debt.md` — any part of the spec you implemented more simply than specified.

## Do not
- Invent additional heuristics beyond the spec without a `decisions.md` entry.
- Read the clock in the engine.
- Prove determinism by comparing two in-process calls.
- Let a Pilates-only session be titled as an interval or fat-loss session (REQ-004).
- Ship a generation failure as an empty workout or a crash.

# Phase 13 — Hardening

## Objective
Systematically work the edge cases: rotation, dark mode, offline, interruptions, hostile input.

This is where the bugs actually are. The happy path has been exercised for twelve phases.

## Read first
- `framework/13_testing_strategy.md` §8 (the manual script)
- `project_memory/known_issues.md` — everything still open

## Agents
**QA** (leads). All others as reviewers of their own areas.

## Files you may touch
- Anything
- `project_memory/*`

## Work

### 1. Run the full manual script
All twelve items in `13_testing_strategy.md` §8, on a physical device, and **record each result**.
These cannot be automated and are where real bugs surface. Priority order by likely yield:

1. TTS absent (A-0005 — the most likely real degradation)
2. Music playing
3. Backgrounded mid-session
4. Screen off mid-session
5. Rotation during work, rest and pause
6. Phone call mid-session
7. Pause and forget
8. Accidental back
9. Airplane mode — this is the offline claim, actually tested
10. Fresh install through onboarding to first history entry
11. 200% font scale
12. TalkBack

### 2. Rotation and process death, exhaustively
Rotate on every screen, in every state. Then enable "Don't keep activities" in developer options
and repeat — that simulates process death and is the fastest way to find state that only lives in
a ViewModel.

### 3. Dark, light and AMOLED
Every screen in all three. Switch mid-session and confirm nothing is lost. The system theme
changing while a workout runs is a real scenario at dusk with auto dark mode.

### 4. Hostile and boundary input
- Duration at 3, 4, 119, 120, 121, 0, negative, and empty.
- Body mass at 0, negative, 500, and a non-numeric string.
- Measurements with absurd values.
- Every modality disabled (must be refused, REQ-011).
- Every exercise excluded by caution tags (must fail with `NoEligibleExercises` and an
  explanation).
- A workout of 3 minutes with HIIT selected (must be prevented at selection time, not fail at
  generation).
- Import a corrupted, truncated, and newer-schema backup file.

### 5. Storage and lifecycle
- Fill the device storage and attempt a photo save.
- Force-stop mid-session and relaunch.
- Clear app data and confirm onboarding reappears.
- Downgrade simulation: write a preference key the current build does not know, confirm it does not
  crash (the defensive reads should handle it).

### 6. Close out project memory
Walk `known_issues.md` end to end. Every entry must be fixed, explicitly accepted with a reason, or
reclassified. An open blocker or major issue means the project is not done (`17_definition_of_done.md`).

Walk `assumptions.md`. Every entry must be confirmed, refuted, or explicitly still open with a
stated reason.

## Exit criteria
- [ ] All twelve manual script items run on a physical device, each result recorded.
- [ ] Rotation on every screen in every state, including with "Don't keep activities" on.
- [ ] All three themes on every screen, including switching mid-session.
- [ ] Every boundary and hostile input from §4 handled with an explanation, not a crash.
- [ ] Storage and lifecycle cases from §5 handled.
- [ ] `known_issues.md` has no open blocker or major entry.
- [ ] `assumptions.md` has no entry in an undetermined state without a stated reason.
- [ ] `./gradlew qualityCheck` green.
- [ ] The emulator suite has actually run and passed (closing KI-0008).

## Project memory updates
- `known_issues.md` — resolve or explicitly accept everything.
- `assumptions.md` — final status on every entry.
- `decisions.md` — phase-log row; any behaviour you had to define for an edge case nobody had
  specified.
- `technical_debt.md` — anything patched rather than fixed.

## Do not
- Report a manual test as run if it was not. If no device was available, say so plainly — an unrun
  test recorded as passing poisons every later decision.
- Close a `known_issues.md` entry without either a fix or a stated acceptance reason.

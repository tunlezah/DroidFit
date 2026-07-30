# Phase 09 — Tracking

## Objective
Make the Progress screen tell the truth. Closes **KI-0002** and **TD-0007** (release blockers).

## Read first
- `framework/06_data_model.md`
- `framework/07_workout_engine_spec.md` §8 (progression and the recovery recommender)
- `framework/02_evidence_base.md` §1.3 (dose) and §4 (progression)
- `project_memory/technical_debt.md` TD-0004, TD-0006, TD-0007

## Agents
**Fitness Science** (the recommender is a programming decision). Android Architecture. QA.

## Files you may touch
- `data/**`, `core/database/**`, `domain/**`
- `feature-progress/**`, `feature-history/**`
- `project_memory/*`

## Work

### 1. `observeWeeklyLoad` — the blocker
It currently returns `flowOf(emptyList())` and the Progress card reads 0 regardless of activity.
That is a **wrong** number, not a missing one, which is worse.

Implement it as a SQL aggregate, not by loading rows and summing in Kotlin. Watch the local
midnight boundary: a session started at 23:59 belongs to the day the user started it, not to UTC's
idea of that day. That boundary is exactly where a timezone bug hides, so test it explicitly.

Add `session_segments` if per-segment completion is needed for "which interval did they quit on?".

### 2. Weekly and monthly summaries
Total minutes, vigorous minutes, session count against the goal. The Progress card must state the
WHO range (REQ-002) — that text is a requirement.

### 3. Streaks
Count only sessions at or above the 0.7 completion threshold. Show streaks; **never** use them to
nag. Recovery is part of training, and guilt-driving a user into a session they should skip is
harmful, not sticky.

### 4. The recovery recommender
`07_workout_engine_spec.md` §8. Recommend RECOVERY when any trigger fires, and **say which one** —
"you've done two hard days in a row" is useful; "recovery recommended" is not.

The 10%-weekly-increase and 3-vigorous-sessions figures are **conventional practice, not
trial-derived**. Record that in `assumptions.md` as the spec instructs — do not present them as
evidence-backed.

### 5. Progression suggestions
Suggest, never apply. The experience level never self-promotes (REQ-015, A-0007). A suggested
promotion opens a screen explaining exactly what changes, and the user decides.

### 6. Calendar view and paging
`feature-history` gets the calendar view, and Paging 3 replacing the 100-row cap (TD-0006).

### 7. Fast DAO tests
TD-0004: add Robolectric with a pre-warmed CI dependency cache so these aggregates get
millisecond tests rather than only emulator coverage. The offline claim must stay true — pre-warm
the cache, do not let tests download at run time.

## Exit criteria
- [ ] `observeWeeklyLoad` implemented as a SQL aggregate; KI-0002 and TD-0007 closed.
- [ ] Test: sessions straddling local midnight are attributed to the correct day.
- [ ] Test: weekly and monthly totals, vigorous minutes, session counts.
- [ ] Test: streaks count only sessions ≥ 0.7 completion.
- [ ] Test per recovery-recommender trigger.
- [ ] The recommendation states its reason.
- [ ] Progression suggests; nothing auto-promotes.
- [ ] Calendar view works; paging replaces the row cap.
- [ ] Robolectric DAO tests run on every push; TD-0004 closed.
- [ ] Any new table has a migration **and a migration test**.
- [ ] Progress screen shows real numbers throughout — no placeholder zeros.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `known_issues.md` — close KI-0002.
- `technical_debt.md` — close TD-0004, TD-0006, TD-0007.
- `assumptions.md` — the 10% and 3-sessions figures as explicit assumptions, with how they could
  be confirmed.
- `decisions.md` — phase-log row; the streak rules; the recommender's trigger thresholds.

## Do not
- Ship a placeholder number on the Progress screen.
- Compute aggregates in Kotlin over loaded rows.
- Auto-promote the experience level.
- Use streaks to pressure the user.
- Present the progression constants as evidence-backed.

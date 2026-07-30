# 07 — Workout engine specification

This is the specification for `domain/engine/WorkoutGenerator`, the app's core. It is
written to be implementable without judgement calls. Where a number appears, use that
number; where a rule appears, do not add another.

**Implement in `domain/`. No Android imports.** The whole point of the module boundary is
that this logic is testable on the JVM in milliseconds.

---

## 1. Contract

```kotlin
fun generate(request: WorkoutRequest): Result<Workout>
```

`WorkoutRequest` carries: `duration`, `style`, `modalities`, `level`, `avoidTags`, `seed`,
`recentExerciseIds`.

Success returns a `Workout` whose `blocks` are exactly `[WARM_UP, MAIN, COOL_DOWN]` in that
order. Failure returns a `GenerationFailure` — one of `NoEligibleExercises`,
`DurationTooShort`, `InsufficientVariety`. **Never** throw, never return an empty workout,
never silently substitute a different style.

### The four invariants

Each has a corresponding property test in phase 06's exit criteria.

1. **Determinism.** Same `WorkoutRequest` → identical `Workout`, field for field, every
   time and in every process. The generator must not read the clock, must not use
   `Math.random()` or `kotlin.random.Random.Default`, and must not iterate a `HashSet` or
   `HashMap` in a way that affects output. Use `kotlin.random.Random(seed)` exclusively, and
   sort any set before iterating it.
2. **Duration fit.** `abs(workout.actualDuration - request.duration) <= 30s`.
3. **Structure.** All three blocks present and non-empty; warm-up at least the style
   minimum; cool-down at least 2 min (3 min for HIIT).
4. **Eligibility.** No segment's exercise is outside `request.modalities`, above
   `request.level`, or carries a tag in `request.avoidTags`.

---

## 2. Duration bounds and budget

Total requested duration `T`, in seconds. Accepted range: **180 s (3 min) to 7200 s
(120 min)** (A-0003). Outside that, fail with `DurationTooShort` (rename it in your head
as "duration out of range"; keep the type as specified so the UI does not change).

Budget split, computed in this order:

```
warmUp   = clamp(round(T * warmUpFraction), styleMinWarmUp, styleMaxWarmUp)
coolDown = clamp(round(T * 0.10), styleMinCoolDown, 300s)
main     = T - warmUp - coolDown
```

| Style | warmUpFraction | min warm-up | max warm-up | min cool-down |
|---|---|---|---|---|
| HIIT | 0.20 | 300 s | 600 s | 180 s |
| ZONE_2 | 0.12 | 180 s | 420 s | 120 s |
| MIXED | 0.15 | 180 s | 480 s | 120 s |
| RECOVERY | 0.15 | 120 s | 300 s | 120 s |

If `main < styleMinMain` after this split, fail with
`DurationTooShort(requested = T, minimum = styleMinWarmUp + styleMinMain + styleMinCoolDown)`.

| Style | min main block | Therefore min total |
|---|---|---|
| HIIT | 480 s | 960 s (16 min) |
| ZONE_2 | 240 s | 540 s (9 min) |
| MIXED | 300 s | 600 s (10 min) |
| RECOVERY | 180 s | 420 s (7 min) |

**Consequence to surface in the UI:** a 5-minute request cannot produce a HIIT session. The
UI must show which styles are available for the chosen duration and disable the rest with a
reason (REQ-024), rather than letting the user pick one and then failing.

---

## 3. Exercise pools

Before building blocks, partition the eligible exercises.

```
eligible = allExercises.filter {
    it.modality in request.modalities &&
    request.level.canAttempt(it.difficulty) &&
    it.cautionTags.none { tag -> tag in request.avoidTags }
}
```

If `eligible` is empty → `NoEligibleExercises(request.modalities, request.level)`.

Then partition by role. Role is derived, not stored:

| Pool | Predicate |
|---|---|
| `warmUpPool` | `metValue <= 4.0` and modality is Pilates, **or** a machine-cardio exercise whose intensity anchor is Zone 2 or lower |
| `cardioPool` | `modality.isMachineCardio` |
| `vigorousPool` | `cardioPool` and `metValue >= 8.0` |
| `thresholdPool` | `cardioPool` and `metValue in 6.0..10.9` |
| `steadyPool` | `cardioPool` and `metValue in 4.0..9.0` |
| `strengthPool` | modality is Pilates and `metValue > 2.5` |
| `mobilityPool` | `metValue <= 2.5` |

Pools overlap deliberately — an exercise at 9.0 MET is in both `steadyPool` and
`vigorousPool`, because whether it is steady or hard work depends on how the segment is
prescribed, not on the exercise.

**Fallbacks when a pool is empty**, in order — apply the first that yields a non-empty pool
and record which fallback was used in the returned `Workout`'s title context:

| Empty pool | Fallback |
|---|---|
| `warmUpPool` | use `mobilityPool`; then the lowest-MET three exercises in `eligible` |
| `vigorousPool` | use `thresholdPool` and cap the segment's intensity at THRESHOLD |
| `thresholdPool` | use `steadyPool` and cap at ZONE_2 |
| `steadyPool` | use `strengthPool`; the session becomes a Pilates session (see §7) |
| `cooldown`/`mobilityPool` | use the lowest-MET exercise in `eligible` |

If a fallback caps intensity, the style is **downgraded** and the workout title must say so
("Mixed (steady)" rather than "Mixed"). Never present a capped session as the style the
user asked for.

---

## 4. Block construction

### 4.1 Warm-up (all styles)

Fill `warmUp` seconds with 2–4 segments drawn from `warmUpPool`, ascending by MET so the
warm-up ramps. Each segment is `SegmentKind.WORK` at `IntensityTarget.ZONE_2`, except the
first, which is `IntensityTarget.RECOVERY`.

Segment count: `min(4, max(2, warmUp / 90s))`. Distribute `warmUp` evenly, giving any
remainder to the last segment.

If a machine-cardio modality is available, the **final** warm-up segment must be on it, so
the user is already on the machine when the main block starts. Insert a
`SegmentKind.TRANSITION` of 20 s before it if the preceding segment was a different
modality.

### 4.2 Main block — HIIT

Choose the largest interval template that fits `main`:

"Main seconds needed" = `rounds × work + (rounds − 1) × recovery`. Note the
`(rounds − 1)`: there is **no recovery after the final work interval**, because the
cool-down serves that purpose. This is the most likely arithmetic slip in the whole
engine — the table below is computed, and your implementation must reproduce it.

| Template | Work | Recovery | Rounds | Main seconds needed | Source |
|---|---|---|---|---|---|
| `4x4` | 240 s | 180 s | 4 | 4×240 + 3×180 = **1500 s** | `helgerud2007` |
| `5x3` | 180 s | 150 s | 5 | 5×180 + 4×150 = **1500 s** | derived |
| `6x2` | 120 s | 120 s | 6 | 6×120 + 5×120 = **1320 s** | derived |
| `8x1` | 60 s | 90 s | 8 | 8×60 + 7×90 = **1110 s** | derived |
| `10x30s` | 30 s | 60 s | 10 | 10×30 + 9×60 = **840 s** | derived |
| `6x30s` | 30 s | 60 s | 6 | 6×30 + 5×60 = **480 s** | derived |

Pick the largest template whose requirement is `<= main`. **`4x4` and `5x3` both need
1500 s**; break that tie in favour of `4x4`, because it is the only template with direct
trial evidence behind it (`helgerud2007`). Order the list with `4x4` first and take the
first match.

Note the smallest template needs exactly 480 s, which is why `styleMinMain` for HIIT is
480 s — the two numbers are the same fact and must stay in step.

Distribute the leftover
(`main - needed`) by lengthening each recovery segment equally, capped at +60 s each; any
remaining leftover becomes an extra `ACTIVE_RECOVERY` segment at the end of the main block.

Work segments: `SegmentKind.WORK`, exercise from `vigorousPool`,
`IntensityTarget.VIGOROUS`, with `roundIndex` and `roundTotal` set so cues can say
"round 3 of 6". Recovery segments: `SegmentKind.ACTIVE_RECOVERY`, exercise from
`steadyPool` on the **same modality** as the work segment, `IntensityTarget.RECOVERY`.

**Keep the same exercise across all rounds** unless `vigorousPool` has 3 or more entries on
the same modality, in which case rotate through up to 3. Changing machine or movement every
round wastes time in transition and is not what the protocol prescribes.

### 4.3 Main block — ZONE_2

One to three continuous `WORK` segments from `steadyPool` at `IntensityTarget.ZONE_2`,
covering the whole `main` budget.

Segment count `= min(3, max(1, main / 600s))`. Splitting a long steady block into segments
is purely so the coach can name a change of position or cadence and so the halfway cue has
a boundary to land on; the intensity does not change.

Insert a 20 s `TRANSITION` between segments only if they use different modalities.

### 4.4 Main block — MIXED

A Zone 2 base with threshold surges.

```
surgeCount = clamp(main / 300s, 2, 5)
surgeDuration = when { main >= 1800s -> 180s; main >= 1200s -> 120s; else -> 60s }
totalSurge = surgeCount * surgeDuration
baseTotal = main - totalSurge
```

If `baseTotal < surgeCount * 120s`, reduce `surgeCount` by one and recompute. If
`surgeCount` falls below 2, fall back to ZONE_2 construction and downgrade the title.

Layout: base segment, surge, base segment, surge, … ending on a base segment. Base segments
share `baseTotal` evenly. Surges are `WORK` at `IntensityTarget.THRESHOLD` from
`thresholdPool`; base segments are `WORK` at `IntensityTarget.ZONE_2` from `steadyPool`.

**Position all surges within the middle third of the main block** where possible — do not
put a surge in the first 90 s (the user is barely warm) or the last 60 s (the cool-down
should follow easy work). If the layout cannot satisfy this, reduce `surgeCount`.

### 4.5 Main block — RECOVERY

Segments of 60–120 s from `mobilityPool` and `strengthPool`, alternating, at
`IntensityTarget.RECOVERY`, filling `main`. No `ACTIVE_RECOVERY` or `REST` segments — the
whole block is already low intensity. Prefer variety here: recovery sessions are where
repeating the same movement is most noticeable.

### 4.6 Cool-down (all styles)

Fill `coolDown` with 2–3 segments from `mobilityPool` at `IntensityTarget.RECOVERY`. If the
main block was machine cardio, the **first** cool-down segment must be easy work on that
same machine (a spin-down) before moving to the floor, with a 20 s `TRANSITION` after it.

---

## 5. Exercise selection

A single function selects from a pool. It must be deterministic and must respect variety.

```
fun pick(pool: List<Exercise>, count: Int, random: Random, recent: List<String>): List<Exercise>
```

Algorithm:

1. Sort `pool` by `id` — an explicit total order, so the input order cannot vary.
2. Score each exercise: `0` if not in `recent`, otherwise `recent.size - recent.indexOf(id)`
   so the most recently used exercise scores highest (worst).
3. Group by score. Shuffle each group with `random`. Concatenate groups ascending by score.
4. Take `count` from the front, without replacement.
5. If `count > pool.size`, take the whole pool, then continue taking from the front again
   (allowing repeats) — but if `pool.size < 2` and `count >= 3`, fail with
   `InsufficientVariety(required = count, available = pool.size)`.

**Never repeat an exercise within a block** while an unused eligible one remains.

---

## 6. Determinism checklist

Before you claim phase 06 is done, verify each of these by inspection *and* by test:

- [ ] The only randomness source is `Random(request.seed)`, created once per `generate`
      call.
- [ ] No `System.currentTimeMillis()`, `Clock`, `Instant.now()` or equivalent anywhere in
      `domain/engine`.
- [ ] Every `Set` is converted to a sorted `List` before it influences output.
- [ ] `Map` iteration never affects output ordering (or `LinkedHashMap` with a documented
      insertion order is used).
- [ ] No `hashCode()` value influences output.
- [ ] The same request generated 1,000 times yields 1,000 identical results.
- [ ] The same request generated in two separate JVM runs yields the same result. (Test by
      asserting against a checked-in golden file, not by comparing two in-process calls —
      the classic way this bug hides.)

---

## 7. Pilates-only sessions

When `request.modalities` contains no machine-cardio modality, `cardioPool` is empty and the
`steadyPool` fallback puts the session on `strengthPool`. This is legitimate — the user
disabled the machines — but it changes what the session *is*.

Rules:

1. The workout title must name it honestly: "Floor Pilates — strength and control", never
   "HIIT" or "Fat-burning intervals".
2. The style is recorded as what was actually built, not what was requested.
3. The session still counts toward weekly minutes. It contributes **zero vigorous
   minutes** for the purposes of the vigorous-session cap in §8.
4. No copy in the session presents it as a visceral-fat intervention comparable to aerobic
   work. This is the direct implementation of `framework/02_evidence_base.md` §1.5 and
   REQ-004.

Where a Pilates modality is combined with a machine, normal construction applies and the
Pilates content fills warm-up, cool-down and (in RECOVERY) the main block.

---

## 8. Progression and the recovery recommender

Phase 09 work; specified here so the engine's interface does not change later.

Inputs: `TrainingLoadSummary` for the last 4 weeks.

Recommend RECOVERY, and say why, when any of:

- `consecutiveVigorousDays >= 2`
- `vigorousMinutes` in the last 7 days `>= 3` sessions' worth for the user's level
- `totalMinutes` in the last 7 days exceeds the trailing 3-week average by more than 30%
- The user's last 2 sessions both had `completionRatio < 0.7` — a signal the current
  prescription is too hard

Weekly volume suggestions increase by at most **10%** over the previous week, and only when
the previous week was at least 80% completed.

The experience level **never** changes automatically (REQ-015, A-0007). The app may suggest
promotion, on a screen that explains exactly what changes, and the user decides.

---

## 9. Worked example

Request: 20 min, MIXED, `{SPIN_BIKE, FLOOR_PILATES}`, INTERMEDIATE, seed 42, no avoid tags,
no recent exercises. `T = 1200 s`.

```
warmUp   = clamp(round(1200 * 0.15), 180, 480) = 180 s
coolDown = clamp(round(1200 * 0.10), 120, 300) = 120 s
main     = 1200 - 180 - 120                    = 900 s   (>= 300 s min, ok)

surgeCount    = clamp(900 / 300, 2, 5) = 3
surgeDuration = 900 < 1200 -> 60 s
totalSurge    = 180 s
baseTotal     = 720 s   (>= 3 * 120 = 360, ok)
base segments = 4 (one more than surges), 180 s each
```

Resulting plan:

| # | Block | Kind | Duration | Intensity | Notes |
|---|---|---|---|---|---|
| 1 | WARM_UP | WORK | 90 s | RECOVERY | March in place (2.8–3.5 MET) |
| 2 | WARM_UP | TRANSITION | 20 s | RECOVERY | Move to the bike |
| 3 | WARM_UP | WORK | 70 s | ZONE_2 | Seated flat road, easy |
| 4 | MAIN | WORK | 180 s | ZONE_2 | Seated flat road |
| 5 | MAIN | WORK | 60 s | THRESHOLD | Seated climb, surge 1 of 3 |
| 6 | MAIN | WORK | 180 s | ZONE_2 | Seated flat road |
| 7 | MAIN | WORK | 60 s | THRESHOLD | Seated climb, surge 2 of 3 |
| 8 | MAIN | WORK | 180 s | ZONE_2 | Seated flat road |
| 9 | MAIN | WORK | 60 s | THRESHOLD | Seated climb, surge 3 of 3 |
| 10 | MAIN | WORK | 180 s | ZONE_2 | Seated flat road |
| 11 | COOL_DOWN | WORK | 60 s | RECOVERY | Easy spin-down |
| 12 | COOL_DOWN | TRANSITION | 20 s | RECOVERY | Off the bike, to the mat |
| 13 | COOL_DOWN | WORK | 40 s | RECOVERY | Supine knee hug |

Total: 180 + 900 + 120 = 1200 s exactly. Note the warm-up transition is charged inside the
warm-up budget, which is why segment 3 is 70 s rather than 90 s.

**Turn this table into a golden-file test.** It is the single most useful test in the
project: it pins the whole algorithm, and any accidental change to any constant breaks it
with a readable diff.

---

## 10. Exit criteria for phase 06

- [ ] `WorkoutGenerator` implemented in `domain/engine`, no Android imports.
- [ ] Property test: determinism across 1,000 random seeds.
- [ ] Golden-file test matching §9 exactly.
- [ ] Property test: duration fit within ±30 s across all styles × all presets × custom
      values at the boundaries (3, 4, 119, 120 min).
- [ ] Property test: structure invariant across all styles and durations.
- [ ] Property test: eligibility invariant, including a case where every exercise carries an
      avoided tag (expect `NoEligibleExercises`).
- [ ] Unit tests for each `GenerationFailure` case.
- [ ] Test: a Pilates-only request produces an honestly titled session with the recorded
      style matching what was built.
- [ ] Test: intensity capping downgrades the title.
- [ ] `./gradlew qualityCheck` green.
- [ ] `project_memory` updated: `decisions.md` for every constant you had to choose that
      this document did not fix; `assumptions.md` for anything you had to guess;
      `known_issues.md` for anything you left undone.

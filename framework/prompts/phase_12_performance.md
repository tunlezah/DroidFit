# Phase 12 — Performance

## Objective
Meet the targets on a mid-range device, and add the gates that keep them met.

## Read first
- `framework/15_device_targets_motorola_edge_60.md` §4
- `core/designsystem/icon/VisceralFitIcons.kt` — the comment explaining the 66 MB incident

## Agents
**DevOps** (leads). Android Architecture. UI Design.

## Files you may touch
- Any module, for optimisation
- `.github/workflows/**`
- `app/src/androidTest/**` — benchmarks
- `project_memory/*`

## Work

### 1. Measure before optimising
Establish a baseline for each target and record the numbers in project memory. Optimising without
a measurement is guessing, and the numbers are also what makes the gates meaningful.

| Metric | Target | Measure with |
|---|---|---|
| Cold start to first frame | < 800 ms | `adb shell am start -W` |
| Frame timing, 60 s of session | zero frames > 16 ms | Macrobenchmark / `dumpsys gfxinfo` |
| APK size | < 12 MB | the CI gate |
| Battery, 45-min session, screen on, AMOLED | < 12% of 5,200 mAh | Battery Historian |

Remember the target is a **Dimensity 7300**, not a flagship. Do not measure on an emulator on a
fast host and call it done.

### 2. Cold start
The likely costs: Hilt graph construction, Room database open, the seeder. The seeder already runs
off the main thread and is not awaited — verify that is still true and that nothing has crept into
`Application.onCreate`.

Add a Baseline Profile. On a mid-range chipset it is one of the few genuinely large wins available.

### 3. Frame timing
The timer is on screen for the whole session, so a dropped frame there is very visible.

Check: the progress ring animates from elapsed time rather than recomposition count; no
recomposition per timer tick beyond the digits themselves; illustrations are not redrawn every
frame; no allocation in a draw scope.

Use the Compose recomposition counts in Layout Inspector to find over-recomposition. A composable
recomposing 60 times a second when only its text changed is the usual culprit.

### 4. Battery
Screen-on time dominates. That is the entire justification for AMOLED mode being the default.

Verify: no wake lock is held (only the window flag); the keep-awake flag releases after 60 s
paused; the foreground service does no polling; no work happens while paused.

### 5. APK size
Currently 2.38 MB minified. Phase 03 added ~60 code-drawn illustrations, so re-measure.

The 12 MB CI ceiling stays. **Do not raise it to accommodate growth** — investigate the growth. It
exists because `material-icons-extended` once inflated the APK to 66 MB for four icons, and
measurement was the only reason that was caught.

### 6. Add the gates
- A Macrobenchmark for startup and for frame timing, run on the emulator job.
- Keep the APK size check.
- Record the baselines in project memory so a future regression is attributable.

## Exit criteria
- [ ] Every target measured on a real Dimensity-class device, numbers recorded.
- [ ] Cold start under 800 ms with a Baseline Profile in place.
- [ ] No frame over 16 ms across a 60 s session sample.
- [ ] APK under 12 MB; the gate passes.
- [ ] Battery under 12% for a 45-minute AMOLED session.
- [ ] No wake lock held; keep-awake releases when paused.
- [ ] Macrobenchmarks added to the emulator job.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `decisions.md` — phase-log row; **the measured baselines** (these are the reference for every
  future regression); any optimisation that traded clarity for speed.
- `technical_debt.md` — any optimisation you deferred, with its cost.
- `known_issues.md` — any target you could not meet, with the measured gap.

## Do not
- Optimise without measuring first.
- Raise the APK ceiling instead of investigating growth.
- Measure on an emulator on a fast host and report it as device performance.
- Introduce a wake lock.

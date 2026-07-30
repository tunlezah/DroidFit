# 10 — Screen specifications

Every screen, its states, and its contract.

---

## 1. The composable contract (ADR-0010)

Every screen is two functions:

```kotlin
@Composable fun XxxRoute(viewModel: XxxViewModel = hiltViewModel())   // public
@Composable internal fun XxxScreen(state: XxxUiState, onAction: ...)  // stateless
```

`Route` resolves the ViewModel and collects state with `collectAsStateWithLifecycle()`.
`Screen` receives state and callbacks and holds all the UI. Previews and screenshot tests
drive `Screen` directly, with no Hilt graph.

**No `hiltViewModel()` inside a `*Screen`.** That single rule is what makes every screen
previewable.

Every screen must handle four states explicitly. A screen that only renders the happy path is
not finished:

1. **Loading** — data has not arrived. Not a blank screen.
2. **Empty** — data arrived and there is none. An explanation, not a spinner.
3. **Content** — the normal case.
4. **Error** — what happened, why, and what to do about it.

## 2. Navigation

Four top-level destinations in a bottom bar: Train, History, Progress, Settings.

Routes are declared in `app/ui/VisceralFitApp.kt`, never inside a feature module — a feature
must not know its own route, or it cannot be reused or renamed without touching it.

`TD-0005`: routes are strings today. Convert to type-safe `@Serializable` routes in phase 05,
**before** any destination takes an argument.

Back behaviour:
- Top-level destinations: back exits the app from the start destination, otherwise returns to
  it.
- Workout player: back **prompts** before discarding an in-progress session. An accidental
  edge swipe must not silently end a 45-minute effort. Predictive back must still animate.
- Detail screens: normal back.

## 3. Train (`feature-workout`)

**Purpose:** configure and start a session. This is the start destination.

| State | Renders |
|---|---|
| Loading | Skeleton chips, no spinner — the layout is known |
| Content | Duration chips, style chips, usable-modality chips, Start button, library card |
| Empty (no usable modalities) | Error text explaining the fix, Start disabled with reason |

Content rules:
- Duration presets from `WorkoutHomeUiState.DURATION_PRESETS_MINUTES`, plus a custom entry
  (phase 05) accepting 3–120 min with out-of-range rejected and explained, never clamped
  silently.
- **Style chips must be disabled when the chosen duration cannot support them**
  (`07_workout_engine_spec.md` §2 minimums), with the reason visible: "Intervals need at least
  16 minutes". Letting the user pick a style and then failing generation is the failure mode
  REQ-024 exists to prevent.
- Start is disabled only when genuinely impossible, always with a stated reason.
- The library card states the exercise count and that everything is local. It is the app's
  quiet privacy claim.

## 4. Workout player (`feature-workout`, phase 07)

The app's most important screen. **A display first, a control surface second** — if the user
must touch it mid-interval, the design failed.

Layout, portrait:

```
┌─────────────────────────────┐
│ ← 12:34 remaining      ⚙︎    │  session progress, exit
├─────────────────────────────┤
│      ╭───────────╮          │
│      │   1:24    │          │  TimerRing + Countdown (96 sp mono)
│      ╰───────────╯          │
│      SEATED CLIMB           │  SegmentLabel (22 sp)
│      Threshold · RPE 6-7    │  IntensityChip (colour + label)
├─────────────────────────────┤
│   [illustration]            │
│   • Add resistance…         │  how_to steps
│   • Drive through…          │
├─────────────────────────────┤
│  Next: seated flat road     │  NextUpBanner
├─────────────────────────────┤
│    ⏸ Pause      ⏭ Skip      │  72 dp targets
└─────────────────────────────┘
```

Rules:
- `KeepScreenOn(enabled = prefs.display.keepScreenOn)` at the root, so toggling the setting
  mid-session takes effect immediately.
- The countdown comes from the **service**, not a composable-local timer (ADR-0008). Rotation,
  backgrounding and process death must not desynchronise it.
- Machine mode swaps to `CountdownLarge` (148 sp) and drops the how-to block — nobody reads
  technique cues at 90 rpm.
- Pause stops the clock, stops speech, and releases the keep-screen-on flag after 60 s of
  being paused. A session paused and forgotten must not hold the screen on all night.
- Skip advances to the next segment and records the skip in the session's completion ratio.
- Landscape: countdown left, detail right (REQ-073).

## 5. Session summary (`feature-workout`, phase 07)

Shown on completion. Content: duration, active duration, estimated energy (or `—`),
segments completed, an optional RPE prompt, an optional note.

Rules:
- Energy shows `~410 kcal estimated` or `— kcal`. Never `0` (D-0005).
- The RPE prompt is skippable — a modal that must be answered before leaving is hostile.
- If the session completed below 70%, say so neutrally ("Ended early — 12 of 20 minutes").
  No guilt framing. Recovery is part of training.

## 6. History (`feature-history`)

| State | Renders |
|---|---|
| Loading | Three placeholder rows |
| Empty | "No sessions yet" + "Finished workouts appear here, on this device only." |
| Content | Reverse-chronological list; tap for the session summary |

Phase 09 adds the calendar view and Paging 3 (TD-0006).

## 7. Progress (`feature-progress`)

Cards: weekly minutes against goal (with the WHO range stated), waist trend, streak,
measurements entry.

Rules:
- The weekly card currently reads 0 always — **KI-0002, a release blocker.** Do not ship it.
- The waist card must carry the "not a measure of visceral fat" statement (REQ-003). That text
  is a requirement, not decoration.
- Sub-1 cm changes report "roughly unchanged" (A-0008). Reporting measurement noise as
  progress is a form of fabrication.
- No composite fitness score, ever.

## 8. Settings (`feature-settings`) — built

Sections: Exercise types, Equipment I have access to, Spoken coaching, Display. Phase 10 adds
Body and measurements, Data (backup/restore), About.

Rules already implemented and to be preserved:
- The whole row is the touch target, 56 dp minimum, with `toggleable` and `Role.Switch` so
  TalkBack announces label and state together as **one** focus stop, not two.
- Speech sub-toggles are **disabled, not hidden**, when the master is off (ADR-0007) — hiding
  them makes users think options vanished.
- Disabling the last enabled modality is refused, with the reason shown (REQ-011).
- An enabled modality whose equipment is unavailable shows an explanatory subtitle.

## 9. Onboarding (`app`, phase 07) — KI-0005

Three screens, skippable except the safety notice:

1. **Safety notice.** Must be acknowledged; writes `safetyNoticeAcknowledged`. This is the
   mitigation for A-0007 and is a release blocker. Content in `02_evidence_base.md` §7.
2. **What do you have access to?** Equipment toggles.
3. **How experienced are you?** Level, defaulting to beginner, with plain descriptions of what
   each means.

No quiz that produces a "personalised plan" — it would imply more personalisation than exists
(`03_competitive_analysis.md` §4).

## 10. Screen checklist

Before calling any screen done:

- [ ] `Route` / `Screen` split, no `hiltViewModel()` in `Screen`.
- [ ] All four states handled.
- [ ] `@Preview` in light, dark and AMOLED.
- [ ] Touch targets ≥ 48 dp, rows ≥ 56 dp.
- [ ] Insets consumed; nothing in the curved-edge zone.
- [ ] Works at 200% font scale without clipping.
- [ ] TalkBack: every element labelled, one focus stop per logical control.
- [ ] Rotation preserves state.
- [ ] No disabled control without a visible reason.
- [ ] No hard-coded colour; everything from the theme.
- [ ] No fabricated number; nulls render as `—`.

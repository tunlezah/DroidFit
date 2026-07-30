# Phase 07 — Workout player, service and onboarding

## Objective
Make a workout performable end to end: the timer service, the player UI, and the onboarding that
shows the safety notice. Closes **KI-0005** (release blocker).

## Read first
- `framework/10_screen_specs.md` §4, §5, §9
- `framework/15_device_targets_motorola_edge_60.md` §3
- `project_memory/architecture_decisions.md` ADR-0008
- `framework/02_evidence_base.md` §7 (safety notice content)

## Agents
**Android Architecture** (the service), **UX Research** (the player), **Fitness Science** (the
safety notice), Accessibility.

## Files you may touch
- `feature-workout/**`
- `app/**` — onboarding, manifest service declaration
- `core/designsystem/**` — only if a component needs extending
- `project_memory/*`

## Work

### 1. The timer service
Per ADR-0008, the **authoritative** session clock and segment cursor live in a
`mediaPlayback`-typed foreground service, not in the ViewModel.

- Declare it in the manifest with `foregroundServiceType="mediaPlayback"`. The permission is
  already present.
- Persistent notification showing the current segment and remaining time, with a deep link back
  into the player (phase 05 added it).
- The ViewModel **observes** the service. It does not own a timer.
- Handle process death: on rebind, the player reflects the service's real state.
- Request `POST_NOTIFICATIONS` when the user starts their **first** workout, with an explanation —
  not at launch. If refused, the session still runs; say plainly that backgrounding will
  eventually stop it, rather than degrading silently.

### 2. The player UI
Layout in `10_screen_specs.md` §4. Non-obvious requirements:

- `KeepScreenOn(enabled = prefs.display.keepScreenOn)` at the root, so toggling the setting
  mid-session takes effect immediately.
- **Pause must release the keep-awake flag after 60 s of being paused.** A session paused and
  forgotten must not hold the screen on all night.
- Skip advances and records the skip in the completion ratio.
- Machine mode is a layout variant that drops the how-to block.
- Landscape works (REQ-073).
- Back prompts before discarding (phase 05 wired the prompt; make it real here).

### 3. Session summary
`10_screen_specs.md` §5. Energy shows `~N kcal estimated` or `— kcal`, never `0`. The RPE prompt
is skippable. Completion below 70% is stated neutrally — no guilt framing.

### 4. Record the session
Write a `CompletedSession` via `HistoryRepository`. `activeDuration` excludes paused time.
`estimatedKilocalories` comes from `EstimateEnergyExpenditure` and is `null` if body mass is
unknown — **do not** substitute a default.

### 5. Onboarding
Three screens (`10_screen_specs.md` §9):
1. **Safety notice** — mandatory acknowledgement, writes `safetyNoticeAcknowledged`. Content from
   `02_evidence_base.md` §7. This is the mitigation for A-0007; do not weaken it.
2. Equipment availability.
3. Experience level, defaulting to beginner.

No quiz that produces a "personalised plan".

## Exit criteria
- [ ] A full session can be performed start to finish.
- [ ] The timer survives rotation, backgrounding, screen-off and process death — verified
      manually, on a device, and recorded.
- [ ] Notification shows correct state; its deep link returns to the player.
- [ ] Keep-screen-on honours the setting live, and releases after 60 s paused.
- [ ] Sessions are recorded with correct active duration and a null-safe energy estimate.
- [ ] Onboarding shows the safety notice and requires acknowledgement on a fresh install.
- [ ] Machine mode and landscape both usable.
- [ ] Back during a session prompts.
- [ ] Accessibility checklist run on the player and onboarding.
- [ ] `./gradlew qualityCheck` green.
- [ ] Manual tests 3, 4, 5, 6, 7, 8 and 12 from `13_testing_strategy.md` §8 run and recorded.

## Project memory updates
- `known_issues.md` — close KI-0005.
- `decisions.md` — phase-log row; the notification content, the 60 s pause threshold if you
  changed it, the permission-request timing.
- `assumptions.md` — A-0007's mitigation is now implemented; update its entry to say so.

## Do not
- Put the clock in the ViewModel or a composable.
- Hold the keep-awake flag indefinitely while paused.
- Substitute a default body mass to make the energy figure non-null.
- Make the RPE prompt mandatory.
- Weaken or skip the safety notice.

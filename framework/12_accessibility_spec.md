# 12 — Accessibility specification

Target: **WCAG 2.1 AA or better**, as the PRD requires.

**Accessibility is not a final pass.** Every UI phase includes the checks in §7. Retrofitting
it at the end produces an app that technically passes and is unpleasant to use.

There is also a specific reason it matters here: this app is used while breathing hard, with
sweaty hands, at arm's length, sometimes lying on the floor. Almost every accessibility
affordance — large targets, high contrast, non-visual state, working at 200% font scale — is
also a usability affordance for the *primary* use case.

---

## 1. Screen readers (TalkBack)

**One focus stop per logical control.** The already-built settings row is the pattern:

```kotlin
Row(
    modifier = Modifier
        .heightIn(min = 56.dp)
        .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
) {
    Column { Text(title); Text(subtitle) }
    Switch(checked = checked, onCheckedChange = null)   // null: not separately focusable
}
```

`onCheckedChange = null` on the `Switch` is load-bearing. Without it TalkBack gives two focus
stops per setting — one for the row, one for the switch — and the user has to work out that
they do the same thing.

Rules:
- Every interactive element has a label. Decorative icons get `contentDescription = null`.
- Exercise illustrations get a position-describing description (`11_illustration_spec.md` §6).
- Use `mergeDescendants = true` on cards so a stat tile reads as one thing.
- State changes during a workout are announced via the spoken cues, which are the app's
  primary channel anyway. Do not add competing `liveRegion` announcements — the user would
  hear each segment change twice.
- Never rely on `contentDescription` to convey something sighted users get from layout order.

## 2. The countdown and TalkBack

A per-second `liveRegion` on the timer would produce continuous, unusable speech.

Rule: the countdown is **not** a live region. It is labelled with the segment name and
remaining time, updated at segment boundaries and on demand, and the second-by-second
information reaches the user through the coaching cues. This is a deliberate divergence from
"announce state changes" and must not be "fixed".

## 3. Colour and contrast

- Every text/background pair meets **4.5:1** (normal) or **3:1** (large, ≥ 18.66 sp bold or
  ≥ 24 sp). Verify with a contrast checker, not by eye.
- **Colour is never the only signal** (REQ-022). Every intensity state carries a text label,
  a distinct ring stroke weight, and a distinct haptic pattern. The threshold/vigorous pair is
  an amber/red confusion for the ~1 in 12 men with a colour vision deficiency, and it is
  exactly the distinction that matters most mid-session.
- AMOLED mode must be re-checked separately: pure black backgrounds change contrast ratios,
  and a colour that passed on `#0E1514` may not on `#000000`.
- Do not disable dynamic colour to guarantee contrast. Instead ensure the layout works with
  any M3 scheme — always pair `surface` with `onSurface`, never with a hand-picked colour.

## 4. Touch targets and motor accessibility

- Minimum 48 × 48 dp for any target; 56 dp row height for lists; **72 dp** for the workout
  player's pause and skip.
- 8 dp minimum between adjacent targets.
- Full-width rows rather than small hit areas — the whole row is the target.
- No gesture is the only way to do something. Everything reachable by a swipe is also
  reachable by a tap.
- No timed interaction. Nothing requires a response within a window, and the workout timer
  itself can be paused indefinitely.
- No double-tap or long-press as a primary action.

## 5. Text scaling

- All text in **sp**. Nothing in `dp`, no `fontSize` computed from a dimension.
- Every screen must work at **200%** font scale with no clipping and no overlap. Test with
  Settings → Display → Font size at maximum.
- Prefer `Column` over fixed-height containers. Where a height must be constrained, use
  `heightIn(min = …)`, never `height(…)`.
- The countdown is the one exception: at 96 sp it is already far above any accessibility floor,
  and it should scale *less* than proportionally so it does not overflow the ring. Cap its
  effective scale at 1.3× and document it.

## 6. Motion and other system settings

- Respect reduce-motion: cross-fades become instant cuts, the progress ring still animates
  (it conveys information, not decoration).
- Respect the system dark-mode setting when `ThemePreference.SYSTEM`.
- Respect the do-not-disturb state for tones — but not for haptics, which are the fallback
  channel.
- Nothing flashes faster than 3 Hz (seizure risk). The countdown's final-3-seconds emphasis
  must pulse at most once per second.

## 7. Per-phase accessibility checklist

Run on **every** UI phase, not once at the end:

- [ ] TalkBack: navigate the whole screen with swipe-right; every stop is labelled and
      meaningful; no duplicate stops.
- [ ] TalkBack: every action is performable.
- [ ] Font scale 200%: no clipping, no overlap, everything reachable.
- [ ] Contrast: every text pair checked in light, dark **and** AMOLED.
- [ ] Colour-blind check: is any state distinguishable by colour alone? (Grayscale the
      screenshot.)
- [ ] Touch targets: 48 dp minimum, 72 dp in the player.
- [ ] Rotation: state preserved, layout usable.
- [ ] Keyboard/D-pad: every control reachable and activatable (matters for external keyboards
      and switch access).
- [ ] Reduce-motion: no essential information conveyed only by animation.

## 8. Automated tests (phase 11)

Compose UI tests asserting:

- Every node with a click action has a non-empty `contentDescription` or text.
- No node smaller than 48 dp has a click action.
- Every image node has either a description or an explicit null.
- Settings rows expose `Role.Switch` and a toggleable state.

Plus Espresso's `AccessibilityChecks.enable()` on the instrumentation suite, which catches
contrast and target-size violations automatically.

Automated checks cannot verify that a label is *useful*. "Button" passes every automated
check and tells the user nothing. The manual TalkBack pass in §7 is not optional.

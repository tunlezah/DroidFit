# Phase 11 — Accessibility audit

## Objective
A full audit and the automated checks that keep it from regressing.

**This is not the first time accessibility is considered** — every UI phase ran the §7 checklist.
This phase is the systematic sweep and the automation.

## Read first
- `framework/12_accessibility_spec.md` — all of it
- `feature-settings/SettingsScreen.kt` — the reference pattern for row semantics

## Agents
**Accessibility** (leads, can veto). UI Design. UX Research.

## Files you may touch
- Any UI module
- `app/src/androidTest/**`
- `project_memory/*`

## Work

### 1. Manual TalkBack pass, every screen
Navigate each screen entirely by swipe-right. For each:
- Every stop is labelled and meaningful. "Button" passes automated checks and tells the user
  nothing.
- **No duplicate stops.** The most common defect: a row and its switch both focusable. The fix is
  `onCheckedChange = null` on the inner control, as in the built settings row.
- Every action performable.
- Illustration descriptions describe the position, not the artwork.
- Confirm the countdown is **not** a live region (spec §2) — a per-second announcement would be
  unusable. This is a deliberate divergence from "announce state changes"; do not "fix" it.

### 2. Contrast, all three themes
Every text/background pair: 4.5:1 normal, 3:1 large. Measured with a checker, not eyeballed.

Re-measure in **AMOLED** separately. Pure black changes ratios, and a colour that passed on
`#0E1514` may fail on `#000000`.

### 3. Colour-blind check
Grayscale a screenshot of every screen. Any state distinguishable by colour alone is a defect
(REQ-022). The threshold/vigorous pair is an amber/red confusion and it is the distinction that
matters most mid-session.

### 4. Text scaling
Every screen at 200%. No clipping, no overlap, everything reachable. The countdown's scale cap
(1.3×) should keep it inside the ring.

### 5. Motor accessibility
48 dp minimum, 56 dp rows, 72 dp player controls, 8 dp separation. Every gesture has a tap
alternative. Nothing timed. No double-tap or long-press as a primary action.

### 6. Keyboard and D-pad
Every control reachable and activatable. Matters for external keyboards and switch access.

### 7. Automated checks
Spec §8. Compose UI tests asserting: clickable nodes have labels; nothing under 48 dp is clickable;
images have a description or explicit null; settings rows expose `Role.Switch`.

Plus `AccessibilityChecks.enable()` on the instrumentation suite for automatic contrast and
target-size checking.

## Exit criteria
- [ ] TalkBack pass on every screen; no unlabelled or duplicate stops.
- [ ] Contrast verified in light, dark and AMOLED, with measurements recorded.
- [ ] Grayscale check on every screen; no colour-only state.
- [ ] 200% font scale on every screen.
- [ ] All touch targets and separations meet the minimums.
- [ ] Keyboard/D-pad traversal complete.
- [ ] Automated accessibility tests written and green.
- [ ] `AccessibilityChecks.enable()` active on the instrumentation suite.
- [ ] Reduce-motion respected; nothing flashes above 3 Hz.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `decisions.md` — phase-log row; any deliberate divergence from a general accessibility
  convention (the countdown live-region decision is the example — record it as reaffirmed).
- `known_issues.md` — any violation you could not fix, with severity and what it blocks.
- `technical_debt.md` — anything fixed partially.

## Do not
- Trade accessibility for aesthetics. This agent's objections are not negotiable against visual
  preference.
- Rely on automated checks alone. They cannot tell whether a label is *useful*.
- Make the countdown a live region.

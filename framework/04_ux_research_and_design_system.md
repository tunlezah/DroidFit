# 04 — UX research and design system

The design brief and its rationale. Implementation lives in `core:designsystem`.

---

## 1. Who this is for, and when

One user, on a Motorola Edge 60, in one of three situations:

| Situation | Phone position | Constraint |
|---|---|---|
| **On a mat** (floor/reformer Pilates) | On the floor, arm's length, often viewed from lying down | Read at ~70 cm, sometimes upside-down relative to the body. Sweaty hands |
| **On a machine** (elliptical, spin) | In a cradle or propped, 60–80 cm away, moving | Read at distance, at a glance, while breathing hard. **No touch interaction expected mid-effort** |
| **Planning or reviewing** | In hand | Normal phone use, one-handed, possibly on the sofa |

Everything in this design system follows from those three. In particular: the workout player
is a *display* first and a *control surface* second. If the user has to touch it mid-interval,
the design has failed.

## 2. Design principles

1. **The timer is the interface.** During a session, the countdown is the largest thing on
   screen by a wide margin. Everything else is secondary.
2. **Never make the user look to know their state.** Spoken cues, colour, and haptics all
   carry state so the screen is confirmation rather than the only channel.
3. **Honest by construction.** No number the app cannot justify; no claim the evidence does
   not support; no disabled control without a stated reason.
4. **Calm, clinical, not hype.** Deep teal rather than urgent red. The app is a tool, not a
   coach shouting.
5. **Every touch target is generous.** 56 dp minimum row height, full-width rows, because
   hands are sweaty and attention is elsewhere.

## 3. Colour

Tokens in `core/designsystem/theme/Colour.kt`.

**Primary: deep teal** (`Teal40` light, `Teal80` dark). Chosen because the obvious
alternative — the orange/red of the fat-burning genre — reads as urgency and hype, and this
app's differentiator is that it does not overclaim. Teal reads as clinical.

**Tertiary: warm amber**, reserved exclusively for streaks and achievements, so a warm
colour appearing means something specific.

**Intensity zones** (`ZoneColours`) are semantic, not decorative. The same zone is the same
colour on the player ring, the session summary and the history chart. Light and dark variants
are separately tuned for contrast, not algorithmically derived.

| Zone | Light | Dark | Meaning |
|---|---|---|---|
| Recovery | `#6E8B8C` | `#9FBDBE` | Easy, RPE 2–3 |
| Zone 2 | `#00786D` | `#4FDBCB` | Conversational, RPE 3–4 |
| Threshold | `#A85D00` | `#FFB95C` | Hard, RPE 6–7 |
| Vigorous | `#B3261E` | `#FF8A80` | Very hard, RPE 8–9 |
| Rest | `#7A8A8B` | `#8B9A9B` | Not working |

**Colour is never the only signal** (REQ-022). Roughly 1 in 12 men has a colour vision
deficiency, and the vigorous/threshold pair is exactly a red-orange confusion. Each state
also carries: a text label, a distinct progress-ring stroke weight, and a distinct haptic
pattern.

**AMOLED mode** collapses dark surfaces to true black. Not a gimmick: unlit pixels on the
Edge 60's pOLED panel draw no power, and a workout screen held open for 45 minutes is exactly
the case where that matters. `dynamicColour` is force-disabled in AMOLED mode because a
wallpaper-derived surface is never pure black.

## 4. Typography

System font throughout — no bundled font (ADR-0011).

**The countdown uses `FontFamily.Monospace`.** With proportional digits the timer visibly
jitters as digit widths change while counting down, which is distracting when it is the only
thing on screen. Monospace is the reliable way to get fixed advance widths without shipping a
variable font. The typeface difference from the rest of the UI is deliberate — it reads as
instrumentation.

| Style | Size | Use |
|---|---|---|
| `TimerTypography.Countdown` | 96 sp | Default player countdown |
| `TimerTypography.CountdownLarge` | 148 sp | Machine mode — legible at 2 m |
| `TimerTypography.SegmentLabel` | 22 sp | Current exercise name |
| M3 `displayLarge`…`labelLarge` | default | Everything else, weights nudged toward SemiBold |

Sizes are in **sp**, so they honour the user's font-size setting. Nothing in the app uses `dp`
for text.

## 5. Layout

**Portrait, in hand:** single column, 20 dp horizontal padding, primary action at the bottom
within thumb reach.

**Portrait, machine mode:** countdown fills the upper 60%, next-exercise name below,
controls at the very bottom in 72 dp targets.

**Landscape:** countdown left, exercise detail right. Must work in v1 (REQ-073); a
purpose-designed layout is FF-0009.

**Edge-to-edge is mandatory** — Android 15 enforces it for targetSdk 35+. The Edge 60 has a
curved display, so content must never sit in the last few dp of horizontal space. Use
`WindowInsets` and `safeDrawing`; never hard-code a status-bar height.

## 6. Motion

- Segment transitions: a 300 ms cross-fade plus a scale-in on the new exercise name. Enough
  to register peripherally, short enough not to hide the timer.
- The progress ring animates continuously, driven by elapsed time, not by recomposition
  count.
- Nothing bounces or overshoots during a workout. Playful motion is fine on Settings and
  History; during a session it is noise.
- All motion respects the system reduce-motion setting: cross-fades become instant cuts.

## 7. Component inventory

Built (skeleton): theme, type scale, four icons, exercise-illustration harness with
placeholder, `KeepScreenOn`, settings row with correct toggle semantics.

To build (phase 04):

| Component | Notes |
|---|---|
| `TimerRing` | Circular progress, zone-coloured, stroke weight varying by zone |
| `CountdownDisplay` | Monospace digits, machine-mode variant |
| `SegmentCard` | Current exercise: name, illustration, cues, muscles |
| `NextUpBanner` | The upcoming exercise, shown during rest |
| `IntensityChip` | Zone name + RPE range + colour + non-colour marker |
| `DurationPicker` | The 8 presets plus a custom entry |
| `StatTile` | One number, one label, honest about nulls (`—`) |
| `WeeklyGoalBar` | Minutes against goal, with the WHO range marked |
| `EmptyState` | Icon + heading + explanation + optional action |
| `SafetyNotice` | The pre-first-session acknowledgement (REQ-005) |

Every component ships with a `@Preview` in light, dark and AMOLED. A component with no
preview cannot be reviewed without running the app, so it is not finished.

## 8. Copy voice

- Second person, present tense, plain British English. "Sit tall", not "The user should
  maintain an upright posture".
- Describe what to *do*, then what to *avoid*. Never explain physiology mid-interval.
- Never predict an outcome for this user. See `02_evidence_base.md` §6 for the prohibited
  claims list — it is not optional style guidance.
- Numbers are always qualified when estimated: `~410 kcal estimated`, never `410 kcal`.
- Errors state what happened, why, and what to do. "No exercises match Reformer Pilates at
  Advanced. Enable another exercise type, or choose Intermediate." — never "Something went
  wrong".

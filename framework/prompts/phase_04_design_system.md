# Phase 04 — Design system completion

## Objective
Build the component inventory the later phases assemble screens from, including machine mode.

## Read first
- `framework/04_ux_research_and_design_system.md` — especially §1 (the three use situations) and
  §7 (the inventory)
- `framework/12_accessibility_spec.md`
- `core/designsystem/` as built

## Agents
**UI Design** (leads). Accessibility. UX Research.

## Files you may touch
- `core/designsystem/**`
- `project_memory/*`

## Work

Build each component from the §7 inventory:

| Component | Key requirement |
|---|---|
| `TimerRing` | Zone-coloured; **stroke weight varies by zone** so it is distinguishable without colour. Driven by elapsed time, not recomposition count |
| `CountdownDisplay` | Monospace. 96 sp default, 148 sp machine mode. Font scale capped at 1.3× so it cannot overflow the ring |
| `SegmentCard` | Exercise name, illustration, how-to steps, muscles |
| `NextUpBanner` | The upcoming exercise, for display during rest |
| `IntensityChip` | Zone name + RPE range + colour + a non-colour marker |
| `DurationPicker` | Eight presets plus custom entry, 3–120 min, out-of-range rejected with an explanation |
| `StatTile` | One number, one label. **Renders `—` for null**, never `0` |
| `WeeklyGoalBar` | Minutes against goal, with the WHO range marked |
| `EmptyState` | Icon + heading + explanation + optional action |
| `SafetyNotice` | The pre-first-session acknowledgement. Content from `02_evidence_base.md` §7 |

Rules that apply to all of them:
- Every component gets `@Preview` in light, dark **and** AMOLED. No preview means it cannot be
  reviewed without running the app, which means it is not finished.
- Stateless and parameterised. A component that reads a ViewModel is not a component.
- Colours only from `MaterialTheme.colorScheme` or `ZoneColours`.
- Text sizes only from the type scale, in `sp`.
- Spacing on the 4 dp grid.
- Test each at 200% font scale.

Also in this phase: implement **machine mode** as a real layout variant, not a font-size switch.
It drops the how-to block entirely — nobody reads technique cues at 90 rpm.

## Exit criteria
- [ ] Every inventory component exists, is stateless, and is parameterised.
- [ ] Every component previews in light, dark and AMOLED.
- [ ] Every component checked at 200% font scale.
- [ ] `TimerRing` distinguishes zones by stroke weight as well as colour.
- [ ] `StatTile` renders `—` for null and this is covered by a test or preview.
- [ ] Machine mode changes layout, not just type size.
- [ ] Accessibility checklist (`12_accessibility_spec.md` §7) run on the component gallery.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `decisions.md` — phase-log row; a `D-` entry for each component API decision that had a real
  alternative.
- `known_issues.md` — any component that could not meet the accessibility bar.
- `technical_debt.md` — any component built simpler than specified, with the repayment trigger.

## Do not
- Let a component reach for a ViewModel or a repository.
- Add a colour to `Palette` without a contrast check in all three themes.
- Build a component you cannot preview.

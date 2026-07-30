# Phase 03 — Illustrations

## Objective
Draw a Compose vector illustration for every `illustration_id` in the catalogue. Closes KI-0003.

## Read first
- `framework/11_illustration_spec.md` — all of it
- `core/designsystem/src/main/kotlin/com/visceralfit/core/designsystem/illustration/ExerciseIllustration.kt`
  — the two worked examples and the `Figure` helpers are the reference

## Agents
**UI Design** (leads). Accessibility (for content descriptions). Fitness Science (a drawing that
depicts bad form is a safety issue).

## Files you may touch
- `core/designsystem/src/main/kotlin/**/illustration/**`
- `project_memory/*`

## Work

1. **Inventory.** List every distinct `illustration_id` in the catalogue. Reuse ids where the
   position is genuinely identical rather than duplicating drawings.
2. **Draw in batches by modality**, so the visual language stays consistent within a group.
   Follow the procedure in spec §5: sketch in the 100×100 box on paper first — choosing
   coordinates by trial in code takes far longer.
3. **Accent the moving part only.** This is the drawing's most useful information: it answers
   "which bit moves?" at a glance. Static limbs are `stroke`; the moving limb is `accent`.
4. **Content descriptions** describe the *position*, not the artwork (spec §6). Base position
   first, then limbs, then what moves. Under 20 words. Never mention that it is a drawing.
5. **Preview every drawing** in light, dark and AMOLED. Review them as a set — inconsistency
   between drawings is more noticeable than imperfection in one.
6. **Check the size impact.** Sixty drawings are sixty pieces of code. Run
   `./gradlew :app:assembleRelease` and confirm the size gate still passes. If illustrations add
   more than about a megabyte, they are more detailed than their purpose warrants.

## Exit criteria
- [ ] Every `illustration_id` in the catalogue has a drawing; no exercise falls back to the
      placeholder.
- [ ] Every drawing has a `@Preview` and a position-describing `contentDescription`.
- [ ] No hard-coded colours anywhere in the illustration package.
- [ ] All drawings use the `Figure` helpers; no raw pixel coordinates.
- [ ] Visual review of all drawings in light, dark and AMOLED, as a set.
- [ ] Fitness Science confirms no drawing depicts a position the safety notes warn against.
- [ ] APK size gate still passes.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `known_issues.md` — close KI-0003. Open an entry for any position that could not be drawn
  legibly at 24 dp or in a list thumbnail.
- `decisions.md` — phase-log row; a `D-` entry for any illustration-style convention you had to
  invent beyond the spec (add it to the spec too).

## Do not
- Add an image asset, a font, or an icon library. ADR-0003 is not negotiable, and the 66 MB
  `material-icons-extended` incident is why the size gate exists.
- Hard-code a colour "just for this one".
- Write a content description like "illustration of the dead bug exercise" — the exercise name is
  already announced separately.

# 11 — Illustration specification

How to draw the exercise artwork. Implementation:
`core/designsystem/illustration/ExerciseIllustration.kt`, which already contains two worked
examples (`pilates_dead_bug`, `spin_seated`) and the placeholder.

Constraint from ADR-0003: **no image assets of any kind.** No stock art, no photography, no
downloaded content, no bundled fonts. Everything is drawn on a Compose `Canvas`.

---

## 1. Why drawn, not imported

| Alternative | Why rejected |
|---|---|
| Licensed stock vectors | Attribution an offline app cannot discharge; per-asset cost |
| Commissioned artwork | Cost, and a dependency on a person for every content addition |
| Raster demos (GIF/WebP) | ~60 demos is tens of megabytes in an app whose appeal is being 2.4 MB |
| Video | All of the above, plus playback complexity and no offline story |

Drawn figures cost authoring time and produce schematic stick figures rather than photography.
That trade is accepted: the **teaching load is carried by the written and spoken cues**
(REQ-041), and the diagram provides orientation — "am I on my back or my side?" — not
technique.

## 2. The drawing system

Every drawing works in a **100 × 100 logical box**, scaled to the available size by the
`Figure` helpers:

```kotlin
private object Figure {
    const val CANVAS = 100f
    fun DrawScope.p(x: Float, y: Float): Offset   // logical point -> pixels
    fun DrawScope.unit(value: Float): Float       // logical length -> pixels
}
```

Never use raw pixel coordinates. A drawing authored in the logical box is resolution- and
size-independent, which is what makes it work at 444 ppi and in a 48 dp list thumbnail alike.

## 3. Colour rules

Only two colours, both passed in from the theme:

| Colour | Use |
|---|---|
| `stroke` = `MaterialTheme.colorScheme.onSurface` | The body, equipment, mat |
| `accent` = `MaterialTheme.colorScheme.primary` | **The moving part only** |

`stroke.copy(alpha = 0.35f–0.6f)` for context elements: the mat, the machine frame, the
flywheel.

**Never hard-code a colour in a drawing.** Doing so breaks dark mode, AMOLED mode and dynamic
colour simultaneously. The accent-for-the-moving-part convention is the drawing's most useful
information: it answers "which bit moves?" at a glance.

## 4. Anatomy conventions

Consistency matters more than accuracy — a user should recognise the visual language across
all 60 drawings.

| Part | Convention |
|---|---|
| Head | Filled circle, radius 6 logical units |
| Limbs and torso | Lines, stroke width 3.2, `StrokeCap.Round` |
| Mat / floor | Line at y ≈ 78, `alpha = 0.35` |
| Equipment frame | Stroke width 2.4, `alpha = 0.45–0.6` |
| Body orientation | Head to the **left** when supine or side-lying; facing **right** when seated or standing |
| Joints | Not drawn. The line bend is the joint |

Keep the figure within x ∈ 8..92, y ∈ 8..92 so nothing clips.

## 5. Authoring procedure

1. Read the exercise's `how_to` and pick **the single most informative moment** — usually the
   position of maximum challenge, not the start.
2. Sketch on paper in the 100 × 100 box first. Coordinates chosen by trial in code take far
   longer.
3. Add a branch in `ExerciseIllustration`'s `when` keyed on the `illustrationId`.
4. Write a `private fun DrawScope.drawXxx(stroke: Color, accent: Color)`.
5. Draw in this order: context (mat, machine) → torso → head → static limbs → **moving limbs
   in accent**.
6. Add a `@Preview` with a real `contentDescription`.
7. Check the preview in light, dark and AMOLED.

## 6. Content descriptions

The `contentDescription` is not a caption — it is the **only** description a screen-reader
user gets.

Describe the **position**, not the artwork:

| Bad | Good |
|---|---|
| "Line drawing of a person" | "Lying on the back, knees bent above the hips, opposite arm and leg extending away." |
| "Dead bug exercise" | (same as above — the name is already announced separately) |
| "Illustration of cycling" | "Seated on an indoor cycle, spine tall, hands resting on the bars." |

Rules: state the base position first, then limb positions, then what is moving. Present tense.
Under 20 words. Never mention that it is a drawing.

## 7. The placeholder

Unknown ids fall back to `drawPlaceholderFigure` — a plain standing figure inside a faint
frame. It is deliberately plain so it reads as **"no diagram"** rather than as a wrong diagram
the user might try to copy. A missing drawing must never block a workout (REQ-046).

Content description for the placeholder: "No diagram available for this exercise yet."

## 8. Phase 03 exit criteria

- [ ] Every `illustration_id` in the seed catalogue has a drawing (KI-0003).
- [ ] Every drawing has a `@Preview` and a position-describing `contentDescription`.
- [ ] No hard-coded colours anywhere in the illustration package.
- [ ] All drawings use the `Figure` helpers; no raw pixel coordinates.
- [ ] Visual review of every drawing in light, dark and AMOLED.
- [ ] Ids are reused where the position is genuinely identical, rather than duplicated.
- [ ] The APK size gate still passes — drawings are code, and 60 of them add up. If the APK
      grows past a megabyte from illustrations, the drawings are too detailed for their
      purpose.
- [ ] `project_memory` updated: close KI-0003, record any position that could not be drawn
      legibly.

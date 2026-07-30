# Phase 05 — Navigation

## Objective
Convert to type-safe routes before any destination takes an argument, add the custom duration
entry point, and get back-handling right. Closes TD-0005.

## Read first
- `framework/10_screen_specs.md` §2
- `framework/15_device_targets_motorola_edge_60.md` §3 (predictive back)
- `project_memory/technical_debt.md` TD-0005

## Agents
**Android Architecture** (leads). UX Research.

## Files you may touch
- `app/src/main/kotlin/**`
- `feature-*/src/main/kotlin/**` — route signatures only
- `project_memory/*`

## Work

1. **Type-safe routes.** Replace the string routes with `@Serializable` route objects. Do this
   **now**, before arguments exist — TD-0005 exists because a typo'd string route is a runtime
   crash rather than a compile error, and that only bites once arguments arrive.
   Routes are declared in `app`, never inside a feature module. A feature that knows its own
   route cannot be renamed or reused without touching it.

2. **Add the destinations later phases need**, as argument-carrying routes: session summary
   (session id), exercise detail (exercise id), workout player (workout id), measurement entry
   (measurement kind).

3. **Custom duration.** Add the entry point on the Train screen. 3–120 minutes, 1-minute
   granularity (A-0003). Out-of-range input is **rejected with an explanation**, never silently
   clamped — a user who types 150 and gets 120 without being told will not understand what
   happened.

4. **Style availability.** Disable style chips the chosen duration cannot support, per the
   minimums in `07_workout_engine_spec.md` §2, with the reason visible: "Intervals need at least
   16 minutes". This is REQ-024, and it prevents the user picking a style and then hitting a
   generation failure.

5. **Back handling.**
   - Top-level: back exits from the start destination, otherwise returns to it.
   - Workout player: back **prompts** before discarding an in-progress session. An accidental
     edge swipe on a curved-edge phone must not silently end a 45-minute effort.
   - Predictive back must still animate — do not intercept it in a way that breaks the preview.

6. **Deep links** for the workout player, so the foreground-service notification can bring the
   user back into a running session.

## Exit criteria
- [ ] All routes type-safe; no string route literals remain.
- [ ] Routes declared only in `app`.
- [ ] Custom duration accepts 3–120 min; out-of-range rejected with a message; tested.
- [ ] Style chips disabled with a visible reason when the duration cannot support them.
- [ ] Back from the player prompts; predictive back still animates.
- [ ] Notification deep link returns to the running session.
- [ ] Navigation test covering every route and its arguments.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `technical_debt.md` — close TD-0005.
- `decisions.md` — phase-log row; the route argument shapes.
- `known_issues.md` — any navigation edge case you could not resolve.

## Do not
- Silently clamp an out-of-range duration.
- Put a route constant inside a feature module.
- Intercept back without handling the predictive gesture.

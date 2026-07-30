# 16 — Glossary

Terms used precisely in this project. Where a term has a loose everyday meaning and a precise
meaning here, the precise one governs.

---

## Exercise science

**Visceral adipose tissue (VAT) / visceral fat** — fat stored around the abdominal organs, as
distinct from subcutaneous fat under the skin. Measurable only by imaging (CT, MRI, DXA).
**This app never estimates it** (REQ-090).

**Subcutaneous adipose tissue (SAT)** — fat under the skin. Responds somewhat differently to
exercise intensity than VAT (`02_evidence_base.md` §1.2).

**Spot reduction** — the idea that exercising a body part reduces fat there specifically. **Not
supported.** The app never implies it.

**MET (metabolic equivalent of task)** — energy cost relative to rest. 1 MET ≈ quiet sitting.
Used as `kcal = MET × kg × hours`. Population averages, not personal measurements.

**HIIT (high-intensity interval training)** — repeated bouts at 85–95% HRmax with recovery
between. Here, the `WorkoutStyle.HIIT` style.

**MICT (moderate-intensity continuous training)** — sustained moderate work. Roughly this
app's `ZONE_2`.

**Zone 2** — in this app, 60–70% HRmax / RPE 3–4, conversational pace. Note that the term is
contested: other models put it at 72–82% HRmax. The conservative definition was chosen
deliberately (`02_evidence_base.md` §2).

**RPE / Borg CR10** — rating of perceived exertion, 1–10. The app's primary intensity language,
because it needs no sensor.

**HRmax** — maximum heart rate. Age-predicted estimates have wide individual error, which is
why the app never computes a target heart rate for the user to chase.

**Norwegian 4×4** — 4 × 4 min at 85–95% HRmax with 3 min active recovery (`helgerud2007`). The
canonical HIIT template.

**Talk test** — using speech ability to gauge intensity. Full sentences ≈ Zone 2; a word or two
≈ vigorous.

**FITT-VP** — Frequency, Intensity, Time, Type, Volume, Progression. ACSM's prescription
framework.

**Reformer** — the spring-loaded carriage apparatus used in reformer Pilates. Requires
equipment, hence off by default (D-0004).

**Active recovery** — continued light movement between hard efforts, distinct from full rest.
`SegmentKind.ACTIVE_RECOVERY`.

**SUCRA** — surface under the cumulative ranking curve. In a network meta-analysis, the
probability that an intervention is the best. Expresses *ranking*, not effect magnitude.

## App domain

**Modality** — an exercise category the user can enable or disable: floor Pilates, reformer
Pilates, elliptical, spin bike. `Modality`.

**Style** — the intensity architecture of a session: HIIT, Zone 2, Mixed, Recovery.
`WorkoutStyle`.

**Segment** — the atomic unit the timer counts down. Exactly one is active at a time.
`Segment`.

**Segment kind** — `WORK`, `ACTIVE_RECOVERY`, `REST`, `TRANSITION`.

**Block** — a phase of a session: `WARM_UP`, `MAIN`, `COOL_DOWN`. `WorkoutBlock`.

**Pool** — a filtered set of eligible exercises for a role (warm-up, vigorous, mobility…).
Derived, not stored (`07_workout_engine_spec.md` §3).

**Seed** — the `Long` that makes generation reproducible. Same request + same seed = identical
workout.

**Seed version** — the version of the bundled exercise catalogue. Distinct from the *seed*
above; unfortunate collision, both terms are established.

**Completion ratio** — fraction of a session actually performed. ≥ 0.7 counts toward streaks
and volume.

**Active duration** — time spent moving, excluding paused time. What gets recorded, as opposed
to wall-clock.

**Caution tag** — a contraindication marker on an exercise (`lower_back`, `knee`, …) that a
user can exclude. Closed set — see `08_exercise_library_spec.md` §2.

**Machine mode** — oversized timer and controls for a phone in a bike or elliptical cradle.

**Cue** — one spoken utterance. `SpeechCue`, with a priority.

**Stale cue** — a cue that could not start within its window and is discarded rather than
spoken late.

## Android and build

**AGP** — Android Gradle Plugin. Pinned to 8.13.2 (TD-0002).

**KSP** — Kotlin Symbol Processing. Generates Room and Hilt code.

**BOM** — bill of materials. The Compose BOM sets all Compose artifact versions coherently;
never pin them individually.

**R8** — the shrinker and optimiser. Turns a 32.58 MB build into 2.38 MB (ADR-0012).

**Version catalogue** — `gradle/libs.versions.toml`. The only place a version may appear.

**Foreground service** — a service with a persistent notification, allowed to run while the app
is backgrounded. The workout service is typed `mediaPlayback` (ADR-0008).

**Edge-to-edge** — drawing behind the system bars. Enforced for targetSdk 35+.

**Insets** — the regions occupied by system UI and, on a curved display, the physically
distorted edges. Consumed with `WindowInsets.safeDrawing`.

**Dynamic colour** — Material You's wallpaper-derived palette. API 31+. Disabled in AMOLED
mode, because a wallpaper-derived surface is never pure black.

**AMOLED mode** — true-black surfaces. On the Edge 60's pOLED panel unlit pixels draw no
power, which matters over a 45-minute screen-on session.

**TalkBack** — Android's screen reader.

**Predictive back** — the gesture preview of where back will take you. Must not be broken by
intercepting back.

## Process

**Project memory** — `/project_memory`, the eight append-only files. See its `README.md`.

**ADR** — architecture decision record. `architecture_decisions.md`.

**Phase** — one unit of the build plan, in `framework/prompts/`. Has exit criteria and
mandatory memory updates.

**Exit criteria** — the checkable conditions for a phase being complete. Not a summary — a
list of commands to run and assertions to make.

**Golden file** — a checked-in expected output. The only reliable way to test cross-process
determinism.

**Release blocker** — an issue that must be fixed before distribution, regardless of phase.
Currently KI-0002, KI-0005, TD-0007.

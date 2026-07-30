# Architecture decisions

Numbered ADRs for structural choices. Template: `_templates/entry_templates.md`.
Numbering: `ADR-NNNN`. An ADR is for decisions that shape *where code lives* and
*what depends on what* — narrower, longer-lived choices than `decisions.md`.

---

### ADR-0001 — Clean Architecture over three layers, features as Gradle modules
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** The PRD asks for MVVM, Clean Architecture, the repository pattern and a
  modular build. Those can be combined in several ways, and the wrong split produces
  either one giant module with architectural rules enforced only by convention, or
  thirty modules whose build overhead exceeds their benefit.
- **Decision:** Three conceptual layers, realised as Gradle modules:
  - `domain` — pure Kotlin/JVM. Models, repository *interfaces*, use cases, the workout
    engine. No `android.*` dependency, enforced by the module having no Android plugin.
  - `data` — repository *implementations*, mappers, the exercise seeder. Depends on
    `core:database`, `core:datastore`, `domain`.
  - `feature-*` — one module per top-level destination. ViewModels and composables.
    Depends on `domain` and `core:*`, never on `data` and never on another feature.
  - `core:*` — shared infrastructure with no business rules: `common`, `designsystem`,
    `database`, `datastore`, `speech`, `testing`.
  - `app` — wiring only: `Application`, `MainActivity`, the navigation graph. It is the
    only module that depends on `data`.
- **Consequences:**
  - Easy: unit-testing the engine and all programming rules on the JVM with no
    Robolectric, no emulator, millisecond test runs.
  - Easy: adding a feature without touching existing ones.
  - Hard (deliberately): a feature reaching into the database directly. It would have to
    add a dependency, which shows up in review.
  - Cost: thirteen modules means a slower clean build and more build files. Accepted;
    incremental builds are faster because a UI change does not recompile the engine.
- **Compliance:** `domain/build.gradle.kts` applies `kotlin-jvm`, not
  `android-library`. Any `android.*` import there is a compile error. No `feature-*`
  module lists `projects.data`.

### ADR-0002 — Debug signing for the distributed artefact
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** The APK must be sideloadable without store involvement. Release signing
  requires a keystore, which requires secret management and a manual bootstrap step
  before CI can ever go green.
- **Decision:** CI's published artefact is debug-signed. The `release` build type stays
  configured, minified and buildable, using the debug signing config, so introducing a
  real keystore later is a one-block change rather than a restructure.
- **Consequences:** Zero-setup installs; no secret rotation. The artefact is unsuitable
  for Play distribution. `applicationIdSuffix = ".debug"` on the debug variant means a
  locally built debug build coexists with the distributed one rather than conflicting.
- **Compliance:** The workflow contains no `secrets.*` reference for signing.
- **Refined by ADR-0012**, which settles *which variant* is shipped. This ADR governs
  signing only; the shipped artefact is the `release` variant using the debug signing
  config, so it is **not** `debuggable` despite being debug-*signed*.

### ADR-0003 — Exercise artwork drawn in Compose, no image assets
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** Every exercise needs an illustration. The options were licensed stock
  art, commissioned art, raster demos, or code-drawn vectors.
- **Decision:** Figures are drawn on a Compose `Canvas` in
  `core:designsystem/illustration`, addressed by a stable `illustrationId` on each
  exercise, with a documented placeholder for ids that have no drawing yet.
- **Consequences:**
  - No licensing or attribution obligations, which an offline app cannot discharge
    anyway.
  - APK stays in the low hundreds of kilobytes rather than tens of megabytes.
  - Scales perfectly on a 444 ppi panel and re-tints for dark and AMOLED themes for free.
  - Cost: drawings are schematic stick figures, not photography. Accepted — the teaching
    load is carried by written and spoken cues, with the diagram as orientation.
  - Cost: authoring each figure is manual work (~60 of them).
- **Compliance:** no files under `res/drawable` for exercise content; no image
  dependencies in any module.

### ADR-0004 — Preferences in DataStore, records in Room
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** Both stores were mandated. Using both for the same data would create two
  sources of truth.
- **Decision:** DataStore Preferences holds scalar settings only (toggles, enum
  selections, the weekly goal). Room holds anything that is a record with a timestamp:
  sessions, measurements, the exercise catalogue, saved workouts.
- **Consequences:** Settings reads are a `Flow` with no query cost and no migration
  machinery. Records get indexes, aggregates and real migrations. The boundary test is
  "would I ever want to query a range of these?" — if yes, Room.
- **Compliance:** no `@Entity` for a settings value; no DataStore key holding a list of
  records.

### ADR-0005 — Exercise content as a reviewable JSON asset
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** ~60 exercises of health guidance need to reach the database, and need to
  be correctable in a patch release.
- **Decision:** A versioned JSON asset, parsed with `ignoreUnknownKeys = false` and
  upserted by `ExerciseSeeder` when `version` exceeds the lowest `seed_version` in the
  table. Withdrawn exercises are deleted by id difference.
- **Consequences:** Content changes appear as a readable diff in review. Content updates
  need no schema migration. A typo'd field name fails loudly at seed time rather than
  producing an exercise with no safety notes. Cost: one parse of a small file per cold
  start, off the main thread.
- **Compliance:** `ignoreUnknownKeys` stays `false`; the seeder is idempotent.

### ADR-0006 — Body-mass sources behind a provider interface
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** The PRD sets a weight-import priority (Health Connect, then a possible
  future vendor API, then manual) and asks for a pluggable design, while v1 ships manual
  entry only.
- **Decision:** `BodyMassProvider` in `domain`, implementations bound `@IntoSet`.
  Consumers ask the set, ordered by priority, for the first available provider. v1 binds
  only `ManualEntryBodyMassProvider`.
- **Consequences:** Adding Health Connect later is a new module plus one binding — no
  call-site changes. No code asks "is Health Connect installed?"; it asks the provider
  whether it is available. Cost: one indirection for a set that currently has one member.
- **Compliance:** nothing outside a provider implementation references a specific
  integration by name.

### ADR-0007 — Coaching cues are individually switchable, not bundled presets
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** The PRD lists six coaching cue types and says each must be independently
  enabled. A "verbosity: low/medium/high" slider would be fewer controls.
- **Decision:** One boolean per cue type in `CoachingPreferences`, plus a master
  `speechEnabled`. Sub-toggles are visibly disabled, not hidden, when the master is off.
- **Consequences:** More settings rows. In exchange, a user who wants only "next
  exercise" and nothing else can have exactly that, which a preset ladder cannot
  express. Disabling rather than hiding keeps the settings screen's shape stable so
  users do not think options vanished.
- **Compliance:** no preset enum collapsing these flags.

### ADR-0008 — Timer state lives in a foreground service, not the ViewModel
- **Date:** 2026-07-30
- **Status:** Accepted (design fixed; implementation is phase 07)
- **Context:** A workout runs 5–90 minutes. The user will rotate the phone, get a call,
  switch apps, and let the screen turn off. A ViewModel survives rotation but not
  process death; a `keepScreenOn` flag does not survive backgrounding.
- **Decision:** The authoritative session clock and segment cursor live in a
  `mediaPlayback`-typed foreground service. The ViewModel observes it. `MainActivity`
  declares no `configChanges`, so recreation genuinely happens and is genuinely tested.
- **Consequences:** Rotation, backgrounding and screen-off cannot desynchronise the
  timer. Requires `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and a persistent notification.
  `mediaPlayback` is the correct type because the service's ongoing output is audio
  coaching; unlike most types it has no 6-hour cap, which a 90-minute session plus
  pauses could otherwise approach.
- **Compliance:** no `Timer`, `CountDownTimer` or `delay`-driven clock inside a
  ViewModel or composable.

### ADR-0009 — One static-analysis tool (detekt) covering both required gates
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** The PRD asks for detekt *and* ktlint in CI.
- **Decision:** detekt with the `detekt-formatting` plugin, which embeds the ktlint rule
  set. `./gradlew detekt` is the single gate; `./gradlew qualityCheck` bundles it with
  unit tests.
- **Consequences:** One configuration file, one place line length is defined, one
  Gradle-compatibility constraint instead of two. Cost: the embedded ktlint version lags
  the standalone release.
- **Compliance:** no second formatting plugin in any module.

### ADR-0010 — Route/Screen split in every feature
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** Composables that obtain their own ViewModel cannot be previewed or
  screenshot-tested without a Hilt graph.
- **Decision:** Each feature exposes a public `XxxRoute()` that resolves the ViewModel
  and collects state, and an internal stateless `XxxScreen(state, callbacks…)` that
  holds all the UI. Previews and tests drive `XxxScreen` directly.
- **Consequences:** Every screen is previewable and screenshot-testable with no DI.
  Cost: one extra function and an explicit callback list per screen — which also
  documents exactly what a screen can do.
- **Compliance:** no `hiltViewModel()` call inside a `*Screen` composable.

### ADR-0011 — No bundled fonts; monospace for timer digits
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** A proportional-digit countdown visibly jitters as the width of each digit
  changes, which is distracting when the timer is the only thing on screen. Tabular
  figures normally mean bundling a variable font.
- **Decision:** Use the system font throughout; use `FontFamily.Monospace` for the
  countdown numerals specifically.
- **Consequences:** No ~400 KB font in an APK whose appeal is being small; no licence to
  track. Digits are fixed-width so the timer is stable. Cost: the countdown's typeface
  differs from the rest of the UI. Deliberate — it reads as instrumentation.
- **Compliance:** no files under `res/font`; no font dependency.

### ADR-0012 — CI ships the release variant, debug-signed and minified
- **Date:** 2026-07-30
- **Status:** Accepted
- **Context:** ADR-0002 fixed *signing* as debug. It did not settle which *build variant*
  gets handed to a phone, and the obvious reading — "debug signing means ship the debug
  variant" — turned out to be expensive. Measured on this codebase:
  | Variant | Size |
  |---|---|
  | `debug` (unminified) | 32.58 MB |
  | `release` (R8 + resource shrinking, debug-signed) | **2.38 MB** |
- **Decision:** The published artefact is `assembleRelease`, whose `signingConfig` is the
  debug config. The debug variant is still built and unit-tested; it is simply not the
  thing that gets sideloaded.
- **Consequences:**
  - A 2.4 MB download instead of 33 MB, on an app whose selling point is being small and
    local.
  - R8 and the ProGuard rules are exercised on every push, so a keep-rule mistake in the
    Room, Hilt or kotlinx-serialization configuration surfaces immediately rather than the
    first time a release is attempted.
  - The artefact is not `debuggable`, so `adb` debugging needs the debug variant built
    locally. Acceptable — CI's job is producing something installable, not something
    attachable.
  - `applicationIdSuffix` applies only to debug, so the sideload APK installs as
    `com.visceralfit.app` and a locally built debug build coexists with it.
- **Compliance:** the workflow calls `assembleRelease`, and asserts the APK stays under a
  12 MB ceiling. That guard exists because the icon-library regression below happened.
- **Related:** the `material-icons-extended` dependency inflated the debug APK to 66 MB
  before being removed (see `core/designsystem/icon/VisceralFitIcons.kt`). The size check
  is what stops that recurring unnoticed.

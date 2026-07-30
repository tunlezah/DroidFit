# Decisions

Append-only log of choices with more than one defensible answer. Template:
`_templates/entry_templates.md`. Numbering: `D-NNNN`.

---

## Phase log

| Phase | Completed | New assumptions? | Notes |
|---|---|---|---|
| Framework authoring (phase −1) | 2026-07-30 | Yes — A-0001..A-0008 | Framework, skeleton and CI created. Verified locally: `qualityCheck` green, release APK 2.38 MB, debug APK 32.58 MB. D-0001..D-0016, ADR-0001..ADR-0012, KI-0001..KI-0008, TD-0001..TD-0008, R-0001..R-0008, FF-0001..FF-0009, UF-0001..UF-0004 |

---

### D-0001 — Deliver a compiling skeleton alongside the framework, not documents alone
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** Ship a verified-building Gradle project, a working GitHub Action and a
  functional vertical slice (Settings → DataStore → UI) together with the specification
  documents.
- **Alternatives considered:**
  - *Documents only.* Rejected: the building agent would spend its first hours
    guessing at Gradle, AGP and Hilt version compatibility, which is exactly the class
    of problem that cannot be resolved by reasoning — only by running a build.
  - *Build the whole app immediately.* Rejected: it produces no reusable framework and
    no reviewable plan.
- **Reason:** Toolchain compatibility in the Android ecosystem is empirical. Three
  version incompatibilities were found and fixed by actually compiling (see D-0002,
  D-0008, TD-0001); none would have been caught by reading documentation.
- **Reverses if:** the toolchain baseline goes stale enough that the pinned versions
  no longer resolve, at which point the skeleton becomes a liability rather than a
  head start.
- **Affects:** whole repository.

### D-0002 — Pin Hilt 2.58, not the latest 2.60.x
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `hilt = "2.58"` in the version catalogue.
- **Alternatives considered:**
  - *Hilt 2.60.1 (latest).* Fails the build outright: "The Hilt Android Gradle plugin
    is only compatible with Android Gradle plugin (AGP) version 9.0.0 or higher".
  - *Move to AGP 9.3.x to unlock newer Hilt.* Rejected for now — AGP 9 is a major
    version with breaking DSL changes, and detekt 1.23.8 (the current stable) is not
    validated against Gradle 9.
- **Reason:** 2.58 is the newest Hilt that works with the AGP 8.13.2 baseline. Binary
  compatibility was established by running the build, not by reading release notes.
- **Reverses if:** the project moves to AGP 9 (see TD-0002), at which point Hilt should
  move to the latest 2.60+ in the same change.
- **Affects:** `gradle/libs.versions.toml`.

### D-0003 — Debug-signed APK as the sideload artefact
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** CI publishes a debug-signed APK. No keystore, no GitHub secrets.
- **Alternatives considered:**
  - *Release signing with a keystore in secrets.* Rejected for now: CI is red until a
    human generates and uploads a keystore, and the project is explicitly not
    Play-Store-bound.
  - *Conditional release signing when secrets exist.* Considered and offered; the
    operator chose debug-only for simplicity.
- **Reason:** The requirement is "a working APK that can be sideloaded". A debug-signed
  APK satisfies that with zero setup and zero secret management.
- **Reverses if:** the app is ever distributed beyond the operator's own devices, or
  needs Play Integrity / any signature-gated API. Migration path is documented in
  `framework/14_ci_cd_and_release.md`.
- **Affects:** `.github/workflows/android-ci.yml`, `app/build.gradle.kts`.
- **Consequence to remember:** the debug build carries `applicationIdSuffix = ".debug"`,
  so it installs alongside — not over — any future release build.

### D-0004 — Reformer Pilates enabled in the catalogue but off by default
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `Modality.DEFAULT_ENABLED` excludes `REFORMER_PILATES`; a separate
  `availableEquipment` set gates whether any equipment-dependent modality can be used
  for generation.
- **Alternatives considered:**
  - *On by default like the others.* Rejected: most users do not own a reformer, and a
    generated workout the user physically cannot perform is worse than one fewer
    option.
  - *Omit reformer entirely.* Rejected: it was explicitly requested.
- **Reason:** Separating "I want this kind of training" from "I have this machine"
  keeps both answers honest and makes the failure mode explanatory rather than silent.
- **Reverses if:** user feedback shows the two-toggle model confuses more than it helps.
- **Affects:** `domain/model/Modality.kt`, `domain/model/Preferences.kt`,
  `feature-settings`.

### D-0005 — Energy estimate returns null rather than a default body mass
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `EstimateEnergyExpenditure` returns `null` when body mass is unknown;
  the UI renders `— kcal`.
- **Alternatives considered:**
  - *Assume a population-average mass (e.g. 70 kg).* Rejected: users read displayed
    numbers as measurements. A wrong-by-30% figure biases every trend and every
    week-over-week comparison built on it.
  - *Force body mass entry at onboarding.* Rejected: a mandatory body-weight prompt is
    a hostile first screen for a weight-related app, and the feature is optional.
- **Reason:** An absent number is honest. A fabricated one is not, and cannot be
  detected downstream.
- **Reverses if:** never, for the default path. A user who opts into an estimate by
  entering their mass gets one.
- **Affects:** `domain/usecase/EstimateEnergyExpenditure.kt`, `feature-history`.

### D-0006 — Rest segments charged at 1.3 MET, not zero
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** Rest and transition segments contribute at 1.3 MET (quiet sitting,
  Compendium code 07021); active recovery at 3.5 MET (Compendium 01210).
- **Reason:** During rest the user is sitting on a bike, not asleep. Charging zero
  understates a 40-minute interval session by a few percent for no reason.
- **Reverses if:** a better-supported resting value appears in a Compendium update.
- **Affects:** `domain/usecase/EstimateEnergyExpenditure.kt`.

### D-0007 — detekt with `detekt-formatting` instead of a separate ktlint plugin
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** One quality tool. `detekt-formatting` wraps the ktlint rule set, so
  `./gradlew detekt` covers both gates the PRD asks for.
- **Alternatives considered:** *detekt + jlleitschuh ktlint-gradle.* Rejected: two
  plugins, two configs, two chances to disagree about line length, and the ktlint
  plugin adds another Gradle-compatibility constraint for no additional coverage.
- **Reason:** Fewer moving parts in the build; identical rule coverage.
- **Reverses if:** detekt's bundled ktlint version falls far behind the standalone one.
- **Affects:** root `build.gradle.kts`, `config/detekt/detekt.yml`, CI.

### D-0008 — minSdk 29, compileSdk/targetSdk 36
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `minSdk = 29`, `compileSdk = targetSdk = 36`.
- **Reason:** The Motorola Edge 60 ships Android 15 (API 35). Targeting 36 keeps the
  app forward-compatible with the next release, and API 35+ behaviours (enforced
  edge-to-edge, foreground-service type rules) apply either way. minSdk 29 keeps
  scoped storage, `Modifier`-friendly window insets and a modern `TextToSpeech`
  surface without carrying compatibility branches for Android 8-era devices the
  operator does not use.
- **Reverses if:** a target user has a device below API 29, or a required API needs
  minSdk above 29.
- **Affects:** `gradle/libs.versions.toml`.

### D-0009 — No INTERNET permission at all
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** The manifest declares no `android.permission.INTERNET`.
- **Reason:** It converts the offline-first goal from a promise into an enforced
  property: any dependency that tries to make a network call fails loudly in testing
  rather than quietly exfiltrating something in production.
- **Reverses if:** an optional integration (Health Connect does *not* need it; a
  future remote backup would) is added, which requires an ADR first.
- **Affects:** `app/src/main/AndroidManifest.xml`.

### D-0010 — Exercise catalogue ships as a JSON asset, seeded into Room
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `app/src/main/assets/exercises_seed.json` is the source of truth;
  `ExerciseSeeder` upserts it into Room on cold start when the bundled version is newer.
- **Alternatives considered:**
  - *Prepackaged `.db` via `createFromAsset`.* Rejected: a binary file cannot be
    reviewed. When the content is health guidance, a reviewer must be able to see that
    a safety note changed.
  - *Hard-coded Kotlin list.* Rejected: mixes content with code and bloats the dex.
- **Reason:** Reviewability of health content, plus content updates without schema
  migrations.
- **Reverses if:** the catalogue grows large enough that parsing cost at startup
  becomes measurable (it is ~60 records; it will not).
- **Affects:** `data/seed/ExerciseSeeder.kt`, `app/src/main/assets/`.

### D-0011 — Four hand-authored icons instead of an icon library
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** The app's four navigation icons are declared as `ImageVector`s in
  `core/designsystem/icon/VisceralFitIcons.kt`. No icon dependency of any kind.
- **Alternatives considered:**
  - *`material-icons-extended`.* **Tried and reverted.** It inflated the debug APK from a few
    megabytes to **66 MB**, because it dexes several thousand `ImageVector` declarations and a
    debug build does not shrink them.
  - *`material-icons-core`.* Covers only two of the four glyphs, is a separate artefact Material 3
    does not bring in, and is on the deprecated path in current Compose. A dependency plus a
    version constraint for two icons.
- **Reason:** Measured, not preferred. Four icons is not worth a dependency, and the APK size is
  one of the app's actual selling points.
- **Reverses if:** the icon count grows past roughly a dozen, at which point `material-icons-core`
  becomes worth its constraint — but never `material-icons-extended` in an unminified variant.
- **Affects:** `core/designsystem/icon/`, `app/ui/VisceralFitApp.kt`, `gradle/libs.versions.toml`.
- **Guard:** the CI APK size ceiling (12 MB) exists because of this incident. It is a real check.

### D-0012 — Settings icon is three sliders, not a gear
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `VisceralFitIcons.Settings` draws three sliders with knobs.
- **Reason:** A gear's teeth need roughly a dozen path segments to read correctly at 24 dp, and a
  hand-authored one looks crude at that size. Sliders are at least as recognisable for a
  preferences screen and are three lines and three rectangles.
- **Reverses if:** user feedback shows the sliders are not read as "settings".
- **Affects:** `core/designsystem/icon/VisceralFitIcons.kt`.

### D-0013 — Intensity bounds validated against named constants, not inline literals
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `IntensityTarget`'s `init` checks against `BORG_SCALE` (1..10) and
  `PLAUSIBLE_HR_PERCENT` (30..100) rather than inline ranges.
- **Reason:** detekt's `MagicNumber` rule flagged the inline literals, and it was right to: the
  numbers are meaningful (the Borg scale's actual bounds, and the range outside which a zone
  definition must be a programming error) and naming them says so. The validation fails loudly
  rather than clamping, because a zone defined outside these bounds is a bug in the framework, not
  bad user input.
- **Affects:** `domain/model/WorkoutStyle.kt`.

### D-0014 — Preferences read path split per section
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `Preferences.toUserPreferences()` delegates to `toCoachingPreferences`,
  `toDisplayPreferences` and `toBodyPreferences`.
- **Alternatives considered:**
  - *Suppress the detekt rule.* Rejected: the rule was right. Every `?:` is a branch, and a single
    function reading ~30 keys scored a cyclomatic complexity of 29.
  - *Raise the complexity threshold.* Rejected: that hides the next genuinely complex function too.
- **Reason:** Someone checking whether one setting falls back correctly should not have to scan
  thirty lines to find it. The rule surfaced a real readability problem rather than a false
  positive.
- **Reverses if:** never; this is strictly better.
- **Affects:** `core/datastore/PreferencesDataSource.kt`. Write path asymmetry recorded as TD-0008.

### D-0015 — `MagicNumber` excluded for the illustration and icon packages only
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `config/detekt/detekt.yml` excludes `core/designsystem/illustration/**` and
  `core/designsystem/icon/**` from the `MagicNumber` rule.
- **Alternatives considered:**
  - *Name every coordinate as a constant.* Rejected: `LEFT_KNEE_X = 62f` is strictly less readable
    than `62f` in a `lineTo`, and a single figure has twenty of them. Phase 03 authors ~60 more
    drawings; this would multiply into roughly a thousand meaningless constants.
  - *Disable `MagicNumber` globally.* Rejected: it caught a real problem in
    `IntensityTarget` (D-0013), where the numbers *did* carry hidden meaning.
  - *`@Suppress` per function.* Rejected: ~60 suppressions is worse than one scoped exclusion,
    and each one is a place a future author might extend the suppression to cover something else.
- **Reason:** Vector artwork is coordinates. In a figure drawn in the 100×100 logical box the
  numbers *are* the content, and no name can convey more than the number does. This is the only
  package in the codebase where that is true.
- **Reverses if:** never for these two packages; **the exclusion must not be widened to any
  other**. A number outside artwork almost always has a meaning worth naming.
- **Affects:** `config/detekt/detekt.yml`.
- **Note:** this is a scoped exclusion with a stated reason, not a weakened gate — the rule stays
  active and blocking everywhere else. Compare `framework/14_ci_cd_and_release.md` §8: "never fix a
  red build by weakening the gate."

### D-0016 — CI ships the release variant rather than the debug variant
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** The workflow's published artefact is `assembleRelease` (debug-signed, R8-minified),
  not `assembleDebug`. See ADR-0012 for the full record.
- **Reason:** Measured: 2.38 MB versus 32.58 MB. This supersedes the natural reading of D-0003
  ("debug-signed" implying the debug variant) — signing and variant are separate choices, and the
  right combination is release-variant with debug-signing.
- **Reverses if:** a real keystore is introduced, at which point the variant stays and only the
  signing config changes.
- **Affects:** `.github/workflows/android-ci.yml`.
- **Refines:** D-0003.

### D-0017 — Architecture rules enforced by a script, not by review
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `scripts/compliance_check.sh` enforces the eleven architecture rules that neither
  the compiler nor detekt can check: module boundaries, injected dispatchers, no `GlobalScope`, no
  `hiltViewModel()` in a `*Screen`, no `INTERNET` permission, no destructive migration, no
  hard-coded versions, no literal colours outside the design system. It runs in CI.
- **Alternatives considered:**
  - *A review checklist only.* Rejected: `framework/05_architecture.md` §9 was originally exactly
    that, and a checklist is only as good as the reviewer's attention on the day. These rules are
    mechanically checkable, so checking them mechanically is strictly better.
  - *Custom detekt rules.* Rejected for now: writing a detekt rule set is a project of its own, and
    several of these checks are about Gradle files and the manifest, which detekt does not see.
  - *Konsist or a similar architecture-test library.* A reasonable future option; recorded as a
    possibility rather than adopted, because a shell script with no dependency was verifiable
    immediately.
- **Reason:** Every rule in this list is one the framework asserts repeatedly. Asserting something
  in four documents and checking it nowhere is how it stops being true.
- **Verification:** the script was tested against **planted violations** (an `android.*` import in
  `domain/`, a literal `Color(0x…)` in a feature) and correctly failed with exit code 2 — so it is
  a real check, not one that always passes. That distinction is the difference between a gate and
  decoration, and the QA agent brief calls out tests that assert nothing as an anti-pattern.
- **Reverses if:** the checks are replaced by a typed architecture-test library, which would be an
  improvement rather than a reversal.
- **Affects:** `scripts/compliance_check.sh`, `.github/workflows/android-ci.yml`,
  `framework/05_architecture.md` §9, the per-phase checklist.
- **Known limits, stated in the script and the doc:** it reads the **source** manifest, so a
  permission added by manifest merging is not caught; and it does not inspect the dependency graph.
  Both need a build, and both are in the Security & Privacy agent's release verification.

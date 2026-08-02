# Decisions

Append-only log of choices with more than one defensible answer. Template:
`_templates/entry_templates.md`. Numbering: `D-NNNN`.

---

## Phase log

| Phase | Completed | New assumptions? | Notes |
|---|---|---|---|
| Permission audit | 2026-08-02 | No new ones | Every manifest permission audited against every permission-gated API in use: `POST_NOTIFICATIONS` is the only runtime permission the app has or needs, and nothing is missing. Request moved from session start to onboarding on the operator's instruction, and a defect fixed where a declined permission was re-asked on every session start. A fabricated framework citation in D-0044 corrected. D-0046 |
| 08 — Coaching | 2026-08-02 | No new ones | The player speaks. `CueScheduler` drives every cue type in spec §1 from the service's tick, as a pure function of session state with per-cue freshness windows; tones and haptics substitute when speech is unavailable. `POST_NOTIFICATIONS` is requested at session start, closing KI-0016. Closes the cue half of KI-0018; A-0005 mitigated in code. **Both manual audio tests outstanding — KI-0023.** Six coaching switches that existed but did nothing are now reachable. D-0043, D-0044, D-0045, KI-0024 |
| Fifth modality | 2026-08-02 | Yes — A-0012 | Mat Pilates split out of `BODYWEIGHT` as its own selectable category, and `supportsAerobicWork` made a constructor argument so a new modality cannot skip the question. Closes KI-0022. Catalogue 72 → 76, version 5. D-0042 |
| Clearance and selection | 2026-07-31 | No new ones | A-0002 and A-0007 answered. Effort ceiling added, defaulting to threshold rather than vigorous. All four modalities enabled by default, and REQ-011's refusal now explained on screen. D-0040, D-0041 |
| Modality correction | 2026-07-31 | No new ones | `FLOOR_PILATES` renamed to `BODYWEIGHT` and aerobic capability made a property of the modality, on the operator's correction. A user with no equipment can now be given real intervals. Closes KI-0020, supersedes part of D-0023. Catalogue 65 → 72. D-0039 |
| 09 — Tracking (partial) | 2026-07-30 | No new ones | `observeWeeklyLoad` implemented, closing KI-0002 and TD-0007: the Progress screen shows real minutes, sessions, vigorous minutes and rest-day advice. Two of engine spec §8's four signals; the other two need a schema change. D-0035..D-0037, KI-0019 |
| 07 — Workout player (partial) | 2026-07-30 | No new ones | Session runs end to end: generate, begin, count down, pause, skip, end, record. Safety notice added, closing KI-0005. Clock in a foreground service per ADR-0008, tested by arithmetic rather than by waiting. **Not done:** TTS cues (phase 08), landscape, device verification — KI-0016..KI-0018. D-0029..D-0034 |
| 06 — Workout engine | 2026-07-30 | Yes — A-0010 | Generator implemented; KI-0001 and KI-0006 closed. Golden file reproduces engine spec §9 exactly, including its exercise choices. Invariants asserted across 1,680 requests. Verified: `qualityCheck` green, 57 data checks, 11 compliance checks, release APK 2.5 MB. D-0020..D-0028, KI-0012..KI-0014, TD-0009 |
| 02 — Content authoring | 2026-07-30 | Yes — A-0009 | Catalogue grown 14 → 65 exercises; KI-0004 and KI-0006 closed. Balance and pool minimums now checked rather than counted. D-0019 |
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

### D-0018 — The specification has its own test suite
- **Date:** 2026-07-30
- **Phase:** Framework authoring
- **Decision:** `scripts/check_framework_data.py` validates the framework's reference data for
  internal consistency — 24 checks covering interval arithmetic, template ordering, style budget
  sums, MET-table coverage, citation-key existence and catalogue id uniqueness. It runs in CI and
  is step 0 of phase 06.
- **Reason:** KI-0010. Four of six HIIT interval durations were wrong, in a document written to be
  implemented literally. The framework deliberately restates the same numbers in prose, in a table
  and in JSON so a reviewer can cross-check them — but nothing was cross-checking them, so the
  redundancy created three places to be wrong instead of one place to be right.
- **Alternatives considered:**
  - *Single source of truth: delete the duplication.* Better in principle, but the prose table is
    what the implementer actually reads, and a JSON file alone is not a specification. Keeping both
    and checking they agree preserves readability without the drift.
  - *Trust careful authoring.* This is what was tried, and it produced four errors.
- **Reverses if:** never. Any project whose specification contains computed values should check
  them.
- **Affects:** `scripts/check_framework_data.py`, CI, phase 06's step 0 and exit criteria, the
  per-phase checklist.

### D-0019 — The exercise catalogue is grown to 65, not the minimum 54
- **Date:** 2026-07-30
- **Phase:** 02
- **Decision:** Author 65 exercises (floor Pilates 28, spin bike 13, reformer 13, elliptical 11)
  against the minimums in `framework/08_exercise_library_spec.md` §1 (54 total).
- **Alternatives considered:**
  - *Hit the minimums exactly.* Rejected: the per-modality, per-level and per-pool minimums are
    simultaneous constraints, and satisfying them all at exactly the totals leaves a catalogue with
    no slack. Excluding one caution tag then empties a pool and the generator starts falling back.
  - *Author many more.* Rejected for now: content depth is the binding constraint, not count, and
    each exercise is five authored prose fields that are health guidance.
- **Reason:** "At least 4 exercises with no caution tags per modality" was the hardest constraint to
  meet, and it is the one that matters most: a user with several exclusions must still get a
  session. Meeting it needed genuinely untagged movements added on purpose, not trimmed tags.
- **Reverses if:** the pool minimums change, or a modality is added.
- **Affects:** `app/src/main/assets/exercises_seed.json` (version 3),
  `scripts/check_framework_data.py`, `ExerciseCatalogueValidationTest`.
- **Verification:** 57 framework data checks and 17 catalogue validation tests, both in CI.

### D-0020 — Intensity anchors are a lookup table, not a MET threshold
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** `domain/engine/IntensityAnchor` maps `(modality, metValue)` to the Compendium's
  intensity anchor, mirroring `framework/data/met_values.json`.
- **Alternatives considered:**
  - *Derive the anchor from MET alone.* **Impossible, not merely worse.** A spin class is 9.0 MET
    anchored at Zone 2 (code 01270); the elliptical at the same 9.0 MET is anchored vigorous
    (02049). No threshold separates them, and the worked example in engine spec §9 requires
    `spin_bike_seated_flat` (9.0 MET) in the warm-up.
  - *Store the anchor on `Exercise` and persist it.* Rejected: it is a Room schema change plus a
    migration, and the anchor is not per-exercise content — it is a property of the
    `(modality, MET)` pair, which is what the Compendium publishes.
- **Reason:** MET is the energetic cost of work; the anchor is what the work *is*. Conflating them
  mis-prescribes sessions (see D-0026).
- **Reverses if:** an exercise ever needs an anchor that differs from other exercises at the same
  modality and MET. Then it becomes authored content and the field moves onto `Exercise`.
- **Affects:** `domain/engine/IntensityAnchor.kt`, `ExercisePools`,
  `scripts/check_framework_data.py`.
- **Guard:** the data check asserts the Kotlin table and `met_values.json` agree in both
  directions, and that every catalogue exercise resolves to a tabulated pair. Drift fails CI.

### D-0021 — An untabulated (modality, MET) pair falls back to a MET threshold
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** `IntensityAnchor.of` approximates from MET (< 3.6 recovery, < 6.0 Zone 2, < 8.0
  threshold, else vigorous) when the pair is absent from the table.
- **Alternatives considered:** throwing. Rejected: a content addition should not crash the app on a
  user's device. The data check turns the same situation into a CI failure, which is where it
  belongs.
- **Reason:** Fail visibly in the build, degrade gracefully at runtime.
- **Reverses if:** the fallback is ever observed in production, which would mean the data check was
  bypassed.
- **Affects:** `IntensityAnchor.approximateFrom`.

### D-0022 — The warm-up's final segment is on the machine the main block uses
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** When several machines are eligible, the warm-up's machine segment is chosen to match
  the modality the main block was built on. The main block is therefore built **first**, and the
  random draws happen in the order main → warm-up → cool-down.
- **Reason:** Engine spec §4.1 requires the final warm-up segment to be on a machine "so the user is
  already on the machine when the main block starts". With an elliptical and a bike both enabled, a
  random machine choice satisfies the letter of the rule and defeats its purpose — the user warms up
  on the elliptical and then moves to the bike.
- **Alternatives considered:** building the warm-up first and forcing the main block to match it.
  Rejected: the main block is the session; the warm-up serves it.
- **Reverses if:** never, unless the rule's rationale changes.
- **Affects:** `DefaultWorkoutGenerator.warmUpExercises`, and the golden file (draw order is part
  of the reproducible output).

### D-0023 — A Pilates-only session is built and recorded as RECOVERY, whatever was requested
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** When no machine-cardio exercise is eligible, the main block uses the RECOVERY
  construction (alternating mobility and strength at recovery intensity), `Workout.style` is
  recorded as `RECOVERY`, the title is the honest Pilates name, and a build note tells the user
  what was substituted.
- **Alternatives considered:**
  - *Implement spec §3's fallback chain literally.* The chain ends at `strengthPool`, which would
    build an interval or surge **structure** out of mat work — a session presented as aerobic
    interval training that is not aerobic training. That is the one substitution
    `framework/02_evidence_base.md` §1.5 and REQ-004 forbid.
  - *Record the requested style and cap the intensity.* Rejected: engine spec §7.2 says the style
    recorded is what was built, and §7.3 says a Pilates session contributes zero vigorous minutes.
    `RECOVERY` is defined as work counting toward volume but not intensity, so it is the honest
    mapping — and it makes the weekly-load accounting correct without a special case.
- **Reason:** The evidence does not support Pilates as a visceral-fat intervention comparable to
  aerobic work. A session labelled "Intervals" that is a mat class misrepresents that.
- **Reverses if:** the catalogue ever gains genuinely vigorous, sustainable Pilates content with
  evidence behind it at that intensity.
- **Affects:** `MainBlockBuilder.build`, `SessionTitle`, `WorkoutGeneratorFailureTest`.

### D-0024 — Repetition is allowed where it is the prescription, and fails where it is a gap
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** Two selection functions. `pick` fails with `InsufficientVariety` when a pool of
  fewer than two must fill three or more slots; `pickCycling` never fails and reuses the ordering.
  Warm-up, cool-down and recovery blocks use `pick`; Zone 2 segments and Mixed base segments use
  `pickCycling`.
- **Reason:** Spec §5.5's failure rule is right for a ramp, where three slots means three
  movements. It is wrong for a continuous steady block, which is *one* effort divided so the coach
  has boundaries to cue on (§4.3) — refusing to build a steady ride on a single-machine catalogue
  would be a bug, not a safeguard.
- **Reverses if:** the segment model gains a way to express "one effort, several cue points"
  without splitting into segments at all.
- **Affects:** `ExercisePicker`, `MainBlockBuilder`.

### D-0025 — No segment shorter than 20 seconds is ever emitted
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** `MIN_SEGMENT_SECONDS = 20`. Leftover time below that is folded into the neighbouring
  segment rather than becoming its own, even where that puts one HIIT recovery slightly over the
  +60 s extension cap. A transition is skipped entirely if paying for it would take the following
  segment below the floor.
- **Reason:** Spec §4.2 says leftover becomes "an extra ACTIVE_RECOVERY segment at the end", which
  for a leftover of 1 s produces a one-second segment. It cannot be cued, the TTS cue is longer
  than the segment, and it reads on screen as a bug. 20 s is the shortest interval the catalogue's
  vigorous cues are authored to fit inside (the 14-word limit), so it is the natural floor.
- **Reverses if:** the cue scheduler gains a way to speak across a segment boundary.
- **Affects:** `SessionConstants.MIN_SEGMENT_SECONDS`, `SegmentPlanning`,
  `MainBlockBuilder.recoveryExtensions`. Asserted by an invariant test.

### D-0026 — Interval templates gain rounds to fill a long session
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** After choosing the largest template that fits, add whole rounds of it while they
  still fit, up to 20 rounds. Only then distribute the leftover into the recovery segments.
- **Alternatives considered:** the specification's fixed round count. Rejected on inspection of the
  output: a 120-minute HIIT request has a 6,300 s main block against `4x4`'s 1,500 s, so the
  literal reading produces four intervals followed by **77 minutes** of active recovery. The
  duration invariant holds and the session is nonsense.
- **Reason:** The spec was written for durations near the template sizes and is silent above them.
  Extra rounds of the chosen protocol is the reading that keeps a long interval session an interval
  session.
- **Reverses if:** the recovery recommender is extended to refuse implausible requests up front,
  which would be a better answer than building them well.
- **Affects:** `IntervalTemplate.MAX_ROUNDS`, `MainBlockBuilder.roundsFor`. Recorded as KI-0013
  because a 20-round 4×4 is still not a sensible prescription — it is merely a coherent one.

### D-0027 — Exercise pools are filtered by MET range **and** intensity anchor
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** Narrow spec §3's MET-only pool predicates with the Compendium anchor: the vigorous
  and threshold pools require an anchor of THRESHOLD or harder, the steady pool requires ZONE_2 or
  easier, and the Pilates warm-up band starts above the mobility band (MET > 2.5).
  `framework/07_workout_engine_spec.md` §3.1 documents it so code and specification stay in step.
- **Reason:** Three real mis-prescriptions in the very first generated plan, all from MET-only
  rules:
  1. `spin_bike_high_cadence_surge` (8.8 MET, anchored vigorous) used as a Mixed session's **Zone 2
     base** — a hard interval prescribed as conversational work.
  2. `spin_bike_seated_flat` (9.0 MET, anchored Zone 2) eligible as a **HIIT work interval** — the
     easy flat road served as a 30-second maximal effort.
  3. `cooldown_thoracic_opener` (2.3 MET) chosen as the **first warm-up segment**. A static stretch
     warms nothing up, and the spec's own worked example uses a march.
- **How they were found:** by reading the first plan the engine produced, not by a failing
  assertion. Every invariant test passed throughout. Recorded prominently because it is the clearest
  evidence in this phase that **invariants do not check whether a session is good programming** —
  the golden file exists precisely so a human reads the output once.
- **Reverses if:** the anchor data is ever found to disagree with how the catalogue actually
  prescribes an exercise, in which case the anchor becomes authored per exercise (see D-0020).
- **Affects:** `ExercisePools.partition`, `framework/07_workout_engine_spec.md` §3.1, the golden
  file.

### D-0028 — Style availability is computed from the engine's own minimums
- **Date:** 2026-07-30
- **Phase:** 06
- **Decision:** `SessionLimits` exposes each style's minimum total from `StyleBudget`, and the Train
  screen disables a style the chosen duration cannot support, showing the minimum on the chip.
  `StyleBudget` itself stays internal.
- **Reason:** REQ-024. A UI that hard-codes "intervals need 16 minutes" drifts from the generator
  the moment a budget constant changes, and the failure mode is the one the requirement exists to
  prevent: the user picks a style and generation fails.
- **Reverses if:** never.
- **Affects:** `domain/engine/SessionBudget.kt`, `WorkoutHomeViewModel`, `WorkoutHomeScreen`.

### D-0029 — The safety notice gates the whole shell, not just the player
- **Date:** 2026-07-30
- **Phase:** 07
- **Decision:** `MainActivity` shows `SafetyNoticeScreen` instead of `VisceralFitApp` until
  `UserPreferences.safetyNoticeAcknowledged` is true. It is not dismissible by back.
- **Alternatives considered:**
  - *Gate only the player.* Rejected: REQ-005 says "before the first session", but a user who has
    already opened Settings and set their level to advanced has made an intensity decision before
    being told to stop on chest pain.
  - *A dismissible banner.* Rejected: acknowledgement is the mitigation for A-0007, and a notice
    that can be swiped away has not been acknowledged.
- **Reason:** REQ-005, and A-0007 remains an open assumption — nobody has confirmed the operator is
  cleared for vigorous exercise, and the app's default programme reaches 85–95% HRmax.
- **Reverses if:** onboarding grows into several steps, in which case the notice becomes the first
  of them rather than a special case.
- **Affects:** `MainActivity`, `MainViewModel`, `app/ui/SafetyNoticeScreen.kt`. Closes KI-0005.
- **Not done:** the copy promises "You can read this notice again at any time from Settings", and
  Settings does not yet offer that. Recorded as KI-0015 rather than left as a lie in shipped copy.

### D-0030 — Skipped time is not credited to the session
- **Date:** 2026-07-30
- **Phase:** 07
- **Decision:** `SessionState.activeElapsed` counts only real elapsed unpaused time, so skipping a
  four-minute interval lowers the completion ratio by four minutes' worth.
- **Alternatives considered:** crediting a skipped segment as completed. Rejected: the completion
  ratio feeds the recovery recommender, which reads two consecutive sessions below 0.7 as a signal
  that the prescription is too hard (engine spec §8). Crediting skips would hide exactly the signal
  that matters, and it is the signal a user generates by skipping.
- **Reason:** A skipped interval was not performed.
- **Reverses if:** never.
- **Affects:** `SessionCoordinator.skip`, `SessionState.completionRatio`.

### D-0031 — An abandoned session is still recorded
- **Date:** 2026-07-30
- **Phase:** 07
- **Decision:** Ending a session early records it, with its real completion ratio, rather than
  discarding it.
- **Reason:** Two reasons, and the second is the important one. A history of good days only is a
  history that cannot show a pattern. And the recovery recommender needs low completion ratios to
  notice the prescription is too hard — a discarded session is a deleted signal.
- **Alternatives considered:** asking whether to save. Rejected: it is a decision the user has no
  basis to make, presented at the worst moment.
- **Reverses if:** users report the history feeling like a record of failures. The fix would then be
  in how history *presents* short sessions, not in whether they are kept.
- **Affects:** `WorkoutPlayerViewModel.finishAndRecord`, the summary screen's neutral wording.

### D-0032 — The energy estimate charges only the part performed
- **Date:** 2026-07-30
- **Phase:** 07
- **Decision:** `finishAndRecord` builds a synthetic `Workout` containing the segments actually
  performed — completed ones in full, the current one truncated to what was done — and estimates
  from that.
- **Reason:** Charging the whole plan when the user stopped a third of the way in would overstate
  expenditure by a factor of three. REQ-081 and D-0005 are about not inflating a number the user
  reads as measured; that applies to the numerator as much as to the body-mass input.
- **Reverses if:** never.
- **Affects:** `WorkoutPlayerViewModel.performedPortionOf`.

### D-0033 — The session clock is computed from timestamps, never accumulated from ticks
- **Date:** 2026-07-30
- **Phase:** 07
- **Decision:** Every `SessionCoordinator` method takes the current monotonic reading as a
  parameter, and elapsed time is `now − lastTick`. A single late advance rolls through as many
  segment boundaries as it covers.
- **Alternatives considered:** counting ticks and multiplying by the interval. Rejected: the process
  will be descheduled, and a 45-minute session that ticked 2,690 times instead of 2,700 would end
  two seconds short — invisibly, and worse on a loaded device.
- **Reason:** Correctness under stalls, and testability: the whole state machine is exercised with
  plain integers, no scheduler and no waiting. A timer you have to wait for is a timer nobody tests
  at the boundaries.
- **Reverses if:** never.
- **Affects:** `SessionCoordinator`, `SessionState.advancedTo`, `SessionCoordinatorTest`.

### D-0034 — Pausing releases the keep-screen-on flag immediately, not after 60 seconds
- **Date:** 2026-07-30
- **Phase:** 07
- **Decision:** `KeepScreenOn(enabled = prefs.keepScreenOn && !isPaused)`.
- **Alternatives considered:** the 60-second grace period `framework/10_screen_specs.md` §4
  specifies. Rejected as written: it needs a second timer whose only job is to release a flag, and
  the case it protects — a pause shorter than a minute — is one where the user is looking at the
  phone anyway, which keeps the screen on by itself.
- **Reason:** Simpler, and it fails in the safe direction. The failure the grace period guards
  against is a screen that dims during a brief pause; the failure immediate release guards against
  is a phone held awake all night. The second is worse.
- **Reverses if:** the dim-on-brief-pause behaviour proves annoying in real use, which is a question
  only device testing answers. Recorded as a divergence from the screen spec.
- **Affects:** `WorkoutPlayerScreen`, and `framework/10_screen_specs.md` §4 now disagrees with the
  code by one detail.

### D-0035 — Weekly volume counts every minute performed; session counts only count completed sessions
- **Date:** 2026-07-30
- **Phase:** 09 (partial)
- **Decision:** `TrainingLoadSummary.totalMinutes` sums the active minutes of **every** session in
  the week, including ones abandoned early. `sessionCount` counts only sessions past
  `CompletedSession.COMPLETION_THRESHOLD` (0.7).
- **Supersedes:** the blanket rule in `CompletedSession.COMPLETION_THRESHOLD`'s own documentation,
  "Sessions past this fraction count toward streaks **and volume**". Volume is now unfiltered.
- **Reason:** Twenty minutes performed are twenty minutes toward the WHO 150, whether or not forty
  were planned. Filtering them out makes the headline number on the Progress screen *wrong* in the
  direction of under-reporting real activity — which is the same class of error as the bug this
  work fixes (KI-0002, a number that always read zero). Consistency is a different question, and
  there a threshold is right: a session abandoned at 20% is not a session you turned up for.
- **Alternatives considered:**
  - *Filter both.* Rejected for the reason above.
  - *Filter neither.* Rejected: streaks and the session count would then reward opening the app and
    stopping.
- **Reverses if:** the completion threshold acquires a second meaning that makes the split
  confusing. It should then be two named constants rather than one reused.
- **Affects:** `DefaultHistoryRepository.observeWeeklyLoad`, `ProgressViewModel`, `WeeklyLoadTest`.

### D-0036 — Vigorous minutes are approximated from the session's style
- **Date:** 2026-07-30
- **Phase:** 09 (partial)
- **Decision:** HIIT and MIXED sessions contribute all their active minutes as vigorous minutes;
  ZONE_2 and RECOVERY contribute none.
- **Why an approximation at all:** `CompletedSession` records the style but not per-segment
  intensity, so the true figure — the sum of the work intervals — is not recoverable. The same
  schema gap as KI-0012.
- **Why it errs high:** a Mixed session's surges are threshold (76–84% HRmax) rather than vigorous
  (85–95%), so counting the whole session overstates. That is deliberate. The figure feeds the
  recovery recommender, whose job is to *warn*; over-warning costs the user an unnecessary easy
  day, under-warning costs them an overreach. Under-counting would be the unsafe direction.
- **What it gets right for free:** a Pilates-only session is recorded as RECOVERY by the generator
  (D-0023), so it contributes zero vigorous minutes — which is exactly what engine spec §7.3
  requires, with no special case anywhere.
- **Reverses if:** a `session_exercises` table lands (KI-0012), after which the real figure is a
  query rather than a guess.
- **Affects:** `DefaultHistoryRepository.VIGOROUS_STYLES`.

### D-0037 — Rest-day advice is shown only when there is something to say
- **Date:** 2026-07-30
- **Phase:** 09 (partial)
- **Decision:** `ProgressUiState.restDaySuggestion` is null unless one of the two computable signals
  from engine spec §8 fires — two or more consecutive vigorous days, or a week more than 30% above
  the trailing average. The other two conditions in §8 are **not** implemented, because they need
  data the schema does not carry, and they are absent rather than approximated.
- **Reason:** An advice card that always shows something stops being read, and a rest-day
  recommendation invented from data that is not there is worse than no recommendation. The copy is
  advice with a reason attached ("that is three hard days in a row"), never an instruction.
- **Reverses if:** the missing signals become computable, at which point the card gets more to say
  rather than a different design.
- **Affects:** `ProgressViewModel.restDaySuggestion`, `ProgressScreen`.

### D-0038 — A recovery block draws only from movements anchored at Zone 2 or easier
- **Date:** 2026-07-30
- **Phase:** 06 (follow-up)
- **Decision:** `PoolFallbacks.strength`, which feeds the recovery main block and therefore every
  Pilates-only session, is restricted to exercises the Compendium anchors no harder than Zone 2.
  Separately, the active-recovery segments between HIIT intervals now fall back through the steady
  pool and then the mobility pool before ever reaching the work pool.
- **Reason:** A recovery block prescribes every segment at RECOVERY intensity, and the unrestricted
  strength pool contained `floor_pilates_mountain_climber_slow` (7.0 MET, anchored threshold) and
  `floor_pilates_star_jumps` (7.5, vigorous). A seven-minute recovery session therefore came out
  containing slow mountain climbers labelled "easy". The HIIT fallback had the same shape: with no
  steady exercise on the work modality it would name a vigorous interval as the active recovery.
- **How it was found:** by the anchor-versus-intensity invariant added to close part of KI-0014,
  **on its first run**. That is the point worth recording — the check was written to catch faults
  already fixed, and it immediately found a fourth of the same kind that nothing else had.
- **Consequence:** two authored floor exercises are now unreachable by the generator. Recorded as
  KI-0020 rather than deleted, because the cause is a modality-model problem, not a content problem.
- **Reverses if:** the modality model gains a bodyweight-cardio category (KI-0020), after which
  those movements have a legitimate home as vigorous work.
- **Affects:** `PoolFallbacks.strength`, `MainBlockBuilder.intervals`,
  `WorkoutGeneratorInvariantTest`.

### D-0039 — `FLOOR_PILATES` was the wrong name, and the wrong constraint came with it
- **Date:** 2026-07-31
- **Phase:** post-07 (operator correction, UF-0007)
- **Supersedes:** part of D-0023. That entry made *every* machine-free session a recovery
  session. The rule was right for Pilates and wrong for everything else in the category.
- **Decision:** Three changes, one idea.
  1. `Modality.FLOOR_PILATES` → `Modality.BODYWEIGHT`, id `bodyweight`, labelled "Floor &
     bodyweight". Exercise ids renamed `floor_pilates_*` → `bodyweight_*`.
  2. New `Modality.supportsAerobicWork`, false only for `REFORMER_PILATES`. The pools key on
     it instead of on `isMachineCardio`, so `cardioPool` became `aerobicPool`.
  3. The strength-only branch now triggers when nothing available is anchored above recovery,
     rather than when no machine is available.
- **Why the name mattered more than a name should:** the label put the whole equipment-free
  category inside the Pilates evidence constraint. `wang2021pilates` found no
  waist-circumference effect from Pilates, so REQ-004 forbids presenting a Pilates session as
  comparable to aerobic work — correctly. But star jumps at 7.5 MET are aerobic work that
  `chen2024nma` supports, and they were filed under "floor Pilates", so the constraint caught
  them too. The consequence was that a user with **no equipment** could only ever be offered
  a recovery session, and two authored exercises were unreachable (the old KI-0020).
- **The operator's correction, verbatim:** "it should be bodyweight and floor excercises. It
  should not have been 'floor pilates' it was 'reformer pilates'." The Pilates they do is the
  reformer; the floor work was never Pilates.
- **Why the constraint is now structural:** putting it on the modality as
  `supportsAerobicWork` means the honesty rule holds everywhere at once, rather than being a
  branch in `MainBlockBuilder` that a future change could bypass. Reformer work cannot be
  prescribed as aerobic because the pools it feeds cannot carry aerobic segments.
- **Why `vigorousPool` needed two admitting conditions:** the 8.0 MET floor excludes star
  jumps (7.5, anchored **vigorous**, code 02020); the anchor alone admits a spin class (9.0,
  anchored **Zone 2**, code 01270). An exercise qualifies when the Compendium calls it
  vigorous outright, or when it is threshold-anchored at 8.0 MET or above.
- **Was this rename safe?** Yes, and only because nothing has shipped. `Modality.id` is the
  persisted form and exercise ids are documented as never renamed once shipped. There are no
  installs, so there is no data to migrate. This was the last moment it was free.
- **Content added:** seven bodyweight cardio exercises — jumping jacks, fast feet, high knees,
  plank jacks, skater hops, squat jumps, burpees — spread across all three levels so a
  beginner with no equipment can be given an interval session. Catalogue is now 72 exercises,
  version 4.
- **Reverses if:** never for the naming. If mat Pilates is ever wanted as a distinct
  modality from general floor work, that is an addition rather than a reversal.
- **Affects:** `Modality`, `ExercisePools`, `MainBlockBuilder`, `SessionTitle`, the catalogue,
  `met_values.json`, `IntensityAnchor`, `framework/07_workout_engine_spec.md` §3.2, the
  golden file, and the modality labels on the Train and Settings screens.
- **Verification:** a new test asserts a bodyweight-only interval request produces genuine
  intervals with vigorous segments on bodyweight movements — the case that was impossible
  before. Two tests assert the reformer and mat-only paths still build and name themselves as
  strength work. The golden file changed in exactly three ways, all traceable to the rename:
  the id digest, the modality name, and one cool-down draw that moved because the pool is
  sorted by id.

### D-0040 — The user sets a ceiling on how hard a session may get
- **Date:** 2026-07-31
- **Phase:** post-07 (answers A-0007)
- **Decision:** New `UserPreferences.effortCeiling`, an `EffortCeiling` of STEADY, THRESHOLD or
  VIGOROUS, **defaulting to THRESHOLD**. The generator honours it, combining it with any cap the
  pool fallback chain applied, and the title and build notes say when it was the binding
  constraint.
- **Why it exists:** A-0007 asked whether the operator is cleared for vigorous exercise. The answer
  was "I can do moderately vigorous exercise" (UF-0008), which is not the same as yes. The default
  programme reached 85–95% of maximum heart rate — the intensity `helgerud2007` prescribes — and
  nothing let the user say otherwise short of avoiding interval sessions entirely.
- **Why THRESHOLD is the default rather than VIGOROUS:** the app cannot know a user's clearance, so
  it does not assume the most permissive answer. 76–84% HRmax is hard but sustainable, matches
  "moderately vigorous", and is still squarely inside what `chen2024nma` supports for visceral fat.
  Raising it is a deliberate act on a screen that states the trade-off in both directions.
- **Why it is separate from `ExperienceLevel`:** those answer different questions. Experience is
  about technique and coordination; this is about cardiovascular clearance. Someone can be an
  advanced Pilates practitioner and still have a reason not to reach 90% HRmax. Deriving one from
  the other would silently couple a safety limit to a skill setting.
- **The asymmetric default, and why:** `WorkoutRequest.effortCeiling` defaults to VIGOROUS while
  `UserPreferences.effortCeiling` defaults to THRESHOLD. The engine's contract is to build what it
  was asked for and it has no view on anyone's medical clearance; the cautious default belongs
  where the user can see and change it. Stated explicitly in both places because a split default
  is otherwise the kind of thing that looks like a bug.
- **Honesty:** a ceiling that binds produces "Intervals (threshold)" rather than "Intervals", plus a
  build note naming the ceiling and where to change it. A ceiling of VIGOROUS produces no cap at
  all — deliberately, since a cap that is always present would make the word meaningless.
- **Reverses if:** never as a concept. The default could move if a user's clearance is confirmed,
  and that is a settings change rather than a code change.
- **Affects:** `UserPreferences`, `EffortCeiling`, `PreferencesDataSource`, `WorkoutRequest`,
  `IntensityAnchor.ceilingOf`, `SegmentPlanning.strictest`, `MainBlockBuilder`,
  `DefaultWorkoutGenerator`, `SettingsScreen`, and the safety notice's opening paragraph, which
  claimed 85–95% and no longer describes the default.
- **Verification:** `WorkoutGeneratorCeilingTest` — five cases covering both binding ceilings, the
  no-op case, and the case where the *pool* was the binding constraint and must not be blamed on
  the user's ceiling.

### D-0041 — Every modality is enabled and available on a fresh install
- **Date:** 2026-07-31
- **Phase:** post-07 (answers A-0002)
- **Supersedes:** ADR-0004, which excluded the reformer from the defaults on the reasoning that
  most users will not own one.
- **Decision:** `Modality.DEFAULT_ENABLED` is all four modalities, and `availableEquipment` defaults
  to all equipment-requiring ones.
- **Reason:** The operator has an elliptical, a spin bike and a reformer (UF-0008), and wants to
  pick any combination day to day — "one day doing spin bike, or tomorrow I do elliptical and
  floor". More generally the old default solved the wrong problem: whether a modality is *usable* is
  already answered by `requiresEquipment` plus the equipment the user has marked, so defaulting it
  off as well hid thirteen reformer exercises behind a second switch with no explanation of why they
  never appeared. A user who does not own something turns it off in one tap; a user who does own it
  should not have to discover that they need to turn it on.
- **What actually enforces the constraint:** REQ-011 — at least one modality must stay enabled — and
  that is checked when toggling, not by the default. The refusal is now explained on screen instead
  of the switch springing back silently, which is what REQ-011 asked for and did not get.
- **Reverses if:** the app is ever distributed beyond this operator, at which point defaulting
  equipment on becomes an assumption about a stranger's home rather than a fact about this one.
  Recorded as the reversal condition precisely because it is easy to forget the default was chosen
  for a known user.
- **Affects:** `Modality.DEFAULT_ENABLED`, `BodyPreferences.availableEquipment`,
  `SettingsViewModel.setModalityEnabled`, `SettingsScreen`.

### D-0042 — Mat Pilates is its own modality, and aerobic capability is now an explicit answer
- **Date:** 2026-08-02
- **Phase:** post-07 (closes KI-0022)
- **Decision:** A fifth `Modality`, `MAT_PILATES`, holding the classical Pilates mat repertoire and
  its named fundamentals — the hundred, roll up, single and double leg stretch, criss cross, side
  kick series, swimming, teaser, jack knife, neck pull, shoulder bridge, lateral breathing, imprint
  and release, spine twist, spine stretch forward, swan. Fourteen exercises were re-tagged out of
  `BODYWEIGHT`, four more were authored, and `supportsAerobicWork` became a constructor argument.
- **Why the split:** the two categories differ in the only two ways a category matters here. The
  user picks them separately — "an easy mat day" is a different intention from "a hard bodyweight
  day", and before the split those were one switch. And the evidence that applies to them is
  different: `wang2021pilates` governs the mat repertoire, `chen2024nma` governs the jumping work.
  One modality forced one answer to `supportsAerobicWork` for movements as unlike each other as
  lateral breathing and burpees, and D-0039 had to resolve that in favour of the burpees.
- **What the user gets:** five independently selectable categories, any combination, minimum one
  (UF-0008). A mat-only session is titled "Mat Pilates: strength and control" and is never framed as
  cardio; a bodyweight-only session can still be genuine intervals.
- **Why `supportsAerobicWork` moved into the constructor (closes KI-0022):** it was
  `this != REFORMER_PILATES`, which defaulted every future modality to aerobic-capable. That is the
  dangerous direction — this very change would have credited the mat repertoire with cardio benefit
  by omission, and no test would have failed. It is now impossible to add a modality without
  answering the question.
- **Where the boundary was drawn, and why it is not arbitrary:** the mat modality is the *method* —
  movements a Pilates instructor names from the repertoire. General core and mobility drills stay on
  bodyweight: dead bug, bird dog, clam, cat cow, forearm plank, side plank. Calling a set of dead
  bugs "Pilates" is a claim about a method (the D-0039 reasoning), and it is the title, not the
  exercise, that makes the claim.
- **MET values:** the split let two exact Compendium codes land on the modality they describe —
  `02103` "Pilates, traditional, mat" (1.8) and `02105` "Pilates, general" (2.8) were previously
  carried on `bodyweight`. Cat cow was the only bodyweight movement left at 1.8; it is a mobilising
  stretch rather than Pilates, so it moved to `02101` at 2.3. The classical work at 3.8 MET is
  approximated from moderate calisthenics (`02022`), the same substitution the reformer makes — see
  A-0012.
- **Catalogue counts:** bodyweight's minimum drops from 24 to 20 and mat Pilates gets 14, revised in
  `framework/08_exercise_library_spec.md` §1 and in both validators. The 24 was set when one
  category carried both; the number came down with the work the category has to carry.
- **Reverses if:** never as a concept — a category the user cannot select separately is not a
  category. The boundary between the two could move if a movement turns out to be misfiled.
- **Affects:** `Modality`, `IntensityAnchor.ENTRIES`, `SessionTitle.strengthName`,
  `exercises_seed.json` (version 4 → 5), `met_values.json`, `CatalogueFixture`, the golden file,
  `SettingsScreen` (display names and per-category descriptions), `WorkoutHomeViewModel.displayName`,
  `MODALITY_MINIMUMS` in `ExerciseCatalogueValidationTest` and `check_framework_data.py`,
  `framework/07_workout_engine_spec.md` §3.2 and §7, `framework/08_exercise_library_spec.md` §1.
- **Verification:** 62 framework-data checks; `WorkoutGeneratorFailureTest` gains a mat-Pilates
  title test, a both-Pilates-modalities title test, and the renamed bodyweight-core test;
  `WorkoutGeneratorInvariantTest` and `WorkoutGeneratorDeterminismTest` gained mat-Pilates modality
  sets, so the anchor-versus-intensity invariant runs over the new modality. Golden file
  re-baselined: the id changes shift the pool shuffle, and the diff was read before it was accepted.

### D-0043 — The cue scheduler is a pure function of session state, with freshness windows
- **Date:** 2026-08-02
- **Phase:** 08 — Coaching
- **Decision:** `CueScheduler` in `feature-workout` decides all coaching output for one instant from
  `(SessionState, nowMillis, CoachingPreferences, speechAvailable)` and returns a `CueBatch` of at
  most one utterance plus an optional tone and haptic. `WorkoutService` calls it on the same 200 ms
  tick that advances the clock, and hands the result to `SpeechCoach` and the new `CueFeedback` port.
- **Why the service and not the player screen:** the screen can be gone, and that is exactly when
  spoken coaching is the point — phone face-down on a bike, or the user in their music app. Cues
  driven from a composable would stop at the moment they became load-bearing. ADR-0008 already put
  the clock in the service for the same reason; this follows it.
- **Why freshness windows rather than firing instants (the central design choice):** a tick lands
  every 200 ms and the process can be descheduled for seconds, so "fires exactly at T" is not
  implementable. Each candidate instead computes how late *this* tick is for it and is dropped when
  that exceeds its own `staleAfterMillis`. This makes the spec's "drop stale cues" a mechanism rather
  than a special case, and it makes each cue's tolerance an explicit, arguable number:

  | Cue | Window | Why that number |
  |---|---|---|
  | Countdown | 700 ms | "Three. Two. One." started later than this finishes after the boundary it counts to, so it would be a lie |
  | Segment start | 3 s | Naming the exercise you are already doing stays useful for a few seconds |
  | Safety warning | 5 s | It has to be heard; the widest window of any cue |
  | Full instructions | 5 s | Useful at any point in a long segment |
  | Everything else | 2 s (the default) | |

- **Why a blocked cue is retried rather than queued:** the spec forbids queueing informational cues
  because a queued "halfway" is spoken during the next interval, where it is simply wrong. Retrying
  inside the cue's own freshness window has the same effect without a queue: a cue either gets
  through while it is still true, or it is never spoken. There is nothing that can arrive late
  because there is nowhere for it to wait.
- **Why one utterance per tick:** two at once is the failure the whole design exists to avoid, so it
  is prevented structurally rather than by a check. The candidate order is *not* the priority enum's
  order — the countdown outranks the segment-start cue even though both are TRANSITION, because a
  countdown is about a boundary that is seconds away while a segment-start cue describes something
  the user can read off the screen.
- **`CueFeedback` is a separate port, not a fallback inside `SpeechCoach`:** `cueTones` and
  `hapticCues` are deliberately independent of `speechEnabled`, so tones are their own channel rather
  than a degraded form of speech. Folding them in would make "I want the vibration and my music"
  depend on speech having failed. The tones are genuinely rising and falling — two DTMF notes of
  known pitch in sequence — because a direction cannot be expressed by one beep, and direction is
  what makes the signal learnable in a single session.
- **Timing constants chosen here, all of them arguable:** minimum utterance gap 1.5 s (the spec's);
  next-exercise lead 5 s (the spec's); countdown lead 3 s (the spec's); boundary-signal tolerance
  1 s; motivational floor 90 s (the spec's) and only in work segments of 40 s or more; full
  instructions scheduled at `word count × 400 ms + 1.5 s` after segment start, where 400 ms/word is
  150 words per minute — the same rate the catalogue's own spoken-cue word limits are derived from,
  so the two agree by construction rather than by coincidence.
- **Motivational wording, recorded because it is the most subjective content in the app:** "Strong.
  Hold this.", "Stay with it.", "Good work. Keep the rhythm.", "Breathe steady.", "Nearly through
  this one." Cycled in order so a line never lands twice running. The editorial rule is that they
  describe effort and never outcome — "strong, hold this" is a fact about what the user is doing,
  whereas anything about fat would be a claim about their body this app cannot make. Unreviewed by
  anyone but the implementer and off by default; recorded as KI-0024.
- **`ProhibitedClaims` moved to `:core:testing`:** it was a private list inside `:app`'s catalogue
  test, which meant every string the app *says* was unchecked. A rule that covers only the place it
  was first written is not a rule.
- **Audio focus:** requested per utterance as `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, abandoned on
  every terminal path — done, error, stop, shutdown — and never held between cues. Cues route as
  `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`, so they behave like satnav instructions: they follow the
  user to their Bluetooth headphones and duck music rather than replacing it. Focus loss stops speech
  mid-utterance and does not resume, which is the phone-call case.
- **Not done, and recorded rather than glossed:** both of phase 08's manual exit criteria — a session
  with TTS disabled at OS level, and a session with music playing to confirm it unducks — need a
  device. KI-0023.
- **Reverses if:** never as a shape. The constants are all arguable and the freshness windows in
  particular should be revisited after one real session.
- **Affects:** `CueScheduler`, `CueText`, `CueBatch`, `CueFeedback`/`CueTone`/`HapticCue`,
  `AndroidCueFeedback`, `AndroidSpeechCoach` (audio focus, attributes, `shutdown` on the port),
  `SpeechCoach`, `WorkoutService`, `WorkoutPlayerViewModel` and `WorkoutPlayerScreen` (the
  unavailability notice), `ProhibitedClaims`, `ExerciseCatalogueValidationTest`.
- **Verification:** `CueSchedulerTest` — 24 tests, one per timing rule in the spec, none of which
  waits: the scheduler reads no clock, so a 20-minute session is driven by integers. `CueTextTest`
  checks every fixed string against `ProhibitedClaims`, the 12-word in-work limit, and that the
  safety cue names all three symptoms REQ-006 lists.

### D-0044 — The notification permission is requested when a session starts, and never blocks it
- **Date:** 2026-08-02
- **Phase:** 08 (closes KI-0016)
- **Decision:** `rememberSessionNotificationPermission` shows a rationale then requests
  `POST_NOTIFICATIONS` at the moment the user starts a generated plan. Every path through it —
  granted, denied, dialog dismissed, or an Android version where the permission does not exist —
  ends by starting the session.
- **Why at session start rather than at launch:** a prompt on first launch, before the user has seen
  what the app does, is the pattern that trains people to tap Deny. Asked as they start their first
  session, the rationale is about something happening now: the pause, skip and end controls on the
  lock screen. Naming that concrete benefit is what
  `framework/15_device_targets_motorola_edge_60.md` §Notification permission asks for.
- **CORRECTION 2026-08-02:** this entry originally cited `framework/11_permissions_and_privacy.md`,
  **which does not exist** — the reference was fabricated, and the same wrong path was in the
  implementation's own KDoc. The real source is
  `framework/15_device_targets_motorola_edge_60.md` §Notification permission. Recorded rather
  than quietly corrected because a plausible-looking citation to a file nobody can open is worse
  than no citation: a reviewer who cannot find it assumes they are looking in the wrong place.
- **SUPERSEDED IN PART BY D-0046:** the *timing* decided here — ask at session start — was
  reversed on the operator's instruction. Everything else in this entry still holds, and the
  "never blocks" rule holds more strongly than before.
- **Why it must never gate the session, stated as a rule:** the permission buys the lock-screen
  controls and nothing else. The clock is in a foreground service that starts without it. A flow that
  could leave a user who tapped Deny unable to train would be a worse defect than the missing
  notification it was added to fix — so the request's result is deliberately *ignored* rather than
  branched on, and `SessionNotificationPermissionTest` asserts there is no permission state in which
  tapping Start does nothing.
- **Why the decision lives in a plain class:** `SessionNotificationPermission` holds the three-way
  choice with the platform behind four lambdas, so it is testable on the JVM with no Compose runtime
  and no device. Below API 33 the permission does not exist, so `isNotificationPermissionGranted`
  answers "granted" rather than making every caller know about the version split.
- **Still unverified:** that the notification then *appears* on Android 16 is device work, part of
  KI-0017. What is tested is that the app asks.
- **Reverses if:** never. The only open question is the wording of the rationale.
- **Affects:** `SessionNotificationPermission`, `WorkoutHomeScreen`.

### D-0045 — The coaching settings are a table, and six switches that did nothing became reachable
- **Date:** 2026-08-02
- **Phase:** 08
- **Decision:** `CoachingToggle`, an enum of the ten user-facing coaching switches with their copy
  and their getter/setter pair, rendered by one `items()` call. `SettingsViewModel` exposes a single
  `setCoaching(CoachingPreferences)` in place of five per-flag setters.
- **What was actually wrong:** `CoachingPreferences` has eleven flags and spec §1 says every cue type
  is independently switchable, but Settings surfaced five. The other six — rest countdown, remaining
  time, motivational prompts, tones, haptics — existed in the model, did nothing at runtime, and had
  no UI. Phase 08 made all of them do something, which turned an unfinished screen into a screen that
  hides working features.
- **Why a table rather than six more rows:** the shape is what caused the omission. Each flag cost a
  composable, a lambda parameter on `SettingsScreen`, a wiring line in `SettingsRoute`, and a setter
  on the ViewModel — four edits in three files, so stopping at five was the path of least resistance.
  It also pushed `SettingsScreen` toward detekt's parameter limit, which would have made adding the
  eleventh a refactor. One row in an enum is now the whole cost.
- **The one column that carries real behaviour:** `needsSpeech`. Tones and haptics are *not* greyed
  out when the master speech switch is off, because they are what substitutes for speech when it is
  unavailable (spec §6) — disabling them with speech off would break the exact case they exist for.
  Every genuinely spoken cue is gated by the master switch.
- **Not exposed:** `speechRate` and `speechPitch`, the two non-boolean flags. They need a slider
  rather than a switch and the service already applies them; left for the phase-10 settings work.
- **Reverses if:** never; the table is strictly cheaper than what it replaced.
- **Affects:** `CoachingToggle`, `SettingsScreen`, `SettingsViewModel`.

### D-0046 — The notification permission is asked during onboarding, and only ever once
- **Date:** 2026-08-02
- **Phase:** post-08 (supersedes the timing half of D-0044)
- **Decision:** `POST_NOTIFICATIONS` is requested as the second and final step of onboarding,
  immediately after the safety notice and before the app proper. The session-start request added
  in D-0044 is removed. A new `UserPreferences.notificationPermissionRequested` records that the
  question has been asked, so it is asked exactly once per install, and Settings grows a row —
  shown only when notifications are off — that opens Android's own notification settings.
- **Why the change:** the operator asked for permissions to be requested on first launch. This
  **departs from `framework/15_device_targets_motorola_edge_60.md` §Notification permission**,
  which says to ask "when the user starts their first workout ... not at launch". The departure is
  deliberate and the spec's concern does not survive contact with this app's actual onboarding:
  the worry behind "not at launch" is a permission prompt arriving before the user knows what the
  app is, and by this point they have read the medical-safety notice and know exactly what it
  does. The spec also does not weigh the cost on the other side - a dialog between "start my
  workout" and the workout starting.
- **The defect this also fixes, which is the more important half:** D-0044's flow had no record
  of having asked, and `isGranted` stays false after a refusal, so a user who tapped Deny got the
  rationale dialog **on every single session start**. That is the app becoming the thing that
  nags, and it would have been found on the first day of real use. The flag fixes it, and it is
  needed for the onboarding step too - without it the step reappears on every launch.
- **Why the flag records only *that* we asked, never the answer:** whether the permission is held
  is a live platform question. The user can grant or revoke it in Android settings while the app
  is backgrounded, so a cached answer is an answer that goes stale.
  `NotificationPermission.isGranted` is read at the point of use, every time.
- **Why a screen rather than the bare system dialog:** "Allow VisceralFit to send you
  notifications?" is a question about a channel, and the honest answer depends on what the
  notifications are for. The screen answers that first - one notification, only while a session
  runs, never makes a sound, and what it buys is lock-screen pause, skip and end - and states
  plainly that workouts run either way.
- **Why Settings needs the row:** a runtime permission can only be requested a limited number of
  times. Once Android stops showing its dialog, an in-app request is a silent no-op, so system
  settings is the only route left. Without the row, "Not now" during onboarding would be
  permanent with no way back, which is not a choice anyone knowingly makes.
- **`NotificationPermission` lives in `core:common`** because three places need the same answer
  and must not disagree: onboarding decides whether to ask, the workout feature relies on the
  notification, Settings offers the way back. Two copies of an API-level check is how one of them
  ends up wrong on one API level.
- **`OnboardingStep` extracted and tested:** the ordering *is* the requirement - safety notice
  before any intensity decision (REQ-005), notification step after it and skipped when there is
  nothing to ask, neither reachable once passed. Four rules about sequence cannot be asserted
  inside a composable's `when`, so the decision is a pure function and each rule is a test.
- **What the audit found besides:** nothing missing. `POST_NOTIFICATIONS` is the only
  dangerous-level permission in the merged manifest and therefore the only one that can be
  requested at all; `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `VIBRATE` are
  normal-level and granted at install. Every permission-gated API in the codebase was checked
  against the manifest and none needs a permission the app lacks. `WAKE_LOCK`,
  `ACTIVITY_RECOGNITION`, `BODY_SENSORS`, `BLUETOOTH_CONNECT`, the exact-alarm permissions and
  battery-optimisation exemption were each considered and are each correctly absent - the sensor
  permissions in particular are exactly what ADR-0008 chose `mediaPlayback` to avoid.
- **Reverses if:** the operator finds the onboarding prompt intrusive, in which case the spec's
  original timing is one line of code away - but the "asked once" flag stays either way, because
  that part was a bug fix rather than a preference.
- **Affects:** `NotificationPermission` (new, `core:common`), `NotificationPermissionScreen`
  (new), `OnboardingStep` (new), `MainActivity`, `MainViewModel`, `UserPreferences`,
  `PreferencesDataSource`, `SettingsScreen`, `WorkoutHomeScreen`; deletes
  `SessionNotificationPermission` and its test.
- **Verification:** `OnboardingStepTest` - seven cases including the exhaustive check that no
  combination of the other flags gets past the safety notice, and the specific regression that a
  declined permission is never asked for twice.

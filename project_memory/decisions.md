# Decisions

Append-only log of choices with more than one defensible answer. Template:
`_templates/entry_templates.md`. Numbering: `D-NNNN`.

---

## Phase log

| Phase | Completed | New assumptions? | Notes |
|---|---|---|---|
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

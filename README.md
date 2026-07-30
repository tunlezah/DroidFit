# VisceralFit

An offline-first Android fitness app for exercise that supports visceral fat reduction, built
around floor Pilates, reformer Pilates, elliptical and spin bike work. Native Kotlin, Jetpack
Compose, Material 3, targeting Android 15+ with the **Motorola Edge 60** as the reference device.

**Status: framework and skeleton complete; the app cannot yet run a workout.** See
[Current state](#current-state).

---

## What this repository contains

| Path | What it is |
|---|---|
| `framework/` | The complete build specification — an executable plan an AI agent can follow without guessing |
| `project_memory/` | The project's durable memory: decisions, assumptions, issues, debt, research |
| `app/`, `core/`, `domain/`, `data/`, `feature-*/` | The Android app — a verified-building multi-module skeleton |
| `.github/workflows/android-ci.yml` | CI producing a sideloadable APK on every push |

**If you are an AI agent picking this up: start at [`framework/00_START_HERE.md`](framework/00_START_HERE.md).**
Then work through `framework/prompts/` in order, beginning with `phase_00_orientation.md`.

**If you are a human:** the shortest path to understanding the project is
[`framework/01_product_requirements.md`](framework/01_product_requirements.md) for what it does and
[`framework/02_evidence_base.md`](framework/02_evidence_base.md) for why it does it that way.

---

## What the app does, and what it deliberately does not

It generates evidence-based sessions from a researched exercise catalogue held entirely on the
device, times them, and coaches them aloud — announcing each exercise before it starts, describing
technique in text and speech, and keeping the screen awake while you train.

It does **not**:

- **Estimate visceral fat.** It cannot be measured without imaging, and the app does not pretend
  otherwise.
- **Claim spot reduction.** No exercise targets abdominal fat specifically. Exercise reduces fat
  systemically.
- **Present Pilates as a visceral-fat intervention.** The meta-analytic evidence shows real
  body-composition and strength benefit but **no** demonstrated waist-circumference effect, and the
  app is built to say exactly that.
- **Touch the network.** There is no `INTERNET` permission, no account, no subscription, no
  telemetry. Offline is not a premium tier; it is the only mode.

Every claim the app makes is traced to a citation in
[`framework/data/references.md`](framework/data/references.md).

---

## Current state

**Working and verified:**

- Multi-module Gradle build (13 modules), compiling on JDK 21 / Gradle 8.14.3 / AGP 8.13.2
- Minified release APK **2.63 MB**, debug-signed so it sideloads without a keystore
- CI: framework data checks, architecture compliance, detekt (with ktlint rules), unit tests,
  APK build, APK verification and size gate
- **A session runs end to end:** choose a duration and style, generate a plan, run it with a
  countdown that survives rotation and backgrounding, pause, skip, end early or finish, and have
  it recorded
- Workout generator implemented to specification, with a golden-file test that reproduces the
  spec's worked example segment for segment and invariants asserted across 1,680 requests
- 65 exercises authored to the content standard, validated in CI
- Safety notice shown and acknowledged before first use
- Weekly training load, vigorous minutes and rest-day advice computed from real history
- Settings screen fully functional end to end

**All four release blockers are closed** (`KI-0001`, `KI-0002`, `KI-0004`, `KI-0005`). What is
*not* done is tracked honestly rather than implied complete:

| Gap | Meaning | Phase |
|---|---|---|
| `KI-0017` | **None of the player has run on a device.** The clock is unit-tested; nothing has been seen | 07 |
| `KI-0018` | No spoken coaching yet — the player is silent | 08 |
| `KI-0016` | `POST_NOTIFICATIONS` is never requested, so session controls may not appear on API 33+ | 07 |
| `KI-0014` | Three engine faults passed every invariant test; the test gap is not closed | 06 |
| `KI-0003` | Most exercises render the placeholder illustration | 03 |

Full detail in [`project_memory/known_issues.md`](project_memory/known_issues.md).

---

## Building

Requires JDK 21 and an Android SDK with platform 36 and build-tools 36.0.0.

```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties   # gitignored

./gradlew qualityCheck          # detekt + unit tests — run before every commit
./gradlew :app:assembleRelease  # the sideload APK (~2.4 MB)
./gradlew :app:assembleDebug    # the debuggable build
```

The pinned version set in `gradle/libs.versions.toml` is a **verified-building baseline**. Two of
its constraints are not documented anywhere upstream and were only discoverable by compiling:
Hilt 2.59+ refuses AGP 8, and kotlinx-datetime 0.7.1 needs an `ExperimentalTime` opt-in on
Kotlin 2.2. If you bump versions and the build breaks, revert to the baseline and change one thing
at a time.

## Installing

1. GitHub Actions → the latest run → Artifacts → `visceralfit-sideload-apk`
2. Unzip and transfer the APK to the phone
3. Settings → Apps → Special app access → Install unknown apps → enable for your file manager
4. Tap the APK

The artefact is the **release** variant (R8-minified) signed with the **debug** key — so it needs
no keystore or secrets, but is 2.4 MB rather than 33 MB. See
[`framework/14_ci_cd_and_release.md`](framework/14_ci_cd_and_release.md) for the reasoning and for
how to move to a real keystore later. Note that migration is a one-way door for installed data,
which is why backup/restore matters.

---

## Continuing the build

1. Read [`framework/00_START_HERE.md`](framework/00_START_HERE.md) in full.
2. Read all eight files in `project_memory/`.
3. Start at `framework/prompts/phase_00_orientation.md` and work in order.
4. **Update `project_memory/` in the same commit as the work.** A phase is not complete until its
   exit criteria pass *and* its memory entries exist.

The eight memory files are append-only. Superseding an entry means adding a new one that says so,
not editing the old one — a decision later reversed is more informative than one that appears never
to have been made.

## Medical disclaimer

This app is not medical advice and does not diagnose, treat or monitor any condition. Consult a
clinician before starting vigorous exercise, particularly if you have a health condition or are new
to it. Stop and seek advice if you experience chest pain, dizziness or unusual breathlessness.

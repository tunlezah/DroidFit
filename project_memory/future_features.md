# Future features

Deliberately out of scope for v1, recorded with enough detail to pick up later.
Template: `_templates/entry_templates.md`. Numbering: `FF-NNNN`.

This file protects scope. When a good idea arrives mid-phase, it goes here rather than
into the phase.

---

### FF-0001 — Health Connect integration
- **Date:** 2026-07-30
- **What:** Read body mass (and possibly heart rate and workout records) from Health
  Connect, and optionally write completed sessions back so other apps see them.
- **Why not now:** Requires the `androidx.health.connect` dependency, a permissions flow,
  and a privacy review of what leaves the app's sandbox. The PRD lists it as
  "architect but do not require".
- **Prerequisites:** Onboarding flow (phase 07) so there is somewhere sensible to ask for
  permission; a settled decision on whether the app writes as well as reads.
- **Architectural hooks already in place:** `BodyMassProvider` with `@IntoSet` bindings
  (ADR-0006). A `HealthConnectBodyMassProvider` in a new `data-healthconnect` module
  needs no changes at any call site. Health Connect does **not** require the INTERNET
  permission, so D-0009 survives.
- **Rough size:** 1–2 days including the permission UX.

### FF-0002 — Wear OS companion
- **Date:** 2026-07-30
- **What:** Drive the timer from a watch, with haptic segment cues on the wrist and
  optional heart rate from the watch sensor.
- **Why not now:** A second app surface roughly doubles the UI and testing surface, and
  the operator's device pairing is unknown.
- **Prerequisites:** The workout engine and player must be stable first — the watch
  reflects session state, so unstable session state would be reflected twice.
- **Architectural hooks already in place:** `domain` is pure Kotlin, so the engine and all
  programming rules are reusable by a Wear module unchanged. Session state already lives in
  a service rather than a ViewModel (ADR-0008), which is the shape a data-layer bridge
  needs.
- **Rough size:** 1–2 weeks.

### FF-0003 — Bluetooth heart-rate strap
- **Date:** 2026-07-30
- **What:** Pair a BLE HR monitor and show live heart rate against the segment's target
  zone, with in-session guidance to ease off or push.
- **Why not now:** BLE permissions, pairing UX and connection recovery are a substantial
  feature on their own, and the app is designed to be fully usable without any sensor
  (which is why `IntensityTarget` leads with RPE — R-0005).
- **Prerequisites:** None architecturally; it is purely additive.
- **Architectural hooks already in place:** `IntensityTarget` already carries a
  `percentHrMaxRange` alongside the RPE range, so the zone targets are defined in HR terms
  and simply unused today.
- **Rough size:** 1 week.

### FF-0004 — On-device AI coaching (Gemini Nano / AICore)
- **Date:** 2026-07-30
- **What:** Generate varied coaching phrasing, adapt encouragement to the user's history,
  or answer "why am I doing this exercise?" — all on-device.
- **Why not now:** Model availability varies by device, output must be constrained so it
  cannot invent health claims (a hard requirement, not a nice-to-have), and the value over
  well-written static cues is unproven.
- **Prerequisites:** A cue-authoring layer that separates *what to say* from *how to say
  it*, so generated text can only ever vary phrasing and never content.
- **Architectural hooks already in place:** `SpeechCue` is a data class carrying text and
  priority, produced by a cue scheduler — a generator could sit in front of it without
  touching the TTS layer.
- **Rough size:** unknown; investigate before committing.
- **Risk to flag:** any generated text is a potential unsupported health claim. If built,
  generated cues must be constrained to a whitelist of phrasings, never free text.

### FF-0005 — Smart scale import (Eufy or similar)
- **Date:** 2026-07-30
- **What:** Import body mass and body-composition readings from a connected scale.
- **Why not now:** There is no official public API. Reverse-engineering a vendor's private
  cloud API would break on their schedule, may violate their terms, and would require the
  INTERNET permission — reversing D-0009 for a convenience feature.
- **Prerequisites:** An official, documented API. Until then, Health Connect (FF-0001) is
  the correct path, since most scale apps write there.
- **Architectural hooks already in place:** `BodyMassProvider` (ADR-0006).
- **Rough size:** small once an API exists; indefinite until then.
- **Decision to remember:** the app will **not** ship an unofficial scraping integration.

### FF-0006 — Multi-week structured programmes
- **Date:** 2026-07-30
- **What:** A 12-week plan with planned progression, deload weeks and a visible calendar,
  rather than one generated session at a time.
- **Why not now:** The generator must be trustworthy for single sessions before it is
  trusted to plan twelve weeks. Also the single most-praised feature in the competitor
  sample (R-0007), so it is a strong v2 candidate rather than a discard.
- **Prerequisites:** Phase 06 (engine) and phase 09 (training load) complete, because
  progression needs load history to progress *from*. The dose evidence in R-0002 —
  3×/week for 12–16 weeks — is exactly a programme shape.
- **Architectural hooks already in place:** `SavedWorkoutRepository`, `WorkoutRequest`
  seeds (so a planned session is reproducible), and `TrainingLoadSummary`.
- **Rough size:** 1 week.

### FF-0007 — Localisation
- **Date:** 2026-07-30
- **What:** Additional languages for UI and exercise content.
- **Why not now:** A-0006 — no requirement, and the exercise catalogue is roughly 60
  entries × 6 prose fields, which is the expensive part.
- **Prerequisites:** Catalogue frozen (phase 02), so translation is not re-done.
- **Architectural hooks already in place:** UI strings are moving to resources; the seed
  asset is versioned and could become `exercises_seed_<locale>.json` with the seeder
  choosing by locale. TTS already resolves `Locale.getDefault()` and reports
  `NO_VOICE_DATA_FOR_LOCALE` distinctly, so an unsupported voice degrades gracefully.
- **Rough size:** 2–3 days of engineering plus translation cost per language.

### FF-0008 — Encrypted local backup with a user-chosen passphrase
- **Date:** 2026-07-30
- **What:** Export history, measurements and preferences to an encrypted file the user
  controls; restore on a new device.
- **Why not now:** Plain-JSON local backup/restore is phase 10 and covers the actual need
  (device migration). Encryption adds key management, and a lost passphrase means lost
  data — which is worse than an unencrypted file the user chose where to put.
- **Prerequisites:** Phase 10 backup format settled.
- **Architectural hooks already in place:** `allowBackup="false"` means the app already
  owns its own backup story rather than relying on Android auto-backup, which matters
  because progress photos must never leave the device implicitly.
- **Rough size:** 2 days.

### FF-0009 — Landscape-optimised workout player for machine use
- **Date:** 2026-07-30
- **What:** A landscape layout for the workout player, designed for a phone in a bike or
  elliptical cradle.
- **Why not now:** Landscape must *work* in v1 (the PRD requires it and rotation is
  tested), but a purpose-built landscape layout is polish beyond that.
- **Prerequisites:** Workout player exists (phase 07).
- **Architectural hooks already in place:** `material3-window-size-class` is already a
  dependency; `DisplayPreferences.machineMode` and `TimerTypography.CountdownLarge` exist
  and are the intended anchor for this work.
- **Rough size:** 2 days.

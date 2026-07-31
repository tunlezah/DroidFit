# Assumptions

Things taken as true without confirmation. Every one is a place the project could be
wrong. Template: `_templates/entry_templates.md`. Numbering: `A-NNNN`.

**Read this file at the start of every phase.** If a phase gives you the information to
settle an open assumption, settle it and update the status — do not leave a confirmed
assumption marked open.

---

### A-0001 — The target device is a Motorola Edge 60 running Android 15  
**CONFIRMED (variant) / CORRECTED (OS version) 2026-07-30 — see UF-0006**
- **Date:** 2026-07-30
- **Assumption:** The primary device is the Motorola Edge 60 (2025): 6.67" pOLED,
  2712×1220 (444 ppi), 120 Hz, HDR10+, MediaTek Dimensity 7300, 5200 mAh, shipping
  Android 15 (API 35).
- **Why we had to assume:** The operator named the phone but not the exact variant. The
  Edge 60, Edge 60 Pro, Edge 60 Fusion and Edge 60 Stylus differ in chipset and panel.
- **Confidence:** high for the base Edge 60; medium that the operator does not actually
  have a Pro or Fusion.
- **How to confirm:** Settings → About phone → Model, and Android version.
- **If wrong:** Minor. The differences that matter to this app are screen size, refresh
  rate and OS version; nothing in the design depends on the chipset. A Fusion has a flat
  panel rather than curved, which only affects edge-inset padding.
- **Status:** open

### A-0002 — The operator has access to an elliptical and a spin bike, but not a reformer
- **Date:** 2026-07-30
- **Assumption:** Default `availableEquipment` is elliptical + spin bike; reformer is off.
- **Why we had to assume:** All four modalities were requested, but a reformer is a
  £1,500+ machine and its inclusion in a list is not evidence of ownership.
- **Confidence:** medium.
- **How to confirm:** Ask, or observe which equipment toggles the operator turns on at
  first run.
- **If wrong:** Trivial and self-correcting — the Settings screen fixes it in two taps,
  and the defaults are only defaults.
- **Status:** open

### A-0003 — "Custom" workout duration means 3–120 minutes  
**CONFIRMED 2026-07-30 — see UF-0006. Verbatim: "3-120 minutes is perfect."**
- **Date:** 2026-07-30
- **Assumption:** Custom duration accepts 3 to 120 minutes in 1-minute steps, alongside
  the eight presets the PRD lists.
- **Why we had to assume:** The PRD says "Custom" without bounds. Unbounded input allows
  a 0-minute or 47-hour session, both of which break the generator.
- **Confidence:** medium-high. 3 minutes is the shortest session that can contain a
  warm-up, one work interval and a cool-down; 120 minutes is double the longest preset.
- **How to confirm:** Ask, or ship it and see whether anyone hits the ceiling.
- **If wrong:** Bounds are one constant in `framework/07_workout_engine_spec.md`.
- **Status:** open

### A-0004 — Reformer MET value is approximated by "Pilates, general"
- **Date:** 2026-07-30
- **Assumption:** Reformer Pilates energy cost is estimated at 2.8 MET, the Compendium's
  "Pilates, general" value (code 02105).
- **Why we had to assume:** The 2024 Adult Compendium has no reformer-specific entry. Its
  Pilates entries are mat-based: 1.8 (traditional mat) and 2.8 (general).
- **Confidence:** low. Spring-loaded resistance work plausibly costs more than mat work,
  and the true figure may be meaningfully higher.
- **How to confirm:** Search for a reformer-specific indirect-calorimetry study; if none
  exists, keep the approximation and keep it labelled.
- **If wrong:** The energy estimate for reformer sessions is low. This is the *safe*
  direction to be wrong in — under-reporting expenditure does not encourage over-eating
  — and the figure is labelled "estimated" throughout. Recorded as a caveat in
  `research_summary.md` R-0006.
- **Status:** open

### A-0005 — Users have, or can install, a text-to-speech engine with voice data
- **Date:** 2026-07-30
- **Assumption:** Spoken coaching works because the device has a TTS engine with data
  for the user's locale. Most Android devices ship Google TTS, but voice data is not
  always downloaded, and it may be absent on a device that has never been online.
- **Why we had to assume:** The app cannot bundle a speech engine, and cannot download
  voice data (no INTERNET permission).
- **Confidence:** high that it works; certain that it sometimes will not.
- **How to confirm:** Handled in code rather than by confirmation —
  `SpeechState.Unavailable` is a first-class state with four distinguished reasons, and
  the workout runs without speech using tones and haptics.
- **If wrong:** Degraded, not broken. Phase 08 must include a manual test with TTS
  disabled at the OS level.
- **Status:** confirmed by design 2026-07-30 (mitigated rather than removed)

### A-0006 — English (UK) only for v1
- **Date:** 2026-07-30
- **Assumption:** `resourceConfigurations` is limited to `en` and `en-rGB`. Exercise
  content is authored in British English.
- **Why we had to assume:** No localisation requirement was given, and exercise cue
  content is the expensive part to translate — roughly 60 exercises × 6 text fields.
- **Confidence:** high.
- **How to confirm:** Ask whether any other language is needed.
- **If wrong:** Significant work. The catalogue would need a per-locale asset and the
  TTS locale handling would need to follow the content language rather than the system
  locale. Hooks noted in `future_features.md` FF-0007.
- **Status:** open

### A-0007 — The operator is a healthy adult without exercise contraindications
- **Date:** 2026-07-30
- **Assumption:** The default programme includes vigorous intervals at 85–95% HRmax,
  which assumes the user is cleared for vigorous exercise.
- **Why we had to assume:** No health information was provided, and asking for a medical
  history is out of scope for an offline app with no clinical role.
- **Confidence:** unknown — this is the assumption with the largest consequence if wrong.
- **How to confirm:** Cannot be confirmed by the app. Mitigated instead: a safety notice
  is shown before the first session and must be acknowledged
  (`safetyNoticeAcknowledged`), advanced content is gated behind an explicit level
  choice, exercises carry `cautionTags` the user can exclude, and every hard interval
  carries a stop-if-symptoms safety note.
- **If wrong:** Potentially serious. This is why the default experience level is
  BEGINNER and the default style is MIXED rather than HIIT, and why the app never
  auto-escalates a user to advanced content.
- **Status:** open — mitigated, not resolved. **Do not weaken these mitigations.**

### A-0008 — Waist circumference is a trend indicator the user measures themselves  
**CONFIRMED 2026-07-30 — see UF-0006. Measured manually at the navel, which is one of the two standard sites.**
- **Date:** 2026-07-30
- **Assumption:** Self-measured waist circumference has roughly ±1 cm of error, so
  changes below 1 cm are not reported as a trend.
- **Why we had to assume:** Self-measurement reliability varies with tape placement,
  breathing and time of day; no per-user error estimate is available.
- **Confidence:** medium.
- **How to confirm:** Literature on self-measured waist reliability; or have the operator
  measure three times in one sitting and observe the spread.
- **If wrong:** The threshold is one constant, `MEANINGFUL_CHANGE_CM`. Being too
  conservative means a real change is called "roughly unchanged" for a week longer than
  necessary — preferable to reporting noise as progress.
- **Status:** open

### A-0009 — The elliptical's light-effort MET value is scaled from the moderate entry
- **Date:** 2026-07-30
- **Phase:** 02
- **Assumption:** An easy elliptical stride costs about 4.0 MET.
- **Why it was needed:** The engine's warm-up pool and its cool-down spin-down both need a
  machine exercise the Compendium anchors at recovery. The spin bike has two such entries
  (3.5 and 4.0 MET, codes 01210 and 01214); the elliptical's lowest published entry is
  moderate effort at 5.0 MET (02048), with no light-effort code. Without a recovery-anchored
  elliptical exercise, an elliptical-only session cannot warm up on the machine or spin down
  on it, and falls through to floor work for both.
- **What was assumed:** that an elliptical and a stationary bike are close enough at an easy
  pace to share the cycling light-effort value. Both are seated-or-supported, continuous,
  low-impact and self-paced.
- **How to confirm it:** indirect calorimetry, or a heart-rate comparison of an easy stride
  against an easy spin at matched perceived effort. Neither is likely to happen, so this
  stays open.
- **If wrong:** the energy estimate for elliptical warm-ups and spin-downs is off by roughly
  ±1 MET over a few minutes — around 5 kcal for an 80 kg user over five minutes. It also
  determines pool membership, which matters more than the number: if the true anchor is
  Zone 2 rather than recovery, the exercise is still a valid warm-up (the pool admits Zone 2)
  but a slightly brisk spin-down.
- **Mitigation:** flagged `approximated_from` in `met_values.json` with the reasoning inline,
  which the data check requires for every approximated value.
- **Status:** open

### A-0010 — 20 rounds is a defensible ceiling on interval extension
- **Date:** 2026-07-30
- **Phase:** 06
- **Assumption:** No user needs more than 20 rounds of an interval template, so capping there
  is safe.
- **Why it was needed:** D-0026 extends the round count to fill long sessions. The extension
  needs a bound, and the specification gives none because it does not contemplate durations
  much above the template sizes.
- **What was assumed:** that 20 rounds — 80 minutes of work at `4x4`, or 30 minutes at
  `6x30s` — is beyond any real prescription, so the cap will never bind in practice for a
  sensible request.
- **How to confirm it:** ask the operator whether they would ever request an interval session
  longer than an hour. A-0007 is the related question and matters more.
- **If wrong:** requests long enough to hit the cap get a trailing active-recovery segment
  instead of more rounds, which is the behaviour the specification describes anyway.
- **Status:** open. See KI-0013: the deeper issue is that the app will build a 20-round
  interval session at all.

### A-0011 — Android 16 does not change the target API surface
- **Date:** 2026-07-30
- **Phase:** post-07
- **Assumption:** The device running Android 16 (API 36) rather than the assumed Android 15
  (API 35) requires no code change, because `compileSdk` and `targetSdk` are already 36.
- **Why it needs stating:** A-0001 assumed API 35, and several framework documents reason
  from it — `15_device_targets_motorola_edge_60.md` and the CI emulator image in
  particular. Being *ahead* of the assumed version is the safe direction for a
  `targetSdk` that already matches, but "safe direction" is not the same as "verified".
- **What is genuinely uncertain:** Android 16 tightens foreground-service behaviour and
  notification handling relative to 15. The player's `mediaPlayback` service and the
  ungranted `POST_NOTIFICATIONS` permission (KI-0016) are exactly the surfaces that
  changed. Since none of the player has run on a device (KI-0017), this is unverified on
  the version that matters rather than merely unverified in general.
- **How to confirm it:** sideload the CI APK onto the device and start a session. That
  single act settles A-0011, KI-0016 and most of KI-0017 at once.
- **Status:** open, and now the cheapest open question to close.

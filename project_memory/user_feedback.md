# User feedback

What the human operator actually said, verbatim, and what was done about it. Template:
`_templates/entry_templates.md`. Numbering: `UF-NNNN`.

**Why verbatim matters:** paraphrase drifts. Six phases later, "should allow selection of
any of these (or removal)" is a different requirement from "let the user pick exercise
types", and only one of them is what was asked for. Quote, then interpret separately, so a
later reader can check the interpretation against the source.

---

### UF-0001 — Original product request
- **Date:** 2026-07-30
- **Source:** Operator, initial session prompt
- **Verbatim:**
  > Create an android fitness app. The app needs to focus on excercise that will reduce
  > visceral fat and can be floor, reformer Pilates, elliptical or spin bike focussed, but
  > should allow selection of any of these (or removal). Should allow customisable time
  > period. App will be heavily slanted towards a Motorola edge 60, so focus on android
  > features from that line (I believe Android 15). It should have the option to announce
  > the exercise coming up. It should research what are evidence backed exercises and pull
  > them in locally. If supported, also have the option to describe both verbally and
  > written how to perform that exercise properly. Maybe have the agent spun up a couple of
  > research agents to discover the best features and highest rated features from other
  > apps and use those features. Have an agent research and come up with the best possible
  > design. App should also allow for keeping the screen on. The AI should also setup a
  > GitHub action that fully compile and outputs a working APK that can be sideloaded (does
  > not need to be store based).
- **Interpretation:** Four modalities, each independently enable/disable-able. Duration is
  user-configurable, not fixed. Android 15 / Edge 60 as the reference device. Pre-announce
  the upcoming exercise. Exercise content researched and stored locally, taught in both
  text and speech. Competitor and design research feeding real feature decisions.
  Keep-screen-on as a user option. CI producing a sideloadable APK — explicitly not a
  store release.
- **Action taken:** Every clause is traced to a numbered requirement in
  `framework/01_product_requirements.md`; the trace table there maps this quote to
  REQ-010, REQ-012, REQ-051, REQ-040..047, REQ-070 and the CI requirements. Research
  findings are R-0001..R-0008 in `research_summary.md`.
- **Still open:** "focus on android features from that line" is broader than what has been
  used so far (edge-to-edge, AMOLED, 120 Hz, pOLED battery behaviour, TTS). Motorola-specific
  extras — Moto Actions, the Smart Connect desktop mode, the Edge 60's IP69 rating for
  poolside use — were considered and not pursued; see `framework/15_device_targets_motorola_edge_60.md`
  §Deliberately not used.

### UF-0002 — Framework structure and project memory requirement
- **Date:** 2026-07-30
- **Source:** Operator, initial session prompt
- **Verbatim:**
  > I want you to beuild me a complete development framework that I could pass to an AI
  > Agent that could build it. I don't want it to be confused or have to guess anything, so
  > I need it to be very robust. The files should be in a single directoy in github in an
  > organised structure. As the project builds, the building prompts should ensure that at
  > each step of the way the project memory directory structure is updated.
  >
  > Structure:
  > directory: /project_memory
  > and the subsequent files:
  > decisions.md, known_issues.md, assumptions.md, architecture_decisions.md,
  > future_features.md, technical_debt.md, research_summary.md, user_feedback.md
- **Interpretation:** The deliverable is a framework a *different* agent can execute
  without guessing. "Very robust" means specifications precise enough to remove judgement
  calls, not documents that merely gesture at what to do. `/project_memory` with exactly
  those eight files, at the repository root. Every phase prompt must include a mandatory
  project-memory update step — updating memory is part of the work, not a postscript.
- **Action taken:** `/project_memory` created with all eight files, seeded with real
  content rather than empty templates, plus a `README.md` and entry templates. The
  framework lives in `/framework`. Every file in `framework/prompts/` ends with a
  **Project memory updates** section that is part of that phase's exit criteria, and
  `framework/00_START_HERE.md` §4 states the protocol.
- **Still open:** nothing.

### UF-0003 — Questions answered before the framework was written
- **Date:** 2026-07-30
- **Source:** Operator, answering four clarifying questions
- **Verbatim (question → answer):**
  > Deliverable: "Framework + compiling skeleton"
  > APK signing: "Debug-signed only"
  > Exercise media: "Hand-authored Compose vector art, no third-party assets"
  > Research: "I research now and bake findings in"
- **Interpretation:** Ship a working build, not documents alone. No keystore or secrets.
  No licensed or stock imagery of any kind. The research is done up front and baked in, so
  the building agent implements established findings instead of researching and possibly
  inventing citations.
- **Action taken:** D-0001 (skeleton), D-0003 and ADR-0002 (debug signing), ADR-0003
  (drawn artwork), R-0001..R-0008 plus `framework/02_evidence_base.md` and
  `framework/data/references.md` (baked-in research).
- **Still open:** nothing. Note that the operator chose debug-only signing over the
  offered "debug always, release when secrets exist" option, so the release path is
  documented but not wired.

### UF-0004 — Defaults the operator did not specify, proceeded on
- **Date:** 2026-07-30
- **Source:** Operator did not respond to these individually; they were stated in-session
  as the defaults being used, with an invitation to change any of them
- **Verbatim (as presented to the operator):**
  > Package / app id: com.visceralfit.app, app name VisceralFit
  > minSdk / target: minSdk 29, compileSdk/targetSdk 36
  > Reformer Pilates: Included but off by default
  > Custom duration: 3–120 min, 1-min granularity, plus the 8 presets
  > Eufy scale: Provider interface + manual entry only. No unofficial API scraping
  > TTS: explicit no-engine/no-voice-data fallback path
  > Progress photos: app-private storage, excluded from backup, optional biometric gate
  > Layout: framework in /framework, /project_memory at repo root, app modules at root
- **Interpretation:** Silence is not agreement. Each of these is a real assumption, and
  each is recorded as such so it can be revisited.
- **Action taken:** Implemented as stated. Recorded as A-0002 (equipment), A-0003
  (duration bounds), A-0006 (locale), D-0004 (reformer default), D-0008 (SDK levels),
  FF-0005 (scale), A-0005 (TTS fallback).
- **Still open:** All of these are open assumptions rather than confirmed requirements.
  The building agent should surface them to the operator at the first natural checkpoint —
  phase 00's exit criteria include exactly that.

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

### UF-0005 — Build the app, with a CI pipeline, and watch it build
- **Date:** 2026-07-30
- **Phase:** 02 and 06
- **Verbatim:**
  > Follow the guidance in framework/00_START_HERE.md and start building this app. There are
  > other files that you should look into the context of as you build. Decisions are in these
  > files, you must build a robust app, a CI pipeline in GitHub and monitor it to ensure it
  > builds. It will be sideloaded so no signing needed.
- **Interpretation:**
  1. Work the phase sequence in `framework/prompts/`, treating the framework's recorded
     decisions as settled rather than re-litigating them.
  2. "Robust" is the reason the phases with the most test surface — 02's catalogue validation
     and 06's engine invariants — were done first and thoroughly, rather than reaching for
     screens.
  3. "A CI pipeline in GitHub and monitor it" — the workflow already existed and was green at
     the framework commit; the instruction is to keep it green as work lands, and to check it
     rather than assume.
  4. "Sideloaded so no signing needed" **confirms ADR-0002**: the release variant is
     debug-signed on purpose, so the APK installs with "install unknown apps" enabled and no
     keystore or secret is needed. No change was required; this closes the question rather
     than opening it. Note the distinction the operator's phrasing glosses over: Android will
     not install a genuinely *unsigned* APK, so "no signing" means "no *release* signing",
     which is what the pipeline already does.
- **Action taken:** phases 02 and 06 completed and pushed; CI watched to green on each push.
- **Still open:** the phase-00 assumption questions (A-0001..A-0008) have **not** been
  answered. A-0007 in particular — whether the operator is cleared for vigorous exercise —
  has a safety dimension and gates whether the default programme is appropriate. It is asked
  again in the summary of this work.

### UF-0006 — Device, duration range and waist measurement confirmed
- **Date:** 2026-07-30
- **Phase:** post-07
- **Verbatim:**
  > So exact phone is Mtorola Edge 60 (australian version). It's currently running Android
  > 16.  3-120 minutes is perfect. I measure my waist around the belly button manually.I am
  > a bit baffled by the decision (KI-0020), could you clarify exactly what you want me to
  > choose between?
- **Interpretation, assumption by assumption:**
  1. **A-0001 — partly confirmed, partly corrected.** The variant is the base Edge 60
     (Australian retail), not the Pro, Fusion or Stylus, which settles the chipset and panel
     the framework assumed. But the OS is **Android 16 (API 36)**, not the Android 15 the
     assumption stated. `compileSdk` and `targetSdk` are already 36, so nothing is
     miscompiled — but the CI emulator was pinned to API 35 *because* of the wrong
     assumption, so it was testing the wrong platform. Corrected to 36. New assumption
     A-0011 records what remains genuinely unverified: Android 16 changed foreground-service
     and notification behaviour, which is precisely where the player is untested.
  2. **A-0003 — confirmed.** 3–120 minutes stands. No change needed; the bound is already
     enforced by the generator and by `SessionLimits`.
  3. **A-0008 — confirmed, with a detail worth keeping.** Measured manually at the navel.
     That is one of the two standard sites (the other is the midpoint between the lowest rib
     and the iliac crest, which is the WHO protocol). Navel measurement reads slightly
     larger and is more sensitive to breathing and posture, which *supports* rather than
     undermines the existing ±1 cm reporting threshold. The waist screen should say which
     site to use so the measurement stays consistent over time — recorded as KI-0021,
     because consistency of site matters far more than which site.
- **Action taken:** assumption statuses updated; CI emulator moved to API 36; KI-0021
  opened. KI-0020 answered separately once the operator has chosen.
- **Still open:** A-0002 (equipment actually available), A-0006 (locale), and **A-0007
  (cleared for vigorous exercise)** — the last of which has a safety dimension and is the
  one that determines whether the default programme is appropriate.

### UF-0007 — The floor category was never Pilates
- **Date:** 2026-07-31
- **Phase:** post-07
- **Verbatim:**
  > Yes, it should be bodyweight and excercises. It should not have been "floor pilates" it
  > was "reformer pilates".
- **Interpretation:** the answer to KI-0020 is yes — a session with no machine should be able
  to be hard — but not by adding a fifth category. The existing category was misnamed. The
  equipment-free category is **bodyweight and floor work**; the Pilates in this app is the
  **reformer**. Implemented as D-0039.
- **Why this was a better answer than either option offered:** both options in KI-0020 took
  the name `floor_pilates` as given and worked around it. Naming the floor category "Pilates"
  put general bodyweight movement inside an evidence constraint that applies to a specific
  method, which is what made star jumps unusable. Renaming removes the problem rather than
  compensating for it.
- **Action taken:** modality renamed, aerobic capability made a property of the modality,
  seven bodyweight cardio exercises authored across all three levels, catalogue at 72.
  KI-0020 closed; KI-0022 opened for the one soft spot the new design has.
- **One assumption made rather than asked:** the classical mat repertoire already authored —
  dead bug, the hundred, roll up, teaser, jack knife — stays in the bodyweight category. It is
  bodyweight floor work, so it fits the new name, and nothing about it is lost. Say so if it
  should instead move to the reformer category or be split out as mat Pilates.
- **Still open:** A-0002 (which equipment you actually have — in particular whether there is a
  reformer, since its 13 exercises are otherwise inert), A-0006 (locale), and **A-0007
  (cleared for vigorous exercise)**. A-0007 matters more now, not less: a no-equipment day can
  reach 85–95% HRmax through jumping, where before it could not.

### UF-0008 — Moderately vigorous, owns a reformer, wants free choice of any combination
- **Date:** 2026-07-31
- **Phase:** post-07
- **Verbatim:**
  > I can do moderatley vigourous excercise. I have a reformer. So you must make sure that I
  > can chop and choose from any combo of "floor" (which is my own body weight excercises
  > etc... "elliptical", "Spin Bike" and "Pilates Reformer" (If we need to add "Floor
  > Reformer" as well, do that. I should be able to select a minimum of one of these
  > categories or all of the, so that I can just opt for one day doing spin bike, or tomorrow
  > I do elliptical and floor. All I need is a minimum of one selected.
- **Interpretation:**
  1. **A-0007 answered, and not with a yes.** "Moderately vigorous" is not clearance for
     85–95% of maximum heart rate, which is what the default programme was building. Treated
     as a ceiling at threshold (76–84%) and implemented as a real setting rather than a
     hard-coded assumption — D-0040. Reading this as "yes to vigorous" would have been the
     unsafe interpretation of an ambiguous phrase, so it was read the other way.
  2. **A-0002 answered.** Elliptical, spin bike and reformer all present. All four modalities
     now default to enabled and available — D-0041.
  3. **Free choice, minimum one.** This is REQ-011, which already existed and was already
     enforced — but silently: the toggle sprang back with no explanation, which reads as a
     broken switch. Now refused with a stated reason.
- **Action taken:** effort ceiling added and defaulted to threshold; all modalities enabled by
  default; REQ-011's refusal explained on screen; the safety notice's opening paragraph
  corrected, since it claimed 85–95% and that is no longer what the app does by default.
- **"Floor Reformer" — not built, and here is why.** There is no such apparatus; a reformer is
  a reformer. The four categories the operator listed all exist and are independently
  selectable, so nothing is missing for the stated goal. The nearby thing that *would* be a
  real fifth category is **mat Pilates** as distinct from general bodyweight work — the
  classical repertoire already in the catalogue (the hundred, roll up, teaser, jack knife)
  versus jumping and burpees. Splitting them would let "an easy Pilates day" and "a hard
  bodyweight day" be chosen separately. Not done, because it is a guess about what was meant
  and it costs a re-tag of roughly fifteen exercises. Asked rather than assumed.
- **Still open:** A-0006 (English only). That is the last one outstanding.

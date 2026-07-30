# Research summary

What research established, with citations. Append-only. Template:
`_templates/entry_templates.md`. Numbering: `R-NNNN`.

Citation keys resolve in `framework/data/references.md`. The fuller narrative version of
the exercise-science findings is `framework/02_evidence_base.md`; this file is the
decision-relevant summary.

**Rule for the building agent:** you may extend this file with new findings. You may not
delete or weaken an existing finding without adding a new entry that explains why, with
its own citation. Never add a finding without a source, and never cite a source you have
not actually read the abstract of.

---

### R-0001 — Exercise reduces visceral adipose tissue; vigorous aerobic work and HIIT rank highest
- **Date:** 2026-07-30
- **Question:** Which exercise modes actually reduce visceral fat, and how do they rank?
- **Finding:** A network meta-analysis of **84 RCTs (4,836 participants)** in adults with
  overweight or obesity found that aerobic exercise of at least moderate intensity,
  resistance training, combined aerobic + resistance, and HIIT were **all** beneficial
  for reducing visceral adipose tissue (VAT). On SUCRA probability ranking, **vigorous-intensity
  aerobic exercise and HIIT had the highest probability of being the best intervention**
  for VAT, and also for body mass, total body fat, BMI, waist circumference and
  subcutaneous fat. **Resistance training was the least effective** of the four, and a
  subgroup analysis found it improved VAT in males and in those with body fat below 40%,
  but not in females or those at or above 40% body fat.
- **Strength of evidence:** systematic review and network meta-analysis of RCTs — the
  strongest tier available for this question.
- **Sources:** `chen2024nma`
- **How it changed the product:** This is the backbone of the whole programme design.
  It is why the four workout styles are HIIT, Zone 2, Mixed and Recovery with **aerobic
  work as the spine of every session**, why the elliptical and spin modalities carry the
  intensity load, and why Pilates is positioned as complementary rather than as the
  primary driver (see R-0003). It is also why MIXED, not HIIT, is the default: HIIT ranks
  best but is not appropriate as an unsupervised starting point (A-0007).
- **Caveats:** Participants had overweight or obesity, so ranking may differ in lean
  populations. SUCRA rankings express probability of being best, not effect magnitude —
  the differences between the top interventions are not necessarily large.

### R-0002 — Effective dose: roughly 3 sessions a week, 30–60 minutes, over 12–16 weeks
- **Date:** 2026-07-30
- **Question:** What dose of exercise actually shifts visceral fat, and how does that
  relate to public-health guidance?
- **Finding:** Across the VAT literature the recurring effective dose is **three sessions
  per week of 30–60 minutes of aerobic exercise, sustained for 12–16 weeks**. Aerobic
  exercise is central, and volumes *below* current public-health recommendations can still
  produce beneficial VAT change. Separately, low-to-moderate intensity preferentially
  reduces abdominal *subcutaneous* fat, while higher intensity is needed to reduce both
  subcutaneous and visceral compartments comparably.
  Guideline anchors:
  - **WHO 2020:** adults should do **150–300 min** moderate **or 75–150 min** vigorous
    aerobic activity per week (or an equivalent mix), plus muscle-strengthening on
    **2+ days** a week.
  - **ACSM 2009 position stand:** 150–250 min/week of moderate activity prevents weight
    gain but produces only modest weight loss; **clinically significant weight loss is
    reported above 250 min/week**; maintenance after loss is better above 250 min/week.
- **Strength of evidence:** systematic reviews and meta-analyses (dose), plus WHO and
  ACSM guidelines.
- **Sources:** `chen2024nma`, `ismail2012`, `who2020`, `donnelly2009`
- **How it changed the product:** Default weekly goal is **150 minutes** (the WHO lower
  bound) rather than 250, because a first goal a user actually hits beats an optimal goal
  they abandon; the Progress screen states the WHO range explicitly so the user can see
  what a higher target would be. It is also why the app tracks *weekly minutes* as its
  headline metric rather than session count, and why the intensity distribution matters
  enough to be modelled (`WorkoutStyle`) rather than left to chance.
- **Caveats:** Dose findings come from trial protocols, which are more consistent than
  real-world behaviour. The 250-minute figure is about weight loss, not VAT specifically.

### R-0003 — Pilates improves body composition but has not been shown to reduce waist circumference
- **Date:** 2026-07-30
- **Question:** Does Pilates belong in a visceral-fat-focused app, and if so in what role?
- **Finding:** A meta-analysis of **11 RCTs (393 participants)** with overweight or
  obesity found Pilates significantly reduced body mass
  (**MD −2.40 kg**, 95% CI −4.04 to −0.77), BMI (**MD −1.17 kg/m²**, 95% CI −1.85 to
  −0.50) and body fat percentage (**MD −4.22%**, 95% CI −6.44 to −2.01), but found
  **no significant effect on waist circumference** (MD −2.65 cm, 95% CI −6.84 to +1.55 —
  the interval crosses zero) and none on lean mass. Effects were larger in obesity-only
  populations and in interventions longer than 10 weeks. Included protocols ran 30–90
  minutes, 3–6 times a week, for 8–24 weeks. A 2025 RCT of *reformer* Pilates in
  overweight and obese women reported body-composition and strength improvements.
- **Strength of evidence:** meta-analysis of RCTs; the reformer-specific evidence is a
  single RCT.
- **Sources:** `wang2021pilates`, `ozturk2025reformer`
- **How it changed the product:** This is the single most important finding for honesty.
  Pilates **is** included — it was requested, and it has real body-composition and
  strength benefits — but the app must not present it as a visceral-fat intervention on a
  par with vigorous aerobic work. Concretely: the generator never builds a
  Pilates-only session and claims a cardiometabolic effect for it; Pilates supplies
  warm-up, core and cool-down content and the RECOVERY style; and the copy never says or
  implies that a Pilates movement targets abdominal fat. The PRD's instruction that
  "Pilates movements should only be included where supported as contributing to the
  overall goal" is implemented as exactly this rule.
- **Caveats:** 393 participants across 11 trials is a modest evidence base, 77% female.
  The waist-circumference null result may reflect low power rather than no effect — but an
  app may not treat a null result as a positive one.

### R-0004 — HIIT and moderate continuous training are comparable for fat loss; HIIT is time-efficient
- **Date:** 2026-07-30
- **Question:** Should the app push HIIT over steady-state work?
- **Finding:** Head-to-head, HIIT and moderate-intensity continuous training (MICT)
  produce broadly **similar** fat-loss outcomes, with HIIT showing advantages on waist
  circumference, percentage fat mass and VO₂peak in some analyses, and no consistent
  statistically significant difference in others. HIIT achieves this in less total time.
  The best-established vigorous protocol is the **Norwegian 4×4**: four 4-minute bouts at
  85–95% HRmax separated by 3 minutes of active recovery, which produced substantially
  greater VO₂max improvement than volume-matched moderate training.
- **Strength of evidence:** multiple meta-analyses (mixed conclusions on superiority),
  plus a well-replicated RCT protocol.
- **Sources:** `chen2024nma`, `sultana2023`, `helgerud2007`
- **How it changed the product:** Both HIIT and ZONE_2 are first-class styles and neither
  is presented as the "real" one. The 4×4 structure is the specified template for the
  HIIT style at durations that fit it (`framework/07_workout_engine_spec.md`). MIXED is
  the default because it hedges an uncertainty the literature has not resolved, and
  because variety sustains adherence — which dominates any difference between the two.
- **Caveats:** Adherence and injury risk over months are not well captured by 8–16 week
  trials, and they matter more than a few percentage points of fat mass.

### R-0005 — Intensity anchors usable without a heart-rate strap
- **Date:** 2026-07-30
- **Question:** How should the app express intensity when it has no heart-rate sensor?
- **Finding:** Zone 2 is variously defined as ~60–70% HRmax (common practical guidance),
  72–82% HRmax (Norwegian five-zone model), 1.5–2.5 mmol/L blood lactate, or 60–75% of
  FTP. Definitions anchored to lactate threshold vary more between individuals than
  percentage-of-max definitions. Vigorous interval work is specified at 85–95% HRmax.
  Perceived exertion on the Borg CR10 scale tracks these ranges closely enough for
  unsupervised training.
- **Strength of evidence:** expert guidance and comparative analysis; definitions are
  genuinely contested.
- **Sources:** `who2020`, `helgerud2007`, `rogers2025zone2`
- **How it changed the product:** `IntensityTarget` carries **both** an RPE range and a
  %HRmax range for every zone, and the UI leads with RPE because the app has no HR input.
  The conservative end of the contested Zone 2 range (60–70% HRmax, RPE 3–4) was chosen
  deliberately: for a beginner, being told to go slightly too easy is a much cheaper error
  than being told to go too hard. The talk test ("you could hold a conversation") is
  included in the cues because it needs no equipment at all.
- **Caveats:** %HRmax estimated from age has wide individual error. This is why the app
  never *computes* a heart rate for the user to chase.

### R-0006 — MET values for energy estimation, from the 2024 Adult Compendium
- **Date:** 2026-07-30
- **Question:** What MET values should the energy estimate use?
- **Finding:** From the 2024 Adult Compendium of Physical Activities:
  | Activity | Code | MET |
  |---|---|---|
  | Elliptical trainer, moderate effort | 02048 | 5.0 |
  | Elliptical trainer, vigorous effort | 02049 | 9.0 |
  | Bicycling, stationary, RPM/spin bike class | 01270 | 9.0 |
  | Bicycling, stationary, 126–150 W | 01228 | 8.0 |
  | Bicycling, stationary, 200–229 W, vigorous | 01236 | 10.8 |
  | Bicycling, stationary, 230–250 W, very vigorous | 01240 | 12.5 |
  | Bicycling, HIIT | 01305 | 8.8 |
  | Bicycling, stationary, 50 W, light | 01214 | 4.0 |
  | Bicycling, stationary, 25–30 W, very light | 01210 | 3.5 |
  | Pilates, traditional, mat | 02103 | 1.8 |
  | Pilates, general | 02105 | 2.8 |
  | Calisthenics, moderate effort | 02022 | 3.8 |
  | HIIT, moderate effort | 02210 | 7.0 |
  | Stretching, mild | 02101 | 2.3 |
  There is **no reformer-specific entry**; 2.8 ("Pilates, general") is used as an
  approximation.
- **Strength of evidence:** the standard reference compendium for activity energy cost.
- **How it changed the product:** These are the values in
  `framework/data/met_values.json` and in the seed catalogue. The formula is
  `kcal = MET × body mass (kg) × hours` (`EstimateEnergyExpenditure`).
- **Caveats:** MET values are population averages and ignore individual efficiency, so
  every displayed figure is labelled "estimated". The reformer approximation is A-0004 and
  is likely an underestimate.

### R-0007 — Competitor teardown: what highly rated apps get right, and what users complain about
- **Date:** 2026-07-30
- **Question:** Which features do well-reviewed fitness and Pilates apps have, and what do
  users complain about?
- **Finding:** Features consistently praised in the highest-rated interval and class apps:
  spoken interval names ahead of time; saveable presets so the user is not reconfiguring
  before every session; a large, readable timer visible from a distance; colour-coded
  work/rest states; offline download of content; structured multi-week programmes rather
  than isolated sessions; and progress attributes broken out by dimension rather than a
  single score. The most-cited complaints across app-store and review-site samples are
  about **subscriptions** (charges continuing after uninstall, cancellation flows that
  fail), **forced accounts and login problems**, and **content locked behind a paywall
  including offline access**.
- **Strength of evidence:** market observation from review aggregators and app listings.
  Not scientific evidence, and vulnerable to selection bias in what reviewers write about.
- **Sources:** review-site and app-listing survey, July 2026 — see
  `framework/03_competitive_analysis.md` for the specific products examined.
- **How it changed the product:** Directly shaped four decisions. (1) Machine mode with a
  148 sp countdown exists because "readable from the bike" is a repeated praise point.
  (2) `announceNextExercise` is on by default because pre-announcing is the single most
  praised coaching feature. (3) The app has **no accounts, no subscription and no
  network**, which structurally eliminates the three largest complaint categories. (4)
  Disabled controls must state *why* they are disabled — silent dead controls were a
  recurring frustration.
- **Caveats:** People write reviews when angry or delighted, so complaint frequency is not
  a measure of prevalence. Feature popularity is also not evidence of efficacy — this
  entry informs UX, never programming.

### R-0008 — Platform constraints: keeping the screen on, and foreground service types
- **Date:** 2026-07-30
- **Question:** How should a workout timer keep the screen awake and keep running in the
  background on Android 15?
- **Finding:** The supported way to keep the display on is the window flag
  `FLAG_KEEP_SCREEN_ON`, set **in an activity and only in an activity**; Compose 1.9 added
  `Modifier.keepScreenOn()` as a declarative equivalent.
  `PowerManager.SCREEN_BRIGHT_WAKE_LOCK` is deprecated, and partial wake locks count
  against the Android vitals excessive-wake-lock metric. For background continuation,
  Android 15 restricts foreground service types: **`mediaPlayback` and `location` may run
  indefinitely** with a persistent notification, while other types are subject to a
  6-hour limit. Apps targeting Android 15+ may not start a `mediaPlayback` foreground
  service from a `BOOT_COMPLETED` receiver.
- **Strength of evidence:** official Android platform documentation and developer guidance.
- **Sources:** Android developer documentation on wake-lock best practice, foreground
  service types, and Android 15 behaviour changes (July 2026).
- **How it changed the product:** `KeepScreenOn` in `core:designsystem` uses the window
  flag with graceful degradation outside an Activity, and takes an `enabled` parameter so
  toggling the setting mid-session takes effect immediately (ADR-0008). The workout
  service is typed `mediaPlayback`, which is both accurate — its ongoing output is audio
  coaching — and the type without a 6-hour cap, relevant for a 90-minute session with
  pauses.
- **Caveats:** Foreground-service policy has tightened in each recent Android release and
  may tighten again. Revisit on each targetSdk bump.

# 02 — Evidence base

This document is the app's scientific spine. Every programming decision the workout engine
makes must be traceable to something on this page, and every claim the app makes to a user
must be supported here.

Citation keys resolve in `framework/data/references.md`. The decision-relevant summary is
`project_memory/research_summary.md` R-0001..R-0008; this document is the working
reference the engine spec depends on.

**If you are the building agent:** you may add evidence. You may not remove or soften a
finding here without adding a `research_summary.md` entry that explains why, with its own
citation. You may never write a claim into the app that is not on this page.

---

## 1. What the evidence actually supports

### 1.1 Exercise reduces visceral adipose tissue

The strongest available synthesis is a network meta-analysis of **84 randomised controlled
trials, 4,836 participants** with overweight or obesity (`chen2024nma`). It found:

- Aerobic exercise at moderate intensity or above **reduces VAT**.
- Resistance training reduces VAT, but is the **least effective** of the modes compared,
  and its effect was present in males and in people below 40% body fat, and **absent** in
  females and people at or above 40% body fat.
- Combined aerobic + resistance reduces VAT.
- HIIT reduces VAT.
- On SUCRA ranking, **vigorous-intensity aerobic exercise and HIIT** had the highest
  probability of being the best intervention — for VAT and also for body mass, total body
  fat, BMI, waist circumference and subcutaneous fat.

An earlier meta-analysis (`ismail2012`) found a significant pooled effect for aerobic
exercise versus control and **no** significant effect for progressive resistance training
versus control, with the aerobic-versus-resistance comparison favouring aerobic without
reaching significance.

### 1.2 Intensity matters for *which* fat compartment moves

Low-to-moderate intensity preferentially reduces abdominal **subcutaneous** fat. Higher
intensity is needed to reduce subcutaneous and **visceral** fat comparably
(`ismail2012`, `chen2024nma`).

**Engine consequence:** every style except RECOVERY must contain work at or above moderate
intensity. A session made entirely of light movement is a valid *recovery* session and is
never presented as a visceral-fat session.

### 1.3 Dose

| Source | Prescription |
|---|---|
| `chen2024nma`, `ismail2012` | ~3 sessions/week, 30–60 min aerobic, 12–16 weeks |
| `who2020` | 150–300 min/week moderate **or** 75–150 min/week vigorous, or an equivalent mix; muscle-strengthening on 2+ days |
| `donnelly2009` (ACSM) | 150–250 min/week prevents weight gain, modest loss; **>250 min/week** for clinically significant loss and for maintenance after loss |

Notably, VAT benefit has been observed at volumes **below** current public-health
recommendations — so a user doing less than 150 min/week is still doing something
worthwhile, and the app must not imply otherwise.

**Engine consequence:** the default weekly goal is 150 minutes (WHO lower bound). The
Progress screen states the WHO range so a user can see what a higher target looks like.
The app's headline metric is weekly *minutes*, not session count.

### 1.4 HIIT versus steady-state

Head-to-head, HIIT and moderate-intensity continuous training produce broadly similar
fat-loss outcomes; some analyses favour HIIT on waist circumference, percentage fat mass
and VO₂peak, others find no consistent significant difference (`sultana2023`,
`chen2024nma`). HIIT achieves comparable results in less time.

The best-replicated vigorous protocol is the **Norwegian 4×4** (`helgerud2007`): 4 × 4 min
at 85–95% HRmax, separated by 3 min of active recovery, after a warm-up. In its original
trial it produced substantially greater VO₂max improvement than volume-matched moderate
continuous training.

**Engine consequence:** HIIT and ZONE_2 are both first-class; neither is the "real" one.
The 4×4 is the specified HIIT template wherever the requested duration accommodates it.
MIXED is the default because the literature has not resolved the superiority question, and
because variety supports adherence — which matters more over months than the difference
between the two.

### 1.5 Pilates — what it does and does not do

A meta-analysis of **11 RCTs, 393 participants** with overweight or obesity
(`wang2021pilates`):

| Outcome | Effect | 95% CI | Significant? |
|---|---|---|---|
| Body mass | −2.40 kg | −4.04 to −0.77 | Yes |
| BMI | −1.17 kg/m² | −1.85 to −0.50 | Yes |
| Body fat % | −4.22% | −6.44 to −2.01 | Yes |
| **Waist circumference** | −2.65 cm | **−6.84 to +1.55** | **No** |
| Lean mass | — | — | No |

Effects were larger in obesity-only populations and in interventions longer than 10 weeks.
Included protocols: 30–90 min, 3–6×/week, 8–24 weeks, 77% female participants.

A 2025 RCT of **reformer** Pilates in overweight and obese women reported improvements in
body composition, strength and psychosomatic measures (`ozturk2025reformer`).

**This is the most important constraint in the document.** Pilates has real, measured
body-composition and strength benefits. It has **not** been shown to reduce waist
circumference, and there is no VAT-specific evidence for it at anything like the strength
of the aerobic evidence.

**Engine consequences, binding:**

1. Pilates supplies warm-up, core, mobility and cool-down content, and is the substance of
   the RECOVERY style.
2. A Pilates-only session may be generated when the user enables only Pilates modalities —
   the user asked for it, and it is beneficial exercise. But it is labelled by what it
   is (strength, control, mobility) and **never** framed as a visceral-fat session.
3. When Pilates is combined with a cardio modality, the aerobic work carries the
   cardiometabolic load and the Pilates work is positioned as complementary.
4. No copy anywhere says or implies a Pilates movement targets abdominal fat.

This is the concrete implementation of the PRD's instruction that "Pilates movements
should only be included where supported as contributing to the overall goal".

---

## 2. Intensity anchors

The app has no heart-rate sensor, so every zone is defined by **both** RPE (Borg CR10) and
%HRmax, and the UI leads with RPE.

| Zone | RPE (CR10) | %HRmax | Talk test | Used by |
|---|---|---|---|---|
| Recovery | 2–3 | 50–60% | Full conversation, easily | RECOVERY, cool-downs, rest |
| Zone 2 | 3–4 | 60–70% | Full sentences, slightly breathy | ZONE_2, MIXED base, warm-ups |
| Threshold | 6–7 | 76–84% | Short phrases only | MIXED surges |
| Vigorous | 8–9 | 85–95% | A word or two | HIIT work intervals |

These are `IntensityTarget.RECOVERY`, `.ZONE_2`, `.THRESHOLD` and `.VIGOROUS` in
`domain/model/WorkoutStyle.kt`.

**On the Zone 2 controversy:** definitions genuinely differ — ~60–70% HRmax in common
practical guidance, 72–82% HRmax in the Norwegian five-zone model, 1.5–2.5 mmol/L lactate,
60–75% of FTP (`rogers2025zone2`). Lactate-anchored definitions vary more between
individuals than percentage-of-max ones.

The **conservative** end (60–70% HRmax, RPE 3–4) was chosen deliberately. For an
unsupervised beginner, being told to go slightly too easy costs a little training effect;
being told to go too hard costs adherence and possibly safety. The talk test is included in
the cue text because it needs no equipment and no arithmetic.

**The app never computes a target heart rate for the user to chase.** Age-predicted HRmax
has wide individual error, and a specific number invites the user to trust it.

---

## 3. Style prescriptions

These are the definitions the engine implements. Full segment-level templates are in
`framework/07_workout_engine_spec.md`.

### HIIT
- **Evidence:** `chen2024nma` (top SUCRA rank), `helgerud2007` (4×4 protocol).
- **Structure:** warm-up ≥ 5 min at Zone 2, then repeated vigorous bouts with active
  recovery, then ≥ 3 min cool-down.
- **Canonical template:** 4 × 4 min at 85–95% HRmax with 3 min active recovery — needs
  ≥ 36 min total. Shorter durations use shorter bouts (see the engine spec's template
  table); the app does not squeeze a 4×4 into 15 minutes and call it a 4×4.
- **Gating:** requires INTERMEDIATE or above. A beginner selecting HIIT is offered MIXED
  and told why, and can override.

### Zone 2
- **Evidence:** `chen2024nma`, `who2020`.
- **Structure:** warm-up ≥ 3 min ramping into zone, continuous Zone 2 work, ≥ 3 min
  cool-down. No intervals.
- **Available to:** all levels. This is the safe default for a beginner.

### Mixed
- **Evidence:** `chen2024nma` (both aerobic and interval modes effective),
  `sultana2023` (no consistent superiority), plus adherence reasoning.
- **Structure:** a Zone 2 base with 2–5 threshold surges of 1–3 min, positioned in the
  middle third of the session.
- **Available to:** all levels. **The app default.**

### Recovery
- **Evidence:** `wang2021pilates` (Pilates body-composition benefit),
  `acsm2021` (recovery and flexibility programming).
- **Structure:** low-intensity mobility and controlled Pilates work throughout. Counts
  toward weekly volume; contributes no vigorous minutes.
- **Available to:** all levels.

---

## 4. Progression

Progression is **conservative and never automatic**.

- Weekly volume increases by no more than ~10% week over week, and only when the previous
  week was largely completed.
- The experience level never changes by itself. Promotion is an explicit user action, with
  a screen explaining what changes.
- Vigorous work is capped: no more than 3 vigorous sessions in any 7-day window, and never
  on consecutive days at ADVANCED, never two days in a row below that.
- After 2 or more consecutive vigorous days, or a week above the user's usual load, the
  app recommends a RECOVERY session rather than silently generating a hard one.

**Evidence:** `acsm2021` FITT-VP progression principles, `who2020` volume framing. The
specific 10% and 3-per-week figures are conventional practice rather than trial-derived —
recorded as such in `project_memory/assumptions.md` when the recovery recommender is built
in phase 09.

---

## 5. Energy expenditure

Formula: **kcal = MET × body mass (kg) × hours** (2024 Adult Compendium,
`pacompendium2024`).

MET values are in `framework/data/met_values.json` and reproduced in
`project_memory/research_summary.md` R-0006. Selected values:

| Activity | Compendium code | MET |
|---|---|---|
| Elliptical, moderate | 02048 | 5.0 |
| Elliptical, vigorous | 02049 | 9.0 |
| Spin bike class | 01270 | 9.0 |
| Stationary bike, 200–229 W, vigorous | 01236 | 10.8 |
| Stationary bike, 230–250 W | 01240 | 12.5 |
| Bicycling HIIT | 01305 | 8.8 |
| Stationary bike, 25–30 W (active recovery) | 01210 | 3.5 |
| Pilates, mat | 02103 | 1.8 |
| Pilates, general | 02105 | 2.8 |
| Calisthenics, moderate | 02022 | 3.8 |
| Stretching, mild | 02101 | 2.3 |
| Quiet sitting (rest segments) | 07021 | 1.3 |

**Known gap:** there is no reformer-specific Compendium entry. 2.8 ("Pilates, general") is
used and is likely an underestimate — recorded as `A-0004`. Underestimating expenditure is
the safe direction to be wrong in.

**Rules:**
- Body mass unknown → the estimate is `null` and the UI shows `—`. Never a default mass
  (D-0005).
- Every displayed figure is labelled "estimated" or prefixed `~`.
- The estimate is never used as an input to programming decisions. It is information for
  the user, not a control signal.

---

## 6. Claims we do not make

Read this list before writing any user-facing copy.

| Never say | Why | Say instead |
|---|---|---|
| "Burns belly fat" / "targets visceral fat" | Spot reduction is not supported. Exercise reduces fat systemically | "Supports whole-body fat loss, including visceral fat" |
| "Your visceral fat is X" | The app has no means of measuring it (REQ-090) | Nothing. Do not offer a figure |
| "Your body fat is X%" | Same — no measurement, and waist is not a substitute | Show the waist trend, labelled as a trend |
| "This Pilates move flattens your stomach" | `wang2021pilates` found no significant waist effect | "Builds core strength and control" |
| "You burned 412 calories" | It is an estimate from a population average | "~410 kcal estimated" |
| "Fat-burning zone" | The concept misrepresents substrate use versus total expenditure | Name the zone and its purpose |
| "Detox" / "boosts metabolism" / "resets your hormones" | Not supported at any useful level | Nothing |
| "Lose N cm in N weeks" | Individual response varies far too much to promise | "Most people see change over 12–16 weeks of consistent training" |
| "Safe for everyone" | It is not (A-0007) | The safety notice, and per-exercise safety notes |

**Voice guidance:** describe what the *exercise* is and what the *evidence* supports.
Never predict an outcome for the individual user. When in doubt, state the mechanism and
let the user draw the conclusion.

---

## 7. Where each claim in the app comes from

| Where it appears | Claim | Source |
|---|---|---|
| Progress screen, weekly card | "WHO recommends 150–300 min moderate or 75–150 vigorous" | `who2020` |
| Progress screen, waist card | "Waist measurements track a trend, not visceral fat" | REQ-003, `wang2021pilates` |
| Zone 2 cue text | "a pace you could hold a conversation at" | Talk test, §2 |
| Vigorous cue text | "eight to nine out of ten" | Borg CR10, §2 |
| HIIT template | 4 min work / 3 min recovery at 85–95% HRmax | `helgerud2007` |
| Any energy figure | "estimated", `~` prefix | `pacompendium2024` |
| Exercise MET values | Per-exercise | `pacompendium2024`, per-code |
| Safety notice | Stop-on-symptoms, consult-a-clinician | `acsm2021` pre-participation guidance |

When you add user-facing copy that makes any factual claim, add a row here.

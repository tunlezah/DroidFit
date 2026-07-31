# 08 — Exercise library specification

How to author the exercise catalogue. This is health guidance, so the authoring standard is
higher than for ordinary content.

Source of truth: `app/src/main/assets/exercises_seed.json`. Schema:
`data/seed/ExerciseSeeder.kt`. Fourteen worked examples already exist — **match their depth,
do not thin it out to hit a count.**

---

## 1. Target counts (phase 02)

| Modality | Minimum | Rationale |
|---|---|---|
| Bodyweight and floor | 24 | Carries warm-up, core, mobility and cool-down for every session |
| Reformer Pilates | 10 | Fewer users, but a session needs enough for variety |
| Elliptical | 8 | Continuous modality: variation is intensity and cadence, not movement |
| Spin bike | 12 | Seated/standing × flat/climb × intensity gives natural variety |
| **Total** | **~54** | |

Also required for the generator's pools to be non-empty at every level:

- At least **3 exercises per modality per experience level**.
- At least **6** with `met_value <= 2.5` (mobility pool, for cool-downs).
- At least **6** with `met_value <= 4.0` on a Pilates modality (warm-up pool).
- At least **4** with `met_value >= 8.0` on a machine modality (vigorous pool).

Verify these with the validation test in §5, not by counting manually.

## 2. Field-by-field authoring standard

### `id`
Lowercase snake_case, prefixed with the modality: `bodyweight_dead_bug`. **Never renamed
once shipped** — session history and favourites reference it.

### `name`
What a competent instructor would call it. Use the established Pilates name where one exists
(`the hundred`, `single leg stretch`) — a user who has taken a class should recognise it.

### `modality`, `difficulty`
Enum ids. Difficulty rules:
- **beginner** — safe to attempt unsupervised with no prior experience; no complex
  coordination; low injury consequence if form degrades.
- **intermediate** — assumes basic body awareness; may require sustained position or
  coordination; form degrading is uncomfortable but not dangerous.
- **advanced** — high load, high coordination, or meaningful injury risk if form fails.
  Standing sprints, loaded reformer work.

When in doubt, rate it harder. A beginner served an intermediate movement is inconvenienced;
an intermediate movement labelled beginner is a hazard.

### `met_value`
From `framework/data/met_values.json`, with the Compendium code. If no code fits, use the
closest, add `approximated_from`, and add an `assumptions.md` entry. **Never invent a value.**

### `how_to`
Ordered steps. Each step is one action. Requirements:

- **Setup before movement.** Where the body is, before what it does.
- **Complete enough to perform correctly with no video** (REQ-041). This is the bar.
- **State the endpoint.** "Stop before your lower back lifts" — not "lower the leg".
- **Include tempo where it matters.** "moving slowly enough that the movement takes about
  four seconds each way".
- **Include breathing where the method specifies it.** Pilates does; spin does not.
- 4–6 steps typical. Under 3 is probably underspecified; over 8 is probably two exercises.

### `spoken_instruction`
One sentence, read aloud when the segment starts. Constraints:

- **Must finish inside the shortest interval this exercise can appear in** (REQ-044). At a
  typical TTS rate of ~150 words per minute, the limits are:

  | Exercise | Limit | Why |
  |---|---|---|
  | `met_value >= 8.0` | **14 words** (~5.5 s) | These appear in 30 s HIIT work intervals, so the cue must be a small fraction of the segment |
  | everything else | **20 words** (~8 s) | These appear in multi-minute blocks, where 8 s is comfortable |

  Say it aloud at speaking pace and time it. The validation test enforces both limits.
- Names the exercise, then gives the single most important cue.
- No numbers that duplicate the on-screen timer.
- Avoid words TTS mangles: prefer "eight to nine out of ten" over "8–9/10".

### `safety_notes`
At least one. Requirements:
- **The specific failure mode**, not generic caution. "Stop lowering the leg the moment your
  lower back arches away from the floor" — not "be careful".
- **Every exercise with `met_value >= 8.0` must include a stop-if-symptoms note** (REQ-006):
  chest pain, dizziness, unusual breathlessness.
- Equipment safety where relevant: "Check the springs are attached before you load the
  carriage."

### `common_mistakes`
At least one. What actually goes wrong, and ideally why it matters: "Letting the lower back
arch, which shifts the work off the deep abdominals."

### `muscles_worked`
Plain names — "Deep abdominals", "Glutes" — not Latin. Primary movers first, 2–4 entries.

### `illustration_id`
Key into the drawing registry. May reference a drawing that does not exist yet; the
placeholder handles it (REQ-046). Reuse an id where the position is genuinely the same.

### `caution_tags`
From this closed set. Do not invent tags — the settings UI enumerates them.

`lower_back`, `neck`, `shoulder`, `wrist`, `knee`, `hip`, `ankle`, `pregnancy`,
`cardiac_caution`, `balance`

Tag generously. A false positive costs the user one exercise; a false negative costs them an
injury.

### `evidence_keys`
Keys from `framework/data/references.md` supporting inclusion. At minimum the modality-level
evidence (`chen2024nma` for cardio, `wang2021pilates` for Pilates). **Never a key that is not
in that file.**

## 3. Content that must not appear

| Never | Why |
|---|---|
| "Targets belly fat", "burns visceral fat", "melts", "torches" | Spot reduction is not supported (`02_evidence_base.md` §6) |
| An exercise-specific fat-loss claim | No evidence at that granularity |
| Rep or set counts in `how_to` | The app is time-based. A rep count fights the timer |
| Prescriptive weights or spring settings as absolutes | Equipment varies. Say "a moderate spring load, typically two to three springs depending on your reformer" |
| "Everyone can do this" | They cannot |
| Diagnostic or therapeutic framing ("fixes your posture", "cures back pain") | Clinical claims |

## 4. Balance requirements

Beyond the counts, the catalogue as a whole must:

- Cover the **full intensity range** per machine modality, so the generator can build both
  Zone 2 and vigorous segments without falling back.
- Include **cool-down-appropriate** movements (`met_value <= 2.5`) that are not just
  stretches — controlled breathing and gentle mobility.
- Include **at least 4 exercises with no `caution_tags`** per modality, so a user with
  several exclusions still has a workable session.
- Avoid **duplicate positions with different names**. Variety must be real, not cosmetic.

## 5. Validation (KI-0006 — write this in phase 02)

A JVM test in `data` that reads the asset from resources and asserts:

- [ ] Parses with `ignoreUnknownKeys = false`.
- [ ] Every `id` unique, snake_case, prefixed with its modality.
- [ ] Every `modality` and `difficulty` resolves to a known enum id.
- [ ] `met_value > 0` and appears in `met_values.json` (or is documented as approximated).
- [ ] `how_to` has ≥ 3 entries, none blank.
- [ ] `spoken_instruction` non-blank, **≤ 14 words when `met_value >= 8.0`, ≤ 20 words
      otherwise**.
- [ ] `safety_notes` and `common_mistakes` each ≥ 1, none blank.
- [ ] `muscles_worked` ≥ 2.
- [ ] Every `caution_tag` is in the closed set from §2.
- [ ] Every `evidence_key` exists in `references.md` (parse it, do not hard-code a list).
- [ ] Every exercise with `met_value >= 8.0` has a safety note matching a stop-if-symptoms
      pattern.
- [ ] No `how_to`, `safety_notes`, `common_mistakes` or `spoken_instruction` string matches a
      prohibited-claim regex built from §3.
- [ ] The per-modality, per-level and per-pool minimum counts from §1 are met.

This test is the actual quality gate. Everything above it is guidance; this is enforcement.

## 6. Updating content after release

Content changes are **not** schema changes (ADR-0005):

1. Edit the asset.
2. Increment `version` at the top of the file.
3. The seeder upserts changed rows and deletes withdrawn ids on the next cold start.
4. User history is untouched — sessions reference exercise ids, and a corrected safety note
   improves the record retroactively rather than corrupting it.

If you withdraw an exercise, note it in `decisions.md` with the reason. A movement removed
because the evidence did not hold up is exactly the kind of thing a future reader needs to
know happened.

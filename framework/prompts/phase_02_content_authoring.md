# Phase 02 — Content authoring

## Objective
Grow the exercise catalogue from 14 to ~54 exercises meeting the authoring standard, and add the
validation test that makes content quality enforceable rather than aspirational.

Closes **KI-0004** (release blocker) and **KI-0006**.

## Read first
- `framework/08_exercise_library_spec.md` — all of it, twice
- `framework/02_evidence_base.md` §6 (prohibited claims) and §1.5 (the Pilates constraint)
- `framework/data/met_values.json`
- The 14 existing entries in `app/src/main/assets/exercises_seed.json` — **match their depth**

## Agents
**Fitness Science** (leads — has veto over every entry). QA (for the validation test).
Documentation.

## Files you may touch
- `app/src/main/assets/exercises_seed.json`
- `data/src/test/kotlin/**` — the validation test
- `framework/data/met_values.json` — if a new MET value is needed
- `framework/08_exercise_library_spec.md` — if the standard needs clarifying
- `project_memory/*`

## Work

### 1. Write the validation test FIRST
Before authoring content. `08_exercise_library_spec.md` §5 lists every assertion. Put it in
`data/src/test/kotlin/com/visceralfit/data/seed/ExerciseCatalogueValidationTest.kt`, reading the
asset from the test classpath.

Writing it first means every exercise you author is checked as you go, rather than 54 of them
being checked at the end when fixing them is tedious and tempting to skip.

The prohibited-claims regex is built from `02_evidence_base.md` §6. It must actually fail on a
string containing "burns belly fat" — write that test case explicitly to prove the gate works.

### 2. Author to the target counts
From `08_exercise_library_spec.md` §1:

| Modality | Minimum | Currently |
|---|---|---|
| Floor Pilates | 24 | 7 |
| Reformer Pilates | 10 | 2 |
| Elliptical | 8 | 2 |
| Spin bike | 12 | 3 |

Plus the pool minimums: ≥3 per modality per level; ≥6 at MET ≤ 2.5; ≥6 Pilates at MET ≤ 4.0;
≥4 machine at MET ≥ 8.0; ≥4 per modality with no caution tags.

Author in modality batches, running the test after each batch.

### 3. Hold the standard
The failure mode of this phase is thin content authored to hit a count. Each entry needs:
- `how_to` that lets someone perform the movement **correctly with no video** (REQ-041). This is
  the bar; 4–6 real steps, with setup before movement, the endpoint stated, and tempo or
  breathing where the method specifies it.
- A `spoken_instruction` of **≤ 14 words** that you have said aloud and timed.
- Safety notes naming the **specific** failure mode, not generic caution.
- A stop-if-symptoms note on everything at MET ≥ 8.0.
- Caution tags from the closed set, tagged generously.

### 4. Bump the seed version
`version` at the top of the asset goes to 2. That is what makes the seeder pick up the new
content on existing installs.

### 5. Verify on device
Install and confirm the Train screen's exercise count matches, and that the seeder upgraded an
existing install rather than requiring a reinstall.

## Exit criteria
- [ ] Validation test exists and covers every assertion in the spec §5.
- [ ] The prohibited-claims regex demonstrably fails on a planted bad string.
- [ ] All per-modality, per-level and per-pool minimums met — asserted by the test, not counted
      by hand.
- [ ] `./gradlew :data:test` green.
- [ ] `./gradlew qualityCheck` green.
- [ ] Seed version bumped; an existing install upgrades in place.
- [ ] Exercise count on the Train screen matches the catalogue.
- [ ] Fitness Science review pass completed against §3 of its brief — with objections raised and
      resolved, not a rubber stamp.

## Project memory updates
- `decisions.md` — phase-log row. A `D-` entry for any difficulty rating that was a genuine
  judgement call, and for any exercise you decided **not** to include.
- `assumptions.md` — any MET approximation, any difficulty rating you are unsure of.
- `known_issues.md` — close KI-0004 and KI-0006. Open an entry for any exercise you could not
  author to standard.
- `research_summary.md` — if authoring surfaced an evidence question.

## Do not
- Author content without the validation test running.
- Include rep or set counts — the app is time-based.
- Rate difficulty down to make more exercises available to beginners.
- Write any claim on the prohibited list. The test will catch it, but a near-miss that passes the
  regex is still wrong.

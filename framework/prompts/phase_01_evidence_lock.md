# Phase 01 — Evidence lock

## Objective
Independently verify the baked-in evidence base, extend it where the catalogue will need
support, and lock it so later phases build on checked facts.

## Read first
- `framework/02_evidence_base.md` — all of it
- `framework/data/references.md`
- `project_memory/research_summary.md` R-0001..R-0006

## Agents
**Fitness Science** (leads). Documentation.

## Files you may touch
- `framework/02_evidence_base.md`
- `framework/data/references.md`
- `framework/data/met_values.json`
- `project_memory/*`

No code.

## Work

### 1. Verify every citation
For each key in `references.md`, confirm the paper exists and that what this framework claims it
found is what it actually found. The DOIs and PMIDs are given; use them.

Note the limitation already recorded: `chen2024nma`, `wang2021pilates` and `ismail2012` were
read at **abstract level** — the full texts were paywalled. If you can access a full text and it
qualifies or contradicts the abstract-level summary, that is a finding worth recording.

**If a citation does not check out, say so loudly.** A fabricated or misread citation is the
worst possible defect in this project, because every programming decision rests on it. Record it
in `known_issues.md` as a blocker and correct `02_evidence_base.md`.

### 2. Fill the gaps the catalogue will need
Phase 02 authors ~54 exercises, each needing `evidence_keys`. Currently the only keys available
are modality-level. Look for:

- Evidence on **warm-up and cool-down** structure and duration (`acsm2021` covers this — cite the
  specific chapter).
- Anything **reformer-specific** beyond the single 2025 RCT. If nothing exists, record that as a
  finding: it justifies the reformer content being conservative.
- Anything on **exercise-order effects** (cardio before or after resistance) if the generator
  will ever mix them in one session.
- A better **reformer MET value** than the 2.8 approximation (A-0004). If none exists, close the
  assumption as "confirmed: no better source exists", which is a real answer.

### 3. Check the intensity anchors
`02_evidence_base.md` §2 chose the conservative end of a genuinely contested Zone 2 definition.
Confirm the ranges are defensible and that the RPE↔%HRmax pairings are consistent with the
sources. If you change one, change it in `IntensityTarget` too and record why.

### 4. Re-run the prohibited-claims list
`02_evidence_base.md` §6 is the list phase 02's validation test will enforce as a regex. Make
sure it is complete — add anything you have seen in the wild that should be banned. It is easier
to add a prohibition now than to retro-fix content.

### 5. Verify §7 is complete
Section 7 maps every factual claim the app currently makes to a source. Walk the built app and
confirm nothing is missing. Any claim without a row is either unsupported or undocumented, and
both are defects.

## Exit criteria
- [ ] Every key in `references.md` verified against the actual paper, or flagged.
- [ ] Any misstatement corrected in `02_evidence_base.md`, with a `research_summary.md` entry
      recording the correction.
- [ ] Warm-up/cool-down evidence cited specifically enough for phase 02 to use.
- [ ] A-0004 (reformer MET) either resolved or explicitly closed as unresolvable.
- [ ] Intensity anchors confirmed or changed-with-reason.
- [ ] Prohibited-claims list reviewed and extended.
- [ ] §7 claim map complete against the built app.
- [ ] `./gradlew qualityCheck` still green (nothing should have changed in code — confirm that).

## Project memory updates
- `research_summary.md` — a new `R-` entry per verification pass and per new finding. Record
  corrections explicitly; do not silently edit the evidence base.
- `assumptions.md` — resolve or re-confirm A-0004. Add any new assumption.
- `decisions.md` — phase-log row. A `D-` entry for any changed anchor or prescription.
- `known_issues.md` — any citation that did not check out.

## Do not
- Delete or soften an existing finding without a new `research_summary.md` entry explaining why,
  with its own citation.
- Add a citation you have not read at least the abstract of.
- Let a market observation from `03_competitive_analysis.md` cross into the evidence base.

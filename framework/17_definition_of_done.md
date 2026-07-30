# 17 — Definition of done

Two gates: per-phase, and project-level. **Read this before claiming anything is finished.**

---

## 1. Per-phase gate

Every phase, without exception:

- [ ] The phase's own exit criteria pass (in its `framework/prompts/phase_NN_*.md`).
- [ ] `./gradlew qualityCheck` green.
- [ ] `./gradlew :app:assembleRelease` succeeds and the APK is under the size ceiling.
- [ ] Every new public declaration has a KDoc comment explaining **why**, not what.
- [ ] Every new screen or component has previews in light, dark and AMOLED.
- [ ] The accessibility checklist (`12_accessibility_spec.md` §7) run for any UI change.
- [ ] `/project_memory` updated in the same commit — the specific files the phase names.
- [ ] The phase log table in `decisions.md` has a row for this phase.
- [ ] Nothing added to `known_issues.md` is a regression introduced by this phase without
      being fixed or explicitly accepted.

A phase whose code works but whose memory entries are missing is **not complete** (REQ-122).

## 2. Release blockers

These must be closed before the app is used for real training. They are tracked in project
memory and repeated here because they are easy to lose:

| Item | What | Where |
|---|---|---|
| **KI-0001** | No workout can be performed — the generator is unimplemented | phase 06 |
| **KI-0002 / TD-0007** | Weekly minutes always reads 0. A *wrong* number, not a missing one | phase 09 |
| **KI-0005** | The safety notice is never shown; no onboarding | phase 07 |
| **KI-0004** | Catalogue is 14 exercises, below the generator's variety needs | phase 02 |
| **KI-0007** | No migration test harness — must exist before the first schema change | phase 06 or earlier |

## 3. Project-level gate

Derived from the PRD's Definition of Done, made checkable.

### Functionality
- [ ] A user can configure and complete a session in every style, at every preset duration and
      at custom durations, on every enabled modality combination.
- [ ] Generation failures are explained with a fix, never silent or fatal.
- [ ] History records sessions correctly, including active-versus-wall-clock duration.
- [ ] Weekly minutes and streaks are computed from real data.
- [ ] Measurements can be recorded and their trend shown honestly.
- [ ] Backup and restore round-trips without data loss.

### Evidence and honesty — the gate that matters most
- [ ] Every programming rule traces to a citation in `framework/data/references.md`.
- [ ] The content validation test passes, including the prohibited-claims regex.
- [ ] No screen estimates visceral fat or body fat percentage.
- [ ] Pilates content is positioned per `02_evidence_base.md` §1.5 — no Pilates-only session
      framed as a visceral-fat intervention.
- [ ] Every energy figure is labelled estimated; unknown body mass renders `—`, never `0`.
- [ ] The safety notice is shown and acknowledged before the first session.
- [ ] `02_evidence_base.md` §7 lists every factual claim the app makes, and each has a source.

### Offline and privacy
- [ ] No `INTERNET` permission in the merged manifest.
- [ ] `./gradlew :app:dependencies` shows no HTTP client, image loader or analytics SDK.
- [ ] A full session completes in airplane mode with identical behaviour.
- [ ] No account, no login, no telemetry, no advertising id.
- [ ] Progress photos are in app-private storage, excluded from backup, never in MediaStore.

### Quality
- [ ] Everything in `13_testing_strategy.md` §7 is covered.
- [ ] The engine's four property tests and the golden-file test pass.
- [ ] The emulator suite has actually run and passed at least once (closing KI-0008).
- [ ] The manual device script (`13_testing_strategy.md` §8) has been run on a physical Edge 60
      and its results recorded in the phase log.
- [ ] detekt clean with no new suppressions.

### Accessibility
- [ ] TalkBack pass on every screen.
- [ ] 200% font scale on every screen with no clipping.
- [ ] Contrast verified in light, dark and AMOLED.
- [ ] No state distinguishable by colour alone.
- [ ] Automated accessibility checks green.

### Performance
- [ ] Cold start under 800 ms.
- [ ] No frame over 16 ms during a 60 s session sample.
- [ ] A 45-minute session costs under 12% battery with AMOLED dark and screen on.
- [ ] APK under 12 MB.

### Build and delivery
- [ ] CI green on the default branch.
- [ ] The APK artefact installs by sideload on a real Edge 60 and runs.
- [ ] The APK verification step (manifest, dex, size) passes.

### Documentation
- [ ] Every `framework/` document reflects the code as built, not as planned.
- [ ] All eight project-memory files current, with no phase missing from the log.
- [ ] Every open assumption either confirmed, refuted, or explicitly still open with a reason.
- [ ] `known_issues.md` has no open blocker or major issue.
- [ ] Root `README.md` explains how to build, install and run.

## 4. What "done" does not mean

- Not "every feature in the PRD's SHOULD list". Those may be deferred with a
  `technical_debt.md` or `future_features.md` entry. What may **not** be deferred is anything
  marked MUST in `01_product_requirements.md`.
- Not "all tests pass". Passing tests over incomplete coverage is a weaker signal than the
  §3 checklist.
- Not "the operator has not complained". Silence is not approval (UF-0004).

## 5. Honest reporting

When reporting completion, state plainly:

- What was built and verified, with the command or test that verified it.
- What was **not** built, and why.
- What is assumed rather than confirmed.
- What is known to be broken.

Do not describe a phase as complete because its code compiles. Do not describe a manual test as
run if it was not — if no physical device was available, say so. An unrun test reported as
passing is the single most damaging thing that can be recorded in this project, because every
later decision is built on it.

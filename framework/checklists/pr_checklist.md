# Pull request / review checklist

For reviewing a change, whether your own or another agent's. Ordered by how much damage the defect
would do if it shipped.

---

## 1. Honesty and safety — blocking

- [ ] No claim that is not in `framework/02_evidence_base.md` §7.
- [ ] Nothing on the prohibited-claims list (§6): spot reduction, "burns belly fat", fat-burning
      zone, detox, outcome predictions for the individual user.
- [ ] No Pilates-only session framed as a visceral-fat intervention (REQ-004).
- [ ] No number displayed that the app cannot justify. Unknown → `—`, never `0`, never a default.
- [ ] The safety notice and per-exercise safety notes are intact and not weakened.
- [ ] No caution tag removed, no difficulty rating lowered, without a `decisions.md` entry.
- [ ] Nothing auto-promotes the user's experience level.

## 2. Privacy — blocking

- [ ] No `INTERNET` permission in the **merged** manifest (check the merged one; a library can add
      it).
- [ ] No HTTP client, image loader, analytics or crash-reporting SDK on the classpath.
- [ ] No advertising id, install referrer or device fingerprint read.
- [ ] Progress photos in app-private storage only, excluded from backup.
- [ ] `allowBackup="false"` unchanged.
- [ ] No user data in a log statement — no body mass, measurements or notes.

## 3. Data integrity — blocking

- [ ] No `fallbackToDestructiveMigration`.
- [ ] Any schema change has a migration **and** a migration test, and the schema JSON is committed.
- [ ] No DataStore key renamed. No shipped exercise id renamed.
- [ ] No retroactive back-fill of historical data (energy estimates in particular).
- [ ] An import merges rather than overwrites, and reports what it did.

## 4. Architecture

- [ ] No `android.*` in `domain/`.
- [ ] No `feature-*` → `data`, and no `feature-*` → `feature-*`.
- [ ] `core:designsystem` has no `domain` dependency.
- [ ] Dispatchers injected; no `Dispatchers.*` outside `core:common`; no `GlobalScope`.
- [ ] No version literal outside the catalogue.
- [ ] No clock or timer in a ViewModel or composable — session state lives in the service
      (ADR-0008).
- [ ] A new integration uses the `@IntoSet` provider pattern rather than naming a specific
      provider at a call site.

## 5. UI and accessibility

- [ ] `Route` / `Screen` split; no `hiltViewModel()` in a `*Screen`.
- [ ] All four states handled.
- [ ] Previews in light, dark and AMOLED.
- [ ] Colours from the theme; no literal `Color(…)` outside `core:designsystem`.
- [ ] Text in `sp`, from the type scale.
- [ ] Targets ≥ 48 dp, rows ≥ 56 dp, player controls 72 dp.
- [ ] One TalkBack focus stop per logical control.
- [ ] No state distinguishable by colour alone.
- [ ] Works at 200% font scale.
- [ ] No disabled control without a visible reason.

## 6. Tests

- [ ] Every new behaviour tested.
- [ ] No sleeping, no real `delay`.
- [ ] Nothing mocks the class under test.
- [ ] Determinism proven by golden file.
- [ ] Failure paths tested, not just the happy path.
- [ ] No test that only asserts "does not throw".

## 7. Build

- [ ] `./gradlew qualityCheck` green.
- [ ] APK under the size ceiling — and if it grew, the growth was investigated rather than the
      ceiling raised.
- [ ] No quality gate disabled, skipped or loosened to get green.
- [ ] No secret or keystore committed.

## 8. Documentation and memory

- [ ] Project memory updated in **this** commit, with ids named in the message.
- [ ] Phase-log row present.
- [ ] No memory entry edited rather than superseded.
- [ ] Every entry gives a reason, not just a description.
- [ ] Any framework document the change contradicts has been corrected.
- [ ] New public declarations have KDoc explaining why.

---

## Reviewing honestly

The most valuable thing a reviewer does is notice a claim that outruns its evidence. That is not a
style question — it is the difference between a useful tool and one that misleads someone about
their health.

The second most valuable is noticing a number that is *wrong* rather than missing. A missing value
is visible to the user; a plausible wrong one is not, and every downstream decision inherits it.

If a change makes the build green by weakening a gate, that is a finding, not a fix.

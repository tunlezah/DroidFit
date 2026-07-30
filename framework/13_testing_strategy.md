# 13 — Testing strategy

What to test, where, and why. The guiding principle: **the tests that matter most run in
milliseconds on the JVM**, which is why `domain` has no Android dependency.

---

## 1. What runs where

| Suite | Where | When | Speed |
|---|---|---|---|
| `domain` unit tests | JVM, no Android | Every push | ms |
| `data` unit tests (mappers, seeder parsing) | JVM | Every push | ms |
| ViewModel tests | JVM, `kotlinx-coroutines-test` | Every push | ms |
| Content validation | JVM | Every push | ms |
| detekt + ktlint rules | JVM | Every push | seconds |
| Room DAO / migration tests | `androidTest`, emulator | Default branch + manual | minutes |
| Compose UI tests | `androidTest`, emulator | Default branch + manual | minutes |
| Accessibility checks | `androidTest`, emulator | Default branch + manual | minutes |
| Manual device tests | Physical Edge 60 | Per phase, recorded in the phase log | — |

**Why the emulator suite is not on every push:** emulator jobs on shared runners are slow and
flaky. A flaky red build that blocks an unrelated PR trains people to ignore CI, which is
worse than the coverage gap. The gate is on the default branch, where a failure is
actionable. (`KI-0008`: the emulator job has never actually run — verify it on the first
manual dispatch.)

## 2. The engine (phase 06) — the highest-value tests in the project

Four property tests, one per invariant in `07_workout_engine_spec.md` §1, plus a golden file.

### Determinism
```
for (seed in 1..1000) {
    val a = generator.generate(request.copy(seed = seed))
    val b = generator.generate(request.copy(seed = seed))
    assertEquals(a, b)
}
```
**Plus a golden-file test**, which is the one that actually catches the bug. Comparing two
in-process calls passes even when the generator depends on `HashSet` iteration order, because
that order is stable within a JVM run. A checked-in expected output catches it.

### Duration fit
Every style × every preset × the custom boundaries (3, 4, 119, 120 min). Assert within ±30 s,
or an expected `DurationTooShort`.

### Structure
All three blocks present and non-empty; warm-up ≥ the style minimum; cool-down ≥ 2 min (3 for
HIIT).

### Eligibility
No segment outside the requested modalities, above the requested level, or carrying an avoided
tag. Include the degenerate case where every exercise carries an avoided tag — expect
`NoEligibleExercises`, not an empty workout.

### Also required
- Each `GenerationFailure` case, by construction.
- A Pilates-only request produces an honestly titled session (§7 of the engine spec).
- Intensity capping downgrades the title.
- The §9 worked example, as a golden file, byte for byte.

## 3. Coaching (phase 08)

The cue scheduler must be testable with a **virtual clock**. If a test needs `Thread.sleep` or
a real `delay`, the design is wrong — inject the time source.

Tests: each cue type fires at its specified moment; priority resolution; stale-cue discard;
the 1.5 s minimum gap; the rest-countdown/next-exercise combination; halfway firing on active
elapsed time across a paused session.

## 4. Persistence

**Room DAO tests** (androidTest): insert, query, aggregate. Particularly the SQL aggregates —
`observeActiveSecondsBetween` with sessions straddling a local midnight is exactly where a
timezone bug hides.

**Migration tests** (androidTest): for every migration, open schema `n` with
`MigrationTestHelper`, insert a representative row, migrate, assert the row survived with
correct values. `KI-0007`: build this harness **before** the first schema change.

**DataStore tests** (JVM): absent key → default; unrecognised enum id → default; out-of-range
number → coerced; concurrent `update` calls do not clobber each other.

`TD-0004`: DAO tests are emulator-only pending a Robolectric setup with a pre-warmed
dependency cache.

## 5. Content validation

The check in `08_exercise_library_spec.md` §5, as a JVM test. This is the enforcement
mechanism for content quality — every other content rule is guidance, this is the gate.

Notably it includes a **prohibited-claims regex** built from `02_evidence_base.md` §6, so a
"burns belly fat" that slips into a cue fails the build rather than shipping.

## 6. UI tests

Compose UI tests drive `XxxScreen` directly with constructed state — no Hilt, no ViewModel, no
database. That is the payoff of ADR-0010.

Per screen: each of the four states renders; each action invokes its callback; disabled
controls show their reason; rotation preserves state.

Add `AccessibilityChecks.enable()` to the instrumentation suite for automatic contrast and
target-size checking.

## 7. Coverage

**No coverage percentage target.** A number invites tests written to raise it, which are the
least useful tests. Instead, these must be covered — verified by inspection at phase exit:

- [ ] Every `domain` use case and the whole engine.
- [ ] Every entity ↔ domain mapper, including the unknown-enum-id path.
- [ ] Every ViewModel's state derivation.
- [ ] Every DAO query, especially aggregates and date boundaries.
- [ ] Every migration.
- [ ] Every screen's four states.
- [ ] Every `GenerationFailure` and `SpeechState.Unavailable` reason.
- [ ] Content validation.

If something in that list has no test, the phase is not done regardless of what a coverage
tool says.

## 8. Manual test script

Run on a physical Edge 60 at each phase exit and record the result in the phase log. These
cannot be automated and are where real bugs are found:

1. **TTS absent.** Disable text-to-speech in system settings. Run a full session. Expect the
   notice, tones and haptics. (A-0005 — the most likely real-world degradation.)
2. **Music playing.** Start music, run a session. Cues must duck briefly and music must resume
   immediately, never stay ducked.
3. **Backgrounded.** Start a session, switch apps for two minutes, return. The timer must be
   correct.
4. **Screen off.** Start a session, press power. Cues must continue; the timer must be correct
   on return.
5. **Rotation.** Rotate during work, during rest, and while paused.
6. **Phone call.** Take a call mid-session.
7. **Pause and forget.** Pause and leave for two minutes. The screen must release the
   keep-awake flag.
8. **Accidental back.** Edge-swipe during a session. Expect a prompt, not silent loss.
9. **200% font scale.** Navigate every screen.
10. **TalkBack.** Navigate every screen.
11. **Airplane mode.** Everything must work identically — this is the offline claim, tested.
12. **Fresh install.** Onboarding, safety notice, first session, first history entry.

## 9. Anti-patterns

| Do not | Because |
|---|---|
| `Thread.sleep` or real `delay` in a test | Slow and flaky. Inject the time source |
| Assert on a formatted string built by the code under test | Tests the formatter twice and nothing else |
| Mock the class under test | Tests the mock |
| Test a `data class`'s generated `equals` | Tests Kotlin |
| Robolectric for `domain` | `domain` has no Android dependency; if a test needs it, the code is in the wrong module |
| A test that only asserts "does not throw" | Passes when the behaviour is wrong |
| Compare two in-process calls to prove determinism | Misses ordering bugs that are stable within a run. Use a golden file |

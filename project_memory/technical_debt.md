# Technical debt

Deliberate shortcuts, each with the condition that should trigger repayment. Template:
`_templates/entry_templates.md`. Numbering: `TD-NNNN`.

This file is not a list of things that are wrong. It is a list of things that were
**chosen** to be suboptimal for a stated reason. Anything genuinely broken belongs in
`known_issues.md`.

---

### TD-0001 — `@OptIn(kotlin.time.ExperimentalTime)` in every module
- **Date:** 2026-07-30
- **Shortcut:** Every module's `kotlin.compilerOptions` adds
  `optIn.add("kotlin.time.ExperimentalTime")`.
- **Why it was acceptable now:** kotlinx-datetime 0.7.1 typealiases `Instant` to
  `kotlin.time.Instant`, which is still experimental on Kotlin 2.2.21. The alternatives
  were downgrading kotlinx-datetime to 0.6.x (adopting an API that is already on its way
  out) or storing epoch millis as raw `Long` in domain models (worse types everywhere to
  work around a compiler flag).
- **Cost of leaving it:** An experimental-API opt-in that a future Kotlin release could
  change under us, and 14 near-identical build-file stanzas.
- **Repay when:** the toolchain moves to Kotlin 2.3 or later, where
  `kotlin.time.Instant` is stable. Delete the `optIn` lines; nothing else should change.
- **Estimated effort:** 15 minutes.
- **Status:** open

### TD-0002 — Pinned to AGP 8.13.2 / Gradle 8.14.3 while AGP 9.3.x is current
- **Date:** 2026-07-30
- **Shortcut:** The build stays on the AGP 8 line even though AGP 9.3.1 is the current
  stable release, which in turn forces Hilt down to 2.58 (D-0002).
- **Why it was acceptable now:** AGP 9 is a major version with breaking DSL changes, and
  detekt 1.23.8 — the current stable, and the tool covering both required quality gates
  (ADR-0009) — is not validated against Gradle 9. Taking both jumps at once, in the same
  change as authoring the framework, would have meant debugging a toolchain instead of
  producing a specification.
- **Cost of leaving it:** Missing AGP 9 build-speed and R8 improvements; the Hilt version
  ceiling; growing distance from the ecosystem's default, which makes future
  Stack-Overflow-shaped answers less applicable.
- **Repay when:** detekt ships a release validated on Gradle 9, **or** the project
  accepts swapping detekt for a Gradle-9-compatible alternative. Do it as a dedicated
  commit: AGP → 9.3.x, Gradle wrapper → 9.6.x, Hilt → latest, then run the full
  `qualityCheck` and the emulator suite before anything else lands on top.
- **Estimated effort:** half a day, mostly reading AGP 9 migration notes.
- **Status:** open

### TD-0003 — No Gradle convention plugins; 14 hand-maintained build files
- **Date:** 2026-07-30
- **Shortcut:** Each module's `build.gradle.kts` repeats the same `android { }`,
  `compileOptions`, `kotlin { }` and source-set configuration.
- **Why it was acceptable now:** A `build-logic` included build with convention plugins
  is the right answer at this module count, but it adds a second Gradle build to debug
  during initial setup — precisely when the toolchain is least trusted. Getting a
  verified-building baseline first was worth more.
- **Cost of leaving it:** Changing the JVM target or a source-set convention means
  editing 14 files, and they will drift.
- **Repay when:** the first time a build-wide change has to be made by hand, or before
  adding a fourteenth module — whichever comes first. Extract
  `visceralfit.android.library`, `visceralfit.android.feature`,
  `visceralfit.android.compose` and `visceralfit.jvm.library` convention plugins into
  `build-logic/`.
- **Estimated effort:** 2–3 hours.
- **Status:** open

### TD-0004 — Room DAO tests are androidTest-only; no Robolectric
- **Date:** 2026-07-30
- **Shortcut:** `core:database` has no JVM tests for its DAOs. Room tests live in
  `androidTest`, which CI only runs on the default branch and on demand.
- **Why it was acceptable now:** Robolectric downloads an `android-all` jar on first use,
  which adds a network dependency and a flake source to the fast test job — in a project
  whose main quality claim is that everything works offline.
- **Cost of leaving it:** DAO regressions are caught by the slower emulator job rather
  than in seconds on every push.
- **Repay when:** phase 09 adds non-trivial aggregate queries (weekly load, streaks).
  Those are worth fast tests. Add Robolectric with a pre-warmed dependency cache in CI so
  the offline story stays true.
- **Estimated effort:** 2 hours including the CI cache.
- **Status:** open

### TD-0005 — Navigation uses string routes, not type-safe routes
- **Date:** 2026-07-30
- **Shortcut:** `TopLevelDestination` carries `route: String`; `NavHost` matches strings.
- **Why it was acceptable now:** The skeleton has four argument-free destinations. Type-safe
  routes pay off once arguments exist (a workout id, a session id, a date range), and the
  argument shapes are not settled until phases 06–09.
- **Cost of leaving it:** A typo'd route is a runtime crash, not a compile error.
- **Repay when:** phase 05, before any destination takes an argument. Convert to
  `@Serializable` route objects.
- **Estimated effort:** 1–2 hours.
- **Status:** open

### TD-0006 — History list is capped at 100 rows rather than paged
- **Date:** 2026-07-30
- **Shortcut:** `HistoryViewModel.RECENT_LIMIT = 100`.
- **Why it was acceptable now:** An explicit cap is honest and bounded. Unbounded loading
  would work fine for months and then quietly degrade.
- **Cost of leaving it:** A user with more than 100 sessions cannot scroll to older ones.
- **Repay when:** phase 09, when the calendar view needs arbitrary date navigation
  anyway. Use Paging 3 with the existing `observeBetween` query.
- **Estimated effort:** 3 hours.
- **Status:** open

### TD-0007 — `DefaultHistoryRepository.observeWeeklyLoad` returns a constant
- **Date:** 2026-07-30
- **Shortcut:** Returns `flowOf(emptyList())` so the skeleton compiles and the Progress
  screen renders its empty state.
- **Why it was acceptable now:** The alternative — throwing `NotImplementedError` — turns
  a known gap into a crash for anyone who opens the Progress tab.
- **Cost of leaving it:** The weekly-minutes card reads 0 regardless of activity, which
  is *wrong*, not merely incomplete. It must not ship in this state.
- **Repay when:** phase 09. This is a **release blocker**, not optional cleanup, and it
  is listed in `17_definition_of_done.md`.
- **Estimated effort:** included in phase 09.
- **Status:** **repaid in phase 09.** Implemented with a test whose first assertion is that the
  function is not a constant, because that is the shape the defect had — a passing build with a
  permanently wrong number on screen. The release-blocking part is closed; two of engine spec §8's
  four recovery signals still need a schema change (KI-0012, D-0037).

### TD-0008 — `write()` in `PreferencesDataSource` is one long function
- **Date:** 2026-07-30
- **Shortcut:** The read path was split into `toCoachingPreferences`, `toDisplayPreferences` and
  `toBodyPreferences` (D-0014) when detekt flagged its complexity. The corresponding **write** path
  is still one `write()` function assigning all ~30 keys.
- **Why it was acceptable now:** A sequence of unconditional assignments has a cyclomatic
  complexity of roughly 1 — it is long, not complex, so detekt does not flag it and a reader can
  scan it linearly. The read path was genuinely hard to check because each line carries a fallback
  branch.
- **Cost of leaving it:** The read and write paths are now asymmetric, which makes it slightly
  easier to add a key to one and forget the other.
- **Repay when:** the next time a settings section is added — split `write()` the same way at that
  point, so the two paths stay mirror images. Better still, add a round-trip test that writes a
  fully-populated `UserPreferences` and reads it back, which catches a forgotten key regardless of
  how the functions are shaped.
- **Estimated effort:** 30 minutes, including the round-trip test.
- **Status:** open

### TD-0009 — The engine's test catalogue is a generated copy of the shipped one
- **Date:** 2026-07-30
- **Phase:** 06
- **Shortcut:** `domain/src/test/.../CatalogueFixture.kt` restates every shipped exercise's id,
  modality, difficulty, MET value and caution tags as a pipe-delimited table, generated from
  `app/src/main/assets/exercises_seed.json`.
- **Why:** `:domain` is a pure Kotlin module with no Android dependency, and therefore no access to
  the app's assets. That boundary is deliberate and worth keeping — it is why the engine's ~1,700
  test requests run in under a second with no device and no Robolectric.
- **Cost of leaving it:** two representations of the same data. A drifted fixture would make the
  golden-file test a test of a catalogue nobody ships, and it would keep passing while doing it.
- **Mitigation already in place:** `scripts/check_framework_data.py` asserts the fixture and the
  asset agree row for row, in both directions, and CI runs it before the tests. So the drift fails
  the build rather than hiding.
- **Repay when:** the catalogue moves out of `app/` into a resource a JVM module can read — most
  likely when a second app module or a shared test-fixtures module appears. At that point the
  fixture becomes a parser over the real file and the generated table goes away.
- **Estimated effort:** 1 hour, most of it deciding where the asset should live.
- **Status:** open

### TD-0010 — The player's Route composables are not covered by any test
- **Date:** 2026-07-30
- **Phase:** 07
- **Shortcut:** `WorkoutPlayerRoute` and `WorkoutHomeRoute` wire the ViewModel to the service and to
  navigation, and neither is tested. The stateless `Screen` composables and the coordinator are.
- **Why:** Testing them needs either Robolectric or an emulator. The logic in them is deliberately
  thin — start the service, call one ViewModel method, navigate — precisely so that the untested
  surface is as small as it can be.
- **Cost of leaving it:** the seams most likely to be wrong are exactly here: is the coordinator
  loaded before the service starts, and does the service stop when the session ends? Both are
  ordering bugs that no unit test can see.
- **Repay when:** the emulator job is confirmed working (KI-0008, KI-0017). A single instrumentation
  test that starts a session from the Train screen and asserts the service is running covers most of
  it.
- **Estimated effort:** 2 hours once the emulator job is trusted.
- **Status:** open

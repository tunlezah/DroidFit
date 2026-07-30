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
  adding a twelfth module — whichever comes first. Extract
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
- **Status:** open — **blocks release**

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

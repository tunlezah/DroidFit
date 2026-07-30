# 05 — Architecture

The authoritative answer to "where does this code go?". See `project_memory/architecture_decisions.md`
for *why* each choice was made; this document is the working rulebook.

---

## 1. Module map

```
app                     Application, MainActivity, nav graph. Wiring only, no business logic.
├── domain              Pure Kotlin/JVM. Models, repository interfaces, use cases, workout engine.
├── data                Repository implementations, mappers, exercise seeder.
├── core:common         Dispatchers, qualifiers, formatting. No business rules.
├── core:designsystem   Theme, type scale, icons, illustrations, shared composables.
├── core:database       Room entities, DAOs, converters, DB, DI module.
├── core:datastore      Preferences persistence.
├── core:speech         Android TextToSpeech implementation of the SpeechCoach port.
├── core:testing        Test fixtures and fakes, shared across modules.
├── feature-workout     Train screen, workout player, coaching orchestration.
├── feature-history     Session history and calendar.
├── feature-progress    Weekly load, measurements, goals.
└── feature-settings    All user preferences.
```

## 2. Dependency rules

**Allowed:**

| From | May depend on |
|---|---|
| `app` | everything |
| `feature-*` | `domain`, `core:common`, `core:designsystem`, `core:speech` (workout only), `core:testing` (tests) |
| `data` | `domain`, `core:common`, `core:database`, `core:datastore` |
| `core:database`, `core:datastore`, `core:speech` | `domain`, `core:common` |
| `core:designsystem` | Compose only. **Not** `domain` |
| `domain` | kotlinx only. **Nothing Android** |

**Forbidden, and why:**

- `feature-* → data`. Features depend on repository *interfaces* in `domain`. Only `app`
  knows which implementation is bound. This is what lets a feature be tested with a fake
  repository and no Hilt graph.
- `feature-* → feature-*`. Cross-feature navigation goes through `app`'s nav graph. If two
  features need shared logic, it belongs in `domain` or a `core:*` module.
- `domain → android.*`. Enforced structurally: `domain` applies `kotlin-jvm`, not
  `android-library`, so an Android import is a compile error. Do not "fix" this by changing
  the plugin.
- `core:designsystem → domain`. The design system must not know about `Modality` or
  `WorkoutStyle`. Feature modules map domain types to display strings — which is why
  `Modality.displayName` lives in the settings feature, not in the enum.

## 3. Layer responsibilities

### domain
Models are `data class`es with `init` validation. Repository interfaces are the ports. Use
cases are single-purpose classes with an `operator fun invoke`. The workout engine lives
here.

Rules: no `Context`, no `Flow` collection (expose `Flow`, do not consume it), no
`Dispatchers.*` references, no clock reads inside the engine.

### data
Implements the ports. Maps entity ↔ domain. Owns the seeder.

Rules: mapping failures return `null` and the repository filters them, rather than throwing
or coercing to a default — a row from a newer app version must not be silently
re-interpreted. Every repository is `@Singleton`.

### feature-*
`XxxRoute` (public, resolves the ViewModel) + `XxxScreen` (internal, stateless). ViewModels
expose one `StateFlow<XxxUiState>` and take actions as methods.

Rules: no `hiltViewModel()` inside a `*Screen` (ADR-0010). UI state is a sealed interface or
a data class with defaults — never a bag of nullable fields. `stateIn` with
`SharingStarted.WhileSubscribed(5_000)`, so rotation does not tear down and rebuild the flow.

## 4. Concurrency

Dispatchers are injected via `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`.
Referencing `Dispatchers.IO` directly outside `core:common/Dispatchers.kt` is a
review-blocking defect — it makes the calling code untestable with a test scheduler.

`@ApplicationScope` is for work that must outlive a screen (the seeder). ViewModels use
`viewModelScope`. Nothing uses `GlobalScope`.

## 5. Offline guarantee

Three layers of enforcement, in increasing strength:

1. **Policy.** Documented here and in REQ-101.
2. **Manifest.** No `INTERNET` permission (D-0009). Any network call fails at runtime.
3. **Dependency audit.** No HTTP client, no image loader, no analytics SDK on the
   classpath. Verify with `./gradlew :app:dependencies` before every release.

Because there is no network permission, there is also no need for certificate pinning, no
API key to leak, and no privacy policy about data transmission. The strongest security
posture available is not having the capability.

## 6. Provider abstraction

`BodyMassProvider` (ADR-0006) is the template for every future integration:

```kotlin
interface BodyMassProvider {
    val providerId: String
    suspend fun isAvailable(): Boolean
    suspend fun latestBodyMassKg(): Double?
}
```

Bound `@IntoSet`. A resolver iterates them in the PRD's priority order — Health Connect,
future official vendor API, manual — and takes the first available. Call sites never name a
specific integration.

Apply the same shape to any future heart-rate source, workout exporter or cloud backup: an
interface in `domain`, implementations bound into a set, a resolver choosing by priority.

## 7. Error handling

| Situation | Handling |
|---|---|
| Generation cannot produce a plan | `Result.failure(GenerationFailure)`, surfaced with the reason and a fix |
| Malformed seed asset | Logged loudly; caught by a build-time validation test (phase 02) |
| Room row with an unknown enum id | Mapped to `null`, filtered, count logged |
| TTS unavailable | `SpeechState.Unavailable` with a specific reason; workout continues |
| Unknown `illustrationId` | Neutral placeholder figure |
| Preference value out of range | Coerced to the model default on read |

The pattern: **degrade visibly, never crash, never fabricate**. A user who can see that
something is missing can work around it. A user shown a plausible wrong value cannot.

## 8. Adding things

**A new screen:** decide whether it is a top-level destination (new `feature-*` module) or a
detail within one (new file in the existing module). Add the route in `app`, never in the
feature.

**A new exercise modality:** add to `Modality` with a `requiresEquipment` answer, add MET
values to `framework/data/met_values.json`, seed at least 12 exercises, add a `displayName`
branch in every feature that renders one (the compiler will find them — `when` over an enum
is exhaustive), record in `decisions.md`.

**A new setting:** add to the relevant `*Preferences` data class with a default, add a
`Keys` entry with a frozen key name, add read and write in `PreferencesDataSource`, add a
row in `SettingsScreen`. Never rename an existing key — it silently resets that setting for
every install.

**A new persisted record type:** entity + DAO in `core:database`, mapper in `data`,
repository interface in `domain`. Bump the DB version and write a tested migration.

## 9. Compliance checks

Run before every commit:

```bash
./gradlew qualityCheck          # detekt (incl. ktlint rules) + every module's unit tests
./scripts/compliance_check.sh   # the rules below, automated
```

`compliance_check.sh` enforces all of these. It runs in CI too, so a violation fails the
build rather than depending on a reviewer noticing:

- [ ] No `android.*` import in `domain/`.
- [ ] No `projects.data` in any `feature-*` build file.
- [ ] No `feature-*` dependency in another `feature-*` build file.
- [ ] No `projects.domain` in `core:designsystem`.
- [ ] No `Dispatchers.IO`/`Default`/`Main` outside `core:common`.
- [ ] No `GlobalScope`.
- [ ] No `hiltViewModel()` inside a `*Screen` composable.
- [ ] No `INTERNET` permission in the source manifest.
- [ ] No `fallbackToDestructiveMigration`.
- [ ] No hard-coded dependency version in a module build file.
- [ ] No literal `Color(0x…)` outside `core:designsystem`.

The script strips comments before matching, because the framework's own documentation
*about* these patterns would otherwise register as uses of them — the manifest comment
explaining why there is no `INTERNET` permission contains the string `INTERNET`.

Two things it deliberately does **not** cover, because they need a build first:

1. **The merged manifest.** A library can add a permission via manifest merging, so the
   source-manifest check is necessary but not sufficient. Before a release, check
   `app/build/intermediates/merged_manifest/release/AndroidManifest.xml` — see
   `framework/agents/security_privacy_agent.md` §Verification.
2. **The dependency graph.** `./gradlew :app:dependencies` is the check for an HTTP client
   or analytics SDK arriving transitively.

If you believe a rule should change, record an ADR **before** changing the script. Editing
the check to make a violation pass is the failure mode this exists to prevent.

# Coding conventions

Read once, then follow. These are the conventions the existing code already follows — the fastest
way to internalise them is to read `domain/model/`, `core/datastore/PreferencesDataSource.kt` and
`feature-settings/SettingsScreen.kt`.

## Kotlin

- Explicit visibility on anything public. Prefer `internal` for module-local declarations.
- `data class` for value types, with `init { require(...) }` validation where an invalid instance is
  a bug. `Segment` and `IntensityTarget` are the examples.
- Sealed interfaces for state and for closed result sets. Not nullable-field bags.
- Enums carry a stable string `id` and a `fromId` companion. **Never persist `name` or the ordinal.**
- No `!!`. If you know it is non-null, restructure so the compiler knows too.
- Extension functions for mapping; top-level, in a `mapper` package.
- `companion object` constants over magic numbers, named for what they mean.

## Naming

| Thing | Convention |
|---|---|
| Repository interface | `XxxRepository` in `domain` |
| Implementation | `DefaultXxxRepository` in `data` |
| Use case | Verb phrase: `EstimateEnergyExpenditure`, with `operator fun invoke` |
| ViewModel | `XxxViewModel`, one per screen |
| UI state | `XxxUiState` |
| Composable screen | `XxxRoute` (public) + `XxxScreen` (internal) |
| Test | `` `does the thing when the condition` `` in backticks |

## Comments

The rule: **explain why, never what.**

```kotlin
// Bad — restates the code
// Increment the counter
counter++

// Good — explains a decision the reader would otherwise question
// Dropped rather than queued: a stale "ten seconds left" spoken twenty seconds
// late actively misleads the user.
return
```

Every non-obvious decision gets a comment naming the reason and, where relevant, pointing at the
framework document or ADR. Every KDoc on a public declaration explains why it exists, not what its
signature already says.

If a comment would just restate the code, delete the comment. If the code needs a comment to be
understood at all, consider whether a better name would remove the need.

## Compose

- Stateless composables; hoist state.
- `Modifier` as the first optional parameter, defaulting to `Modifier`.
- No `hiltViewModel()` inside a `*Screen`.
- `collectAsStateWithLifecycle()`, never `collectAsState()`.
- Colours from `MaterialTheme.colorScheme` or `ZoneColours`. No literal `Color(…)` outside
  `core:designsystem`.
- Text sizes in `sp`, from the type scale.
- `@Preview` for every composable, in light, dark and AMOLED.
- Lambdas as parameters, not callbacks on an interface.

## Coroutines

- Inject dispatchers. `Dispatchers.IO` outside `core:common` is a defect.
- `viewModelScope` in ViewModels, `@ApplicationScope` for work outliving a screen. Never
  `GlobalScope`.
- Expose `Flow` from repositories; collect only in ViewModels and composables.
- `stateIn(scope, WhileSubscribed(5_000), initial)` for UI state — the timeout means rotation does
  not tear down and rebuild the flow.

## Tests

- `` `backtick names` `` describing the behaviour, not the method.
- Arrange / act / assert, visually separated.
- One behaviour per test.
- No sleeping, no real `delay`. Inject the time source.
- Fakes over mocks where a fake is cheap. `core:testing` holds shared fixtures.
- Determinism is proven with a golden file, never by comparing two in-process calls.

## Commits

```
phase NN: short imperative summary

What changed and why, 2-5 lines. Not a file list — the diff is the file list.

Project memory: decisions.md D-0021..D-0023, technical_debt.md TD-0004
```

(Those ids are illustrative — use the real ones you added.)

Every commit compiles and passes both `./gradlew qualityCheck` and
`./scripts/compliance_check.sh`. The memory line is not optional.

## Things that are always wrong

- A version literal in a module build file.
- `android.*` in `domain`.
- A `feature-*` depending on `data` or another feature.
- `fallbackToDestructiveMigration`.
- A fabricated number where the honest answer is `—`.
- A disabled control with no stated reason.
- A health claim not in `02_evidence_base.md` §7.
- Renaming a DataStore key or an exercise id that has shipped.
- Weakening a quality gate to get a green build.

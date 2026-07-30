# 06 — Data model

Room is the record store, DataStore is the settings store (ADR-0004). This document is
authoritative for both; `core/database/entity/Entities.kt` must match it.

---

## 1. Conventions, binding

| Rule | Reason |
|---|---|
| Enums stored as their stable string `id`, never the ordinal or `name` | Reordering an enum silently corrupts every stored row |
| Instants stored as epoch **milliseconds** (INTEGER), UTC | Unambiguous, sortable, timezone-independent |
| Local dates stored as **epoch days** (INTEGER) | "Today" must not shift when the user travels |
| Durations stored as **seconds** (INTEGER) | Millisecond precision is noise for a workout |
| String lists stored newline-delimited via `Converters` | Short, diffable in a DB inspector, never queried into |
| Column names `snake_case` via `@ColumnInfo` | Consistent SQL regardless of Kotlin naming |
| Nullable means "genuinely unknown" | A null energy estimate is information; a 0 is a lie |

## 2. Tables

### `exercises`
The bundled catalogue, seeded from JSON (ADR-0005). User data never lives here.

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | Stable slug. **Never renamed once shipped** — history and favourites reference it |
| `name`, `modality`, `difficulty` | TEXT | Enum ids for the latter two |
| `met_value` | REAL | From the Compendium; > 0 |
| `how_to`, `safety_notes`, `common_mistakes`, `muscles_worked`, `caution_tags`, `evidence_keys` | TEXT | Newline-delimited lists |
| `spoken_instruction` | TEXT | Single sentence |
| `illustration_id` | TEXT | Key into the drawing registry |
| `is_per_side` | INTEGER | Boolean |
| `seed_version` | INTEGER | Which catalogue version this row came from |

Indexes: `modality`, `difficulty` — both are filter columns in the generator's hot path.

### `sessions`
Completed workouts. **The one irreplaceable table.**

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | |
| `workout_id`, `title`, `style` | TEXT | |
| `modalities` | TEXT | Newline-delimited modality ids |
| `started_at_epoch_ms`, `completed_at_epoch_ms` | INTEGER | |
| `active_seconds` | INTEGER | Excludes paused time — work done, not wall-clock |
| `estimated_kcal` | INTEGER NULL | Null when body mass was unknown. **Never back-filled retroactively** |
| `perceived_exertion` | INTEGER NULL | Borg CR10, if given |
| `completion_ratio` | REAL | 0.0–1.0 |
| `note` | TEXT NULL | |

Indexes: `started_at_epoch_ms` (every history and aggregate query filters on it), `style`.

**Why `estimated_kcal` is never back-filled:** if the user enters their body mass in March,
sessions from January still have null. Retroactively computing them with today's mass would
invent history. The UI shows `—` for those, which is correct.

### `measurements`
Optional self-measurements.

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | |
| `kind` | TEXT | `MeasurementKind.id` |
| `recorded_on_epoch_day` | INTEGER | Local date |
| `value` | REAL | |
| `unit` | TEXT | Stored explicitly so a units-system change does not reinterpret old rows |

Unique index on `(kind, recorded_on_epoch_day)` — re-recording the same measurement on the
same day **corrects** it rather than adding a duplicate. This is why the DAO uses `@Upsert`.

### Tables to add in later phases

| Table | Phase | Purpose |
|---|---|---|
| `saved_workouts` + `saved_workout_segments` | 10 | Favourites, custom workouts, interval builder |
| `achievements` | 10 | Unlocked achievements with a timestamp |
| `progress_photos` | 10 | Filename in app-private storage + date. **Never a MediaStore uri** |
| `session_segments` | 09 | Per-segment completion, for "which interval did they quit on?" |

Each needs a version bump and a tested migration.

## 3. Migrations

**`fallbackToDestructiveMigration` is never enabled.** A user's training history cannot be
regenerated and there is no cloud copy by design.

Process for every schema change:

1. Bump `VisceralFitDatabase.version`.
2. Write an explicit `Migration(n, n+1)`.
3. Commit the new schema JSON from `core/database/schemas/` — it is the diffable record.
4. Write a migration test using `MigrationTestHelper` that opens schema `n`, inserts a
   representative row, migrates, and asserts the row survived with correct values.
5. Never drop a column carrying user data. To retire one, stop writing it and leave it.

**KI-0007:** the `MigrationTestHelper` harness does not exist yet. Build it **before** the
first schema change, not after.

Additive changes (a new nullable column, a new table) are simple. Anything else — renaming,
retyping, splitting a table — needs a `decisions.md` entry first.

## 4. DataStore preferences

Backing file: `user_preferences`. Key names in `PreferencesDataSource.Keys` are **frozen**.

**Renaming a key silently resets that setting for every existing install.** If a key must
change meaning, add a new key and migrate in code.

Every read is defensive: absent key → model default; unrecognised enum id → model default;
out-of-range number → coerced. A preferences file written by a future version must not brick
a downgrade, and an unknown modality id must not crash the workout screen.

Writes go through `update { }` taking a transform, not a whole object — two settings rows
toggled in quick succession must not clobber each other.

## 5. Backup and restore (phase 10)

`allowBackup="false"`: the app owns its backup story rather than relying on Android
auto-backup. This matters because progress photos must never leave the device implicitly.

Export format: a single JSON file the user chooses the location for, containing schema
version, sessions, measurements, preferences and saved workouts. **Progress photos are not
included** in v1 — see FF-0008.

Import rules: reject a file whose schema version is newer than the app's. Merge rather than
replace, keyed on `(workout_id, started_at_epoch_ms)` for sessions and
`(kind, recorded_on_epoch_day)` for measurements. Never silently overwrite existing history
— report what was added, what was skipped as duplicate, and what was rejected.

## 6. Query performance

The app's data is small — hundreds of sessions, dozens of exercises. Do not pre-optimise.

The two things that actually matter:

1. **Aggregates in SQL, not in Kotlin.** `observeActiveSecondsBetween` uses `SUM` in the
   query. Loading a year of rows to sum a column in Kotlin is the mistake to avoid.
2. **`Flow` returns from DAOs, not `suspend` + manual refresh.** Room invalidates
   automatically; polling is never needed.

`TD-0006`: the history list is capped at 100 rows pending Paging 3 in phase 09.

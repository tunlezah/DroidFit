# Phase 10 — Measurements, photos, goals, backup

## Objective
The optional self-tracking features, and the backup/restore that makes the app's data portable.

## Read first
- `framework/06_data_model.md` §2 (tables to add) and §5 (backup)
- `framework/01_product_requirements.md` §8
- `project_memory/assumptions.md` A-0008
- `framework/agents/security_privacy_agent.md`

## Agents
**Security & Privacy** (leads — this phase handles the most sensitive data in the app).
Fitness Science (measurement interpretation). UX Research.

## Files you may touch
- `core/database/**`, `data/**`, `domain/**`
- `feature-progress/**`, `feature-settings/**`
- `project_memory/*`

## Work

### 1. Measurements
Body mass, waist, hip, chest, thigh. Entry, editing, deletion, trend display.

- The `(kind, recorded_on_epoch_day)` unique index means re-recording the same measurement on the
  same day **corrects** it. Use `@Upsert`; do not create duplicates.
- Sub-1 cm changes report "roughly unchanged" (A-0008). Reporting measurement noise as progress is
  a form of fabrication.
- The waist card **must** carry the "not a measure of visceral fat" statement (REQ-003).
- Entering body mass makes energy estimates non-null **from that point forward only**. Do not
  back-fill historical sessions — computing January's sessions with March's mass invents history
  (`06_data_model.md` §2).

### 2. Progress photos — the most sensitive feature in the app
- App-private storage (`context.filesDir`), **never** MediaStore, never shared storage. A photo in
  shared storage is visible to every gallery app on the device.
- Excluded from backup. `allowBackup="false"` already covers this; do not weaken it.
- Optional biometric gate before viewing.
- Store a filename plus a date in the DB, never a content URI.
- Deleting a photo deletes the file, not just the row.
- **Not included in the export** in v1 (FF-0008).

### 3. Goals
Weekly minutes goal, editable, defaulting to 150 (the WHO lower bound, REQ-002). If the user sets a
goal above 300, note that WHO's range tops out there — informationally, not as a refusal.

### 4. Favourites, custom workouts, interval builder
`saved_workouts` + `saved_workout_segments`. A saved workout stores the `WorkoutRequest` **and its
seed**, so re-running a favourite reproduces it exactly — that is what the seed is for.

The interval builder must produce a `Workout` that satisfies the engine's structural invariants, or
be clearly marked as a custom session outside the programming rules. Do not let a hand-built
session masquerade as an evidence-based one.

### 5. Achievements
`achievements` table. Milestones only — first session, first week at goal, 10 sessions, a 4-week
streak. No arbitrary gamification, and nothing that pressures the user.

### 6. Backup and restore
`06_data_model.md` §5. A single JSON file, location chosen by the user.

Import rules that matter: reject a newer schema version; **merge rather than replace**, keyed on
`(workout_id, started_at_epoch_ms)` and `(kind, recorded_on_epoch_day)`; and report what was added,
what was skipped as duplicate, and what was rejected. Never silently overwrite history.

This is also the mitigation for the one-way door in `14_ci_cd_and_release.md` §4 — moving to a
release keystore later requires uninstalling, which deletes local data.

## Exit criteria
- [ ] Measurements: entry, edit, delete, trend; same-day re-entry corrects rather than duplicates.
- [ ] Sub-1 cm changes report "roughly unchanged".
- [ ] Waist card carries the required statement.
- [ ] Body mass does not back-fill historical energy estimates.
- [ ] Photos in app-private storage, excluded from backup, optional biometric gate; deletion
      removes the file.
- [ ] Goal editable; above-300 note shown.
- [ ] Favourites re-run reproduces the identical workout (seed preserved) — tested.
- [ ] Custom sessions are distinguishable from generated ones.
- [ ] Backup round-trips with no data loss — tested.
- [ ] Import rejects a newer schema, merges rather than replaces, and reports the outcome.
- [ ] Every new table has a migration and a migration test.
- [ ] **Security & Privacy verification run** (`security_privacy_agent.md` §Verification):
      merged manifest permissions and dependency graph both clean.
- [ ] `./gradlew qualityCheck` green.

## Project memory updates
- `decisions.md` — phase-log row; the backup format; the achievement list; the photo storage
  decision.
- `assumptions.md` — A-0008 status.
- `future_features.md` — FF-0008 (encrypted backup) still open; note what the v1 format leaves out.
- `known_issues.md` — anything deferred.

## Do not
- Write a photo anywhere outside app-private storage.
- Include photos in the export without solving the privacy question first.
- Back-fill historical energy estimates.
- Let an import silently overwrite existing history.
- Add gamification that pressures the user.

# Phase 14 — Release

## Objective
Produce a distributable build and leave the documentation true.

## Read first
- `framework/17_definition_of_done.md` — the whole project-level gate
- `framework/14_ci_cd_and_release.md` §6 (versioning)
- All eight `project_memory/` files

## Agents
**DevOps** (leads). **Documentation**. Security & Privacy (final verification).

## Files you may touch
- `app/build.gradle.kts` — version only
- `README.md`
- `framework/**`
- `project_memory/*`

## Work

### 1. Walk the Definition of Done
Every checkbox in `17_definition_of_done.md` §3, honestly. Anything unchecked is either fixed now
or reported as unmet — **not** quietly ticked.

Pay particular attention to §3's "Evidence and honesty" block. It is the gate that matters most and
the easiest to wave through.

### 2. Final security and privacy verification
From `security_privacy_agent.md`:
```bash
./gradlew :app:assembleRelease
grep -i "uses-permission" app/build/intermediates/merged_manifest/release/AndroidManifest.xml
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```
Check the **merged** manifest, not the source one — a library can add a permission via manifest
merging, and checking only the source misses it. Expect exactly four permissions: foreground
service, media-playback FGS type, post-notifications, vibrate. Expect no HTTP client, image loader
or analytics SDK anywhere in the graph.

### 3. Version
Set `versionName` and bump `versionCode`. Do not derive either from a commit count — a
`versionCode` that changes every push makes it impossible to tell which build is on the phone.

### 4. Tag and build
Tag the release. Confirm CI produces the APK, and **install it on a physical Edge 60 and run a
complete workout on it.** A CI-green artefact that has never been installed is not a verified
release.

### 5. Documentation truth pass
This is the Documentation agent's core job and the phase where drift gets caught.

Read every file in `framework/` against the code as built. Documents that were accurate when
written and are now confidently wrong are worse than no documents. Specific things that drift:
- `05_architecture.md` module map versus `settings.gradle.kts`.
- `06_data_model.md` tables versus the actual entities and schema JSON.
- `07_workout_engine_spec.md` constants versus the implementation.
- `14_ci_cd_and_release.md` §1 toolchain table versus the version catalogue.
- `02_evidence_base.md` §7 claim map versus the claims the app actually makes.

### 6. Root README
Written for a human who has just cloned the repo: what the app is, its honest limitations (it does
not measure visceral fat), how to build, how to install the APK, where the framework and project
memory are, and how to continue the build.

### 7. Final project memory pass
- Every phase has a log row in `decisions.md`.
- No `known_issues.md` blocker or major issue open.
- Every assumption has a final status.
- `research_summary.md` reflects everything learned, not just the initial research.
- `user_feedback.md` captures everything the operator said across the whole build, verbatim.

### 8. Report honestly
Per `17_definition_of_done.md` §5: what was built and verified with what evidence; what was not
built and why; what is assumed; what is known broken. Include the release blockers if any remain
open — a release with a known-open blocker is a decision the operator gets to make, not one to hide.

## Exit criteria
- [ ] Every Definition of Done checkbox either satisfied or explicitly reported unmet.
- [ ] Merged-manifest permission check clean; dependency graph clean.
- [ ] Version set and bumped deliberately.
- [ ] Release tagged; CI green; APK artifact produced.
- [ ] **The APK installed on a physical Edge 60 and a complete workout performed on it.**
- [ ] Every `framework/` document verified against the code.
- [ ] Root `README.md` written for a newcomer.
- [ ] All eight memory files current and complete.
- [ ] An honest completion report delivered to the operator.

## Project memory updates
- `decisions.md` — final phase-log row; the release version and what it contains.
- All other files — final consistency pass.

## Do not
- Tick a Definition of Done box that is not actually satisfied.
- Ship without installing and running the artefact on a real device.
- Describe the project as complete while a release blocker is open. Report it and let the operator
  decide.
- Leave a framework document contradicting the code.

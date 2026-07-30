# Phase 00 — Orientation

## Objective
Establish that the build works on your machine, that you have read the framework, and that the
open assumptions are surfaced to the operator before any of them get baked deeper in.

**Write no feature code in this phase.**

## Read first
Everything, in this order:

1. `framework/00_START_HERE.md`
2. `framework/01_product_requirements.md`
3. `framework/02_evidence_base.md`
4. `framework/05_architecture.md`
5. All eight files in `project_memory/`
6. `framework/17_definition_of_done.md`

Then skim `03`, `04`, `06`–`16`. You will come back to each when its phase arrives.

## Agents
Documentation agent (for the memory discipline). No others.

## Files you may touch
- `project_memory/*` — additions only
- `README.md` at the repository root, if anything in it is wrong

Nothing else.

## Work

### 1. Verify the toolchain
```bash
java -version                      # expect 21
./gradlew --version                # expect Gradle 8.14.3
```
You need an Android SDK with platform 36 and build-tools 36.0.0, and
`local.properties` containing `sdk.dir=/path/to/sdk`. That file is gitignored — create it, do
not commit it.

### 2. Verify the build
```bash
./gradlew qualityCheck
./gradlew :app:assembleRelease
ls -la app/build/outputs/apk/release/
```
Expect: detekt clean, tests passing, an APK around 2.4 MB.

If any of this fails, **stop and diagnose before doing anything else.** The baseline was
verified building; a failure here means an environment difference, and finding it now is far
cheaper than finding it in phase 06. Record what you found in `known_issues.md` and, if you
changed a version to fix it, in `decisions.md`.

### 3. Install and run
Sideload the APK onto a device or emulator. Confirm:
- The app launches.
- Four bottom-navigation destinations work.
- Settings toggles persist across an app restart. (This is the vertical slice that proves
  DataStore, Hilt, Compose and navigation are all correctly wired.)
- The Train screen shows a non-zero exercise count. If it shows 0, seeding failed — check
  logcat for `VisceralFit`.

### 4. Confirm CI
Push the branch. Confirm the `verify` job goes green and uploads an APK artifact. If the
repository has never run the workflow, this is the first real test of it.

### 5. Surface the open assumptions
This is the substantive output of the phase. Read `project_memory/assumptions.md` and put the
open ones to the operator as concrete questions. The ones that matter most:

- **A-0001** — exact Edge 60 model and Android version. (Settings → About phone.)
- **A-0002** — which equipment do you actually have access to?
- **A-0003** — is 3–120 minutes the right custom-duration range?
- **A-0004** — reformer MET value is approximated; is there a reformer to measure against?
- **A-0006** — English only, or is another language needed?
- **A-0007** — are you cleared for vigorous exercise (85–95% HRmax intervals)? This one is not
  optional to ask; it determines whether the default programme is appropriate.
- **A-0008** — how are you measuring waist circumference?

Ask them as a batch, not one at a time. Record the answers verbatim in `user_feedback.md` and
update each assumption's status.

### 6. Confirm the plan
Tell the operator what the phase sequence is and roughly what each will produce, and flag the
three release blockers (KI-0001, KI-0002, KI-0005) so there is no surprise later that the app
cannot run a workout yet.

## Exit criteria
- [ ] `./gradlew qualityCheck` green locally.
- [ ] `./gradlew :app:assembleRelease` produces an APK under 12 MB.
- [ ] The APK installs and runs; settings persist across restart; exercise count is non-zero.
- [ ] CI `verify` job green, artifact uploaded.
- [ ] The assumption questions have been put to the operator.
- [ ] Answers recorded verbatim; assumption statuses updated.

## Project memory updates
- `decisions.md` — add a phase-log row for phase 00. Add a `D-` entry for anything you had to
  change to get the build working.
- `assumptions.md` — update the status of every assumption the operator answered. Add any new
  assumption you had to make about the environment.
- `known_issues.md` — anything that did not work as documented, including a framework document
  that turned out to be wrong.
- `user_feedback.md` — a `UF-` entry with the operator's answers, quoted verbatim.

## Do not
- Write feature code.
- "Fix" a framework document by changing the code to match it. If they disagree, work out which
  is right and record the finding.
- Skip step 5 because it feels like admin. Those assumptions are the places this project is most
  likely to be quietly wrong, and A-0007 has a safety dimension.

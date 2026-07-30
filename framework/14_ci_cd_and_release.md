# 14 — CI/CD and release

Workflow: `.github/workflows/android-ci.yml`. Verified locally; the emulator job is unverified
(KI-0008).

---

## 1. The toolchain baseline

Verified building together. **This is a known-good set — change one thing at a time.**

| Component | Version | Note |
|---|---|---|
| JDK | 21 (Temurin) | |
| Gradle | 8.14.3 | Wrapper committed |
| AGP | 8.13.2 | Not 9.x — see TD-0002 |
| Kotlin | 2.2.21 | |
| KSP | 2.2.21-2.0.5 | Paired with the Kotlin version |
| Hilt | 2.58 | **Not 2.59+** — those require AGP 9 (D-0002) |
| Compose BOM | 2026.05.01 | Never pin individual Compose artifacts |
| Room | 2.8.4 | |
| detekt | 1.23.8 | Covers ktlint rules via `detekt-formatting` (ADR-0009) |
| compileSdk / targetSdk / minSdk | 36 / 36 / 29 | D-0008 |

Two incompatibilities in this set were only discoverable by building:

1. **Hilt 2.59+ refuses AGP 8** with an explicit error. Nothing in the release notes says so.
2. **kotlinx-datetime 0.7.1 needs `@OptIn(kotlin.time.ExperimentalTime)`** on Kotlin 2.2,
   because its `Instant` is now a typealias to the still-experimental `kotlin.time.Instant`
   (TD-0001).

If you bump versions and the build breaks, **revert to this table first**, then re-attempt one
change at a time. Record the result in `decisions.md` either way.

## 2. What the pipeline does

Job `verify` (every push and PR):

1. Check out, set up JDK 21, set up Gradle with caching.
2. `python3 scripts/check_framework_data.py` — the framework's own numbers agree with each other.
3. `./scripts/compliance_check.sh` — the architecture rules the compiler cannot enforce.
4. `./gradlew detekt --continue` — static analysis and formatting.
5. `./gradlew qualityCheck` — every module's unit tests, including `:domain:test`.
6. `./gradlew :app:assembleRelease` — the sideload artefact.
7. **Verify the APK.** Not just that a file exists: assert it contains `AndroidManifest.xml`
   and `classes.dex`, and that it is under the 12 MB ceiling.
8. Upload the APK (30-day retention) and test reports (14 days, always).

Job `instrumentation` (default branch and manual dispatch only): KVM, emulator at API 35
(matching the Edge 60's shipping OS), `connectedDebugAndroidTest`.

`--continue` on detekt is deliberate: report every violation in one run rather than making the
agent fix one, push, and wait to discover the next.

## 3. The artefact: release variant, debug-signed

ADR-0002 (signing) + ADR-0012 (variant). Measured:

| Variant | Size |
|---|---|
| `debug`, unminified | 32.58 MB |
| `release`, R8 + resource shrinking, debug-signed | **2.38 MB** |

So CI ships `assembleRelease`, whose `signingConfig` is the debug config. Consequences:

- No keystore, no secrets, no expiry. Installs on any device with "install unknown apps"
  enabled.
- R8 and the ProGuard rules run on every push, so a bad keep rule for Room, Hilt or
  kotlinx-serialization surfaces immediately rather than the first time a release is attempted.
- The artefact is **not** `debuggable`. For `adb` debugging, build `debug` locally.
- `applicationIdSuffix = ".debug"` applies only to the debug variant, so a locally built debug
  build installs alongside the distributed one.

### The APK size gate

The workflow fails above 12 MB. This is a real check, not ceremony: adding
`material-icons-extended` for four icons inflated the APK to **66 MB**, and the only reason it
was caught was measuring. The gate stops that class of regression silently landing.

Raising the ceiling requires a `decisions.md` entry stating what grew and why.

## 4. Installing the APK

1. Actions → the run → Artifacts → `visceralfit-sideload-apk`.
2. Unzip, transfer to the phone.
3. On the Edge 60: Settings → Apps → Special app access → Install unknown apps → enable for
   your file manager or browser.
4. Tap the APK, install.

Because it is debug-signed, the signature does not match any future release-signed build.
Moving to a real keystore later means uninstalling first — which deletes local data, since
`allowBackup="false"`. **Export a backup before that migration.** This is a genuine one-way
door and is why phase 10's backup/restore matters.

## 5. Switching to a release keystore

If distribution ever needs real signing:

1. Generate a keystore. Store the password somewhere durable — losing it means never updating
   an installed app again.
   ```bash
   keytool -genkeypair -v -keystore visceralfit.jks -keyalg RSA -keysize 4096 \
           -validity 10000 -alias visceralfit
   ```
2. `base64 -w0 visceralfit.jks` → GitHub secret `KEYSTORE_BASE64`. Add
   `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. In `app/build.gradle.kts`, add a `signingConfigs.create("release")` reading those from
   environment variables, and point the release build type at it **when they are present**,
   falling back to debug signing when they are not — so a fork without secrets still builds.
4. In the workflow, decode the secret to a file before `assembleRelease`.
5. Never commit the keystore. `.gitignore` already excludes `*.jks` and `*.keystore`.
6. Record it as an ADR superseding ADR-0002.

## 6. Versioning

`versionCode` and `versionName` are in `app/build.gradle.kts`. Currently 1 / `0.1.0`.

Convention: `versionName` is `major.minor.patch`; bump the minor at each completed phase group,
and always bump `versionCode` when the APK is distributed — Android refuses to install an
older `versionCode` over a newer one.

Phase 14 introduces release tagging. Do not automate version bumping from commit counts: a
`versionCode` that changes on every push makes it impossible to tell which build is on the
phone.

## 7. Local commands

```bash
./gradlew qualityCheck            # detekt + every module's unit tests. Run before every commit
./scripts/compliance_check.sh     # module boundaries, permissions, migrations, colours
python3 scripts/check_framework_data.py  # the spec's own numbers agree with each other
./gradlew :domain:test            # fast: the engine and use cases
./gradlew :app:assembleRelease    # the sideload APK
./gradlew :app:assembleDebug      # the debuggable build
./gradlew detekt --continue       # all violations in one pass
./gradlew :app:dependencies --configuration releaseRuntimeClasspath  # audit the graph
```

The local Android SDK path goes in `local.properties` (`sdk.dir=…`), which is gitignored. CI
uses the runner's pre-installed SDK, so no `local.properties` is needed there.

## 8. When CI is red

| Symptom | Likely cause |
|---|---|
| `Unresolved reference` in a module that compiled before | A version bump. Revert to the §1 table |
| Hilt plugin "only compatible with AGP 9.0.0 or higher" | Hilt bumped past 2.58. See D-0002 |
| `This declaration needs opt-in … ExperimentalTime` | A new module missing the `optIn` line (TD-0001) |
| detekt fails on a Compose function name | The rule needs `ignoreAnnotated: ['Composable']` in `config/detekt/detekt.yml` |
| detekt `MagicNumber` fires on expected values in tests | Specifying `excludes` for a rule **replaces** detekt's defaults rather than adding to them. `**/test/**` and `**/androidTest/**` must be restated. This bit once already |
| detekt `ImportOrdering` fires and the imports look fine | ktlint sorts by raw ASCII, so `PaddingValues` comes before `heightIn` (uppercase sorts first). Sort the block literally, not by eye |
| APK size gate fails | An asset or icon library crept in. Check `:app:dependencies` |
| OOM during KSP | `org.gradle.jvmargs` above 5g on a 7 GB runner. Keep it at 4g |
| Emulator job times out | Known-fragile. It is gated off PRs for this reason |

**Never fix a red build by weakening the gate.** Disabling a detekt rule, raising the size
ceiling or skipping a test to get green is how a project loses its quality signal. Fix the
cause, or record a `technical_debt.md` entry explaining the exception.

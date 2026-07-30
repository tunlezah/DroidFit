# 15 — Device target: Motorola Edge 60

The reference device. Assumption A-0001 — confirm the exact model before relying on anything
here.

---

## 1. Hardware

| Property | Value | Why it matters here |
|---|---|---|
| Display | 6.67" pOLED, 2712×1220, ~444 ppi | Vector artwork must be resolution-independent (ADR-0003). Raster demos would look soft |
| Refresh rate | 120 Hz | The timer must not drop frames; a stuttering countdown is very visible |
| Peak brightness | up to 4,500 nits, HDR10+ | Readable outdoors; no need for a high-contrast "outdoor mode" |
| Panel type | pOLED, **curved edges** | True-black AMOLED mode saves real power. Curved edges mean content must never reach the horizontal extremes |
| Chipset | MediaTek Dimensity 7300 | Mid-range. Do not assume flagship headroom — see §4 |
| Battery | 5,200 mAh, 68 W wired | A 45-minute screen-on session is well within budget, but screen-on is the dominant cost |
| Storage | up to 512 GB UFS 4.0, microSD | Ample. The APK is ~2.4 MB anyway |
| Durability | IP68/IP69, MIL-STD-810H | Sweat and gym-floor drops are non-issues |
| OS | Android 15 (API 35) | The behaviour baseline |

## 2. SDK levels

`minSdk 29`, `compileSdk 36`, `targetSdk 36` (D-0008).

Targeting 36 rather than 35 keeps the app forward-compatible, and the API 35+ behaviour
changes apply either way. minSdk 29 avoids compatibility branches for Android 8-era devices
the operator does not use.

## 3. Android 15 behaviours that change the code

### Edge-to-edge is enforced
Apps targeting SDK 35+ get edge-to-edge whether they ask for it or not. `enableEdgeToEdge()`
is called in `MainActivity`, and every screen must consume insets properly. On a curved panel
this is not cosmetic: text in the last few dp of horizontal space is physically distorted.

Use `WindowInsets.safeDrawing`. Never hard-code a status-bar or navigation-bar height.

### Foreground service types
Only some types may run indefinitely. The workout service is typed **`mediaPlayback`**
(ADR-0008) because:
- Its ongoing output *is* audio — spoken coaching cues. The type is accurate, not a loophole.
- `mediaPlayback` has no 6-hour cap, which a 90-minute session with pauses could approach.
- The alternative, `health`, requires a sensor permission (`BODY_SENSORS`,
  `ACTIVITY_RECOGNITION` or `HIGH_SAMPLING_RATE_SENSORS`) that this app has no use for.

Requires `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and a persistent notification. Note: an app
targeting Android 15+ may **not** start a `mediaPlayback` foreground service from a
`BOOT_COMPLETED` receiver — irrelevant here, but do not add one.

### Keeping the screen on
`FLAG_KEEP_SCREEN_ON` on the activity window. Not a wake lock:
`SCREEN_BRIGHT_WAKE_LOCK` is deprecated, and partial wake locks count against the Android
vitals excessive-wake-lock metric.

`core/designsystem/modifier/KeepScreenOn.kt` implements this. It takes an `enabled` parameter
so toggling the setting mid-session takes effect immediately, and it degrades silently
outside an Activity (previews, screenshot tests) rather than throwing. Compose 1.9's
`Modifier.keepScreenOn()` does the same job; the helper exists for those two behaviours.

### Predictive back
Supported by default with Navigation Compose. Do not intercept back without handling the
predictive gesture. During a workout, back must prompt before discarding the session — an
accidental swipe must not silently end a 45-minute effort.

### Notification permission
`POST_NOTIFICATIONS` is runtime-requested on API 33+. Request it when the user starts their
first workout, with an explanation — not at launch, and not as a blocking gate. If refused,
the workout still runs; only the persistent notification is absent, which means
backgrounding the app will eventually stop the session. Say that plainly rather than
silently degrading.

## 4. Performance targets

The Dimensity 7300 is mid-range. Targets, verified in phase 12:

| Metric | Target | How to measure |
|---|---|---|
| Cold start to first frame | < 800 ms | `adb shell am start -W` |
| Frame timing during a session | zero frames > 16 ms over a 60 s window | Macrobenchmark / `dumpsys gfxinfo` |
| APK size | < 12 MB (currently 2.38 MB) | CI size gate |
| Battery, 45-min session, screen on, AMOLED dark | < 12% of a 5,200 mAh battery | Battery Historian |

Screen-on time dominates battery. This is the entire justification for AMOLED mode being the
default rather than a curiosity.

## 5. Motorola-specific features: deliberately not used

Considered and rejected, recorded so a later phase does not revisit them without reason:

| Feature | Why not |
|---|---|
| Moto Actions (chop for torch, twist for camera) | No public API. Gesture control of a workout would be nice, and is not available |
| Moto Secure / Smart Connect desktop mode | Adds a whole second layout for a use case nobody has for a floor-Pilates app |
| Moto AI | Motorola-proprietary, no public SDK, and would break the no-network guarantee |
| Edge lighting on the curved panel | No public API |
| IP69 poolside use | Real, but there is no swimming content and no reason to design for it |

**What "slanted towards the Edge 60" actually means in this codebase:** AMOLED true-black
mode tuned for its pOLED panel; 120 Hz-aware motion; curved-edge inset handling; vector-only
artwork for its 444 ppi density; Android 15 foreground-service and edge-to-edge compliance;
performance targets set for a Dimensity 7300 rather than a flagship. That is device
optimisation. Proprietary API integration is not available to a third-party app.

## 6. Testing on other devices

The app should work on any API 29+ device. Specific things that will differ:

- **No dynamic colour below API 31** — the theme already branches on this.
- **Flat-panel devices** get slightly more usable horizontal space; the inset handling
  degrades gracefully.
- **LCD devices** get no battery benefit from AMOLED mode, but no penalty either.
- **Devices with no TTS voice data** hit the `SpeechState.Unavailable` path (A-0005). Test
  this deliberately by disabling TTS in system settings — it is the most likely real-world
  degradation and the easiest to forget.

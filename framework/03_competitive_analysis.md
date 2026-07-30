# 03 — Competitive analysis

Market observation, surveyed July 2026 across app-store listings and review aggregators for
interval-timer, Pilates and general fitness applications.

**Scope limit, read this first:** this document informs **UX only**. Feature popularity is
not evidence of physiological efficacy. Nothing here may be used to justify a programming
decision — those come from `02_evidence_base.md` and nowhere else. If a competitor does
something the evidence does not support, we do not copy it.

Synthesis recorded as `project_memory/research_summary.md` R-0007.

---

## 1. Products examined

| Product | Category | Why it was examined |
|---|---|---|
| Seconds Interval Timer | Interval timer | The reference implementation of interval timing: 4M+ downloads, 8,000+ 5-star reviews |
| Tabata Stopwatch Pro | Interval timer | Feature-dense and largely free |
| Interval Timer (Android) | Interval timer | Minimalist design praised for clarity |
| Chrono List | Circuit / HIIT timer | Wear OS support |
| Pilates Anytime | Pilates library | Largest catalogue, 3,700+ classes, offline download |
| ALIGN by Bailey Brown | Pilates programmes | Structured multi-week programmes |
| FitOn | Multi-discipline | Generous free tier; offline is paid |
| Hoola | Low-impact / Pilates | Multi-dimensional progress attributes |
| WallPilates | Wall Pilates | Structured 28-day plan, high satisfaction score |

---

## 2. Features consistently praised

Ranked by how often they appeared as an explicit positive.

### 2.1 Spoken interval names, announced ahead of time
The single most-cited feature in interval-timer reviews. Users specifically value hearing
*what is coming* rather than only a beep marking that something changed — it means they do
not have to look at the phone mid-effort.

**Adopted.** `CoachingPreferences.announceNextExercise`, **default on**. REQ-051.

### 2.2 Saved presets
Repeatedly described as the difference between using an app and abandoning it: users resent
reconfiguring before every session.

**Adopted, partially.** REQ-023 (SHOULD), phase 10. The skeleton already persists the
default duration and style so the Train screen opens pre-configured, which covers most of
the value.

### 2.3 A large timer, readable from a distance
Praised specifically in the context of being on a machine or across a room. One review
described the timer as "easy to see even from a long distance" as a headline benefit.

**Adopted, and pushed further.** `TimerTypography.Countdown` at 96 sp, and
`CountdownLarge` at 148 sp for machine mode — designed for a phone in a bike cradle 60–80 cm
away. REQ-021, REQ-075.

### 2.4 Colour-coded work and rest states
Users rely on peripheral colour to know their state without reading.

**Adopted, with an accessibility constraint.** `ZoneColours` gives each intensity a
semantic colour used identically on every surface — **and** REQ-022 requires a non-colour
signal alongside it, because roughly 1 in 12 men has a colour vision deficiency. Colour is
never the only channel.

### 2.5 Offline access
Praised where present, and one of the loudest complaints where it is paywalled.

**Adopted maximally.** The app has no network capability at all (D-0009). This is the
clearest place where VisceralFit is structurally better than the market rather than
comparably good: offline is not a premium tier, it is the only mode.

### 2.6 Structured multi-week programmes
The most-praised feature in the Pilates and class-library segment. Users want a plan, not a
menu of sessions.

**Deferred, deliberately.** `future_features.md` FF-0006. The generator must be trustworthy
for one session before it is trusted with twelve weeks. Note that the dose evidence
(R-0002: 3×/week for 12–16 weeks) describes exactly a programme shape, so this is the
highest-value v2 feature.

### 2.7 Progress broken out by dimension
Praised over a single composite score — users found "strength, flexibility, cardio,
mindfulness" more meaningful than one number.

**Partially adopted.** The app tracks weekly minutes, vigorous minutes, streaks and
measurement trends separately, and deliberately computes no composite "fitness score" —
a composite would be a fabricated number, which REQ-081 and D-0005 prohibit in spirit.

### 2.8 Wear OS support
Praised where present, mostly for wrist haptics rather than for the watch UI.

**Deferred.** FF-0002. The haptic value is partly captured by
`CoachingPreferences.hapticCues` on the phone.

---

## 3. Complaint themes

These are what users are angry about. Three of the four are structurally impossible in this
app, which is the strongest competitive position available.

### 3.1 Subscriptions — the dominant complaint
Charges continuing after uninstall; cancellation flows that freeze or fail; support that
does not respond; users describing products as scams over billing rather than over quality.

**Structurally eliminated.** No subscription, no in-app purchase, no billing code, no
payment SDK. REQ-020.

### 3.2 Forced accounts and login failures
Login codes that never arrive; no way to manage billing; being locked out of content
already paid for.

**Structurally eliminated.** No account, no login, no server. REQ-020.

### 3.3 Content behind a paywall, including offline access
Users particularly resent offline download being the premium upsell.

**Structurally eliminated.** All content ships in the APK.

### 3.4 Dead controls with no explanation
A recurring low-grade frustration: a button that does nothing, a toggle with no visible
effect, a feature that silently requires something the user does not have.

**Adopted as a hard rule.** REQ-024: no disabled control without a visible reason. The
skeleton already demonstrates this — the Start button reads "Start — workout engine arrives
in phase 06", and disabling the last enabled modality is refused with an explanation rather
than silently ignored.

### 3.5 Timers that die when the screen sleeps or the app backgrounds
Common in the interval-timer category.

**Addressed architecturally.** Keep-screen-on (REQ-070) plus timer state in a foreground
service (ADR-0008), so backgrounding, rotation and screen-off cannot desynchronise the
session.

---

## 4. Where we deliberately differ from the market

| Market norm | Our choice | Why |
|---|---|---|
| Video demonstrations | Drawn diagrams + written + spoken cues | ADR-0003: licensing, APK size, and the offline guarantee |
| A composite "fitness score" | Separate honest metrics | A composite is a fabricated number |
| Estimated body fat percentage | Nothing | REQ-090: no means of measuring it |
| Streak pressure and loss-aversion mechanics | Streaks shown, never used to nag | Recovery is part of training; guilt-driving a user into a session they should skip is harmful, not sticky |
| Aggressive push notifications | None beyond the in-workout foreground notification | No account, no engagement metric to optimise |
| "Fat-burning zone" framing | Named zones with stated purposes | The concept is misleading (`02_evidence_base.md` §6) |
| Onboarding quiz producing a "personalised plan" | Four explicit settings | A quiz that produces a plan from six answers implies more personalisation than exists |

---

## 5. What we are *not* competing on

Recorded so a later phase does not drift into it:

- **Content volume.** Pilates Anytime has 3,700+ classes. We will have ~60 exercises,
  combinatorially arranged. Competing on catalogue size is unwinnable and unnecessary.
- **Production values.** No studio video, no music licensing, no instructor personalities.
- **Community and social features.** No leaderboards, no sharing, no friends. These require
  a server, which requires the network permission we have refused.
- **Nutrition.** Out of scope entirely. It is a different product with different evidence
  requirements.

---

## 6. How to extend this document

If a later phase examines more products:

1. Add them to §1 with a reason for examining them.
2. Add newly observed praised features to §2 — with the **adoption decision** stated, not
   just the observation. An observation with no decision is noise.
3. Add complaints to §3 with how we avoid them.
4. Record the synthesis as a new `research_summary.md` entry, and never let a market
   observation cross into `02_evidence_base.md`.

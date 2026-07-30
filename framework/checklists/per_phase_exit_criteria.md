# Universal per-phase exit criteria

Run these on **every** phase, in addition to that phase's own criteria. Copy the block into your
phase-completion note and fill it in honestly.

```markdown
## Phase NN exit check

### Build
- [ ] `./gradlew qualityCheck` green
- [ ] `./scripts/compliance_check.sh` exits 0
- [ ] `./gradlew :app:assembleRelease` succeeds, APK under 12 MB (actual: ____ MB)
- [ ] CI green on the pushed branch

### Code
- [ ] Every new public declaration has KDoc explaining *why*
- [ ] No `android.*` import in `domain/`
- [ ] No `feature-*` depends on `data` or on another feature
- [ ] No version literal outside `gradle/libs.versions.toml`
- [ ] No `Dispatchers.*` outside `core:common`
- [ ] No `hiltViewModel()` inside a `*Screen` composable
- [ ] No `!!`
- [ ] No fabricated number where the honest answer is `—`

### UI changes only
- [ ] Every new composable previews in light, dark and AMOLED
- [ ] Accessibility checklist run (`framework/12_accessibility_spec.md` §7)
- [ ] Works at 200% font scale
- [ ] No disabled control without a visible reason
- [ ] All four screen states handled (loading / empty / content / error)

### Content or claims changed
- [ ] `python3 scripts/check_framework_data.py` exits 0
- [ ] Every claim traces to `framework/02_evidence_base.md` §7
- [ ] Nothing on the prohibited-claims list (§6)
- [ ] Content validation test green

### Persistence changed
- [ ] Schema version bumped
- [ ] Migration written **and** migration test written
- [ ] Schema JSON committed
- [ ] No `fallbackToDestructiveMigration`
- [ ] No DataStore key renamed

### Tests
- [ ] Every new behaviour has a test
- [ ] No test sleeps or uses a real `delay`
- [ ] No test asserts only "does not throw"
- [ ] Determinism claims rest on a golden file, not two in-process calls

### Project memory — mandatory
- [ ] Phase-log row added in `decisions.md`
- [ ] `decisions.md` — every non-obvious choice, with alternatives and reversal condition
- [ ] `architecture_decisions.md` — any structural decision, as a numbered ADR
- [ ] `assumptions.md` — anything assumed; anything this phase *settled* updated to closed
- [ ] `known_issues.md` — anything left broken; anything fixed marked fixed
- [ ] `technical_debt.md` — any deliberate shortcut, with its repayment trigger
- [ ] `research_summary.md` — anything researched, with citations
- [ ] `future_features.md` — anything deliberately deferred
- [ ] `user_feedback.md` — anything the operator said, verbatim
- [ ] Commit message names the entry ids added

### Honesty
- [ ] Every manual test reported as run was actually run
- [ ] Nothing described as complete that merely compiles
- [ ] Any framework document contradicted by this phase's code has been corrected
```

## The three that get skipped

Experience says these are the ones that quietly get dropped. If you are short on time, do these
and defer something else:

1. **The memory updates.** A phase whose code works and whose memory is missing is not complete
   (REQ-122), and the "why" is already forgotten by the next session.
2. **Settling assumptions the phase resolved.** Leaving one open when you had the answer means the
   next agent re-investigates it.
3. **Correcting a framework document the code has outgrown.** A document that was true when
   written and is now confidently wrong is worse than no document.

# Entry templates

Copy the relevant block, fill it in, append it to the bottom of the file's log. Keep
the headings exactly as written — the phase exit checks grep for them.

---

## decisions.md

```markdown
### D-NNNN — <short title>
- **Date:** YYYY-MM-DD
- **Phase:** <phase number and name>
- **Decision:** <what was chosen, in one sentence>
- **Alternatives considered:** <the other real options, and why each lost>
- **Reason:** <why this one won>
- **Reverses if:** <what would have to become true for this to be wrong>
- **Affects:** <files, modules or requirements>
```

---

## architecture_decisions.md

```markdown
### ADR-NNNN — <short title>
- **Date:** YYYY-MM-DD
- **Status:** Accepted | Superseded by ADR-NNNN | Deprecated
- **Context:** <the forces at play; what problem demanded a decision>
- **Decision:** <the structural choice>
- **Consequences:** <what becomes easy, what becomes hard>
- **Compliance:** <how a reviewer can tell the code still follows this>
```

---

## assumptions.md

```markdown
### A-NNNN — <short title>
- **Date:** YYYY-MM-DD
- **Assumption:** <what is being taken as true>
- **Why we had to assume:** <what information was missing>
- **Confidence:** high | medium | low
- **How to confirm:** <the specific action that would settle it>
- **If wrong:** <the blast radius>
- **Status:** open | confirmed YYYY-MM-DD | refuted YYYY-MM-DD
```

---

## known_issues.md

```markdown
### KI-NNNN — <short title>
- **Date:** YYYY-MM-DD
- **Severity:** blocker | major | minor | cosmetic
- **Area:** <module or screen>
- **Symptom:** <what a user or developer observes>
- **Reproduction:** <numbered steps, or the command that shows it>
- **Cause:** <if known; "unknown" is acceptable>
- **Workaround:** <if any>
- **Status:** open | fixed in <commit> | won't fix (<reason>)
```

---

## technical_debt.md

```markdown
### TD-NNNN — <short title>
- **Date:** YYYY-MM-DD
- **Shortcut:** <what was done instead of the right thing>
- **Why it was acceptable now:** <the trade-off>
- **Cost of leaving it:** <what it makes slower, riskier or worse>
- **Repay when:** <the trigger condition>
- **Estimated effort:** <rough size>
- **Status:** open | repaid in <commit>
```

---

## research_summary.md

```markdown
### R-NNNN — <question researched>
- **Date:** YYYY-MM-DD
- **Question:** <what was being established>
- **Finding:** <the answer, stated precisely>
- **Strength of evidence:** systematic review / meta-analysis / guideline / RCT / expert opinion / market observation
- **Sources:** <citation keys from framework/data/references.md, or URLs for non-scientific sources>
- **How it changed the product:** <the concrete consequence, or "none — recorded for context">
- **Caveats:** <limits of the finding>
```

---

## future_features.md

```markdown
### FF-NNNN — <feature name>
- **Date:** YYYY-MM-DD
- **What:** <the feature, in user terms>
- **Why not now:** <the reason it is out of scope>
- **Prerequisites:** <what must exist first>
- **Architectural hooks already in place:** <what a future implementer can build on>
- **Rough size:** <estimate>
```

---

## user_feedback.md

```markdown
### UF-NNNN — <topic>
- **Date:** YYYY-MM-DD
- **Source:** <who said it, in what channel>
- **Verbatim:** > <exact quote; do not paraphrase>
- **Interpretation:** <what it was taken to mean>
- **Action taken:** <what changed as a result, with entry ids or commits>
- **Still open:** <anything unresolved>
```

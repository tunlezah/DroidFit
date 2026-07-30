# Project memory

Durable state for the VisceralFit build. This directory is the project's institutional
memory: it exists so that an agent starting with no context can reconstruct *why* the
codebase looks the way it does without re-deriving anything or re-litigating settled
questions.

## The eight files

| File | Holds | Answers the question |
|---|---|---|
| `decisions.md` | Choices with more than one defensible answer | "Why is it done this way?" |
| `architecture_decisions.md` | Numbered ADRs for structural choices | "Why is the code shaped like this?" |
| `assumptions.md` | Things taken as true without confirmation | "What might we be wrong about?" |
| `known_issues.md` | Bugs and gaps not yet fixed | "What is broken right now?" |
| `technical_debt.md` | Deliberate shortcuts | "What did we defer, and when must we pay it back?" |
| `research_summary.md` | Findings with citations | "What do we actually know?" |
| `future_features.md` | Deliberately out of scope | "What did we decide not to build yet?" |
| `user_feedback.md` | What the human operator said | "What was actually asked for?" |

## Rules

1. **Append-only.** Entries are numbered and dated. To change a past entry, add a new
   one that supersedes it and mark the old one `SUPERSEDED BY <id>`. Never delete or
   silently rewrite history — a decision that was later reversed is more informative
   than one that appears never to have been made.
2. **Every entry gives a reason.** The "why" is the entire value. An entry that only
   states what was done is noise.
3. **Every entry states its reversal condition** where one exists: what would have to
   become true for this to be the wrong call.
4. **Updated in the same commit as the work.** Not afterwards, not in a batch. The
   commit message names the entry ids it added.
5. **Short entries, many of them.** Ten precise entries beat one essay.

## Numbering

| Prefix | File |
|---|---|
| `D-NNNN` | `decisions.md` |
| `ADR-NNNN` | `architecture_decisions.md` |
| `A-NNNN` | `assumptions.md` |
| `KI-NNNN` | `known_issues.md` |
| `TD-NNNN` | `technical_debt.md` |
| `R-NNNN` | `research_summary.md` |
| `FF-NNNN` | `future_features.md` |
| `UF-NNNN` | `user_feedback.md` |

Ids are never reused, even after an entry is closed or superseded.

Templates: `_templates/entry_templates.md`.

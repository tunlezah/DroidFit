# Project memory protocol

The full rules for `/project_memory`. `framework/00_START_HERE.md` §4 is the summary; this is the
detail.

## Why this exists

You will lose your context window. Someone — possibly you, possibly a different agent, possibly
the operator in six months — will need to understand why the code looks the way it does without
re-deriving it.

The value is not the record of *what* happened. It is the record of *why*, and of what would make
the decision wrong. A file full of "changed X to Y" entries is worthless. A file that says "chose Y
over X because Z, and would revert if W" is what makes a project maintainable by someone who was
not there.

## Every phase

**At the start:** read all eight files. They are short by design. In particular read
`assumptions.md` — a phase often gives you the information to settle an open assumption, and
leaving it open when you could have closed it is a wasted opportunity.

**At the end, before committing:** update every file that has something to record, using the
templates in `_templates/entry_templates.md`. Then add the phase-log row in `decisions.md`.

## Which file gets what

The distinctions that get confused:

| Confusion | Resolution |
|---|---|
| `decisions.md` vs `architecture_decisions.md` | ADRs are for *structure* — where code lives, what depends on what. Decisions are everything else with more than one defensible answer |
| `known_issues.md` vs `technical_debt.md` | Ask "did we choose this?" Yes → debt. No → issue |
| `assumptions.md` vs `known_issues.md` | An assumption is something you believe and have not verified. An issue is something you know is wrong |
| `research_summary.md` vs `02_evidence_base.md` | The evidence base is the working reference the engine depends on. The summary is the decision-relevant record of what research established and what changed as a result |
| `future_features.md` vs `technical_debt.md` | Debt is a shortcut in something that exists. A future feature is something that does not exist yet |

## Hard rules

1. **Append-only.** Never edit or delete a past entry. To change one, add a new entry and mark the
   old one `SUPERSEDED BY <id>`. A decision that was later reversed is *more* informative than one
   that appears never to have been made — it tells the next reader that the alternative was tried.
2. **Ids are never reused**, even after an entry is closed.
3. **Every entry answers why.**
4. **Every entry states its reversal condition** where one exists.
5. **Same commit as the work.** Not afterwards, not batched. The commit message names the ids.
6. **Empty is valid; silence is not.** A phase with no new assumptions gets an explicit "no new
   assumptions" in its log row.
7. **Verbatim quotes in `user_feedback.md`.** Paraphrase drifts, and six phases later the
   paraphrase is what gets built. Quote first, interpret separately, so a later reader can check
   the interpretation against the source.

## Writing a good entry

Compare these two versions of the same entry. (`D-0031` here is an invented illustration, not a
real entry — do not go looking for it.)

> ### D-0031 — Use Paging 3
> - **Decision:** Added Paging 3 to the history list.

Useless. It says what the diff already says.

> ### D-0031 — Paging 3 for history, replacing the 100-row cap
> - **Decision:** History uses Paging 3 with a page size of 30, backed by the existing
>   `observeBetween` query.
> - **Alternatives considered:** *Raise the cap to 1,000.* Rejected — it defers the problem
>   without solving it and holds 1,000 rows in memory to render eight. *Manual "load more"
>   button.* Rejected — the calendar view needs arbitrary date navigation anyway, which paging
>   gives for free.
> - **Reason:** The calendar view in this phase needs to jump to any date. Paging is the only
>   option that serves both the list and the calendar.
> - **Reverses if:** the calendar view is removed, at which point a simple cap would be simpler.
> - **Affects:** `feature-history`, `SessionDao`.

That entry lets a future reader decide whether the decision still holds, without re-deriving
anything.

## Anti-patterns

| Do not | Because |
|---|---|
| Batch memory updates at the end of several phases | Attribution is lost, and the "why" is already forgotten |
| Write an entry that only states what changed | The diff already says what changed |
| Edit a past entry to make it look right | Destroys the record of what was actually believed |
| Leave an assumption open that a phase settled | The next agent re-investigates it |
| Close a `known_issues.md` entry with no fix and no stated acceptance | Looks resolved, is not |
| Paraphrase the operator | Drift. Quote |
| Record a decision without its alternatives | The alternatives are the useful part |
| Claim a phase complete without its memory entries | REQ-122 — it is not complete |

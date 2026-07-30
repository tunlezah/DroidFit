# Specialist agent briefs

The PRD asks for ten specialist roles. This directory holds their briefs.

## How to use these

You do not need to literally spawn ten subagents. What matters is that each perspective is
**actually applied** at the right moment, with its own standards, rather than everything being
done in one undifferentiated pass. Two ways to use them:

1. **As subagent briefs.** Spawn an agent with the brief as its instructions when a phase calls
   for that role. Useful when the work is genuinely parallel — for instance, authoring exercise
   content while drawing illustrations.
2. **As review lenses.** Read the brief and apply it deliberately to work you have just done.
   The `Reviews` section of each brief is a checklist; running it yourself is far better than
   not running it at all.

Each phase prompt names which agents apply. The value is in the **adversarial** ones: the
Fitness Science agent exists to say "you cannot claim that", and the Accessibility agent exists
to say "that has two focus stops". If a review pass never objects to anything, it was not
really run.

## The agents

| Agent | Owns | Objects when |
|---|---|---|
| `fitness_science_agent.md` | Evidence, claims, programming | A claim outruns its evidence |
| `android_architecture_agent.md` | Module boundaries, patterns | Code is in the wrong layer |
| `ux_research_agent.md` | Flows, states, real-world use | A flow assumes an attentive user |
| `ui_design_agent.md` | Visual system, components | Something invents its own styling |
| `accessibility_agent.md` | WCAG AA+, TalkBack, scaling | An element is unreachable or unlabelled |
| `competitive_analysis_agent.md` | Feature shape, market context | We are rebuilding a known mistake |
| `qa_test_agent.md` | Test quality, coverage of what matters | A test asserts nothing useful |
| `security_privacy_agent.md` | Data locality, permissions | Anything could leave the device |
| `devops_agent.md` | Build, CI, artefact | The build is fragile or the gate is weakened |
| `documentation_agent.md` | Framework docs, project memory | Documentation drifts from the code |

## Conflict resolution

Agents will disagree. The priority order when they do:

1. **Safety and honesty** (Fitness Science, Security & Privacy). These can veto. A feature that
   is beautiful and makes an unsupported health claim does not ship.
2. **Accessibility.** Can veto a UI decision.
3. **Architecture.** Can veto a structural shortcut.
4. **UX and UI.** Shape the solution within the above.
5. **Competitive.** Informs, never decides — and never touches programming.
6. **QA, DevOps, Documentation.** Gate the work rather than direct it.

Record any resolved conflict in `project_memory/decisions.md`, including the losing position.
A decision that records why the alternative lost is far more useful later than one that only
records the winner.

# Copilot Instructions

## GitHub Spec Kit Handoff

Whenever a GitHub Spec Kit command or skill finishes, its completion response
MUST include the single most appropriate next step in the Spec Kit workflow.
Name the command explicitly (for example, `/speckit-plan`) and briefly explain
why it is next.

Choose the next step from the actual state of the feature artifacts and gates,
not from a fixed sequence alone:

| Completed step | Usual next step |
|---|---|
| `/speckit-specify` | `/speckit-clarify` when material ambiguities remain; otherwise `/speckit-plan` |
| `/speckit-clarify` | `/speckit-plan` once clarifications are recorded and the spec is ready |
| `/speckit-plan` | `/speckit-tasks` after the plan and design artifacts are reviewed |
| `/speckit-tasks` | `/speckit-analyze` to check consistency before implementation |
| `/speckit-analyze` | Address blocking findings, then `/speckit-implement` when the artifacts are ready |
| `/speckit-implement` | `/speckit-converge` after the implementation tasks are complete |
| `/speckit-converge` | `/speckit-implement` if convergence tasks were added; otherwise review the feature and open a pull request |
| `/speckit-checklist` | Continue with the command indicated by checklist readiness, commonly `/speckit-clarify` or `/speckit-plan` |

If prerequisites, unresolved decisions, failed checks, or review gates block
the usual next step, state the blocker and recommend the command or action that
resolves it first. If the Spec Kit workflow for the feature is complete and no
further Spec Kit command applies, say so and suggest the appropriate review or
delivery action instead.

This is a handoff only: recommend the next step, but do not run it unless the
user asks. Do not omit the recommendation just because a command completed
successfully or because its own output already lists generated artifacts.

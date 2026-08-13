# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project reference

`README.md` (repo root) has the project overview, structure, setup, environment
config, and Allure reporting commands. Point there for onboarding/setup
questions instead of re-deriving them.

## Multi-agent automation pipeline

Automation code changes in this repo (new or changed test coverage) go
through the agent pipeline defined in `.claude/agents/`:
`automation-orchestrator` → `task-analysis-agent` → `planning-agent` →
`coding-agent` → `code-review-agent` (with a rework loop) →
`code-quality-agent`.

**Context hygiene rule:** every handoff between pipeline stages passes
file paths, not inlined document content. Each subagent has its own
`Read` tool and loads a document itself rather than receiving it pasted
into its prompt. The orchestrator does not carry a document's full
content in its own context once that document has been written to disk —
only paths and each subagent's small structured handoff block (`STATUS`,
`SCORES`, `SUMMARY`, etc.) persist between stages; anything else is
re-read from disk at the point of need, not held for the whole run.

Apply this same pattern whenever adding or modifying agents or skills
under `.claude/agents/` or `.claude/skills/` — especially
orchestrator-style agents that spawn subagents and hand off documents
between pipeline stages.

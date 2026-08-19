---
name: readme-changelog-updater
description: "Update repository README and changelog files to match recent implementation changes. Use when DSH needs to summarize newly delivered capabilities, corrected setup details, module status changes, or daily development outputs in project documentation before commit or release."
whenToUse: "更新 README 与 Changelog"
---

# README Changelog Updater

Keep repository-facing project documentation aligned with what the code actually changed.

## Workflow

### 1. Read the real change set first

Inspect:

- git diff / diff stat
- relevant module docs
- current README sections
- existing changelog style and recent entries

Do not update docs from vague memory of what "should have changed."

### 2. Update only the affected claims

Typical targets include:

- current module capability lists
- setup or access path corrections
- interface or architecture milestone notes
- daily changelog entries

Avoid rewriting unrelated README sections.

### 3. Match repository documentation tone and granularity

README updates should:

- reflect current shipped or implemented status
- avoid speculative future claims
- stay consistent with existing structure

Changelog updates should:

- summarize the concrete day or change range
- mention meaningful implementation outcomes
- call out tests or risks only when relevant

### 4. Prefer accuracy over completeness theater

If something was not actually implemented or verified, do not document it as done.

## Output Rules

- Keep README high-signal.
- Keep changelog entries concrete and date-aligned.
- Match existing file naming and formatting style.

## Typical Deliverables

- README capability/status updates
- new dated changelog entries
- corrections to setup or interface access docs

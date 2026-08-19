---
name: release-note-writer
description: "Summarize completed repository changes into concise release or handoff notes. Use when DSH needs to turn implemented diffs, test outcomes, and documentation updates into a human-readable change summary for review, handoff, or release communication."
whenToUse: "编写发布或交接说明"
---

# Release Note Writer

Turn implemented change sets into concise notes that help others understand what actually changed.

## Workflow

### 1. Read the implemented change and supporting evidence

Inspect:

- git diff / diff stat
- test summaries when available
- README or changelog updates when already prepared

### 2. Summarize by outcome, not by file inventory

Group notes around:

- user-visible capabilities
- developer workflow changes
- contract or schema implications
- known limitations or follow-up items

Do not produce a raw file-by-file changelog unless the user specifically asks for one.

### 3. Keep the audience in mind

Release notes should help readers answer:

- what changed
- why it matters
- what is still incomplete or risky

### 4. Stay faithful to verified scope

Do not present untested or planned work as completed.

## Output Rules

- Prefer short, high-signal prose or flat bullets.
- Emphasize outcomes over implementation minutiae.
- Include residual risks only when they materially matter.

## Typical Deliverables

- release summary for a feature branch
- handoff notes after a development session
- concise change communication for review or deployment prep

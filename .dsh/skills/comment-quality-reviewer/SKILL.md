---
name: comment-quality-reviewer
description: "Review changed code comments for quality, necessity, and consistency in repository-backed projects. Use when DSH needs to inspect whether new or modified comments explain why the code exists, stay aligned with behavior, remain human-readable in Chinese, and avoid misleading or stale commentary."
whenToUse: "审查注释质量与一致性（中文注释）"
---

# Comment Quality Reviewer

Review comments as part of code quality, with emphasis on whether they help humans understand intent safely.

## Workflow

### 1. Review only comments that matter

Focus on:

- public methods
- complex business logic
- newly added explanatory comments
- comments near tricky branching or rule enforcement

Ignore obvious comments that add no value unless they create noise or contradiction.

### 2. Check comment purpose

Good comments should explain:

- why the code is structured this way
- what invariant or business rule it protects
- why a seemingly odd choice is necessary

Flag comments that only restate the code.

### 3. Check comment correctness

Verify whether the comment still matches:

- current behavior
- current parameter meaning
- current exception or status logic

Stale comments are worse than no comments.

### 4. Check language and readability

For this project, comments under review should be:

- human-readable
- Chinese
- concise but meaningful

## Output Rules

- Report only comments that are misleading, missing where needed, or low-quality enough to matter.
- Tie feedback to specific code locations.
- Prefer Why-oriented guidance over "add more comments everywhere."

## Typical Deliverables

- comment quality review findings
- stale or misleading comment warnings
- guidance on where intent comments are actually needed

---
name: git-diff-reviewer
description: "Review repository changes strictly from the current git diff rather than scanning the whole project. Use when DSH needs to perform incremental code review, pre-commit review, or change-focused risk analysis against modified files only, especially in multi-agent workflows where review scope must stay bounded."
whenToUse: "基于 git diff 做增量审查"
---

# Git Diff Reviewer

Review only the increment being changed. Treat unchanged code as background, not the primary review target.

## Workflow

### 1. Build review scope from git state first

Start from:

- `git diff`
- `git diff --stat`
- `git status`

Identify the actual changed files before reading code deeply. Do not scan the whole repository unless a changed file forces a dependency check.

### 2. Review by changed behavior, not by file count

For each changed area, determine:

- what behavior changed
- what risk the change introduces
- whether the change is internally consistent

Do not turn review into style commentary with no behavioral consequence.

### 3. Escalate only relevant context

Read surrounding implementation only when needed to answer:

- does this change break an existing contract
- does this branch now miss a required case
- does this data flow violate a known rule

Avoid broad architectural review beyond the diff unless the user explicitly asks for it.

### 4. Produce actionable findings

Each finding should state:

- where it is
- what the risk is
- why it matters
- what direction would fix it

## Output Rules

- Findings first, summary second.
- Cite file paths and lines when available.
- Prefer concrete risk over generic cleanup advice.
- If no findings are present, say so explicitly.

## Typical Deliverables

- incremental review of changed files
- pre-commit diff review
- bounded review report for multi-agent workflows

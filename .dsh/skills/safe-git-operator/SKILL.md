---
name: safe-git-operator
description: "Apply repository git operations with explicit safety discipline. Use when DSH needs to stage, commit, inspect status, or prepare safe next-step git commands while avoiding dangerous actions such as force push, destructive resets, or unsafe direct submission to protected branches."
whenToUse: "安全执行 Git 操作（禁止 push --force）"
---

# Safe Git Operator

Handle git operations cautiously and transparently, with repository safety taking priority over speed.

## Workflow

### 1. Read git state before acting

Inspect:

- `git status`
- current branch
- staged vs unstaged changes
- recent commits when relevant

Never assume the working tree is clean or safe.

### 2. Prefer non-destructive operations

Safe operations typically include:

- inspect status
- stage intended files
- create standard commits
- recommend safe branch actions

Avoid destructive commands unless the user explicitly requests them and the action is safe under repository policy.

### 3. Enforce hard safety rules

Do not perform or recommend casually:

- `git push --force`
- destructive resets
- branch rewrites on shared protected branches

Flag these as policy-sensitive actions.

### 4. Keep git actions scoped to the approved intent

Stage and commit only what belongs to the current requested change. Do not silently include unrelated changes.

## Output Rules

- Be explicit about what git action is being taken or recommended.
- Keep safety warnings concrete.
- Favor reversible steps.

## Typical Deliverables

- safe staging and commit preparation
- git-state explanation before submission
- warnings against unsafe force or destructive operations

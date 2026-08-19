---
name: branch-guard
description: "Guard repository branching and submission safety before commit or push. Use when DSH needs to verify the current branch, prevent direct work on protected branches such as main or develop, or recommend safe branch and submission behavior before version-control operations."
whenToUse: "提交或推送前保护分支安全"
---

# Branch Guard

Protect the repository from unsafe branch usage before commit or push actions.

## Workflow

### 1. Read current git state first

Inspect:

- current branch
- branch tracking status
- git status

Do not assume the user is already on a safe branch.

### 2. Enforce protected branch discipline

Treat these as protected unless the user explicitly overrides repository policy:

- `main`
- `master`
- `develop`

If work is happening directly on a protected branch, flag it before commit or push planning.

### 3. Check submission readiness context

Before blessing a branch for submission, confirm whether:

- the branch purpose matches the current work
- the change is ready for commit
- any required review or test gates are still pending

### 4. Recommend the safest next step

Examples:

- stay on the current feature branch
- create a new feature branch before committing
- stop before pushing to a protected branch

## Output Rules

- Keep guidance concrete and branch-specific.
- Prioritize safety over convenience.
- Explicitly warn when current branch usage violates project policy.

## Typical Deliverables

- branch safety check
- protected-branch warning
- recommended next git step before commit or push

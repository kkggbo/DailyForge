---
name: conventional-commit-writer
description: "Write repository-appropriate Conventional Commit messages for implementation changes. Use when DSH needs to generate commit subjects and optional bodies that summarize the actual change scope, match the repository's commit discipline, and stay aligned with feature, fix, refactor, docs, or test intent."
whenToUse: "编写 Conventional Commit 消息"
---

# Conventional Commit Writer

Write commit messages that describe the real change clearly and fit the repository's submission discipline.

## Workflow

### 1. Read the actual change first

Start from:

- `git diff --stat`
- `git diff`
- relevant README or changelog updates if they are part of the same submission

Do not write the commit message from the user prompt alone.

### 2. Pick the correct Conventional Commit type

Choose the narrowest accurate type, such as:

- `feat`
- `fix`
- `refactor`
- `perf`
- `docs`
- `test`

Do not label everything as `feat` just because code changed.

### 3. Scope the message by shipped meaning

The subject should reflect what changed for the codebase, not the implementation trivia.

Prefer:

- feature capability
- bug fixed
- refactor boundary
- documentation update scope

Avoid low-signal subjects like:

- "update code"
- "fix some issues"
- "adjust logic"

### 4. Add a body only when it helps

A body is useful when it clarifies:

- important secondary changes
- migration or compatibility impact
- linked issue or ticket reference

Keep the body concise and operational.

## Output Rules

- Use Conventional Commits syntax.
- Keep the subject short and specific.
- Match the repository's current language preference if it is already consistent.
- If an issue or ticket id is supplied, include it in the body.

## Typical Deliverables

- one commit subject
- optional commit body
- alternative wording when scope could fit more than one type

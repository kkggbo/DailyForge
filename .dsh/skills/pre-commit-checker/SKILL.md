---
name: pre-commit-checker
description: "Check whether a repository change is ready for commit based on local gates and supporting evidence. Use when DSH needs to verify formatting, lint, test status, review completion, or other pre-commit expectations before allowing a submission step to proceed."
whenToUse: "基于门禁证据判断是否可提交"
---

# Pre Commit Checker

Decide whether the change is ready to be committed based on actual gate evidence, not optimism.

## Workflow

### 1. Gather pre-commit evidence

Check the latest available signals for:

- git status / diff state
- formatting or lint results if they were run
- test results
- review findings
- contract check results when relevant

If a gate was not checked, mark it as not checked rather than assuming pass.

### 2. Evaluate the required gates

Typical commit gates include:

- working tree in expected state
- no known blocking review findings
- required tests passed
- required docs updated

Apply only the gates that the repository or current workflow actually uses, but be explicit about them.

### 3. Separate blockers from soft warnings

Blockers:

- failing required tests
- medium/high risk review findings when policy forbids commit
- unresolved contract mismatch

Warnings:

- optional docs not yet refreshed
- not-run noncritical checks

### 4. Produce a go / hold recommendation

End with one of:

- ready to commit
- not ready to commit
- ready with noted risk

## Output Rules

- Keep the report operational and concise.
- State missing evidence explicitly.
- Tie blockers to concrete next actions.

## Typical Deliverables

- pre-commit gate summary
- commit readiness verdict
- blocker list before submission

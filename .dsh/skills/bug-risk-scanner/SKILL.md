---
name: bug-risk-scanner
description: "Scan changed code for concrete bug risk in repository-backed application work. Use when DSH needs to review modified frontend or backend code for null handling, missed branches, stale state, invalid assumptions, edge-case failures, or logic regressions that could break real behavior."
whenToUse: "对变更代码做 Bug 风险审查"
---

# Bug Risk Scanner

Look for real failure modes, not theoretical perfection.

## Workflow

### 1. Read the changed behavior first

Understand what the change is supposed to do before searching for bugs. Use:

- git diff
- nearby code
- relevant interface or PRD docs when needed

### 2. Check common high-value bug classes

Review for:

- null / undefined handling gaps
- missing branch coverage
- stale or inconsistent state updates
- off-by-one or order issues
- bad assumptions about required data
- mismatch between validation and actual business rules
- unintended fallthrough after refactor

### 3. Focus on runtime consequences

For each suspected issue, ask:

- what concrete scenario triggers it
- what wrong behavior results
- whether it is user-visible, data-damaging, or blocking

Avoid speculative nitpicks that are unlikely to matter in practice.

### 4. Distinguish severity clearly

Label findings by impact:

- high: likely broken behavior, data corruption, or blocker
- medium: meaningful risk or regression path
- low: minor but real correctness or maintainability risk

## Output Rules

- Report only substantive bug risks.
- Tie each finding to a concrete scenario.
- Prefer exact file references.
- If uncertain, state the assumption behind the finding.

## Typical Deliverables

- bug-focused review notes on changed code
- risk-ranked findings for pre-submit review
- scenario-based regression concerns

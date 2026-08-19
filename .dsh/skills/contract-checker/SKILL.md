---
name: contract-checker
description: "Compare implemented frontend and backend API contracts against repository docs. Use when DSH needs to verify whether frontend API clients and types, backend Controller endpoints and DTO/VO shapes, and interface documentation are consistent enough to enter integration or submission stages."
whenToUse: "前端/后端/文档三方 API 契约一致性检查"
---

# Contract Checker

Perform a contract consistency pass before integration or submission. Focus on mismatches that can break联调.

## Workflow

### 1. Gather the three contract surfaces

Read all relevant sources for the target module:

- interface docs under `docs/interfaces/`
- frontend API client and types under `frontend/src/features/**/api` and `types`
- backend controller, request DTO, response VO, and Swagger annotations

When necessary, also read:

- PRD for semantic intent
- backend/frontend module docs for naming conventions

### 2. Compare by endpoint, not by file

For each endpoint, verify:

- method matches
- path matches
- auth expectation matches
- query/path/body parameters match
- required vs optional semantics match
- enum values match
- response shape matches
- pagination and wrapper conventions match

Do not stop at "fields look similar". Check meaning, not only names.

### 3. Flag semantic mismatches explicitly

Look for common breakpoints:

- frontend sends field not accepted by backend
- backend requires field not documented or not typed in frontend
- doc says partial update but backend expects full replacement
- enum labels differ across doc / frontend / backend
- frontend infers field semantics not guaranteed by backend

### 4. Produce a decision-oriented report

Report only what matters for integration:

- blocking mismatch
- likely mismatch / ambiguity
- verified consistent area

Classify each finding by severity:

- blocking
- important
- minor

### 5. Recommend the source of truth

When mismatch exists, recommend which side should change based on:

- latest user-confirmed decision
- current interface doc status
- implemented repository behavior

Do not silently "average" disagreements.

## Output Rules

- Write in Chinese unless the user requests another language.
- Keep findings concrete and reference file paths.
- Prefer endpoint-by-endpoint findings over generic comments.
- If no mismatch is found, say so explicitly and mention residual risk if review scope was limited.

## Required Output Structure

1. Review scope
2. Blocking findings
3. Important findings
4. Minor findings
5. Verified consistent areas
6. Recommended follow-up actions

## Contract Check Checklist

Before finishing, verify:

- all compared endpoints are named explicitly
- findings distinguish syntax mismatch from semantic mismatch
- file references are included
- blocking issues are clearly separated from nice-to-have cleanup

## Typical Deliverables

- contract consistency review before联调
- pre-submit API mismatch review
- short fix list for frontend and backend agents

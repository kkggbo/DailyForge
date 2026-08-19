---
name: frontend-type-sync
description: "Keep frontend TypeScript feature types synchronized with backend-facing contract semantics. Use when DSH needs to define or revise request and response types, enum unions, field nullability, derived UI models, or contract-mapping types so frontend code matches interface docs and implemented backend shapes."
whenToUse: "保持前端 TS 类型与 API 契约语义同步"
---

# Frontend Type Sync

Keep frontend types aligned with the real API contract and feature semantics.

## Workflow

### 1. Read the contract source of truth

Read:

- interface docs
- feature API client
- current feature types
- backend DTO/VO only when contract clarification is needed

Prefer the interface doc and confirmed user decision as the primary semantic source.

### 2. Separate raw contract types from UI-specific types

When useful, distinguish:

- transport/request types
- raw response types
- editor/view-model or local UI types

Do not force all feature state into one flattened interface if transport and UI semantics differ.

### 3. Be precise about optionality

For each field, decide explicitly:

- required
- optional
- nullable
- empty string allowed or not

Do not collapse all uncertainty into `string | null | undefined` unless the contract truly permits it.

### 4. Keep enum and code values stable

Use literal unions or narrow string types for:

- status codes
- structure types
- category codes
- role codes

Do not let known enum-like values degrade into generic `string` unless extension flexibility is required.

## Output Rules

- Prefer clear, feature-local types.
- Keep names aligned with existing project terminology.
- Avoid premature generic utility types.
- Preserve readability for future editors.

## Typical Deliverables

- updated `types/*.ts`
- clearer API request/response typing
- explicit local editor/view-model typing
- enum and nullability corrections

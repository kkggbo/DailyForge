---
name: frontend-backend-type-bridge
description: "Bridge backend transport models and frontend TypeScript feature types in repository-backed applications. Use when DSH needs to ensure backend DTO/VO semantics, frontend request and response types, and mapping assumptions remain aligned across both sides of the contract during active feature development."
whenToUse: "保持后端传输模型与前端 TS 类型语义对齐"
---

# Frontend Backend Type Bridge

Keep backend transport models and frontend feature types semantically aligned during implementation.

## Workflow

### 1. Read both sides of the contract

Inspect:

- interface docs
- backend DTO / VO classes
- frontend API request and response types
- frontend local editor/view-model types when relevant

### 2. Compare semantics, not only structure

Check:

- required vs optional
- nullable vs omitted
- enum values
- summary vs detail payload shape
- field purpose and naming

Do not stop at "the shapes look similar enough."

### 3. Clarify where divergence is intentional

Some frontend types may intentionally differ from backend transport models, such as:

- local form text state
- derived view models
- grouped UI-only structures

When divergence is intentional, make the bridge explicit in mapping or naming.

### 4. Produce actionable sync guidance

When mismatch exists, identify:

- which side is wrong
- whether docs also need an update
- whether a mapper/assembler layer should absorb the difference

## Output Rules

- Focus on contract-critical mismatches first.
- Use repository module terminology.
- Keep recommendations concrete and implementation-friendly.

## Typical Deliverables

- sync notes between backend DTO/VO and frontend types
- concrete field alignment fixes
- mapping-layer guidance when transport and UI models differ by design

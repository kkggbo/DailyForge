---
name: dto-vo-designer
description: "Design backend DTO and VO structures for repository-backed application modules. Use when DSH needs to define or refactor request DTOs, query objects, response VOs, transport-facing data shapes, or module contract objects so controller contracts stay explicit and decoupled from persistence entities."
whenToUse: "设计 DTO/VO 传输对象"
---

# DTO VO Designer

Design transport-layer objects that make the backend contract explicit and stable.

## Workflow

### 1. Read the contract and existing module style first

Inspect:

- interface docs
- PRD when semantics are unclear
- neighboring module DTO / VO classes
- current controller signatures

Do not design DTOs and VOs from database tables alone.

### 2. Separate request and response intent

Distinguish clearly between:

- create / update request DTOs
- query/filter DTOs
- list/detail response VOs
- summary vs full-detail payloads

Do not overload one object for multiple incompatible endpoint semantics unless the repository already does so intentionally.

### 3. Keep contracts transport-facing

DTOs and VOs should reflect:

- what the API accepts
- what the API returns
- what frontend or external callers need to know

They should not leak persistence-only or internal workflow fields without clear intent.

### 4. Match repository naming

Follow current naming conventions such as:

- `CreateXxxRequest`
- `UpdateXxxRequest`
- `XxxQuery`
- `XxxResponse`
- `XxxSummary`

Prefer consistency over originality.

## Output Rules

- Keep fields minimal but sufficient.
- Use explicit field names and types.
- Align with current controller and Swagger style.
- Avoid speculative future-proofing fields.

## Typical Deliverables

- request DTO classes
- query/filter DTO classes
- detail and summary VO classes
- cleaner transport contracts for controller endpoints

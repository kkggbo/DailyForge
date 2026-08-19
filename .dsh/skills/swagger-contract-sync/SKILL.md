---
name: swagger-contract-sync
description: "Synchronize backend Swagger annotations and endpoint signatures with repository interface contracts. Use when DSH needs to make Spring Boot controllers, request DTOs, and response VOs accurately reflect the latest API documentation, parameter semantics, and auth expectations."
whenToUse: "同步 Swagger 注解与接口契约"
---

# Swagger Contract Sync

Keep backend annotations and endpoint signatures aligned with the written interface contract.

## Workflow

### 1. Read the interface document first

Before changing annotations, inspect:

- target interface doc under `docs/interfaces/`
- target controller
- request DTO / query object
- response VO

Treat the latest confirmed interface doc as the primary contract source unless the user has explicitly changed the decision in the thread.

### 2. Align the endpoint surface

Check:

- route path
- HTTP method
- auth requirement
- path/query/body params
- required vs optional semantics
- enum and example values

Swagger annotations should describe the implemented contract, not a vague approximation.

### 3. Keep docs close to behavior

Update:

- `@Operation`
- `@Schema`
- parameter-level descriptions and examples

Only describe what the code actually does or is confirmed to do after this change.

### 4. Flag contract drift explicitly

If the backend code, Swagger annotations, and interface docs disagree, surface the mismatch and choose a concrete alignment direction.

## Output Rules

- Keep annotations high-signal and concise.
- Use examples where they clarify semantics.
- Match repository naming and terminology.
- Do not fabricate OpenAPI structure outside the actual code annotations unless the repository already uses separate spec files.

## Typical Deliverables

- updated controller Swagger annotations
- updated DTO / VO schema descriptions
- cleaner alignment between code and interface docs

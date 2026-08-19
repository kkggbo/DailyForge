---
name: api-contract-writer
description: "Produce or update API contract documentation for repository-backed modules. Use when DSH needs to define or revise endpoints, request and response bodies, parameter semantics, error codes, auth rules, integration constraints, or frontend-backend contract details before or during implementation."
whenToUse: "产出或更新 API 契约（接口）文档，中文输出"
---

# API Contract Writer

Write interface contracts that frontend and backend can implement against without guessing parameter meaning.

## Workflow

### 1. Read the real upstream sources first

Before writing the contract, read:

- latest confirmed requirements from the thread
- related PRD under `docs/prd/`
- existing module interface docs under `docs/interfaces/`
- relevant backend docs and current controller shape if the module already exists
- relevant frontend API client and types if the module already exists

Base the contract on the current project, not on generic REST conventions alone.

### 2. Define the contract surface

For each endpoint, explicitly define:

- method
- path
- auth requirement
- request parameters
- request body
- response body
- error codes
- validation and semantic rules

If the module is evolving, clearly distinguish:

- existing behavior
- new or changed behavior

### 3. Make parameter semantics unambiguous

For every important field, clarify:

- meaning
- whether it is required
- whether it allows null / empty / omitted
- format
- enum values
- whether it is user-facing text, system code, snapshot data, or identifier

Call out tricky semantics explicitly, such as:

- exact match vs fuzzy match
- replace vs append
- full update vs partial update
- current state vs history record

### 4. Write integration constraints

Always include the integration rules that commonly break联调:

- route prefix assumptions
- auth header format
- pagination defaults
- enum values frontend must not invent
- fields frontend must not infer
- fields backend revalidates on save

### 5. Define error behavior concretely

List recommended error codes and HTTP statuses. Prefer the project's existing error naming style.

For each non-trivial endpoint, explain when key error codes fire.

## Output Rules

- Write in Chinese unless the user requests another language.
- Match current project style and existing interface docs.
- Use examples for representative request and response bodies.
- Keep the contract implementation-facing; avoid architecture digressions.

## Required Sections

Unless the user asks for a lighter format, include:

1. Document scope
2. Common conventions
3. Endpoint list
4. Shared data structures
5. Endpoint details
6. Recommended error codes
7. Integration constraints
8. Change notes if updating an existing contract

## Contract Quality Checklist

Before finishing, verify:

- frontend can build request types directly from the doc
- backend can implement controller DTO/VO directly from the doc
- parameter semantics are explicit
- error codes are not hand-wavy
- no hidden behavior is left to inference

## Typical Deliverables

- new file under `docs/interfaces/<module>_接口文档.md`
- update to an existing interface doc after scope or schema changes
- short contract diff summary for联调

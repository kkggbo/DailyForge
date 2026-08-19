---
name: api-client-integrator
description: "Implement or refactor frontend API client integration against repository-backed backend contracts. Use when DSH needs to create request helpers, query parameter mapping, auth header usage, response unwrapping, error handling, or feature-level API wrappers that align with existing interface docs and shared HTTP utilities."
whenToUse: "实现或重构前端 API 客户端集成，与后端契约对齐"
---

# API Client Integrator

Connect frontend feature code to backend contracts without drifting from docs or shared HTTP patterns.

## Workflow

### 1. Read the contract and current client style

Before changing API code, read:

- interface docs under `docs/interfaces/`
- current feature `api/*.ts`
- shared request utilities
- relevant frontend types

Do not infer API semantics from UI assumptions alone.

### 2. Map parameters deliberately

For each request, check:

- route path
- auth requirement
- path params
- query params
- request body shape
- omitted vs null handling

Be especially careful with optional filters, pagination, and enum values.

### 3. Keep client wrappers thin but explicit

Feature API wrappers should:

- expose readable function names
- map contract fields explicitly
- avoid hidden transformation that the rest of the feature cannot see

Do not bury important semantics inside shared helpers unless the repository already standardizes them there.

### 4. Align error handling with project norms

Use existing request / response error handling patterns. If the project has module-specific error mappers, integrate with them instead of inventing a new style.

## Output Rules

- Follow the repository's current request utility conventions.
- Keep API wrappers feature-scoped.
- Keep request and response typing explicit.
- Avoid adding extra abstraction layers without real reuse.

## Typical Deliverables

- updated `api/*.ts`
- request mapping fixes
- response unwrapping and typing aligned with docs
- clearer integration between feature code and shared HTTP layer

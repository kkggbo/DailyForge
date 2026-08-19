---
name: business-rule-guard
description: "Implement or review backend business rule enforcement in repository-backed application services and domain services. Use when DSH needs to encode validation rules, state transitions, ownership checks, status restrictions, or feature-specific invariants so backend behavior matches product decisions instead of only passing structural validation."
whenToUse: "将产品规则转化为显式后端业务校验"
---

# Business Rule Guard

Turn product rules into explicit backend enforcement.

## Workflow

### 1. Read the rule sources first

Inspect:

- PRD
- interface doc
- existing module DDD or backend docs
- current service logic if the module already exists

Business rules should come from confirmed product decisions, not from what is easiest to code.

### 2. Separate structural validation from business validation

Structural validation includes:

- required fields
- basic ranges
- enum format

Business validation includes:

- ownership
- editable state restrictions
- activation rules
- conflict checks
- sequence rules

Do not rely on DTO annotations alone for business safety.

### 3. Make state restrictions explicit

When a feature has workflow or status constraints, enforce them visibly in service or domain logic.

Examples:

- only one active template at a time
- cannot edit completed records
- cannot delete historical committed data
- can modify future-only records but not past executed ones

### 4. Use repository-consistent error signaling

Choose existing business exception / error code patterns. Make rule failures specific enough that frontend and tests can react to them meaningfully.

## Output Rules

- Express rules as readable code, not as scattered conditionals.
- Prefer central rule points for repeated invariants.
- Keep product semantics obvious.
- Avoid hidden side effects.

## Typical Deliverables

- service/domain validation logic
- state and ownership guards
- explicit error code use for business rule failures

---
name: auth-permission-checker
description: "Integrate authentication and permission rules into backend feature endpoints in repository-backed Spring Boot projects. Use when DSH needs to enforce login requirements, current-user ownership checks, role-based restrictions, invite-gated capability checks, or module-specific authorization behavior without breaking existing security conventions."
whenToUse: "应用项目现有安全模型的认证与授权规则"
---

# Auth Permission Checker

Apply authentication and authorization rules in feature code using the repository's existing security model.

## Workflow

### 1. Read current security conventions first

Inspect:

- current security config
- auth-related shared utilities
- representative controllers and services
- role / invite / permission semantics already used in the project

Do not invent a new authorization model if the project already uses a lightweight one.

### 2. Separate auth from business ownership checks

Distinguish:

- must be logged in
- must own the resource
- must have role or invite-gated capability
- must be admin or elevated role

Do not blur all failures into the same generic denial.

### 3. Enforce checks at the right layer

Use:

- controller or shared security for login requirements
- service/domain layer for ownership and business-scoped permission checks

This keeps security visible while preserving reusable business enforcement.

### 4. Match repository error behavior

Use existing auth and permission failure patterns rather than inventing new ad hoc exceptions or response bodies.

## Output Rules

- Stay inside the repository's existing security model.
- Keep permission logic explicit.
- Avoid overengineering RBAC when the project does not use it.
- Make ownership and role checks testable.

## Typical Deliverables

- secured controller entry points
- service-level ownership / role checks
- consistent permission failure behavior

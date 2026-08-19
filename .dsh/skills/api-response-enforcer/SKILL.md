---
name: api-response-enforcer
description: "Enforce repository-specific API response and exception conventions in backend modules. Use when DSH needs to implement or refactor Spring Boot endpoints so they consistently return the project's ApiResponse wrapper, integrate with global exception handling, and avoid ad hoc response styles."
whenToUse: "确保端点统一返回 ApiResponse<T> 并接入全局异常处理"
---

# API Response Enforcer

Keep backend HTTP responses and error behavior aligned with the repository's established API contract style.

## Workflow

### 1. Read the current shared conventions

Inspect:

- shared `ApiResponse<T>` usage
- global exception handler
- existing error code enum / business exception style
- representative controllers

Do not substitute a generic `Result<T>` or raw response entity style if the repository already standardizes on `ApiResponse<T>`.

### 2. Standardize success responses

Ensure controller endpoints:

- return the correct wrapper
- avoid inconsistent ad hoc payloads
- preserve expected pagination or nested data structure when the module already defines it

### 3. Standardize error behavior

Use the existing exception flow:

- throw business exceptions where appropriate
- rely on global interception for consistent HTTP and response body mapping
- avoid manual try/catch wrappers in controllers unless the repository explicitly requires them

### 4. Keep semantics visible

Success and failure semantics should be readable from:

- method signature
- thrown exception type
- error code choice

Do not bury contract behavior inside undocumented helpers.

## Output Rules

- Follow existing shared response conventions exactly.
- Prefer repository error code naming style.
- Keep controller code free of repeated wrapper boilerplate when shared helpers already solve it.

## Typical Deliverables

- controller methods returning `ApiResponse<T>`
- error handling aligned with global exception rules
- elimination of inconsistent response styles

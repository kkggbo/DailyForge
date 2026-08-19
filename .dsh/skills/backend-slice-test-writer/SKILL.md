---
name: backend-slice-test-writer
description: "Write backend slice tests for repository-backed Spring Boot modules. Use when DSH needs to add or refine focused controller-layer tests, request binding tests, validation tests, auth entry-point tests, or API response shape tests using the repository's current `@WebMvcTest` and mock-based testing style."
whenToUse: "用 @WebMvcTest 编写 Controller 层切片测试"
---

# Backend Slice Test Writer

Write focused backend slice tests that prove controller-layer behavior without dragging in the whole application unnecessarily.

## Workflow

### 1. Read the controller contract and current test style first

Inspect:

- target controller
- request DTO / response VO
- interface docs
- neighboring `@WebMvcTest` or equivalent slice tests

### 2. Test the controller boundary

Focus on:

- route and method behavior
- request binding
- validation failure behavior
- auth requirements at the controller boundary
- response status and wrapper shape
- controller-to-service interaction contract

Do not turn a slice test into a hidden integration test.

### 3. Mock downstream dependencies deliberately

Mock:

- service/application service
- auth dependencies when the repository style requires it

Use mocks to isolate controller behavior, not to recreate the whole business layer.

### 4. Assert transport behavior explicitly

Prefer assertions on:

- HTTP status
- response JSON fields
- error code and message structure
- whether downstream service was invoked correctly

## Output Rules

- Prefer `@WebMvcTest` when it fits the repository pattern.
- Keep one scenario per test where practical.
- Align JSON assertions with real API contract fields.
- Keep service behavior mocked and narrow.

## Typical Deliverables

- controller slice tests
- request validation coverage
- auth and wrapper behavior verification

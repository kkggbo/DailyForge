---
name: service-unit-test-writer
description: "Write backend service unit tests for repository-backed Spring Boot modules. Use when DSH needs to test application services or domain services with mocked persistence dependencies, verify business rule enforcement, state transitions, branching behavior, or exception outcomes using the repository's current JUnit and Mockito patterns."
whenToUse: "编写后端 Service 单元测试"
---

# Service Unit Test Writer

Write backend service tests that prove business behavior independently from transport and persistence plumbing.

## Workflow

### 1. Read the service rules first

Inspect:

- target application service or domain service
- related PRD or interface doc when needed
- current test style in the module
- repository and mapper dependencies used by the service

### 2. Mock persistence and collaborators

Service unit tests should typically mock:

- mapper or repository dependencies
- external collaborators
- clock or auth context helpers when relevant

Do not hit the database in a unit test.

### 3. Cover business branches

Prioritize:

- happy path
- state restriction failures
- ownership or permission failures
- not-found conditions
- branching behavior with meaningful outcomes

### 4. Assert outcomes clearly

Assert:

- returned data
- thrown business exception
- state transitions
- collaborator invocation when behaviorally relevant

Avoid shallow tests that only mirror implementation line by line.

## Output Rules

- Use current JUnit and Mockito style in the repo.
- Keep scenario names explicit.
- Use Given-When-Then structure for readability.
- Focus on business behavior over transport details.

## Typical Deliverables

- application service unit tests
- domain service rule tests
- exception and branch coverage around business logic

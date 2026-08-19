---
name: controller-service-builder
description: "Implement backend endpoint flow across Controller and application service layers in repository-backed Spring Boot projects. Use when DSH needs to add or refactor endpoint handlers, request validation entry points, service orchestration, DTO/VO wiring, or module-level business flow while preserving the repository's current layering."
whenToUse: "实现 Controller 与 Application Service 端点流程"
---

# Controller Service Builder

Implement backend feature flow across controller and service layers without collapsing responsibilities.

## Workflow

### 1. Read the contract and existing module behavior first

Before coding, inspect:

- interface docs
- target controller and related controllers
- existing application services
- current DTO / VO / assembler style
- shared response and exception handling

Do not write endpoint logic from assumptions alone.

### 2. Keep responsibilities explicit

Use:

- controller for HTTP entry, auth context extraction, and request binding
- application service for orchestration and module use-case flow
- domain service only when business rules need their own boundary

Do not move persistence-heavy or branching business flow into the controller.

### 3. Treat the endpoint as a use case

For each endpoint, make the flow explicit:

- input accepted
- validation entry point
- business action
- persistence calls
- response assembly

This should be obvious from the code without tracing through hidden utility layers.

### 4. Preserve repository conventions

Match current project behavior for:

- route path style
- auth principal extraction
- pagination wrapper style
- logging expectations
- naming of `create/update/get/list/delete` methods

## Output Rules

- Use Spring Boot controller conventions already present in the repo.
- Favor readable orchestration over clever abstraction.
- Keep controller methods short.
- Keep service methods concrete and use-case driven.

## Typical Deliverables

- implemented controller endpoints
- matching application service methods
- DTO/VO orchestration aligned with the module contract

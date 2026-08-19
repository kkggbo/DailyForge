---
name: frontend-interaction-test-writer
description: "Write frontend interaction tests for repository-backed React and TypeScript features. Use when DSH needs to cover user actions such as clicking, typing, dialog flow, validation behavior, loading states, or list updates with the project's existing React Testing Library and Vitest or Jest stack."
whenToUse: "为前端 feature 编写覆盖用户交互路径的测试"
---

# Frontend Interaction Test Writer

Write frontend tests that exercise real user interaction paths and regression-prone UI behavior.

## Workflow

### 1. Read the page or component behavior first

Inspect:

- target component or page
- surrounding feature flow
- existing frontend tests nearby
- current shared render helpers if present

Use real feature behavior as the source of truth, not assumed UI expectations.

### 2. Cover user-observable interactions

Prioritize tests for:

- clicking buttons or actions
- typing and field changes
- opening and closing dialogs
- submit and cancel flow
- validation message display
- loading, empty, and error states
- list or card content updates after user action

### 3. Assert the visible outcome

Prefer assertions on:

- rendered text
- enabled / disabled state
- visible error or success messages
- presence or absence of modal content
- changed list content

Avoid fragile assertions on implementation details unless there is no better observable surface.

### 4. Fit the repository test stack

Use the project's current:

- React Testing Library patterns
- async waiting style
- mock or provider setup helpers
- Vitest or Jest conventions

## Output Rules

- Keep tests close to user behavior.
- Keep setup explicit enough to understand.
- Add only meaningful scenario coverage.
- Preserve readability over clever helper abstraction.

## Typical Deliverables

- `*.test.tsx` interaction tests
- regression coverage for dialogs, forms, and state transitions
- improved confidence in real UI behavior

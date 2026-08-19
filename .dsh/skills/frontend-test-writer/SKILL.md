---
name: frontend-test-writer
description: "Write frontend interaction tests for repository-backed React and TypeScript features. Use when DSH needs to add or update component and page tests, user interaction coverage, API mock behavior, or regression-focused frontend test cases that follow the project's current testing stack and feature structure."
whenToUse: "为 React feature 添加组件和页面测试"
---

# Frontend Test Writer

Add frontend tests that exercise real user flows and guard against regressions in feature behavior.

## Workflow

### 1. Read the feature behavior and existing test style

Before writing tests, inspect:

- the target page/component behavior
- existing tests in the repository
- current testing stack and helpers
- API mocking approach if present

Do not write tests against imagined behavior; derive them from the feature and the latest contract.

### 2. Test user-observable behavior first

Prioritize:

- clicking
- typing
- opening and closing dialogs
- validation messages
- list changes
- submit and loading behavior

Avoid shallow tests that only assert implementation details.

### 3. Mock API behavior deliberately

When network interaction exists, test:

- success path
- empty path when relevant
- error path when relevant

Use the project's existing mock style. If MSW is already in use, follow that pattern.

### 4. Keep tests maintainable

Group by user behavior. Use clear Given-When-Then style in structure and naming where practical.

Do not over-fragment tests into tiny assertions that obscure the scenario.

## Output Rules

- Write tests in the repository's current frontend test stack.
- Focus on feature behavior, not internals.
- Keep test setup local unless shared helpers already exist.
- Add only meaningful coverage.

## Typical Deliverables

- `*.test.tsx`
- interaction and API mock coverage
- regression tests for feature-critical user flows

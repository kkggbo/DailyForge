---
name: given-when-then-test-writer
description: "Write repository-backed automated tests using clear Given-When-Then structure. Use when DSH needs to add or refactor frontend or backend test cases so scenario setup, action, and expected outcome are explicit, readable, and maintainable across the project's current testing stack."
whenToUse: "编写 Given-When-Then 结构测试"
---

# Given When Then Test Writer

Write tests as behavior scenarios, not as loose assertion collections.

## Workflow

### 1. Read the feature behavior and current test style first

Inspect:

- target feature code
- relevant interface or PRD docs when behavior is ambiguous
- neighboring tests in the same module
- project test helpers and fixtures

Do not generate tests from guesswork or only from implementation details.

### 2. Structure tests around scenario flow

Each test should make these parts obvious:

- Given: the initial state, fixtures, mocks, and setup
- When: the user action or service invocation
- Then: the observable result or assertion

This can appear in naming, comments, or code grouping, but the scenario flow must be easy to scan.

### 3. Prefer behavior over internal mechanics

Test:

- what the user or caller observes
- what the API returns
- what the system state becomes

Avoid tests that only prove a helper method was called unless that is the meaningful behavior boundary.

### 4. Keep tests maintainable

Prefer:

- one clear scenario per test
- shared setup only when it reduces noise without hiding meaning
- readable fixture names

Avoid giant tests that cover multiple unrelated branches at once.

## Output Rules

- Match the repository's current test framework and style.
- Keep test names concrete.
- Keep assertions focused on the promised behavior.
- Use Given-When-Then as a readability discipline, not ceremony.

## Typical Deliverables

- clearer unit and integration tests
- refactored test structure for scenario readability
- reusable but not overabstracted setup helpers

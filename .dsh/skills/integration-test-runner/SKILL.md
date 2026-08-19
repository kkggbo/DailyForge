---
name: integration-test-runner
description: "Plan, run, and summarize repository-backed integration tests when a change needs broader verification than unit or slice tests. Use when DSH needs to exercise module-level integration behavior, multi-layer backend flow, frontend-backend contract-sensitive paths, or test command execution results before release decisions."
whenToUse: "规划、运行与汇总集成测试"
---

# Integration Test Runner

Run broader verification when isolated tests are not enough and summarize the results in a way the main control session can use.

## Workflow

### 1. Identify what integration scope is actually needed

Before running tests, determine whether the change needs:

- backend module integration
- frontend feature integration with mocks
- end-to-end-like local flow checks

Do not run large integration suites by reflex when a narrower test surface is enough.

### 2. Read the relevant test entry points

Inspect:

- existing integration tests
- target module boundaries
- related commands or scripts for running tests

### 3. Execute meaningful verification

When running tests, focus on:

- affected module flows
- contract-sensitive paths
- regression-prone scenarios

If something cannot be run, say exactly why.

### 4. Summarize results operationally

Report:

- what was run
- what passed
- what failed
- whether failure is new, expected, or unrelated

## Output Rules

- Be explicit about executed scope.
- Distinguish not-run from pass.
- Prefer concise operational summaries over raw output dumps.

## Typical Deliverables

- integration test execution summary
- targeted verification notes for release readiness
- blocker identification when broader flows fail

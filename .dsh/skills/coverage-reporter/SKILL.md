---
name: coverage-reporter
description: "Summarize test execution and coverage signals for repository-backed changes. Use when DSH needs to gather pass/fail results, note untested areas, interpret coverage output when available, or produce a concise testing report for review or release coordination."
whenToUse: "汇总测试执行与覆盖率信号"
---

# Coverage Reporter

Turn raw test and coverage signals into a concise readiness report.

## Workflow

### 1. Read the executed test evidence

Gather:

- test command outputs
- pass/fail summaries
- coverage output if the project produced it
- scope of the change when interpreting gaps

Do not invent coverage numbers when no tool output exists.

### 2. Separate certainty levels

Clearly distinguish:

- confirmed passed tests
- confirmed failed tests
- not executed
- coverage available
- coverage unavailable

### 3. Highlight meaningful risk

Call out:

- critical changed paths with no direct tests
- coverage gaps in rule-heavy services or UI interaction flows
- whether failures block submission

### 4. Make the report consumable by release coordination

Summaries should help the main control session answer:

- is testing sufficient
- what still needs verification
- what currently blocks commit or release

## Output Rules

- Prefer concise, decision-oriented reporting.
- Do not overclaim coverage precision.
- Tie risk notes to changed behavior, not generic quality slogans.

## Typical Deliverables

- test summary report
- coverage note for changed areas
- input to release or submission readiness decisions

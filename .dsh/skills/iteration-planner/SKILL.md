---
name: iteration-planner
description: "Split a confirmed feature into prioritized implementation iterations and actionable task lists. Use when DSH needs to turn PRD and interface decisions into phased delivery scope, frontend and backend task breakdowns, sequencing, dependency notes, and acceptance-ready execution plans."
whenToUse: "将 feature 拆分为实现迭代与任务清单"
---

# Iteration Planner

Turn confirmed requirements into a realistic execution plan that multiple agents can follow.

## Workflow

### 1. Read the locked inputs

Read the latest:

- confirmed thread decisions
- PRD
- interface docs
- relevant backend/frontend docs
- current repository state when needed to detect existing capability

Do not plan against stale or hypothetical requirements.

### 2. Plan by deliverable slices

Break work into iterations that are:

- user-valuable
- testable
- dependency-aware
- small enough to finish cleanly

Prefer slices such as:

- contract and schema preparation
- backend capability
- frontend capability
- integration and regression

Avoid giant one-shot plans that hide critical path risk.

### 3. Set priority explicitly

For each task or iteration, indicate:

- must-have
- should-have
- later / optional

When tradeoffs exist, bias toward the shortest path to a stable MVP.

### 4. Produce agent-ready task lists

When the user is using multiple agents, split work into:

- frontend task list
- backend task list
- data / migration tasks only when needed
- testing / review tasks

Each task should state:

- goal
- write scope
- prerequisite docs
- blocked-by dependency if any
- verification expectation

### 5. Mark the critical path

Always identify what must happen first, for example:

- PRD -> interface doc -> backend/frontend implementation
- schema change -> backend persistence -> controller -> frontend integration

This prevents parallelization that looks efficient but creates rework.

## Output Rules

- Write in Chinese unless the user requests another language.
- Optimize for execution, not presentation flourish.
- Prefer flat, high-signal sections.
- Include task ordering and dependency notes.

## Required Sections

1. Planning basis
2. Iteration split
3. Priority notes
4. Frontend task list
5. Backend task list
6. Optional data / DTO / test / review tasks
7. Critical path and recommended execution order

## Planning Quality Checklist

Before finishing, verify:

- tasks are concrete enough for an agent to execute
- dependencies are explicit
- MVP boundary is preserved
- no task assumes undocumented behavior
- parallel tasks have disjoint write scope where possible

## Typical Deliverables

- one planning doc section in the thread
- implementation task lists for frontend and backend agents
- iteration summary used by the main control session

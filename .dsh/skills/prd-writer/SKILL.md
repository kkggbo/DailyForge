---
name: prd-writer
description: "Draft or update product requirement documents for repository-backed software work. Use when DSH needs to turn a user idea, feature request, bugfix scope, or module discussion into a structured PRD with goals, scope, non-goals, business rules, user flow, edge cases, and acceptance criteria before implementation."
whenToUse: "产出结构化 PRD（范围/非目标/验收标准），中文输出"
---

# PRD Writer

Produce a decision-ready PRD for one concrete feature or module. Optimize for implementation clarity, not business theater.

## Workflow

### 1. Build context from the repository and thread

Read the latest relevant context before writing:

- current user request and confirmed decisions in the thread
- existing PRD files under `docs/prd/`
- related interface docs under `docs/interfaces/`
- related backend/frontend docs under `docs/backend/` and `docs/frontend/`
- README and changelog only when they clarify current shipped behavior

Do not invent product behavior when the repository already documents it. If docs and conversation disagree, explicitly state the mismatch and follow the latest confirmed user decision.

### 2. Lock the scope

State all of the following explicitly:

- problem being solved
- target user
- value to the user
- in-scope capability
- out-of-scope capability
- dependencies on other modules

Prefer a narrow and shippable MVP slice over a broad aspirational design.

### 3. Write implementation-useful sections

Unless the user asks for a lighter format, include:

1. Background / goal
2. Target users
3. Scope
4. Non-goals
5. Functional requirements
6. Business rules and validation rules
7. Key page or interaction flow
8. Edge cases / failure cases
9. Data or contract impact
10. Acceptance criteria
11. Open questions only if genuinely unresolved

Keep each requirement concrete enough that frontend and backend agents can implement without guessing.

### 4. Write acceptance criteria as testable statements

Acceptance criteria must be observable and binary where possible.

Good pattern:

- Given X, when Y, then Z
- User can / cannot do N
- API returns A when B
- Page displays C after D

Avoid vague criteria such as:

- "experience is smooth"
- "logic is correct"
- "works well"

### 5. Capture non-goals aggressively

Non-goals are mandatory whenever there is a risk of scope creep. Include explicit exclusions such as:

- AI capability not included yet
- no admin capability in this iteration
- no mobile optimization beyond current responsive baseline
- no historical migration for old data

## Output Rules

- Write in Chinese unless the user requests another language.
- Match repository naming. Reuse module terms already used in docs.
- Prefer Markdown headings and flat bullet lists.
- Do not turn the PRD into an architecture document.
- Do not silently add technical choices unless they are already decided elsewhere.

## PRD Quality Checklist

Before finishing, verify:

- scope and non-goals are both present
- business rules are explicit
- edge cases are not omitted
- acceptance criteria are testable
- no requirement depends on hidden assumptions

## Typical Deliverables

- new file under `docs/prd/<module>_PRD.md`
- update to an existing PRD when feature scope evolves
- short companion summary for frontend/backend task splitting

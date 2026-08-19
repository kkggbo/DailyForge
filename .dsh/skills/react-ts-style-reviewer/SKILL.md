---
name: react-ts-style-reviewer
description: "Review changed React and TypeScript frontend code for repository-consistent structure and maintainability. Use when DSH needs to assess modified frontend files against project ESLint and typing expectations, component responsibility patterns, hook usage clarity, and sustainable UI implementation style."
whenToUse: "审查前端代码可维护性与项目一致性"
---

# React TS Style Reviewer

Review changed frontend code for maintainability, type discipline, and consistency with the repository's current React patterns.

## Workflow

### 1. Read local frontend patterns first

Inspect nearby files in the same feature to understand:

- component splitting style
- hook usage
- API and type organization
- Tailwind or styling conventions
- current TypeScript strictness patterns

### 2. Review maintainability-relevant frontend style

Focus on:

- clear component responsibilities
- explicit props and types
- sane state ownership
- effect and async flow readability
- overgrown page components
- repeated logic that should be centralized

### 3. Flag style that creates bugs later

Look for:

- weak typing where contract is known
- state coupling that will become fragile
- hidden side effects in render or event handlers
- too many boolean flags driving one component
- UI logic mixed deeply into API mapping code

### 4. Keep the review practical

Style comments should help the next engineer change the code safely. Avoid low-value nitpicks that don't affect correctness or maintainability.

## Output Rules

- Prefer concrete frontend maintainability findings.
- Reference files and components.
- Distinguish hard issues from optional cleanup.
- Align with project ESLint and existing style before generic React preferences.

## Typical Deliverables

- React/TypeScript review findings on changed code
- component, hook, and typing maintainability concerns
- frontend structure feedback tied to real future risk

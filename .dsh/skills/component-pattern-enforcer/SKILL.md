---
name: component-pattern-enforcer
description: "Enforce consistent component design patterns in repository-backed React and TypeScript code. Use when DSH needs to shape component boundaries, Props definitions, composition style, naming, responsibility split, or reusable component conventions so new UI code matches the existing project architecture."
whenToUse: "强制执行一致的 React/TS 组件设计模式"
---

# Component Pattern Enforcer

Keep component structure clean, typed, and consistent with the repository.

## Workflow

### 1. Read existing component patterns first

Inspect representative components in the same feature and neighboring features. Pay attention to:

- prop naming
- component file granularity
- local helper placement
- event callback style
- type alias vs interface usage

### 2. Define boundaries clearly

Each component should have one primary responsibility. Split when a component is simultaneously:

- data orchestrator
- modal manager
- large form renderer
- reusable list item renderer

Do not split purely for the sake of smaller files.

### 3. Type Props explicitly

Always define Props clearly. Include:

- required vs optional fields
- event callback signatures
- discriminated values when useful

Do not leave event shapes implicit when the parent-child contract matters.

### 4. Prefer composition over configuration sprawl

When a component accumulates many boolean flags or mutually exclusive rendering branches, check whether it should be:

- split into multiple components
- composed from smaller pieces
- driven by a more explicit data model

## Output Rules

- Use TypeScript types or interfaces deliberately and consistently.
- Keep components readable at first scan.
- Avoid generic utility components unless there is clear reuse.
- Do not introduce memoization by default unless the repository already needs it there.

## Typical Deliverables

- cleaner `components/*.tsx`
- explicit Props typing
- improved component responsibility split
- reduced prop and branch ambiguity

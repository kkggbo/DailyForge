---
name: tailwind-ui-refiner
description: "Refine Tailwind-based UI implementation inside repository-backed frontend projects. Use when DSH needs to improve spacing, hierarchy, responsiveness, action grouping, visual density, state styling, or interaction polish while preserving the project's established Tailwind and component patterns."
whenToUse: "优化 Tailwind UI（层级/间距/状态样式）"
---

# Tailwind UI Refiner

Improve UI clarity and interaction polish while staying inside the repository's existing visual language.

## Workflow

### 1. Read the local visual context first

Inspect surrounding pages and components to understand:

- current spacing rhythm
- card and section treatment
- button hierarchy
- typography usage
- responsive conventions

Do not redesign one screen in a way that clashes with the rest of the app unless explicitly requested.

### 2. Refine hierarchy before decoration

Prioritize:

- grouping
- spacing
- readable density
- primary vs secondary actions
- error and empty-state clarity

Do not add visual noise to compensate for weak structure.

### 3. Improve state visibility

Ensure the UI clearly communicates:

- disabled states
- hover and focus states
- loading states
- selected states
- destructive actions

### 4. Respect implementation maintainability

Tailwind class usage should remain understandable. When classes become unreasonably dense or repeated, refactor only as much as the project currently does elsewhere.

## Output Rules

- Stay within Tailwind and project conventions.
- Avoid introducing a whole new design system.
- Prefer purposeful changes over broad cosmetic churn.
- Preserve mobile and desktop usability.

## Typical Deliverables

- cleaner layout and spacing
- stronger action hierarchy
- better responsive structure
- improved state styling and visual clarity

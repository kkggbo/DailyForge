---
name: react-page-builder
description: "Build or refactor React pages in repository-backed TypeScript frontend projects. Use when DSH needs to implement feature pages, route-level screens, page state flow, page composition, loading/error states, or user interactions while following the repository's existing page patterns."
whenToUse: "构建或重构路由级页面"
---

# React Page Builder

Implement route-level pages and page composition with strong alignment to the current project's UI and state patterns.

## Workflow

### 1. Read adjacent pages and route entry points

Inspect:

- current feature pages
- route usage
- page-level state patterns
- current loading / empty / error presentation style

Do not build a page in isolation from the rest of the frontend.

### 2. Separate page orchestration from reusable UI

The page should typically own:

- route params
- top-level data loading
- dialog open/close state
- submit / refresh orchestration

Move reusable UI pieces into components when they are meaningful outside a single block of the page.

### 3. Handle page states explicitly

For each page, account for:

- loading
- empty state
- error state
- success state
- submit in progress state when relevant

Do not hide these transitions inside ad hoc conditionals spread across the page.

### 4. Respect existing UX patterns

Follow the current repository's style for:

- headers
- action bars
- card/grouping structure
- modal / dialog usage
- destructive action confirmation

## Output Rules

- Use function components and hooks.
- Keep top-level page files readable.
- Do not over-abstract with unnecessary hooks or helper layers.
- Preserve the repository's visual language unless the user explicitly asks for redesign.

## Typical Deliverables

- new or updated `pages/*.tsx`
- page-level orchestration logic
- extracted supporting components where appropriate

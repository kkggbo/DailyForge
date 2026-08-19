---
name: frontend-module-scaffolder
description: "Create or extend frontend feature module scaffolding in repository-backed React projects. Use when DSH needs to set up a new feature directory, page/component/api/type/lib structure, route entry points, or baseline file layout that matches the repository's existing frontend conventions before detailed implementation."
whenToUse: "创建或扩展前端 feature 模块目录骨架"
---

# Frontend Module Scaffolder

Create a frontend feature skeleton that matches the repository's existing structure. Optimize for consistency with the current codebase, not for generic boilerplate.

## Workflow

### 1. Read the existing frontend structure first

Before creating anything, inspect:

- `frontend/src/features/`
- neighboring feature modules
- route/page structure
- current naming conventions for `api/`, `components/`, `hooks/`, `lib/`, `pages/`, and `types/`
- shared infrastructure under frontend docs when needed

Do not invent a new feature layout if the repository already has a stable pattern.

### 2. Scaffold only the files the feature actually needs

Start from the smallest useful structure. Typical files may include:

- `api/*.ts`
- `components/*.tsx`
- `hooks/*.ts`
- `lib/*.ts`
- `pages/*.tsx`
- `types/*.ts`

Do not generate placeholder files that have no immediate implementation purpose.

### 3. Match naming and import style

Use:

- existing feature naming style
- existing relative import conventions
- existing page and component suffix conventions

Prefer extending the project's current patterns over introducing your personal defaults.

### 4. Leave clean extension points

When scaffolding, create obvious places for later work:

- request functions in `api/`
- types in `types/`
- UI building blocks in `components/`
- mapping / validation / formatting helpers in `lib/`

Avoid putting all first-pass code into one oversized page file.

## Output Rules

- Keep the scaffold lean.
- Use TypeScript.
- Prefer ASCII unless the repository already uses non-ASCII identifiers in code.
- Avoid speculative abstraction.

## Typical Deliverables

- a new feature directory under `frontend/src/features/<module>/`
- route/page entry files aligned with the current router structure
- minimal supporting API/type/lib files ready for implementation

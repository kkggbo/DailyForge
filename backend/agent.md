# Backend Agent Rules

## Scope

This file defines long-term working conventions for the `backend` directory.

## Documentation First Rule

When a new backend module is created or substantially expanded, documentation must be created or updated before or together with code changes.

Required documentation workflow:

1. Create a corresponding module documentation directory under:
   - `D:\Computer Science\DailyForge\docs\backend\<module_name>`
2. Add and maintain at least the following module documents:
   - `<module_name>_接口文档.md`
   - `<module_name>_DDD.md`
3. The API document must explain:
   - module purpose
   - API list
   - request and response formats
   - core business flow
   - error handling behavior
   - dependencies on shared infrastructure
4. The DDD document must explain:
   - module scope and boundaries
   - database tables and key fields used by the module
   - core business rules and state transitions
   - API implementation logic and transaction boundaries
   - security/authentication behavior if applicable
   - Java package structure and class responsibilities
   - error code design
   - extensibility notes and future constraints
5. Prefer using the global skill `backend-ddd-writer` to draft the DDD first, then tailor it to the actual module and repository context.
6. If the module changes existing shared behavior, also update relevant shared docs under:
   - `D:\Computer Science\DailyForge\docs`

## Module Documentation Naming

Recommended naming:

- module directory:
  - `auth_module`
  - `user_module`
  - `profile_module`
  - `plan_module`
- document file:
  - `auth_接口文档.md`
  - `auth_DDD.md`
  - `auth_时序与流程.md`
  - `plan_接口文档.md`
  - `plan_DDD.md`

## Minimum API Document Requirements

For every externally exposed API, documentation should include:

- path
- HTTP method
- authentication requirement
- request headers if needed
- request body format
- response body format
- field meanings
- success behavior
- failure behavior
- implementation logic summary

## Keep Docs Synchronized

When backend interfaces or business rules change:

- update the corresponding module docs in the same change set
- avoid letting documentation lag behind implementation for core flows

## Current Shared Docs

Shared backend infrastructure docs currently include:

- `D:\Computer Science\DailyForge\docs\后端基础设施设计.md`
- `D:\Computer Science\DailyForge\docs\数据库设计.md`
- `D:\Computer Science\DailyForge\docs\MySQL建表草案.md`

Module docs should reference these files when shared mechanisms are reused.

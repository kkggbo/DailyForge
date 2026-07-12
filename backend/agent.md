# Backend Agent Rules

## Scope

This file defines long-term working conventions for the `backend` directory.

## Documentation First Rule

When a new backend module is created or substantially expanded, documentation must be created or updated before or together with code changes.

Product input documents should be treated as the upstream source of truth before drafting backend technical docs or writing code.

Default source directories:

1. Interface documents:
   - `D:\Computer Science\DailyForge\docs\interfaces`
   - naming: `<module_name>_接口文档.md`
2. PRD documents:
   - `D:\Computer Science\DailyForge\docs\prd`
   - naming: `<module_name>_PRD.md`
3. If a module has a dedicated database change proposal, read it together with the PRD and interface doc before implementation:
   - common naming: `D:\Computer Science\DailyForge\docs\<module_name>_数据库改造清单.md`

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

## Implementation Plan Before Coding

Before writing backend module code, produce a detailed implementation plan from the latest PRD, interface doc, DDD, migration SQL, and current codebase state.

Prefer using the global skill `backend-impl-planner` to generate this implementation plan before coding.

The implementation plan should be detailed enough that another engineer or agent can implement directly without making product or architecture decisions on their own.

At minimum, the plan should cover:

- implementation scope and locked assumptions
- module boundaries and cross-module dependency changes
- public API coverage and request / response contract implications
- entity / mapper / DTO / VO / assembler / service / controller changes
- transaction boundaries and data consistency rules
- validation rules and error code additions
- debug logging expectations and sensitive-data logging constraints
- method comments / Javadoc expectations for controller, service, and custom mapper methods
- unit tests, integration tests, and acceptance checks
- documentation sync tasks if docs need to move with the code change

If the user requests code implementation directly for a backend module, the preferred workflow is:

1. verify or refresh the upstream docs if needed
2. generate the detailed implementation plan
3. implement the code
4. run verification
5. summarize code and documentation impact

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

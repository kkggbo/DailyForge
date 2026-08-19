---
name: spring-module-scaffolder
description: "Create or extend backend module scaffolding in repository-backed Spring Boot projects. Use when DSH needs to set up a new backend feature package, align package structure with existing modules, or add the baseline controller, application, domain, infrastructure, DTO, and VO layout before detailed implementation."
whenToUse: "创建后端 feature 模块包结构骨架"
---

# Spring Module Scaffolder

Create backend feature structure that matches the repository's real Spring Boot module conventions.

## Workflow

### 1. Read existing module structure first

Inspect:

- existing module packages under `backend/src/main/java/`
- neighboring modules with similar complexity
- current placement of:
  - `interfaces/rest`
  - `interfaces/dto`
  - `interfaces/vo`
  - `application/service`
  - `application/assembler`
  - `domain/service`
  - `infrastructure/persistence`

Do not impose a generic layered package layout if the repository already has one.

### 2. Scaffold only required layers

Create the minimum useful package/file structure for the target module. Typical deliverables may include:

- controller shell
- request / response DTO / VO shells
- application service shell
- domain service shell when rules justify it
- persistence mapper / entity shells only if needed now

Do not generate unused placeholder classes.

### 3. Match current naming conventions

Use repository-consistent naming for:

- controller classes
- application services
- policy / domain services
- mapper names
- request / response object names

### 4. Leave clean extension points

Set up the module so later implementation can plug into:

- security context access
- exception handling
- shared response wrapper
- mapper conventions
- tests

Avoid putting all first-pass logic into one oversized controller or service.

## Output Rules

- Follow current repository structure, not textbook Spring diagrams.
- Keep scaffolding lean.
- Use only classes and packages that are immediately useful.
- Prefer consistency over abstraction.

## Typical Deliverables

- new backend feature package structure
- minimal controller/service/DTO/VO skeletons
- repository-aligned module entry points ready for implementation

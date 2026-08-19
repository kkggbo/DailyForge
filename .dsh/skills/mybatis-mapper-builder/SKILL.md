---
name: mybatis-mapper-builder
description: "Implement or refactor MyBatis or MyBatis-Plus persistence mappers in repository-backed Spring Boot projects. Use when DSH needs to add mapper interfaces, SQL queries, query providers, result mappings, or persistence entities that align with the repository's current infrastructure and feature-specific data access style."
whenToUse: "实现或重构 MyBatis/MyBatis-Plus Mapper"
---

# MyBatis Mapper Builder

Build persistence mappings that align with the repository's existing MyBatis style and real feature queries.

## Workflow

### 1. Read current persistence patterns first

Inspect:

- neighboring mapper interfaces
- current entity classes
- query provider usage if present
- SQL annotation style vs XML style actually used in the repo

Do not impose a different MyBatis style if the repository already has a stable one.

### 2. Build for real query use cases

Each mapper method should correspond to a real access pattern, such as:

- get by id
- list by user and status
- paged search with filters
- relation batch load

Avoid speculative methods that no service calls.

### 3. Keep SQL explicit and readable

Queries should make clear:

- join path
- filter semantics
- pagination behavior
- sort order
- active/inactive visibility rules

When query semantics are subtle, prefer clarity over terse SQL.

### 4. Preserve entity and mapper boundaries

Mapper responsibilities:

- fetch and persist data
- not enforce business rules beyond query semantics

Do not move application orchestration into mapper code.

## Output Rules

- Match current repository mapper naming and annotation style.
- Use explicit selected columns when the query purpose is narrow.
- Keep mapper methods aligned with actual service needs.
- Prefer batch loading for one-to-many enrichment patterns when appropriate.

## Typical Deliverables

- new or updated mapper interfaces
- persistence entities
- query provider updates
- batch load and filtered search queries

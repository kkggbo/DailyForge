---
name: backend-ddd-writer
description: "Generate or update backend module DDD documents in a repository-specific style. Use when DSH needs to write a Detailed Design Document for a backend module before implementation, refresh an outdated module design doc after API or schema changes, or follow an existing project reference format while keeping the design aligned with the actual codebase, database schema, security model, and package structure."
whenToUse: "生成或更新后端模块 DDD 设计文档"
---

# Backend DDD Writer

## Overview

Use this skill to produce a backend module DDD that is concrete enough to guide implementation, code review, and later refactoring. The output must reflect the target repository's actual package layout, shared infrastructure, current database schema, and current API contract rather than a generic template.

## Workflow

### 1. Build context from the repository first

Read the target module's real context before drafting anything:

- module API doc under `docs/backend/<module_name>/<module_name>_接口文档.md` if it exists
- shared docs such as database design, DDL draft, backend infrastructure design
- actual code under the backend module package
- migration SQL, entities, mappers, security config, and shared error handling
- any user-provided reference doc or formatting example

Do not write the DDD from memory or from assumptions when the repository already contains the relevant source of truth.

### 2. Mirror the project's preferred DDD density and structure

If the repository already has a reference backend technical-solution document, mirror:

- section granularity
- table-heavy vs prose-heavy balance
- whether diagrams are used
- how implementation status is expressed
- how package structure and cross-module dependencies are shown

Do not copy domain nouns from the reference doc. Copy the structural style only.

### 3. Make the DDD implementation-facing

The DDD should be useful to the engineer who is about to code the module. Include concrete decisions, not vague goals.

At minimum, cover:

- scope and phase boundaries
- data model and key tables actually touched by the module
- state or lifecycle rules
- API list and implementation logic
- DTO / VO inventory and mapping conventions
- authentication / authorization behavior if relevant
- error code additions or mappings
- debug logging strategy and sensitive-data logging constraints
- Swagger / OpenAPI annotation conventions for controller, DTO, and VO layers
- transaction boundaries and concurrency handling
- Java package and class responsibilities
- configuration items
- extension points and known future constraints

Prefer naming exact tables, fields, package names, config keys, and planned classes when the repository gives enough signal.

### 4. Distinguish current state from planned state

- Mark existing infrastructure separately from components that still need to be built.
- Call out mismatches between docs and code when they affect implementation.
- Do not imply a feature is already implemented unless the repository proves it.

### 5. Write the doc in the module doc directory

Default output location:

- `docs/backend/<module_name>/<module_name>_DDD.md`

If the repository has module documentation governance rules, update them when the DDD requirement or naming convention changes.

## Output Checklist

Before finalizing, verify the DDD:

- matches the actual module name and package path
- references the real API paths
- references the real tables and fields
- does not contradict existing migrations or shared docs
- records non-obvious decisions such as token strategy, transaction scope, and concurrency control
- includes DTO / VO, debug log, and Swagger annotation guidance when the module exposes APIs
- is readable by a future engineer without opening a large amount of code first

## References

Read [references/backend-ddd-template.md](references/backend-ddd-template.md) when you need a section skeleton and a concrete content checklist for the final document.

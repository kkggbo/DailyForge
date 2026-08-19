---
name: seed-data-writer
description: "Create or update base seed data for repository-backed schemas. Use when DSH needs to add dictionary rows, bootstrap records, lookup tables, system defaults, or feature-supporting reference data that should be inserted through migration-aligned SQL and remain understandable to future developers."
whenToUse: "创建或更新基础种子数据"
---

# Seed Data Writer

Write seed data that supports the feature without turning migrations into opaque data dumps.

## Workflow

### 1. Read the feature and schema context first

Inspect:

- current migration files
- existing seed data files
- feature docs that explain why the data is needed

Do not insert reference data without understanding how the application will use it.

### 2. Distinguish seed data from user data

Use seed data for:

- fixed dictionaries
- system-defined categories
- bootstrap records
- demo or support data explicitly accepted by the project

Do not treat runtime business records as seed data unless the user has explicitly chosen that approach.

### 3. Keep seed data maintainable

Prefer:

- readable grouped inserts
- stable ids only when the repository already relies on fixed ids
- comments when value meaning is not obvious

Avoid huge unstructured insert blocks that future editors cannot safely modify.

### 4. Respect migration order and dependencies

Ensure referenced tables and foreign keys already exist before inserts run.

When seed data depends on prior rows, keep ordering obvious.

## Output Rules

- Match current SQL style and timestamp conventions.
- Keep inserts deterministic when the repository depends on stable reference values.
- Write only the seed data that the feature actually needs.

## Typical Deliverables

- migration inserts for dictionary and bootstrap data
- updates to existing seed datasets
- clearly grouped reference records for new feature support

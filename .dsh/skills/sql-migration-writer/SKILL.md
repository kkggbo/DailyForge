---
name: sql-migration-writer
description: "Write or update ordered SQL migration scripts in repository-backed backend projects. Use when DSH needs to add schema changes, alter existing tables, introduce new columns or constraints, or create migration steps that fit the repository's current db/migration versioning and rollout conventions."
whenToUse: "编写 SQL 迁移脚本（顺序 SQL）"
---

# SQL Migration Writer

Write migration scripts that match the repository's actual schema history and rollout style.

## Workflow

### 1. Read current schema history first

Inspect:

- existing files under `backend/src/main/resources/db/migration/`
- current table definitions referenced by the module
- related database design docs
- any already-confirmed product and contract decisions that drive the schema change

Do not write a migration in isolation from the current migration chain.

### 2. Design forward-only changes

Prefer changes that can be applied in sequence without rewriting migration history.

When changing existing schema, account for:

- existing data compatibility
- default values or backfill steps
- nullable vs non-null rollout
- foreign key ordering

Do not assume an empty database unless the user has explicitly accepted a destructive reset.

### 3. Keep migration scope explicit

A migration should clearly communicate:

- what changed
- why it changed
- whether it is destructive, additive, or transitional

If a migration is intentionally destructive, say so in comments and related docs.

### 4. Match repository style

Follow the current repository's conventions for:

- file naming
- SQL formatting
- comments
- charset and engine usage
- foreign key and index naming

## Output Rules

- Write plain SQL that matches the current migration folder style.
- Prefer explicit column definitions over shorthand.
- Keep statements grouped by table or migration purpose.
- Avoid mixing unrelated feature changes into one migration.

## Typical Deliverables

- new migration files under `backend/src/main/resources/db/migration/`
- adjusted migration comments and sequencing notes
- companion notes about destructive or compatibility-sensitive changes

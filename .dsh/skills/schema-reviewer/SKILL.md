---
name: schema-reviewer
description: "Review or refine database schema design in repository-backed application projects. Use when DSH needs to check table shape, column nullability, defaults, constraints, naming, archive strategy, snapshot usage, or relational consistency before or during migration authoring."
whenToUse: "审查数据库 Schema 设计"
---

# Schema Reviewer

Review schema design for correctness, maintainability, and fit with the product rules already decided.

## Workflow

### 1. Read the product and contract intent first

Inspect:

- PRD
- interface docs
- database design docs
- current migrations

Schema review must be grounded in the actual business model, not only normalized-database preferences.

### 2. Check the core data model decisions

Review:

- entity boundaries
- whether tables represent current state, history, snapshot, or association
- required vs optional fields
- delete/archive strategy
- status fields and state transitions

Flag places where the schema blurs business meaning.

### 3. Verify column-level decisions

Check:

- type size and precision
- nullability
- defaults
- unique constraints
- timestamp strategy
- identifier and snapshot naming clarity

### 4. Check consistency with current repository style

Schema should align with:

- existing primary key style
- existing datetime precision
- existing comment density
- existing enum storage style

Do not introduce a parallel schema dialect inside one project.

## Output Rules

- Focus on meaningful schema risks and clarity issues.
- Distinguish hard problems from optional cleanup.
- Tie recommendations back to product behavior when possible.

## Typical Deliverables

- schema review notes before migration writing
- concrete change recommendations for tables and columns
- clarifications on current-state vs history vs snapshot modeling

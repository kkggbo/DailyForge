---
name: naming-format-normalizer
description: "Normalize naming and formatting conventions across backend and frontend transport models. Use when DSH needs to align camelCase field names, enum value representation, date and time formatting, id naming, and code-vs-label conventions between interface docs, backend DTO/VO classes, and frontend types."
whenToUse: "规范化契约面命名与值格式"
---

# Naming Format Normalizer

Normalize contract-facing names and value formats so frontend, backend, and docs describe the same data consistently.

## Workflow

### 1. Read all contract surfaces

Inspect:

- interface docs
- backend DTO / VO classes
- frontend types when relevant
- assembler logic when value transformation occurs

### 2. Normalize the important conventions

Check and align:

- camelCase field names
- id field naming (`userId`, `templateId`, etc.)
- enum codes
- date / datetime format
- number precision expectations
- code field vs display label field semantics

### 3. Prefer contract stability

When inconsistencies exist, recommend the least disruptive contract correction that still improves clarity. Avoid gratuitous renames if the repository already shipped a stable public field.

### 4. Make transformation points explicit

If backend and frontend representations differ by design, ensure the conversion point is visible in assembler or mapping logic rather than silently scattered.

## Output Rules

- Favor consistency with current repository contract style.
- Be precise about formatting semantics.
- Distinguish internal naming from public contract naming.

## Typical Deliverables

- aligned field naming across DTO/VO and frontend types
- clarified date/time and enum formatting
- reduced contract ambiguity around ids, labels, and codes

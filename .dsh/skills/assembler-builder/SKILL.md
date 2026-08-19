---
name: assembler-builder
description: "Implement or refactor backend assembler and mapping logic between persistence entities, domain data, and transport DTO/VO objects. Use when DSH needs to keep conversion logic explicit, reusable, and repository-aligned without exposing entities directly through controllers."
whenToUse: "实现实体 / DTO / VO 之间的映射（Assembler）"
---

# Assembler Builder

Build explicit mapping logic between persistence and transport layers.

## Workflow

### 1. Read the source and target models first

Inspect:

- persistence entities
- request DTOs
- response VOs
- existing assembler classes in the repository

Do not write mapping code before the source and target semantics are clear.

### 2. Map by meaning, not by identical field name alone

When converting, consider:

- snapshot vs live field meaning
- ids vs display text
- enum code vs human-readable value
- summary vs detail payload scope

Do not assume same-name fields always mean the same thing.

### 3. Keep mapping logic centralized when reused

If the same conversion appears in multiple controller/service paths, keep it in an assembler or dedicated mapper helper rather than duplicating field copies.

### 4. Avoid leaking entities outward

Controllers should return VO objects, not persistence entities. Assemblers are the explicit boundary that makes this visible and maintainable.

## Output Rules

- Match the repository's current assembler style.
- Prefer readable manual mapping unless the project already standardizes a generator.
- Keep methods narrow and named by conversion direction.

## Typical Deliverables

- assembler classes under application layer
- entity-to-VO mapping methods
- DTO-to-domain or DTO-to-command conversion methods
- reduced controller and service field-copy noise

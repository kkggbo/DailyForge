---
name: index-designer
description: "Design and review SQL indexes for repository-backed application schemas. Use when DSH needs to add, remove, or evaluate indexes for new tables or changed queries, including uniqueness, covering opportunities, redundant indexes, and feature-specific query paths."
whenToUse: "设计数据库索引（按真实查询路径）"
---

# Index Designer

Design indexes based on real query paths and existing repository conventions.

## Workflow

### 1. Read the query patterns first

Before adding indexes, inspect:

- related API contract filters and sort requirements
- backend mapper SQL
- service-level lookup patterns
- existing index strategy in neighboring tables

Do not add indexes based only on table shape.

### 2. Map indexes to concrete access paths

For each proposed index, identify:

- query predicate columns
- sort columns
- uniqueness expectation
- whether an existing composite index already covers the access path

Avoid single-column indexes that are already covered by a stronger composite index unless there is a specific reason.

### 3. Review redundancy and cost

Check for:

- overlapping indexes
- duplicate uniqueness and regular indexes
- indexes on low-value columns without query use
- write-heavy cost of excessive indexing

### 4. Match migration and naming style

Follow the repository's current conventions for:

- index naming
- key ordering
- uniqueness expression

## Output Rules

- Explain indexes in terms of query behavior, not vague performance claims.
- Prefer a small, justified index set.
- Call out redundant indexes explicitly.

## Typical Deliverables

- index additions in migrations
- recommendations to remove redundant indexes
- query-to-index mapping notes for review

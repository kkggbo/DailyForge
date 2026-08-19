---
name: query-performance-checker
description: "Review backend persistence queries for performance risks in repository-backed application projects. Use when DSH needs to inspect SQL, mapper access patterns, pagination behavior, one-to-many loading strategy, or index fit to catch N+1 risks, unnecessary scans, or avoidable query amplification before integration."
whenToUse: "审查持久化查询性能（N+1/分页/索引）"
---

# Query Performance Checker

Review persistence access patterns for concrete performance risks relevant to the implemented feature.

## Workflow

### 1. Read the actual query path

Inspect:

- mapper SQL
- service orchestration around mapper calls
- expected list sizes and pagination
- one-to-many enrichment flow

Do not make performance claims without understanding the real query path.

### 2. Check common risk patterns

Review for:

- N+1 loading
- repeated per-row lookups
- missing pagination on large list endpoints
- sort without useful index support
- over-fetching all columns for lightweight list use cases
- redundant query passes for the same data

### 3. Match performance review to product usage

Prioritize the queries that matter for:

- list pages
- dashboard summaries
- repeated editor load paths
- activation flows that touch many rows

Do not over-optimize cold or rare code paths first.

### 4. Produce actionable guidance

Recommendations should be concrete, such as:

- batch load relation X
- add composite index Y
- replace per-item detail query with one grouped load
- narrow selected columns

## Output Rules

- Focus on likely real bottlenecks, not theoretical micro-optimizations.
- Tie performance feedback to specific methods and SQL.
- Distinguish blocking risks from later improvements.

## Typical Deliverables

- performance review notes for mappers and services
- concrete query and index improvement recommendations
- N+1 and pagination risk findings

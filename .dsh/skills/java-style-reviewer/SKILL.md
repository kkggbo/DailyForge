---
name: java-style-reviewer
description: "Review changed Java backend code for repository-consistent style and maintainability. Use when DSH needs to assess modified Spring Boot or Java files against project conventions and practical engineering quality, using Alibaba Java guidance as a fallback when the repository does not already settle the pattern."
whenToUse: "审查 Java 代码项目一致性（阿里规范兜底）"
---

# Java Style Reviewer

Review Java changes for consistency and maintainability, with repository style taking precedence over generic style dogma.

## Workflow

### 1. Read local Java patterns first

Inspect nearby code in the same module to understand:

- naming style
- method structure
- package layering
- logging style
- DTO / VO / assembler conventions

Only fall back to broader Java guidance when the repository does not already make the pattern clear.

### 2. Review maintainability-relevant style

Focus on:

- method readability
- naming clarity
- layer responsibility
- exception and logging consistency
- avoidable duplication
- overly dense conditional logic

Do not spend review budget on harmless formatting trivia.

### 3. Check for style that hides defects

Flag patterns such as:

- ambiguous method names
- too much work in controller methods
- confusing boolean parameters
- mixed responsibilities in one class
- magic strings or codes repeated across logic

### 4. Keep the review operational

Style feedback should help future maintainers understand and modify the code safely, not merely satisfy aesthetics.

## Output Rules

- Prefer concrete maintainability findings.
- Cite files and methods.
- Distinguish repository-style mismatch from optional cleanup.
- Use Alibaba Java guidance only where repository style is silent.

## Typical Deliverables

- Java maintainability review findings
- layering and naming issues in changed backend code
- consistency feedback on new or refactored classes

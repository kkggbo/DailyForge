---
name: form-interaction-designer
description: "Design and implement form interaction behavior in repository-backed frontend projects. Use when DSH needs to build or refine validation flow, dirty-state handling, reset and undo behavior, modal forms, inline editing, conditional sections, or user-friendly input interactions beyond simple static forms."
whenToUse: "设计表单交互行为（验证/脏状态/保存）"
---

# Form Interaction Designer

Implement forms that are efficient and predictable for real user behavior, not just structurally valid.

## Workflow

### 1. Understand the real workflow of the form

Before editing the form, identify:

- what the user is trying to finish
- which fields are core vs optional
- what is saved immediately vs saved manually
- whether there is dirty state, draft state, reset, or undo

### 2. Make validation behavior deliberate

Clarify:

- field-level validation
- submit-level validation
- required vs optional fields
- when to show errors

Do not show aggressive validation noise before the user has meaningfully interacted.

### 3. Optimize repeated input

When the user repeats similar actions, prefer interaction shortcuts such as:

- copy previous item
- preserve previous selection
- collapse optional fields behind explicit actions
- use modal selection instead of repeated inline search when appropriate

### 4. Keep mutable UI state separate from saved domain state

Separate:

- local UI toggles
- transient search/filter state
- persisted form values

This is especially important for dialogs, drafts, and unsaved changes warnings.

## Output Rules

- Follow existing repository form patterns first.
- Keep interactions predictable.
- Bias toward reducing repeated work and cognitive load.
- Prefer explicit confirmation for destructive actions.

## Typical Deliverables

- improved form state flow
- clearer validation behavior
- better reset / undo / dirty-state handling
- progressive disclosure for optional sections

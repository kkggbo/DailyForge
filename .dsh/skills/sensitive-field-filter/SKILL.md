---
name: sensitive-field-filter
description: "Prevent sensitive backend data from leaking through DTO, VO, and mapping logic. Use when DSH needs to review or implement transport models and assemblers so secrets, internal auth data, passwords, or restricted fields are excluded, nulled, or withheld from public API responses."
whenToUse: "防止敏感字段通过 DTO/VO 泄露"
---

# Sensitive Field Filter

Ensure transport-layer models do not expose fields that should stay internal.

## Workflow

### 1. Read the actual model boundaries first

Inspect:

- persistence entities
- DTO / VO classes
- assembler logic
- representative controller outputs

Do not assume "not currently used by frontend" is enough protection.

### 2. Identify sensitive categories

Examples include:

- passwords
- password hashes
- tokens
- invite secrets
- internal-only flags
- audit-only values not meant for clients

Also consider fields that are not secret but still should not be publicly exposed because they create coupling or misuse risk.

### 3. Filter at the transport boundary

Prefer not to include sensitive fields in VOs at all. If a field must exist for compatibility reasons, ensure the exposed value is intentionally controlled.

### 4. Re-check indirect leaks

Look for:

- debug-style raw entity returns
- auto-generated `toString` or serialization leakage in response objects
- nested child objects exposing more than intended

## Output Rules

- Remove or exclude sensitive fields rather than relying on frontend discipline.
- Keep the exposure decision explicit.
- Align with repository auth and profile safety expectations.

## Typical Deliverables

- filtered DTO / VO design
- safer assembler logic
- removal of accidental sensitive field exposure

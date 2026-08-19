---
name: security-risk-scanner
description: "Review changed code for concrete security issues in repository-backed applications. Use when DSH needs to inspect modified backend or frontend code for SQL injection, XSS, auth bypass, unsafe exposure, hardcoded secrets, insecure trust assumptions, or contract changes that weaken security behavior."
whenToUse: "应用层安全风险审查"
---

# Security Risk Scanner

Review security risk in the changed code with a practical, application-focused lens.

## Workflow

### 1. Identify the security surface of the change

Check whether the diff touches:

- request handling
- persistence queries
- auth and permission logic
- user-generated content rendering
- secret or token flow
- admin or invite-gated capabilities

### 2. Review the most relevant security classes

Look for:

- SQL injection risk through unsafe query composition
- XSS risk from unsanitized rendering or HTML injection
- permission or ownership bypass
- hardcoded secrets or credentials
- data exposure through DTO / VO leakage
- missing auth requirement on sensitive endpoints
- overbroad trust in client-provided fields

### 3. Anchor findings in exploit paths

For each issue, explain:

- what attacker or user input path exists
- what trust boundary is crossed
- what the consequence is

Avoid abstract "security best practice" advice with no exploit path.

### 4. Prioritize by damage potential

Classify:

- high: exploit likely causes unauthorized access, secret leakage, or destructive abuse
- medium: meaningful weakness with constrained exploitability
- low: limited but real hardening gap

## Output Rules

- Keep findings specific and scenario-driven.
- Cite affected files.
- Focus on changed code, not whole-project hardening wishes.
- If no meaningful security issue is found, say so explicitly.

## Typical Deliverables

- security findings on changed code
- auth / permission review notes
- DTO exposure and unsafe query concerns

---
name: api-mock-builder
description: "Build or refine API mocks for repository-backed frontend tests. Use when DSH needs to create or update mock handlers, mock payloads, success and failure responses, or feature-specific network test doubles that align with the latest interface docs and frontend API contracts."
whenToUse: "构建前端测试用 API Mock（MSW），贴近真实契约"
---

# API Mock Builder

Create API mocks that reflect the real contract closely enough to support meaningful frontend tests.

## Workflow

### 1. Read the contract and frontend client first

Inspect:

- interface docs
- frontend API client wrappers
- target test scenarios
- existing mock infrastructure such as MSW handlers

Do not mock guessed payloads when contract docs already exist.

### 2. Mock the scenarios the feature actually depends on

Prefer mocks for:

- normal success response
- empty response when it changes UI behavior
- validation or business failure response
- network or server failure when the UI has error handling

### 3. Keep payloads realistic

Mock payloads should preserve:

- field names
- nullability
- enum values
- wrapper structure
- pagination shape when relevant

Do not oversimplify mocks so much that they hide integration issues.

### 4. Organize mocks by feature meaning

Group handlers and fixtures around scenarios rather than giant generic mock dumps.

## Output Rules

- Use the repository's existing mock style and tooling.
- Keep mock data readable.
- Align closely with current API docs and frontend types.
- Prefer scenario-specific handlers over one mega handler with many branches.

## Typical Deliverables

- new or updated MSW handlers
- realistic feature fixtures
- success / empty / failure mock paths for frontend tests

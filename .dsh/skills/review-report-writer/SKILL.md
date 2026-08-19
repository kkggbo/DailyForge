---
name: review-report-writer
description: "Produce concise risk-ranked review reports from incremental code review findings. Use when DSH needs to turn technical review observations into a clear report with high, medium, and low risk sections, overall release impact, and actionable next steps for submit or rollback decisions."
whenToUse: "汇总审查发现为高/中/低风险报告"
---

# Review Report Writer

Turn review findings into a decision-ready report for the main control session.

## Workflow

### 1. Gather the actual findings first

Start from:

- diff review findings
- bug risk findings
- security findings
- style or comment findings that materially matter

Do not inflate an empty review into a long report.

### 2. Group by risk, not by source tool

Organize findings into:

- high risk
- medium risk
- low risk

Within each risk level, present the most important issues first.

### 3. Make the decision implication obvious

The report should help answer:

- can this change proceed
- what blocks submission
- what is safe to defer

### 4. Keep the report operational

For each finding, include:

- file or area
- concise issue statement
- impact
- recommended next action

## Output Rules

- Findings first, summary second.
- Keep the report concise and decision-oriented.
- Explicitly state when risk is low enough to proceed.
- If there are no findings, say so clearly.

## Typical Deliverables

- high/medium/low risk code review report
- submission gate summary
- concise rollback-or-proceed recommendation

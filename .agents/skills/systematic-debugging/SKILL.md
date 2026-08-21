---
name: systematic-debugging
description: Use when debugging issues, crashes, failing tests, or unexpected runtime behaviors. Mandates a 4-phase root-cause analysis process to isolate the real issue before attempting fixes.
---

# Systematic Debugging Superpower

Mandates a structured root-cause analysis process. Never guess or apply speculative patches.

## The 4 Phases of Debugging

### 1. Root Cause Tracing
- Reproduce the failure consistently.
- Collect full stack traces, log outputs, and exact inputs that triggered the defect.
- Identify the exact line, contract violation, or state mismatch causing the failure.

### 2. Pattern Analysis
- Inspect similar usages across the codebase.
- Check if this is an isolated bug or part of a systemic architectural pattern.

### 3. Hypothesis & Testing
- Formulate a testable hypothesis explaining *why* the bug occurred.
- Write a failing reproduction test that isolates this exact condition.

### 4. Minimal Targeted Fix & Verification
- Apply the minimal code change that satisfies the reproduction test.
- Re-run full test suites to ensure no regressions are introduced.
- Remove temporary debug logs or scaffolding.

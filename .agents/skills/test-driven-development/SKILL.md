---
name: test-driven-development
description: Enforce a strict Test-Driven Development (TDD) cycle (Red-Green-Refactor) for implementing logic, fixing bugs, and validating contracts. Use when creating new classes, repositories, ViewModels, parsers, or fixing runtime defects.
---

# Test-Driven Development (TDD) Superpower

Enforces a disciplined **RED-GREEN-REFACTOR** cycle.

## Core Rules

1. **Never write production code before a failing test exists.**
2. **Every test must fail first** for the expected reason (verify the failure output).
3. **Write the minimum code** necessary to pass the test.
4. **Refactor** with the safety net of passing tests.

## The TDD Cycle

```
  ┌────────────────────────────────────────────────┐
  │ 1. RED: Write a small, targeted failing test   │
  └──────────────────────┬─────────────────────────┘
                         ▼
  ┌────────────────────────────────────────────────┐
  │ 2. VERIFY: Run the test and observe failure    │
  └──────────────────────┬─────────────────────────┘
                         ▼
  ┌────────────────────────────────────────────────┐
  │ 3. GREEN: Write minimal code to pass test      │
  └──────────────────────┬─────────────────────────┘
                         ▼
  ┌────────────────────────────────────────────────┐
  │ 4. REFACTOR: Clean up code while tests stay 🟢 │
  └────────────────────────────────────────────────┘
```

## Checklist for Each Feature / Fix

- [ ] Define the specific behavior or contract to test.
- [ ] Place tests in the corresponding source set (e.g. `shared/src/commonTest/kotlin/...`).
- [ ] Run test using `./gradlew :shared:allTests` or `./gradlew testDebugUnitTest`.
- [ ] Confirm failure is due to missing logic, not compilation/syntax errors.
- [ ] Implement the simplest logic to make the test pass.
- [ ] Verify test passes.
- [ ] Refactor formatting, naming, and structure without altering test behavior.

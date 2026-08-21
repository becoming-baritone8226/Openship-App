---
name: verification-before-completion
description: Mandate end-to-end verification and testing before marking a task or milestone complete. Use before concluding any implementation step.
---

# Verification Before Completion Superpower

Ensures all requirements, tests, and build checks are verified before declaring work complete.

## Verification Checklist

1. **Build Integrity**:
   - Run compilation tasks (`./gradlew assembleDebug` or `./gradlew compileCommonMainKotlinMetadata`).
   - Ensure zero build warnings/errors related to new code.

2. **Automated Test Execution**:
   - Run unit and integration test suites (`./gradlew test` / `./gradlew allTests`).
   - Confirm all assertions pass.

3. **Behavioral Inspection**:
   - Review log outputs, state changes, and responses to ensure expected behavior matches the spec.

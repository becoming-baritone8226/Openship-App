---
name: subagent-driven-development
description: Use when decomposing large tasks across specialized subagents (e.g. codebase researcher, test runner, accessibility auditor) to run parallel, focused investigation and execution.
---

# Subagent-Driven Development Superpower

Enables disciplined delegation to specialized subagents.

## Core Rules

1. **Clear Task Boundaries**: Provide each subagent with a self-contained, specific prompt and explicit output expectations.
2. **Context Isolation**: Use subagents for heavy search, broad documentation lookup, or background verification to keep the primary agent context clean.
3. **Synthesis**: Primary agent reviews subagent findings, reconciles diffs, and verifies end-to-end integration.

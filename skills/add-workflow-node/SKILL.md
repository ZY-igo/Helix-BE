---
name: add-workflow-node
description: Add or update a workflow node in the Helix workflow framework. Use when Codex needs to introduce a new DSL node type, extend node config, implement node compilation, wire node runtime execution, or verify what files and contracts must change for a new workflow node in this project.
---

# Add Workflow Node

Add new workflow nodes by following the Helix split between DSL definition, compilation, runtime execution, and Temporal integration.

Read [references/helix-workflow-node-guide.md](./references/helix-workflow-node-guide.md) before editing workflow-node code. Use it as the primary checklist.

## Workflow

1. Classify the node.
   Determine whether the new node is:
   - a pure control-flow node
   - a data-producing node
   - a human-wait node
   - an external side-effect node

2. Define the DSL contract.
   Add or update the node type enum and decide the minimum config shape the DSL must provide.

3. Implement compilation.
   Add a `NodeCompiler` under the node package.
   Validate required config fields aggressively.
   Convert raw DSL config into runtime-friendly compiled config.
   Precompile expressions at compile time when possible.

4. Implement runtime execution.
   Add a `WorkflowNodeExecutor`.
   Keep workflow-thread logic deterministic.
   Route external I/O through `WorkflowRuntimeBridge`, not directly from workflow code.

5. Verify transitions and outputs.
   Decide whether the node chooses `nextNodeId` directly or returns a `branchKey`.
   Decide what output variables are emitted and whether names can collide with existing workflow variables.

6. Test the node.
   Add compiler tests for invalid and valid DSL.
   Add executor tests for status, output, and branch behavior.

## Files To Touch

Always inspect these areas when adding a node:

- `src/main/java/com/sipc115/helix/domain/workflow/DslNodeType.java`
- `src/main/java/com/sipc115/helix/context/node/<node>/`
- `src/main/java/com/sipc115/helix/integration/workflow/compiler/`
- `src/main/java/com/sipc115/helix/integration/workflow/runtime/`
- `src/test/java/com/sipc115/helix/integration/workflow/compiler/`

Inspect Temporal workflow files only if the node needs engine-specific behavior such as signals, activity calls, sleep, or child workflow support.

## Decision Rules

- Prefer compile-time validation over runtime guessing.
- Prefer storing normalized, compiled values in `CompiledNode.config`.
- Prefer executor logic that only reads compiled config and `ExecutionContext`.
- Prefer activity routing for side effects.
- Avoid direct Spring-bean assumptions inside Temporal workflow implementation classes.
- Avoid adding node-specific logic to `AbstractWorkflowOrchestrator` unless the abstraction itself is incomplete.

## Project-Specific Constraints

- Treat `NodeCompilerRegistry` and `NodeExecutorRegistry` as the primary extension points.
- Preserve the existing separation between DSL compile phase and execution phase.
- Respect the current branch model: executors either set `nextNodeId` or a `branchKey`.
- Account for current framework gaps called out in the reference guide before relying on them.


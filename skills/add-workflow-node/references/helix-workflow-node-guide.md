# Helix Workflow Node Guide

## Purpose

Use this guide when implementing a new workflow node in Helix. The goal is to preserve the existing architecture:

`WorkflowDsl` -> `DefaultDslCompiler` -> `ExecutionPlan` -> `AbstractWorkflowOrchestrator` -> node executor -> `TransitionResolver`

## Current Architecture

### Core flow

1. DSL is saved and compiled by `WorkflowDefinitionApplicationService`.
2. `DefaultDslCompiler` validates the graph, compiles each node through `NodeCompilerRegistry`, and emits `ExecutionPlan`.
3. `WorkflowExecutionApplicationService` loads the plan and starts a Temporal workflow.
4. `AbstractWorkflowOrchestrator.executePlan()` loops through compiled nodes and calls the executor registered for each node type.
5. `TransitionResolver` picks the next node when the executor does not set `nextNodeId`.

### Extension points

- `DslNodeType`: add the enum entry.
- `NodeCompiler`: define DSL validation and compile output.
- `WorkflowNodeExecutor`: define runtime behavior.
- `WorkflowRuntimeBridge`: use when the node needs activities, sleep, human signal waiting, or future child workflow support.

## Required Work For A New Node

### 1. Add the node type

Update `DslNodeType` with the new enum value.

Check whether any tests or clients assume a fixed set of node types.

### 2. Create a node package

Prefer the existing pattern:

`src/main/java/com/sipc115/helix/context/node/<node-name>/`

Typical files:

- `<NodeName>NodeCompiler.java`
- `<NodeName>NodeExecutor.java`
- optional config DTOs if the config is non-trivial
- optional definition classes if the node follows an existing package pattern

### 3. Implement compile-time validation

In the compiler:

- reject missing config
- reject malformed field types
- reject empty required strings and collections
- compile expressions here, not during workflow execution, when the project already supports it
- normalize config into a shape that the executor can read directly

Use `CompileContext` if the node needs:

- expression engine access
- metadata access
- node index access
- declared variable inspection

### 4. Implement runtime execution

In the executor:

- implement `supports()` with the exact `DslNodeType.name()`
- read only `CompiledNode`, `ExecutionContext`, and `WorkflowRuntimeBridge`
- return `NodeExecutionResult.completed()` unless the node has a different lifecycle
- set `output`, `branchKey`, or `nextNodeId` explicitly when needed

### 5. Decide transition semantics

Use one of these patterns:

- linear node: return completed result and rely on graph edge order
- branch node: set `branchKey`
- direct-jump node: set `nextNodeId`
- waiting node: return waiting status or block through bridge semantics, depending on the existing node pattern

## Important Project-Specific Notes

### Determinism boundary

The project runs orchestration in Temporal. Do not add non-deterministic logic directly in workflow classes or executors if that logic runs on the workflow thread.

Do not:

- call external systems directly from workflow code
- read mutable global process state
- use random values or wall-clock time without Temporal APIs

Prefer:

- `bridge.invokeActivityTask(...)` for side effects
- `bridge.durableSleep(...)` for delays
- `bridge.awaitHumanSignal(...)` for signal waits

### Current gaps to account for

- Condition-node compilation is stronger than condition-node execution. The current `ConditionNodeExecutor` uses `defaultBranch` instead of evaluating the compiled expression. Do not model a new branch node by copying that limitation unless the feature is intentionally static.
- `DslOrchestratorWorkflowImpl.currentState()` currently returns a minimal empty view. Do not rely on query state for node correctness.
- `invokeChildWorkflow()` is still a placeholder. Avoid designing a new node that depends on real child-workflow execution unless that bridge is implemented first.
- `WorkflowExecutors` uses static fields to expose Spring-managed registries to Temporal workflow instances. Avoid introducing additional static mutable state.
- Execution outputs are flattened into workflow variables via `context.getVariables().putAll(...)`. Be deliberate about output names to avoid collisions.

### Variable contract

Node outputs are stored twice:

- under the node id as a nested map
- flattened into global variables

When defining output keys:

- avoid generic names like `result`, `data`, or `status` unless they are intentionally global
- prefer stable, domain-specific keys
- document the emitted shape in the compiler config or node package comments if the output is complex

### Config shape

Prefer config DTOs when:

- the node has nested structures
- the node has multiple optional branches
- the node contains reusable policy fields

Prefer raw map passthrough only for very simple nodes.

### Where to keep special logic

Put logic in the smallest layer that can own it safely:

- DSL schema and normalization: compiler
- engine-agnostic execution behavior: executor
- Temporal-specific execution primitives: `WorkflowRuntimeBridge` implementation
- generic traversal and status transitions: orchestrator

Avoid pushing node-specific branches into `AbstractWorkflowOrchestrator`.

## Suggested Implementation Checklist

1. Add enum entry in `DslNodeType`.
2. Create node package under `context/node`.
3. Add compiler with validation and compiled config shaping.
4. Add executor with deterministic runtime behavior.
5. Confirm Spring auto-discovery works through `@Component` where applicable.
6. Confirm registry lookup uses the intended type name.
7. Confirm branch behavior matches graph edges.
8. Confirm output keys do not pollute existing workflow variables.
9. Add compiler tests for:
   - valid node config
   - missing required fields
   - malformed field types
   - invalid expressions if applicable
10. Add executor tests for:
   - completed status
   - waiting status if applicable
   - branch key or next node selection
   - output map shape

## Anti-Patterns

- Adding a node type without a compiler.
- Adding a compiler that only copies raw config without validation.
- Performing external calls directly in workflow logic.
- Emitting ambiguous output keys that can overwrite unrelated variables.
- Depending on `currentState()` for correctness.
- Designing a node around child workflows before the bridge is real.
- Fixing node-specific behavior by hardcoding branches in orchestrator code.

## Minimal Node Template

Use this shape as the default mental model:

1. `DslNodeType` enum entry
2. `@Component` compiler:
   - `supportType()`
   - `validate(...)`
   - `compile(...)`
3. `@Component` executor:
   - `supports(...)`
   - `execute(...)`
4. tests

If the node is only a compile-time variation of an existing runtime behavior, prefer extending compiled config and reusing an executor rather than duplicating executors.

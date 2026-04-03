/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.*;

import com.sipc115.helix.integration.workflow.engine.TemporalWorkflowRuntimeBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DslOrchestratorWorkflowImpl {

    private static volatile NodeExecutorRegistry defaultNodeExecutorRegistry;
    private static volatile TransitionResolver defaultTransitionResolver;

    private final List<HumanSignalPayload> bufferedSignals = new ArrayList<>();
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final TransitionResolver transitionResolver;
    private ExecutionContext currentContext;

    public static void configureDefaults(
            NodeExecutorRegistry nodeExecutorRegistry,
            TransitionResolver transitionResolver
    ) {
        defaultNodeExecutorRegistry = nodeExecutorRegistry;
        defaultTransitionResolver = transitionResolver;
    }

    public DslOrchestratorWorkflowImpl() {
        this(
                defaultNodeExecutorRegistry != null ? defaultNodeExecutorRegistry : NodeExecutorRegistryHolder.INSTANCE,
                defaultTransitionResolver != null ? defaultTransitionResolver : TransitionResolverHolder.INSTANCE
        );
    }

    public DslOrchestratorWorkflowImpl(NodeExecutorRegistry nodeExecutorRegistry, TransitionResolver transitionResolver) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.transitionResolver = transitionResolver;
    }

    public void run(ExecutionPlan plan, Map<String, Object> input) {
        WorkflowRuntimeBridge bridge = createWorkflowRuntimeBridge();
        ExecutionContext context = new ExecutionContext(plan, input);
        this.currentContext = context;

        Long executionId = extractExecutionId(input);
        if (executionId != null) {
            context.setExecutionId(executionId);
        }

        executePlan(plan, context, bridge);
    }

    private Long extractExecutionId(Map<String, Object> input) {
        if (input == null) {
            return null;
        }
        Object executionId = input.get("_executionId");
        if (executionId instanceof Long) {
            return (Long) executionId;
        } else if (executionId instanceof Integer) {
            return ((Integer) executionId).longValue();
        } else if (executionId instanceof String) {
            try {
                return Long.parseLong((String) executionId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private WorkflowRuntimeBridge createWorkflowRuntimeBridge() {
        return new TemporalWorkflowRuntimeBridge(bufferedSignals);
    }

    private void executePlan(
            ExecutionPlan plan,
            ExecutionContext context,
            WorkflowRuntimeBridge bridge
    ) {
        String currentNodeId = plan.getEntryNodeId();
        context.setWorkflowStatus(ExecutionStatus.RUNNING);

        while (currentNodeId != null) {
            context.setCurrentNodeId(currentNodeId);
            CompiledNode node = plan.getNodes().get(currentNodeId);
            context.getNodeStatuses().put(currentNodeId, ExecutionStatus.RUNNING);

            context.incrementExecutionOrder();

            WorkflowNodeExecutor executor = nodeExecutorRegistry.get(node.getType().name());
            NodeExecutionResult result = executor.execute(node, context, bridge);

            if (result.getOutput() != null && !result.getOutput().isEmpty()) {
                context.getVariables().put(node.getId(), result.getOutput());
                context.getVariables().putAll(result.getOutput());
            }

            updateStatus(context, currentNodeId, result);

            if ("END".equals(node.getType().name())) {
                context.setWorkflowStatus(ExecutionStatus.COMPLETED);
                return;
            }

            currentNodeId = result.getNextNodeId() != null
                    ? result.getNextNodeId()
                    : transitionResolver.nextNode(plan, node.getId(), result.getBranchKey());
        }

        context.setWorkflowStatus(ExecutionStatus.COMPLETED);
    }

    private void updateStatus(ExecutionContext context, String nodeId, NodeExecutionResult result) {
        if (result.getStatus() == ExecutionStatus.WAITING_SIGNAL) {
            context.setWorkflowStatus(ExecutionStatus.WAITING_SIGNAL);
            context.getNodeStatuses().put(nodeId, ExecutionStatus.WAITING_SIGNAL);
        } else if (result.getStatus() == ExecutionStatus.COMPLETED) {
            context.getNodeStatuses().put(nodeId, ExecutionStatus.COMPLETED);
            context.setWorkflowStatus(ExecutionStatus.RUNNING);
        } else {
            context.getNodeStatuses().put(nodeId, result.getStatus());
            context.setWorkflowStatus(result.getStatus());
        }
    }

    public void provideHumanInput(HumanSignalPayload payload) {
        bufferedSignals.add(payload);
    }

    public WorkflowStateView currentState() {
        return currentContext != null ? currentContext.toView() : new WorkflowStateView();
    }

    private static class NodeExecutorRegistryHolder {
        static NodeExecutorRegistry INSTANCE = new NodeExecutorRegistry(List.of());
    }

    private static class TransitionResolverHolder {
        static TransitionResolver INSTANCE = new TransitionResolver();
    }
}

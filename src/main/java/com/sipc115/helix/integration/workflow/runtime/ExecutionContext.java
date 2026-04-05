package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Data
public class ExecutionContext {

    private ExecutionPlan plan;
    private Map<String, Object> variables = new TreeMap<>();
    private Map<String, ExecutionStatus> nodeStatuses = new TreeMap<>();
    private String currentNodeId;
    private ExecutionStatus workflowStatus = ExecutionStatus.PENDING;
    private Long executionId;
    private Integer executionOrder = 0;
    private Long currentNodeTraceId;
    private Map<String, Object> expressionCache = new TreeMap<>();
    private Set<String> neededNodeOutputs = new TreeSet<>();

    public ExecutionContext() {
    }

    public ExecutionContext(ExecutionPlan plan, Map<String, Object> input) {
        this.plan = plan;
        if (input != null) {
            this.variables.putAll(input);
        }
    }

    public void incrementExecutionOrder() {
        this.executionOrder++;
    }

    public void clearExpressionCache() {
        this.expressionCache.clear();
    }

    public Object getCachedExpression(String expression) {
        return expressionCache.get(expression);
    }

    public void cacheExpressionResult(String expression, Object result) {
        expressionCache.put(expression, result);
    }

    public void cleanupNodeOutputIfNotNeeded(String nodeId, Map<String, Object> output) {
        if (!neededNodeOutputs.contains(nodeId)) {
            if (output != null) {
                output.clear();
            }
        }
    }

    public void cleanupVariablesForNode(String nodeId) {
        Set<String> keysToRemove = new TreeSet<>();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(nodeId + ".")) {
                keysToRemove.add(key);
            }
        }
        keysToRemove.forEach(variables::remove);
    }

    public WorkflowStateView toView() {
        WorkflowStateView view = new WorkflowStateView();
        if (plan != null) {
            view.setWorkflowId(plan.getWorkflowId());
        }
        view.setCurrentNodeId(currentNodeId);
        view.setStatus(workflowStatus);
        view.getVariables().putAll(variables);
        view.getNodeStatuses().putAll(nodeStatuses);
        return view;
    }
}

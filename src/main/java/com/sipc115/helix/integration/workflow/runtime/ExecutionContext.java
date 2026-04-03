package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ExecutionContext {

    private ExecutionPlan plan;
    private Map<String, Object> variables = new HashMap<>();
    private Map<String, ExecutionStatus> nodeStatuses = new HashMap<>();
    private String currentNodeId;
    private ExecutionStatus workflowStatus = ExecutionStatus.PENDING;
    private Long executionId;
    private Integer executionOrder = 0;
    private Long currentNodeTraceId;

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

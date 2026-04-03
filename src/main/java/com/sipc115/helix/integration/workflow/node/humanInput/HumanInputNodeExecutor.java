/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HumanInputNodeExecutor implements WorkflowNodeExecutor {

    private static WorkflowTraceService traceService;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        HumanInputNodeExecutor.traceService = traceService;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.HUMAN_INPUT.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        NodeExecutionTraceEntity trace = null;
        if (traceService != null && context.getExecutionId() != null) {
            try {
                trace = traceService.startNodeExecution(
                    context.getExecutionId(),
                    node.getId(),
                    node.getType().name(),
                    "NORMAL",
                    context.getExecutionOrder(),
                    context.getVariables()
                );
                context.setCurrentNodeTraceId(trace.getId());
            } catch (Exception e) {
            }
        }

        HumanSignalPayload payload = bridge.awaitHumanSignal(node.getId());

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setOutput(payload.getPayload());

        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), result.getOutput());
            } catch (Exception e) {
            }
        }

        return result;
    }
}

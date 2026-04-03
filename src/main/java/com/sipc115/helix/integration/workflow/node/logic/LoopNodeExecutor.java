package com.sipc115.helix.integration.workflow.node.logic;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import io.temporal.workflow.ChildWorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoopNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(LoopNodeExecutor.class);
    private static WorkflowTraceService traceService;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        LoopNodeExecutor.traceService = traceService;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.LOOP.name().equals(type);
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

        log.info("Executing loop node: {}", node.getId());

        Map<String, Object> config = node.getConfig();
        Integer maxRounds = getIntValue(config, "maxRounds", 10);

        Map<String, Object> loopInput = new HashMap<>(context.getVariables());
        CompiledExpression compiledExitCondition = (CompiledExpression) config.get("compiledExitCondition");
        loopInput.put("compiledExitCondition", compiledExitCondition);

        Object exitConditionObj = config.get("exitCondition");
        if (exitConditionObj instanceof String) {
            loopInput.put("exitCondition", exitConditionObj);
        }

        String taskQueue = (String) config.getOrDefault("taskQueue", "helix-task-queue");
        String workflowId = "loop-" + node.getId() + "-" + System.currentTimeMillis();

        ChildWorkflowOptions childWorkflowOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build();

        LoopSubWorkflow loopWorkflow = io.temporal.workflow.Workflow.newChildWorkflowStub(
                LoopSubWorkflow.class,
                childWorkflowOptions
        );

        LoopResult result = loopWorkflow.execute(maxRounds, loopInput);

        context.getVariables().put("loopIterations", result.getIterations());
        context.getVariables().put("loopLastResult", result.getLastResult());
        context.getVariables().put("loopExitReason", result.getExitReason());

        NodeExecutionResult executionResult = new NodeExecutionResult();
        executionResult.setStatus(ExecutionStatus.COMPLETED);
        executionResult.setOutput(result.toOutput());

        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), executionResult.getOutput());
            } catch (Exception e) {
            }
        }

        return executionResult;
    }

    private Integer getIntValue(Map<String, Object> config, String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}

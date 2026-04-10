/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.logic.loop;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import io.temporal.workflow.ChildWorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoopNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(LoopNodeExecutor.class);

    @Override
    public boolean supports(String type) {
        return DslNodeType.LOOP.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        log.info("开始执行循环节点: {}", node.getId());

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

        LoopResult loopResult = loopWorkflow.execute(maxRounds, loopInput);

        Map<String, Object> output = new HashMap<>();
        output.put("iterations", loopResult.getIterations());
        output.put("lastResult", loopResult.getLastResult());
        output.put("exitReason", loopResult.getExitReason());
        output.put("success", loopResult.isSuccess());

        NodeExecutionResult executionResult = new NodeExecutionResult();
        executionResult.setStatus(ExecutionStatus.COMPLETED);
        executionResult.setOutput(output);

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

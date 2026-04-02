/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.Return.ReturnStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskExecutionException;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 返回步骤执行器
 */
@Component
public class ReturnStepExecutor implements AiStepExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReturnStepExecutor.class);

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            ReturnStepConfig returnConfig = (ReturnStepConfig) stepConfig;

            // 1. 解析结果表达式
            // 2. 构建返回结果
            // 3. 更新任务状态

            Map<String, Object> result = new java.util.HashMap<>();
            if (returnConfig.getResult() != null) {
                for (Map.Entry<String, Object> entry : returnConfig.getResult().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    Object resolvedValue = resolveValue(value, state);
                    result.put(key, resolvedValue);
                }
            }

            // 更新任务状态
            state.getVars().put("result", result);
            outputSnapshot = result;

            log.info("Return step completed with {} result fields", result.size());

        } catch (AiTaskExecutionException e) {
            log.error("AI task execution failed at return step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = e.getOriginalMessage();

        } catch (Exception e) {
            log.error("Unexpected error in return step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = "系统异常：" + e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        StepTrace trace = new StepTrace(
            stepConfig.getId(),
            stepConfig.getType(),
            status,
            null,
            outputSnapshot,
            durationMs
        );

        if (errorMessage != null) {
            trace.setErrorMessage(errorMessage);
        }

        return trace;
    }

    /**
     * 解析值
     * <p>
     * 解析表达式或直接返回值。
     *
     * @param value 原始值
     * @param state 任务状态
     * @return 解析后的值
     */
    private Object resolveValue(Object value, AiTaskState state) {
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.startsWith("${") && strValue.endsWith("}")) {
                // 提取变量名
                String varName = strValue.substring(2, strValue.length() - 1);
                // 从任务状态中获取变量值
                return state.getVars().get(varName);
            }
        }
        return value;
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.RETURN.name();
    }
}

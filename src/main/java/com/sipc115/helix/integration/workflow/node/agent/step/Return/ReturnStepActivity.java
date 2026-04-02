/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Return;

import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回步骤 Activity
 * <p>
 * 执行返回步骤，将结果返回给调用者
 *
 * @author Helix Team
 * @since 2.0.0
 */
@ActivityInterface
@Component
public class ReturnStepActivity implements AiStepActivity {

    @Override
    @ActivityMethod
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            ReturnStepConfig returnConfig = (ReturnStepConfig) stepConfig;

            // 1. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            if (returnConfig.getResult() != null) {
                for (Map.Entry<String, Object> entry : returnConfig.getResult().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // 解析表达式
                    if (value instanceof String) {
                        String valueStr = (String) value;
                        if (valueStr.startsWith("${") && valueStr.endsWith("}")) {
                            String varName = valueStr.substring(2, valueStr.length() - 1).trim();
                            value = state.getVars().get(varName);
                        }
                    }

                    result.put(key, value);
                }
            }

            // 2. 保存返回结果
            state.setOutput(result);
            outputSnapshot = result;

        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
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
        trace.setErrorMessage(errorMessage);
        return trace;
    }
}

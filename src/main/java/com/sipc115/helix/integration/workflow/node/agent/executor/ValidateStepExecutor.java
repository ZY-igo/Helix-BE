/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.Validate.ValidateStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.Validate.ValidatorConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskExecutionException;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证步骤执行器
 */
@Component
public class ValidateStepExecutor implements AiStepExecutor {

    private static final Logger log = LoggerFactory.getLogger(ValidateStepExecutor.class);

    private final AiClient aiClient;

    /**
     * 构造函数
     *
     * @param aiClient AI 客户端
     */
    @Autowired
    public ValidateStepExecutor(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            ValidateStepConfig validateConfig = (ValidateStepConfig) stepConfig;
            LlmConfig llmConfig = state.getLlmConfig();

            // 1. 获取输入内容
            String input = validateConfig.getInput();
            if (input == null || input.isEmpty()) {
                throw AiTaskExecutionException.validationFailed(
                    stepConfig.getId(),
                    "输入内容不能为空"
                );
            }

            // 2. 执行验证
            Map<String, Object> validationResult = new HashMap<>();
            boolean allPassed = true;

            for (ValidatorConfig validator : validateConfig.getValidators()) {
                String validatorPrompt = validator.getPrompt();
                if (validatorPrompt != null && !validatorPrompt.isEmpty()) {
                    // 调用 AI 进行验证
                    String systemPrompt = llmConfig != null ? llmConfig.getSystemPrompt() : null;
                    String validationResponse = aiClient.chat("VALIDATE", systemPrompt, validatorPrompt + "\n\n" + input);

                    // 解析验证结果
                    boolean passed = validationResponse.contains("通过") || validationResponse.contains("通过") || validationResponse.contains("valid");
                    validationResult.put(validator.getKind(), passed);
                    if (!passed) {
                        allPassed = false;
                    }
                }
            }

            validationResult.put("passed", allPassed);

            // 3. 处理验证结果
            String varName = validateConfig.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, validationResult);
                outputSnapshot = validationResult;
            }

        } catch (AiTaskExecutionException e) {
            log.error("AI task execution failed at step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = e.getOriginalMessage();

        } catch (Exception e) {
            log.error("Unexpected error in validate step: {}", stepConfig.getId(), e);
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

        trace.setErrorMessage(errorMessage);

        return trace;
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.VALIDATE.name();
    }
}

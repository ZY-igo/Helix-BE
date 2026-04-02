/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.ValidateStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.ValidatorConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证步骤执行器类
 * <p>
 * 执行 AI 验证步骤，验证内容是否符合要求。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class ValidateStepExecutor implements AiStepExecutor {
    
    private AiClient aiClient;
    private LlmConfig llmConfig;

    /**
     * 设置 AI 客户端
     * 
     * @param aiClient AI 客户端实例
     */
    public void setAiClient(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * 设置 LLM 配置
     * 
     * @param llmConfig LLM 配置
     */
    public void setLlmConfig(LlmConfig llmConfig) {
        this.llmConfig = llmConfig;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        
        try {
            ValidateStepConfig validateConfig = (ValidateStepConfig) stepConfig;
            
            // 1. 获取输入内容
            String input = validateConfig.getInput();
            if (input == null || input.isEmpty()) {
                throw new IllegalArgumentException("Input cannot be empty");
            }
            
            // 2. 执行验证
            Map<String, Object> validationResult = new HashMap<>();
            boolean allPassed = true;
            
            for (ValidatorConfig validator : validateConfig.getValidators()) {
                String validatorPrompt = validator.getPrompt();
                if (validatorPrompt != null && !validatorPrompt.isEmpty()) {
                    // 调用 AI 进行验证
                    String systemPrompt = llmConfig.getSystemPrompt();
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
        } catch (Exception e) {
            status = "FAILED";
            // TODO: 处理异常
        }
        
        long durationMs = System.currentTimeMillis() - startTime;
        return new StepTrace(
                stepConfig.getId(),
                stepConfig.getType(),
                status,
                null, // 输入快照
                outputSnapshot,
                durationMs
        );
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.VALIDATE.name();
    }
}

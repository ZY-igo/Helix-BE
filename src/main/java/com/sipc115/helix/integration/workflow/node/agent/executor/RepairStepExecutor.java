/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.RepairStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;

/**
 * 修复步骤执行器类
 * <p>
 * 执行 AI 修复步骤，根据反馈修复内容。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class RepairStepExecutor implements AiStepExecutor {
    
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
            RepairStepConfig repairConfig = (RepairStepConfig) stepConfig;
            
            // 1. 获取输入内容和反馈
            String input = repairConfig.getInput();
            String feedback = repairConfig.getFeedback();
            
            if (input == null || input.isEmpty()) {
                throw new IllegalArgumentException("Input cannot be empty");
            }
            
            if (feedback == null || feedback.isEmpty()) {
                throw new IllegalArgumentException("Feedback cannot be empty");
            }
            
            // 2. 生成修复提示词
            String prompt = repairConfig.getPromptTemplate();
            if (prompt == null || prompt.isEmpty()) {
                prompt = "请根据以下反馈修复内容：\n\n反馈：{{feedback}}\n\n内容：{{input}}";
            }
            
            // 替换模板变量
            prompt = prompt.replace("{{feedback}}", feedback);
            prompt = prompt.replace("{{input}}", input);
            
            // 3. 调用 AI 进行修复
            String systemPrompt = llmConfig.getSystemPrompt();
            String repairedContent = aiClient.chat("REPAIR", systemPrompt, prompt);
            
            // 4. 处理修复结果
            String varName = repairConfig.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, repairedContent);
                outputSnapshot = repairedContent;
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
        return AiFlowStepConfig.StepType.REPAIR.name();
    }
}

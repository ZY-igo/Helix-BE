/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.GenerateStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;

/**
 * 生成步骤执行器类
 * <p>
 * 执行 AI 生成步骤，调用 AI 模型生成内容。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class GenerateStepExecutor implements AiStepExecutor {
    
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
            GenerateStepConfig generateConfig = (GenerateStepConfig) stepConfig;
            
            // 1. 解析提示模板
            String prompt = generateConfig.getPromptTemplate();
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Prompt template cannot be empty");
            }
            
            // 2. 调用 AI 模型
            String systemPrompt = llmConfig.getSystemPrompt();
            String generatedContent = aiClient.chat("GENERATE", systemPrompt, prompt);
            
            // 3. 处理生成结果
            String varName = generateConfig.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, generatedContent);
                outputSnapshot = generatedContent;
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
        return AiFlowStepConfig.StepType.GENERATE.name();
    }
}

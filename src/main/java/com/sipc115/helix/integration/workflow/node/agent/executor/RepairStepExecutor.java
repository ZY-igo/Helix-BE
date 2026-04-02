/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.googlecode.aviator.AviatorEvaluator;
import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.Repair.RepairStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskExecutionException;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 修复步骤执行器
 */
@Component
public class RepairStepExecutor implements AiStepExecutor {

    private static final Logger log = LoggerFactory.getLogger(RepairStepExecutor.class);

    private final AiClient aiClient;

    /**
     * 构造函数
     *
     * @param aiClient AI 客户端
     */
    @Autowired
    public RepairStepExecutor(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            RepairStepConfig config = (RepairStepConfig) stepConfig;
            LlmConfig llmConfig = state.getLlmConfig();

            // ⭐ 1. 解析输入表达式
            String input = resolveExpression(config.getInput(), state.getVars());
            String feedback = resolveExpression(config.getFeedback(), state.getVars());

            if (input == null || input.isEmpty()) {
                throw AiTaskExecutionException.validationFailed(
                    stepConfig.getId(),
                    "输入内容不能为空"
                );
            }

            if (feedback == null || feedback.isEmpty()) {
                throw AiTaskExecutionException.validationFailed(
                    stepConfig.getId(),
                    "反馈内容不能为空"
                );
            }

            // ⭐ 2. 构建修复提示词
            String prompt = buildRepairPrompt(config, input, feedback, state.getVars());

            // ⭐ 3. 调用 AI 进行修复
            String systemPrompt = llmConfig != null ? llmConfig.getSystemPrompt() : null;
            String repairedContent = aiClient.chat("REPAIR", systemPrompt, prompt);

            // ⭐ 4. 保存修复结果
            String varName = config.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, repairedContent);
                outputSnapshot = repairedContent;

                log.info("Repaired content saved to variable '{}': {} chars",
                    varName, repairedContent.length());
            }

        } catch (AiTaskExecutionException e) {
            log.error("AI task execution failed at step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = e.getOriginalMessage();

        } catch (Exception e) {
            log.error("Unexpected error in repair step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = "系统异常：" + e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        // 创建 StepTrace 并设置错误信息
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
     * ⭐ 解析表达式
     */
    private String resolveExpression(String expr, Map<String, Object> vars) {
        if (expr == null) {
            return null;
        }

        try {
            Object result = AviatorEvaluator.execute(expr, vars);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to resolve expression '{}', treating as literal", expr);
            return expr; // 如果解析失败，当作普通字符串返回
        }
    }

    /**
     * ⭐ 构建修复提示词
     */
    private String buildRepairPrompt(RepairStepConfig config, String input,
                                     String feedback, Map<String, Object> vars) {
        String template = config.getPromptTemplate();

        if (template == null || template.isEmpty()) {
            template = "请根据以下反馈修改内容：\n\n反馈：{{feedback}}\n\n原始内容：{{input}}\n\n修改后的内容：";
        }

        // 替换固定占位符
        template = template.replace("{{feedback}}", feedback);
        template = template.replace("{{input}}", input);

        // 替换其他变量
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (template.contains(placeholder)) {
                template = template.replace(placeholder,
                    entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }

        return template;
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.REPAIR.name();
    }
}

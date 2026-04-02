/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Generate;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生成步骤 Activity
 * <p>
 * 执行 AI 生成步骤，调用 AI 模型生成内容
 *
 * @author Helix Team
 * @since 2.0.0
 */
@ActivityInterface
@Component
public class GenerateStepActivity implements AiStepActivity {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final AiClient aiClient;

    @Autowired
    public GenerateStepActivity(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    @ActivityMethod
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            // ⭐ 类型转换：基类 → 子类（向下转型）
            if (!(stepConfig instanceof GenerateStepConfig)) {
                throw new IllegalArgumentException(
                    "Expected GenerateStepConfig but got: " + stepConfig.getClass().getName()
                );
            }

            GenerateStepConfig config = (GenerateStepConfig) stepConfig;

            // 1. 渲染提示词模板
            String renderedPrompt = renderTemplate(config.getPromptTemplate(), state.getVars());

            if (renderedPrompt == null || renderedPrompt.isEmpty()) {
                throw new IllegalArgumentException("提示词模板不能为空");
            }

            // 2. 调用 AI 模型
            String systemPrompt = state.getLlmConfig() != null ? state.getLlmConfig().getSystemPrompt() : null;
            String generatedContent = aiClient.chat("GENERATE", systemPrompt, renderedPrompt);

            if (generatedContent == null || generatedContent.isEmpty()) {
                throw new IllegalStateException("AI 模型返回空内容");
            }

            // 3. 保存生成结果
            String varName = config.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, generatedContent);
                outputSnapshot = generatedContent;
            }

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

    /**
     * 渲染模板中的 {{variable}} 占位符
     */
    private String renderTemplate(String template, Map<String, Object> vars) {
        if (template == null) {
            return "";
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = vars.get(varName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}

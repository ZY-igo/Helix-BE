/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Repair;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 修复步骤 Activity
 * <p>
 * 执行 AI 修复步骤，调用 AI 模型修复内容
 *
 * @author Helix Team
 * @since 2.0.0
 */
@ActivityInterface
@Component
public class RepairStepActivity implements AiStepActivity {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final AiClient aiClient;

    @Autowired
    public RepairStepActivity(AiClient aiClient) {
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
            RepairStepConfig config = (RepairStepConfig) stepConfig;

            // 1. 获取输入内容和反馈
            String input = resolveInput(config.getInput(), state);
            String feedback = resolveInput(config.getFeedback(), state);

            // 2. 渲染修复提示词
            String template = config.getPromptTemplate();
            if (template == null || template.isEmpty()) {
                template = "请根据以下反馈修改内容：\n\n反馈：{{feedback}}\n\n原始内容：{{input}}\n\n修改后的内容：";
            }

            // 替换固定占位符
            template = template.replace("{{feedback}}", feedback);
            template = template.replace("{{input}}", input);

            // 替换其他变量
            for (Map.Entry<String, Object> entry : state.getVars().entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                if (template.contains(placeholder)) {
                    template = template.replace(placeholder,
                            entry.getValue() != null ? entry.getValue().toString() : "");
                }
            }

            // 3. 调用 AI 模型修复
            String systemPrompt = state.getLlmConfig() != null ? state.getLlmConfig().getSystemPrompt() : null;
            String repairedContent = aiClient.chat("REPAIR", systemPrompt, template);

            // 4. 保存修复结果
            String varName = config.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, repairedContent);
                outputSnapshot = repairedContent;
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
     * 解析输入表达式
     */
    private String resolveInput(String input, AiTaskState state) {
        if (input == null) {
            return "";
        }

        if (input.startsWith("${") && input.endsWith("}")) {
            String varName = input.substring(2, input.length() - 1).trim();
            Object value = state.getVars().get(varName);
            return value != null ? value.toString() : "";
        }

        return input;
    }
}

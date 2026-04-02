/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Validate;

import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import com.sipc115.helix.integration.workflow.node.agent.runtime.ValidationResult;
import com.sipc115.helix.integration.workflow.node.agent.runtime.ValidationResult.ValidatorDetail;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 验证步骤 Activity
 * <p>
 * 执行 AI 验证步骤，调用 AI 模型验证内容
 *
 * @author Helix Team
 * @since 2.0.0
 */
@ActivityInterface
@Component
public class ValidateStepActivity implements AiStepActivity {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final AiClient aiClient;

    @Autowired
    public ValidateStepActivity(AiClient aiClient) {
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
            ValidateStepConfig config = (ValidateStepConfig) stepConfig;

            // 1. 获取输入内容
            String input = resolveInput(config.getInput(), state);

            // 2. 执行验证
            List<ValidationResult.ValidatorDetail> validatorResults = new ArrayList<>();
            boolean allPassed = true;

            for (ValidatorConfig validator : config.getValidators()) {
                String renderedPrompt = renderTemplate(validator.getPrompt(), state.getVars());
                String systemPrompt = state.getLlmConfig() != null ? state.getLlmConfig().getSystemPrompt() : null;
                String validationResult = aiClient.chat("VALIDATE", systemPrompt, renderedPrompt);

                // 解析验证结果
                boolean passed = parseValidationResult(validationResult);
                allPassed &= passed;

                ValidationResult.ValidatorDetail validatorDetail = new ValidationResult.ValidatorDetail();
                validatorDetail.setValidator(validator.getKind());
                validatorDetail.setPassed(passed);
                validatorDetail.setFeedback(validationResult);
                validatorResults.add(validatorDetail);
            }

            // 3. 构建验证结果
            ValidationResult validationResult = new ValidationResult();
            validationResult.setPassed(allPassed);
            validationResult.setDetails(validatorResults);

            // 4. 保存验证结果
            String varName = config.getOutputVar();
            if (varName != null) {
                state.getVars().put(varName, validationResult);
                outputSnapshot = validationResult;
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

    /**
     * 解析验证结果
     */
    private boolean parseValidationResult(String result) {
        // 简单实现：如果结果包含 "通过"、"pass"、"成功" 等关键词，则认为验证通过
        result = result.toLowerCase();
        return result.contains("通过") || result.contains("pass") || result.contains("成功") || result.contains("valid");
    }
}

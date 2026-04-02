/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Repair;

import com.sipc115.helix.integration.workflow.node.agent.spi.AbstractStepDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 修复步骤定义
 * <p>
 * 负责解析和验证 REPAIR 类型的步骤配置
 * <p>
 * DSL 配置示例：
 * <pre>
 * {
 *   "id": "repair-answer",
 *   "type": "REPAIR",
 *   "input": "${answer}",
 *   "feedback": "${validationResult.feedback}",
 *   "prompt": {
 *     "template": "请根据以下反馈修改内容：\n\n反馈：{{feedback}}\n\n原始内容：{{input}}\n\n修改后的内容："
 *   },
 *   "model": "glm-4",
 *   "temperature": 0.7,
 *   "output": {
 *     "var": "repairedAnswer"
 *   }
 * }
 * </pre>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li><b>id</b>：步骤唯一标识（必填）</li>
 *   <li><b>type</b>：步骤类型，固定为 "REPAIR"（必填）</li>
 *   <li><b>input</b>：要修复的输入内容，支持表达式（必填）</li>
 *   <li><b>feedback</b>：修复反馈信息，支持表达式（必填）</li>
 *   <li><b>prompt</b>：提示词配置（可选）
 *     <ul>
 *       <li><b>template</b>：修复提示词模板，支持变量占位符（必填）</li>
 *     </ul>
 *   </li>
 *   <li><b>model</b>：模型名称，如 "glm-4"、"gpt-4o" 等（可选，默认 "glm-4"）</li>
 *   <li><b>temperature</b>：温度参数，控制生成文本的随机性，范围 0.0-1.0（可选，默认 0.7）</li>
 *   <li><b>output</b>：输出配置（可选）
 *     <ul>
 *       <li><b>var</b>：修复结果存储的变量名（必填）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class RepairStepDefinition extends AbstractStepDefinition<RepairStepConfig> {

    @Override
    public String supportedType() {
        return "REPAIR";
    }

    @Override
    public RepairStepConfig parseConfig(Map<String, Object> stepConfig) {
        RepairStepConfig config = new RepairStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("REPAIR");
        config.setInput((String) stepConfig.get("input"));
        config.setFeedback((String) stepConfig.get("feedback"));

        @SuppressWarnings("unchecked")
        Map<String, Object> promptConfig = (Map<String, Object>) stepConfig.get("prompt");
        if (promptConfig != null) {
            config.setPromptTemplate((String) promptConfig.get("template"));
        }

        config.setModel((String) stepConfig.getOrDefault("model", "glm-4"));
        config.setTemperature(getDoubleValue(stepConfig, "temperature", 0.7));

        @SuppressWarnings("unchecked")
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }

        return config;
    }

    @Override
    public void validate(Map<String, Object> stepConfig, int stepIndex, String nodeId) {
        validateBasicInfo(stepConfig, stepIndex, nodeId);

        String input = (String) stepConfig.get("input");
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Repair step " + stepIndex + " must have input: " + nodeId);
        }

        String feedback = (String) stepConfig.get("feedback");
        if (feedback == null || feedback.isEmpty()) {
            throw new IllegalArgumentException("Repair step " + stepIndex + " must have feedback: " + nodeId);
        }

        Map<?, ?> promptConfig = (Map<?, ?>) stepConfig.get("prompt");
        if (promptConfig != null) {
            String template = (String) promptConfig.get("template");
            if (template == null || template.isEmpty()) {
                throw new IllegalArgumentException("Repair step " + stepIndex + " prompt must have template: " + nodeId);
            }
        }
    }
}

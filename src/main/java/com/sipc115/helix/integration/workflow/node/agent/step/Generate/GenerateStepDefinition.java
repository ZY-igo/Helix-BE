/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Generate;

import com.sipc115.helix.integration.workflow.node.agent.spi.AbstractStepDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 生成步骤定义
 * <p>
 * 负责解析和验证 GENERATE 类型的步骤配置
 * <p>
 * DSL 配置示例：
 * <pre>
 * {
 *   "id": "generate-answer",
 *   "type": "GENERATE",
 *   "prompt": {
 *     "template": "请根据以下问题生成详细回答：{{query}}"
 *   },
 *   "model": "glm-4",
 *   "temperature": 0.7,
 *   "output": {
 *     "var": "answer"
 *   }
 * }
 * </pre>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li><b>id</b>：步骤唯一标识（必填）</li>
 *   <li><b>type</b>：步骤类型，固定为 "GENERATE"（必填）</li>
 *   <li><b>prompt</b>：提示词配置（必填）
 *     <ul>
 *       <li><b>template</b>：提示词模板，支持变量占位符（必填）</li>
 *     </ul>
 *   </li>
 *   <li><b>model</b>：模型名称，如 "glm-4"、"gpt-4o" 等（可选，默认 "glm-4"）</li>
 *   <li><b>temperature</b>：温度参数，控制生成文本的随机性，范围 0.0-1.0（可选，默认 0.7）</li>
 *   <li><b>output</b>：输出配置（可选）
 *     <ul>
 *       <li><b>var</b>：生成结果存储的变量名（必填）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class GenerateStepDefinition extends AbstractStepDefinition<GenerateStepConfig> {

    @Override
    public String supportedType() {
        return "GENERATE";
    }

    @Override
    public GenerateStepConfig parseConfig(Map<String, Object> stepConfig) {
        GenerateStepConfig config = new GenerateStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("GENERATE");

        // 解析提示词配置
        @SuppressWarnings("unchecked")
        Map<String, Object> promptConfig = (Map<String, Object>) stepConfig.get("prompt");
        if (promptConfig != null) {
            config.setPromptTemplate((String) promptConfig.get("template"));
        }

        config.setModel((String) stepConfig.getOrDefault("model", "glm-4"));
        config.setTemperature(getDoubleValue(stepConfig, "temperature", 0.7));

        // 解析输出变量
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

        Map<?, ?> promptConfig = (Map<?, ?>) stepConfig.get("prompt");
        if (promptConfig == null) {
            throw new IllegalArgumentException("Generate step " + stepIndex + " must have prompt configuration: " + nodeId);
        }

        String template = (String) promptConfig.get("template");
        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Generate step " + stepIndex + " must have prompt template: " + nodeId);
        }

        Map<?, ?> outputConfig = (Map<?, ?>) stepConfig.get("output");
        if (outputConfig != null) {
            String var = (String) outputConfig.get("var");
            if (var == null || var.isEmpty()) {
                throw new IllegalArgumentException("Generate step " + stepIndex + " output must have var: " + nodeId);
            }
        }
    }
}

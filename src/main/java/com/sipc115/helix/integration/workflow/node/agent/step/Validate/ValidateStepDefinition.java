/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Validate;

import com.sipc115.helix.integration.workflow.node.agent.spi.AbstractStepDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 验证步骤定义
 * <p>
 * 负责解析和验证 VALIDATE 类型的步骤配置
 * <p>
 * DSL 配置示例：
 * <pre>
 * {
 *   "id": "validate-answer",
 *   "type": "VALIDATE",
 *   "input": "${answer}",
 *   "validators": [
 *     {
 *       "kind": "accuracy",
 *       "prompt": "请验证以下回答是否准确：{{input}}"
 *     },
 *     {
 *       "kind": "completeness",
 *       "prompt": "请验证以下回答是否完整：{{input}}"
 *     }
 *   ],
 *   "output": {
 *     "var": "validationResult"
 *   }
 * }
 * </pre>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li><b>id</b>：步骤唯一标识（必填）</li>
 *   <li><b>type</b>：步骤类型，固定为 "VALIDATE"（必填）</li>
 *   <li><b>input</b>：要验证的输入内容，支持表达式（必填）</li>
 *   <li><b>validators</b>：验证器列表（必填，至少一个）
 *     <ul>
 *       <li><b>kind</b>：验证器类型（必填）</li>
 *       <li><b>prompt</b>：验证提示词（必填）</li>
 *       <li><b>schema</b>：Schema 引用（可选）</li>
 *       <li><b>fields</b>：要验证的字段列表（可选）</li>
 *     </ul>
 *   </li>
 *   <li><b>output</b>：输出配置（可选）
 *     <ul>
 *       <li><b>var</b>：验证结果存储的变量名（必填）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ValidateStepDefinition extends AbstractStepDefinition<ValidateStepConfig> {

    @Override
    public String supportedType() {
        return "VALIDATE";
    }

    @Override
    public ValidateStepConfig parseConfig(Map<String, Object> stepConfig) {
        ValidateStepConfig config = new ValidateStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("VALIDATE");
        config.setInput((String) stepConfig.get("input"));

        // 解析验证器列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validatorConfigs = 
            (List<Map<String, Object>>) stepConfig.getOrDefault("validators", new ArrayList<>());
        List<ValidatorConfig> validators = new ArrayList<>();

        for (Map<String, Object> validatorConfig : validatorConfigs) {
            ValidatorConfig validator = new ValidatorConfig();
            validator.setKind((String) validatorConfig.get("kind"));
            validator.setPrompt((String) validatorConfig.get("prompt"));
            validator.setSchemaRef((String) validatorConfig.get("schema"));

            // 解析字段列表
            Object fieldsObj = validatorConfig.get("fields");
            if (fieldsObj instanceof List) {
                List<String> fields = new ArrayList<>();
                ((List<?>) fieldsObj).forEach(f -> fields.add(String.valueOf(f)));
                validator.setFields(fields);
            }

            validators.add(validator);
        }

        config.setValidators(validators);

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
            throw new IllegalArgumentException("Validate step " + stepIndex + " must have input: " + nodeId);
        }

        List<?> validators = (List<?>) stepConfig.get("validators");
        if (validators == null || validators.isEmpty()) {
            throw new IllegalArgumentException("Validate step " + stepIndex + " must have at least one validator: " + nodeId);
        }

        for (int i = 0; i < validators.size(); i++) {
            Object validatorObj = validators.get(i);
            if (!(validatorObj instanceof Map)) {
                throw new IllegalArgumentException("Validator " + i + " in step " + stepIndex + " must be a map: " + nodeId);
            }

            Map<?, ?> validatorMap = (Map<?, ?>) validatorObj;
            String kind = (String) validatorMap.get("kind");
            if (kind == null || kind.isEmpty()) {
                throw new IllegalArgumentException("Validator " + i + " in step " + stepIndex + " must have kind: " + nodeId);
            }
        }
    }
}

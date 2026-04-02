/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Return;

import com.sipc115.helix.integration.workflow.node.agent.spi.AbstractStepDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回步骤定义
 * <p>
 * 负责解析和验证 RETURN 类型的步骤配置
 * <p>
 * DSL 配置示例：
 * <pre>
 * {
 *   "id": "return-result",
 *   "type": "RETURN",
 *   "result": {
 *     "answer": "${finalAnswer}",
 *     "validationResult": "${validationResult}",
 *     "attempts": "${attempts}"
 *   }
 * }
 * </pre>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li><b>id</b>：步骤唯一标识（必填）</li>
 *   <li><b>type</b>：步骤类型，固定为 "RETURN"（必填）</li>
 *   <li><b>result</b>：返回结果映射（可选）
 *     <ul>
 *       <li><b>key</b>：返回结果的键（必填）</li>
 *       <li><b>value</b>：返回结果的值，支持表达式（必填）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ReturnStepDefinition extends AbstractStepDefinition<ReturnStepConfig> {

    @Override
    public String supportedType() {
        return "RETURN";
    }

    @Override
    public ReturnStepConfig parseConfig(Map<String, Object> stepConfig) {
        ReturnStepConfig config = new ReturnStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("RETURN");

        Object resultObj = stepConfig.get("result");
        if (resultObj instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            ((Map<?, ?>) resultObj).forEach((key, value) -> result.put(key.toString(), value));
            config.setResult(result);
        } else {
            config.setResult(new HashMap<>());
        }

        return config;
    }

    @Override
    public void validate(Map<String, Object> stepConfig, int stepIndex, String nodeId) {
        validateBasicInfo(stepConfig, stepIndex, nodeId);

        Object result = stepConfig.get("result");
        if (result != null && !(result instanceof Map)) {
            throw new IllegalArgumentException("Return step " + stepIndex + " result must be a map: " + nodeId);
        }
    }
}

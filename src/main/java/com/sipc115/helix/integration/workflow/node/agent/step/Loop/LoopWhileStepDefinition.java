/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Loop;

import com.sipc115.helix.integration.workflow.node.agent.spi.AbstractStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.Generate.GenerateStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.If.IfStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Repair.RepairStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Return.ReturnStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Validate.ValidateStepDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 循环步骤定义
 * <p>
 * 负责解析和验证 LOOP_WHILE 类型的步骤配置
 * <p>
 * DSL 配置示例：
 * <pre>
 * {
 *   "id": "generate-until-valid",
 *   "type": "LOOP_WHILE",
 *   "condition": "${validationResult.passed == false && attempts < 3}",
 *   "maxRounds": 3,
 *   "body": [
 *     {
 *       "id": "generate-answer",
 *       "type": "GENERATE",
 *       "prompt": {
 *         "template": "请根据以下问题生成详细回答：{{query}}"
 *       },
 *       "output": {
 *         "var": "answer"
 *       }
 *     },
 *     {
 *       "id": "validate-answer",
 *       "type": "VALIDATE",
 *       "input": "${answer}",
 *       "validators": [
 *         {
 *           "kind": "accuracy",
 *           "prompt": "请验证以下回答是否准确：{{input}}"
 *         }
 *       ],
 *       "output": {
 *         "var": "validationResult"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li><b>id</b>：步骤唯一标识（必填）</li>
 *   <li><b>type</b>：步骤类型，固定为 "LOOP_WHILE"（必填）</li>
 *   <li><b>condition</b>：循环条件表达式，支持 Aviator 表达式语法（必填）</li>
 *   <li><b>maxRounds</b>：最大循环次数（可选，默认 3）</li>
 *   <li><b>body</b>：循环体步骤列表（必填，至少一个）</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class LoopWhileStepDefinition extends AbstractStepDefinition<LoopWhileStepConfig> {

    @Override
    public String supportedType() {
        return "LOOP_WHILE";
    }

    @Override
    public LoopWhileStepConfig parseConfig(Map<String, Object> stepConfig) {
        LoopWhileStepConfig config = new LoopWhileStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("LOOP_WHILE");
        config.setCondition((String) stepConfig.get("condition"));
        config.setMaxRounds(getIntValue(stepConfig, "maxRounds", 3));

        // 解析循环体
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bodySteps = 
            (List<Map<String, Object>>) stepConfig.getOrDefault("body", new ArrayList<>());
        List<AiFlowStepConfig> bodyFlow = new ArrayList<>();
        for (Map<String, Object> bodyStep : bodySteps) {
            // 递归解析子步骤
            bodyFlow.add(parseSubStep(bodyStep));
        }
        config.setBody(bodyFlow);

        return config;
    }

    @Override
    public void validate(Map<String, Object> stepConfig, int stepIndex, String nodeId) {
        validateBasicInfo(stepConfig, stepIndex, nodeId);

        String condition = (String) stepConfig.get("condition");
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("LoopWhile step " + stepIndex + " must have condition: " + nodeId);
        }

        List<?> bodySteps = (List<?>) stepConfig.get("body");
        if (bodySteps == null || bodySteps.isEmpty()) {
            throw new IllegalArgumentException("LoopWhile step " + stepIndex + " must have body: " + nodeId);
        }

        // 验证子步骤
        validateSubSteps(bodySteps, stepIndex, nodeId, "body");
    }

    /**
     * 解析子步骤
     *
     * @param stepConfig 子步骤配置
     * @return 子步骤配置对象
     */
    private AiFlowStepConfig parseSubStep(Map<String, Object> stepConfig) {
        String type = (String) stepConfig.get("type");
        switch (type) {
            case "GENERATE":
                return new GenerateStepDefinition().parseConfig(stepConfig);
            case "VALIDATE":
                return new ValidateStepDefinition().parseConfig(stepConfig);
            case "REPAIR":
                return new RepairStepDefinition().parseConfig(stepConfig);
            case "IF":
                return new IfStepDefinition().parseConfig(stepConfig);
            case "LOOP_WHILE":
                return new LoopWhileStepDefinition().parseConfig(stepConfig);
            case "RETURN":
                return new ReturnStepDefinition().parseConfig(stepConfig);
            default:
                throw new IllegalArgumentException("Unsupported step type: " + type);
        }
    }

    /**
     * 验证子步骤
     *
     * @param steps 子步骤列表
     * @param stepIndex 父步骤索引
     * @param nodeId 节点 ID
     * @param part 部分名称
     */
    private void validateSubSteps(List<?> steps, int stepIndex, String nodeId, String part) {
        for (int i = 0; i < steps.size(); i++) {
            Object stepObj = steps.get(i);
            if (!(stepObj instanceof Map)) {
                throw new IllegalArgumentException(part + " step " + i + " in step " + stepIndex + " must be a map: " + nodeId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> stepMap = (Map<String, Object>) stepObj;
            String type = (String) stepMap.get("type");
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException(part + " step " + i + " in step " + stepIndex + " must have a type: " + nodeId);
            }

            // 根据类型验证子步骤
            switch (type) {
                case "GENERATE":
                    new GenerateStepDefinition().validate(stepMap, i, nodeId);
                    break;
                case "VALIDATE":
                    new ValidateStepDefinition().validate(stepMap, i, nodeId);
                    break;
                case "REPAIR":
                    new RepairStepDefinition().validate(stepMap, i, nodeId);
                    break;
                case "IF":
                    new IfStepDefinition().validate(stepMap, i, nodeId);
                    break;
                case "LOOP_WHILE":
                    new LoopWhileStepDefinition().validate(stepMap, i, nodeId);
                    break;
                case "RETURN":
                    new ReturnStepDefinition().validate(stepMap, i, nodeId);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported step type: " + type + " in " + part + " step " + i + " of step " + stepIndex + " in " + nodeId);
            }
        }
    }
}

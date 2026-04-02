/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.If;

import com.sipc115.helix.integration.workflow.node.agent.spi.AbstractStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Generate.GenerateStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Loop.LoopWhileStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Repair.RepairStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Return.ReturnStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.Validate.ValidateStepDefinition;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 条件步骤定义
 * <p>
 * 负责解析和验证 IF 类型的步骤配置
 * <p>
 * DSL 配置示例：
 * <pre>
 * {
 *   "id": "check-validation",
 *   "type": "IF",
 *   "condition": "${validationResult.passed}",
 *   "then": [
 *     {
 *       "id": "generate-final-answer",
 *       "type": "GENERATE",
 *       "prompt": {
 *         "template": "请根据以下内容生成最终回答：{{answer}}"
 *       },
 *       "output": {
 *         "var": "finalAnswer"
 *       }
 *     }
 *   ],
 *   "else": [
 *     {
 *       "id": "repair-answer",
 *       "type": "REPAIR",
 *       "input": "${answer}",
 *       "feedback": "${validationResult.feedback}",
 *       "output": {
 *         "var": "repairedAnswer"
 *       }
 *     }
 *   ]
 * }
 * </pre>
 * <p>
 * 配置项说明：
 * <ul>
 *   <li><b>id</b>：步骤唯一标识（必填）</li>
 *   <li><b>type</b>：步骤类型，固定为 "IF"（必填）</li>
 *   <li><b>condition</b>：条件表达式，支持 Aviator 表达式语法（必填）</li>
 *   <li><b>then</b>：条件为真时执行的步骤列表（必填，至少一个）</li>
 *   <li><b>else</b>：条件为假时执行的步骤列表（可选）</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class IfStepDefinition extends AbstractStepDefinition<IfStepConfig> {

    @Override
    public String supportedType() {
        return "IF";
    }

    @Override
    public IfStepConfig parseConfig(Map<String, Object> stepConfig) {
        IfStepConfig config = new IfStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("IF");
        config.setCondition((String) stepConfig.get("condition"));

        // 解析 then 分支
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> thenSteps = 
            (List<Map<String, Object>>) stepConfig.getOrDefault("then", new ArrayList<>());
        List<AiFlowStepConfig> thenFlow = new ArrayList<>();
        for (Map<String, Object> thenStep : thenSteps) {
            // 递归解析子步骤
            thenFlow.add(parseSubStep(thenStep));
        }
        config.setThenSteps(thenFlow);

        // 解析 else 分支
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elseSteps = 
            (List<Map<String, Object>>) stepConfig.getOrDefault("else", new ArrayList<>());
        List<AiFlowStepConfig> elseFlow = new ArrayList<>();
        for (Map<String, Object> elseStep : elseSteps) {
            // 递归解析子步骤
            elseFlow.add(parseSubStep(elseStep));
        }
        config.setElseSteps(elseFlow);

        return config;
    }

    @Override
    public void validate(Map<String, Object> stepConfig, int stepIndex, String nodeId) {
        validateBasicInfo(stepConfig, stepIndex, nodeId);

        String condition = (String) stepConfig.get("condition");
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("If step " + stepIndex + " must have condition: " + nodeId);
        }

        List<?> thenSteps = (List<?>) stepConfig.get("then");
        if (thenSteps == null || thenSteps.isEmpty()) {
            throw new IllegalArgumentException("If step " + stepIndex + " must have then branch: " + nodeId);
        }

        // 验证子步骤
        validateSubSteps(thenSteps, stepIndex, nodeId, "then");

        List<?> elseSteps = (List<?>) stepConfig.get("else");
        if (elseSteps != null && !elseSteps.isEmpty()) {
            validateSubSteps(elseSteps, stepIndex, nodeId, "else");
        }
    }

    /**
     * 解析子步骤
     *
     * @param stepConfig 子步骤配置
     * @return 子步骤配置对象
     */
    private AiFlowStepConfig parseSubStep(Map<String, Object> stepConfig) {
        String type = (String) stepConfig.get("type");
        return switch (type) {
            case "GENERATE" -> new GenerateStepDefinition().parseConfig(stepConfig);
            case "VALIDATE" -> new ValidateStepDefinition().parseConfig(stepConfig);
            case "REPAIR" -> new RepairStepDefinition().parseConfig(stepConfig);
            case "IF" -> new IfStepDefinition().parseConfig(stepConfig);
            case "LOOP_WHILE" -> new LoopWhileStepDefinition().parseConfig(stepConfig);
            case "RETURN" -> new ReturnStepDefinition().parseConfig(stepConfig);
            default -> throw new IllegalArgumentException("Unsupported step type: " + type);
        };
    }

    /**
     * 验证子步骤
     *
     * @param steps 子步骤列表
     * @param stepIndex 父步骤索引
     * @param nodeId 节点 ID
     * @param branch 分支名称
     */
    private void validateSubSteps(List<?> steps, int stepIndex, String nodeId, String branch) {
        for (int i = 0; i < steps.size(); i++) {
            Object stepObj = steps.get(i);
            if (!(stepObj instanceof Map)) {
                throw new IllegalArgumentException(branch + " branch step " + i + " in step " + stepIndex + " must be a map: " + nodeId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> stepMap = (Map<String, Object>) stepObj;
            String type = (String) stepMap.get("type");
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException(branch + " branch step " + i + " in step " + stepIndex + " must have a type: " + nodeId);
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
                    throw new IllegalArgumentException("Unsupported step type: " + type + " in " + branch + " branch step " + i + " of step " + stepIndex + " in " + nodeId);
            }
        }
    }
}

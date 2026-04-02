/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.activity;

import com.sipc115.helix.integration.workflow.node.agent.step.Generate.GenerateStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.If.IfStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.Loop.LoopWhileStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.Repair.RepairStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.Return.ReturnStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.step.Validate.ValidateStepActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 步骤 Activity 工厂
 * <p>
 * 根据步骤类型获取对应的 Activity 实例
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiStepActivityFactory {

    private final Map<String, AiStepActivity> activityMap = new HashMap<>();

    @Autowired
    public AiStepActivityFactory(
            GenerateStepActivity generateStepActivity,
            ValidateStepActivity validateStepActivity,
            RepairStepActivity repairStepActivity,
            IfStepActivity ifStepActivity,
            LoopWhileStepActivity loopWhileStepActivity,
            ReturnStepActivity returnStepActivity) {

        activityMap.put("GENERATE", generateStepActivity);
        activityMap.put("VALIDATE", validateStepActivity);
        activityMap.put("REPAIR", repairStepActivity);
        activityMap.put("IF", ifStepActivity);
        activityMap.put("LOOP_WHILE", loopWhileStepActivity);
        activityMap.put("RETURN", returnStepActivity);
    }

    /**
     * 根据步骤类型获取对应的 Activity 实例
     *
     * @param stepType 步骤类型
     * @return Activity 实例
     * @throws IllegalArgumentException 如果步骤类型不支持
     */
    public AiStepActivity getActivity(String stepType) {
        AiStepActivity activity = activityMap.get(stepType);
        if (activity == null) {
            throw new IllegalArgumentException("Unsupported step type: " + stepType);
        }
        return activity;
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.Transition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 转换解析器类
 * <p>
 * 负责解析工作流中的转换关系，根据当前节点和分支键确定下一个节点。
 * 实现了工作流执行过程中的路径选择逻辑。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class TransitionResolver {

    /**
     * 确定下一个节点
     * <p>
     * 根据执行计划、当前节点 ID 和分支键，查找并返回下一个节点的 ID。
     * 
     * @param plan 执行计划，包含工作流的转换定义
     * @param currentNodeId 当前节点 ID
     * @param branchKey 分支键，用于选择条件分支
     * @return 下一个节点的 ID，如果没有找到则返回 null
     */
    public String nextNode(ExecutionPlan plan, String currentNodeId, String branchKey) {
        // 获取执行计划中的所有转换
        List<Transition> transitions = plan.getTransitions();
        
        // 如果没有转换定义，返回 null
        if (transitions == null || transitions.isEmpty()) {
            return null;
        }
        
        // 遍历所有转换，查找匹配的转换
        for (Transition transition : transitions) {
            // 跳过源节点不是当前节点的转换
            if (!Objects.equals(currentNodeId, transition.getFrom())) {
                continue;
            }
            
            // 处理无条件转换（conditionKey 为 null 且 branchKey 为 null）
            if (transition.getConditionKey() == null && branchKey == null) {
                return transition.getTo();
            }
            
            // 处理条件转换（conditionKey 与 branchKey 匹配）
            if (Objects.equals(branchKey, transition.getConditionKey())) {
                return transition.getTo();
            }
        }
        
        // 没有找到匹配的转换，返回 null
        return null;
    }
}

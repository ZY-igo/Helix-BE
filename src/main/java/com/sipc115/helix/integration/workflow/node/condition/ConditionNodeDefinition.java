/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.condition;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * 条件节点定义
 * <p>
 * 定义条件节点（CONDITION）的元数据和配置类型。
 * 条件节点用于根据表达式结果决定工作流的执行路径。
 * <p>
 * DSL 配置结构：
 * <pre>
 * {
 *   "id": "check-age",
 *   "type": "CONDITION",
 *   "config": {
 *     "condition": "age >= 18",
 *     "expressionLanguage": "aviator",
 *     "defaultBranch": "true"
 *   }
 * }
 * </pre>
 * <p>
 * 配置字段说明：
 * <ul>
 *   <li>condition: 条件表达式，用于判断执行路径</li>
 *   <li>expressionLanguage: 表达式语言类型，如 "aviator"、"spel"</li>
 *   <li>defaultBranch: 默认分支，当表达式执行失败时使用（"true" 或 "false"）</li>
 * </ul>
 * <p>
 * 执行结果：
 * <ul>
 *   <li>表达式结果为 true 时，走 "true" 分支</li>
 *   <li>表达式结果为 false 时，走 "false" 分支</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ConditionNodeDefinition implements NodeDefinition<ConditionNodeConfig> {

    /**
     * 获取节点类型
     *
     * @return CONDITION 节点类型枚举
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.CONDITION;
    }

    /**
     * 获取配置类
     *
     * @return ConditionNodeConfig 配置类
     */
    @Override
    public Class<ConditionNodeConfig> configClass() {
        return ConditionNodeConfig.class;
    }
}

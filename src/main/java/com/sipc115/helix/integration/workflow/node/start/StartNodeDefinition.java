/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.start;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

/**
 * 开始节点定义
 * <p>
 * 定义开始节点（START）的元数据和配置类型。
 * 开始节点是工作流的入口点，每个工作流必须有且仅有一个开始节点。
 * <p>
 * DSL 配置结构：
 * <pre>
 * {
 *   "id": "start",
 *   "type": "START",
 *   "config": {
 *     "initVariables": {
 *       "defaultUser": "system",
 *       "maxRetries": 3
 *     },
 *     "description": "工作流开始"
 *   }
 * }
 * </pre>
 * <p>
 * 配置字段说明：
 * <ul>
 *   <li>initVariables: 初始化变量（可选）
 *     <ul>
 *       <li>以键值对形式定义工作流开始时的初始变量</li>
 *       <li>这些变量可以在后续节点中引用</li>
 *     </ul>
 *   </li>
 *   <li>description: 工作流描述（可选）</li>
 * </ul>
 * <p>
 * 节点角色：
 * <ul>
 *   <li>NodeRole.ENTRY - 表示这是工作流的入口节点</li>
 * </ul>
 * <p>
 * 执行行为：
 * <ul>
 *   <li>初始化工作流上下文</li>
 *   <li>将 initVariables 中的变量设置到工作流上下文中</li>
 *   <li>标记工作流状态为 RUNNING</li>
 *   <li>立即执行下一个节点</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class StartNodeDefinition implements NodeDefinition<StartNodeConfig> {

    /**
     * 获取节点类型
     *
     * @return START 节点类型枚举
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.START;
    }

    /**
     * 获取节点角色
     * <p>
     * 开始节点的角色为 ENTRY，表示工作流入口。
     *
     * @return NodeRole.ENTRY
     */
    @Override
    public NodeRole role() {
        return NodeRole.ENTRY;
    }

    /**
     * 获取配置类
     *
     * @return StartNodeConfig 配置类
     */
    @Override
    public Class<StartNodeConfig> configClass() {
        return StartNodeConfig.class;
    }
}

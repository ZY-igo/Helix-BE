/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.end;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

/**
 * 结束节点定义
 * <p>
 * 定义结束节点（END）的元数据和配置类型。
 * 结束节点是工作流的出口点，标记工作流执行完成。
 * <p>
 * DSL 配置结构：
 * <pre>
 * {
 *   "id": "end",
 *   "type": "END",
 *   "config": {
 *     "outputVariables": {
 *       "result": "${finalResult}",
 *       "status": "completed"
 *     },
 *     "resultStatus": "SUCCESS",
 *     "resultMessage": "工作流执行成功"
 *   }
 * }
 * </pre>
 * <p>
 * 配置字段说明：
 * <ul>
 *   <li>outputVariables: 输出变量（可选）
 *     <ul>
 *       <li>以键值对形式定义工作流结束时的输出数据</li>
 *       <li>支持表达式，如 "${finalResult}" 表示引用变量 finalResult 的值</li>
 *     </ul>
 *   </li>
 *   <li>resultStatus: 结果状态（可选）
 *     <ul>
 *       <li>SUCCESS: 成功</li>
 *       <li>FAILED: 失败</li>
 *       <li>CANCELLED: 已取消</li>
 *     </ul>
 *   </li>
 *   <li>resultMessage: 结果消息（可选），描述工作流执行结果</li>
 * </ul>
 * <p>
 * 节点角色：
 * <ul>
 *   <li>NodeRole.EXIT - 表示这是工作流的出口节点</li>
 * </ul>
 * <p>
 * 执行行为：
 * <ul>
 *   <li>计算 outputVariables 中的表达式值</li>
 *   <li>将结果存储到工作流输出中</li>
 *   <li>标记工作流状态为 COMPLETED</li>
 *   <li>工作流执行结束</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class EndNodeDefinition implements NodeDefinition<EndNodeConfig> {

    /**
     * 获取节点类型
     *
     * @return END 节点类型枚举
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.END;
    }

    /**
     * 获取节点角色
     * <p>
     * 结束节点的角色为 EXIT，表示工作流出口。
     *
     * @return NodeRole.EXIT
     */
    @Override
    public NodeRole role() {
        return NodeRole.EXIT;
    }

    /**
     * 获取配置类
     *
     * @return EndNodeConfig 配置类
     */
    @Override
    public Class<EndNodeConfig> configClass() {
        return EndNodeConfig.class;
    }
}

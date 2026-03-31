/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;

/**
 * 开始节点执行器
 * <p>
 * 负责执行工作流的开始节点，标记工作流执行开始。
 * 实现了 WorkflowNodeExecutor 接口，支持 START 类型的节点。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class StartNodeExecutor implements WorkflowNodeExecutor {
    /**
     * 检查是否支持指定类型的节点
     * <p>
     * 只支持 START 类型的节点。
     * 
     * @param type 节点类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.START.name().equals(type);
    }

    /**
     * 执行开始节点
     * <p>
     * 执行开始节点，返回完成状态，标记工作流执行开始。
     * 
     * @param node 编译后的节点
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接
     * @return 节点执行结果，状态为完成
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        return NodeExecutionResult.completed();
    }
}

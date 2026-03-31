package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.CompiledNode;

/**
 * 工作流节点执行器接口
 * <p>
 * 定义了执行工作流节点的方法，用于执行不同类型的工作流节点
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public interface WorkflowNodeExecutor {
    /**
     * 检查当前执行器是否支持指定类型的节点
     * 
     * @param type 节点类型
     * @return 是否支持该类型的节点
     */
    boolean supports(String type);
    
    /**
     * 执行工作流节点
     * <p>
     * 执行指定的工作流节点，并返回执行结果
     * </p>
     * 
     * @param node 编译后的节点
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接器
     * @return 节点执行结果
     */
    NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge);
}

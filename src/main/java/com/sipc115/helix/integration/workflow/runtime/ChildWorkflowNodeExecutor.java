package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.CompiledNode;

import java.util.Map;

/**
 * 子工作流节点执行器
 * <p>
 * 负责执行子工作流类型的节点，通过WorkflowRuntimeBridge调用子工作流并获取结果
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public class ChildWorkflowNodeExecutor implements WorkflowNodeExecutor {
    /**
     * 检查当前执行器是否支持指定类型的节点
     * 
     * @param type 节点类型
     * @return 是否支持该类型的节点
     */
    @Override
    public boolean supports(String type) {
        return "CHILD_WORKFLOW".equals(type);
    }

    /**
     * 执行子工作流节点
     * <p>
     * 通过WorkflowRuntimeBridge调用子工作流，并将子工作流的执行结果作为当前节点的输出
     * </p>
     * 
     * @param node 编译后的节点
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接器
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        // 调用子工作流并获取结果
        String childResult = bridge.invokeChildWorkflow(node, context.getVariables());
        // 创建完成状态的执行结果
        NodeExecutionResult result = NodeExecutionResult.completed();
        // 设置输出结果，包含子工作流的执行结果
        result.setOutput(Map.of("childResult", childResult));
        return result;
    }
}

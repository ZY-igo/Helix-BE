/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.humanInput;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;

/**
 * 人工输入节点执行器
 * <p>
 * 负责执行工作流的人工输入节点，等待并处理人工输入信号。
 * 实现了 WorkflowNodeExecutor 接口，支持 HUMAN_INPUT 类型的节点。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class HumanInputNodeExecutor implements WorkflowNodeExecutor {
    /**
     * 检查是否支持指定类型的节点
     * <p>
     * 只支持 HUMAN_INPUT 类型的节点。
     * 
     * @param type 节点类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.HUMAN_INPUT.name().equals(type);
    }

    /**
     * 执行人工输入节点
     * <p>
     * 等待人工输入信号，获取输入数据并返回执行结果。
     * 
     * @param node 编译后的节点
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接
     * @return 节点执行结果，包含人工输入数据
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        // 等待人工输入信号
        HumanSignalPayload payload = bridge.awaitHumanSignal(node.getId());
        
        // 创建完成状态的执行结果
        NodeExecutionResult result = NodeExecutionResult.completed();
        
        // 设置输出数据为人工输入的有效载荷
        result.setOutput(payload.getPayload());
        
        return result;
    }
}

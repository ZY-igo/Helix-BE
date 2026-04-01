/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.end;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.springframework.stereotype.Component;

/**
 * 结束节点执行器
 * <p>
 * 负责执行工作流的结束节点，标记工作流执行完成。
 * 实现了 WorkflowNodeExecutor 接口，支持 END 类型的节点。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class EndNodeExecutor implements WorkflowNodeExecutor {
    /**
     * 检查是否支持指定类型的节点
     * <p>
     * 只支持 END 类型的节点。
     * 
     * @param type 节点类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.END.name().equals(type);
    }

    /**
     * 执行结束节点
     * <p>
     * 执行结束节点，返回完成状态，标记工作流执行完成。
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

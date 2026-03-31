package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行上下文类
 * <p>
 * 用于存储工作流执行过程中的上下文信息，包括执行计划、变量、节点状态等
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Data
public class ExecutionContext {
    /**
     * 执行计划
     */
    private final ExecutionPlan plan;
    
    /**
     * 变量映射
     */
    private final Map<String, Object> variables = new HashMap<>();
    
    /**
     * 节点状态映射
     */
    private final Map<String, ExecutionStatus> nodeStatuses = new HashMap<>();
    
    /**
     * 当前节点ID
     */
    private String currentNodeId;
    
    /**
     * 工作流状态
     */
    private ExecutionStatus workflowStatus = ExecutionStatus.PENDING;

    /**
     * 构造函数
     * 
     * @param plan 执行计划
     * @param input 输入变量
     */
    public ExecutionContext(ExecutionPlan plan, Map<String, Object> input) {
        this.plan = plan;
        if (input != null) {
            this.variables.putAll(input);
        }
    }

    /**
     * 转换为工作流状态视图
     * <p>
     * 将执行上下文转换为WorkflowStateView对象，用于返回给客户端
     * </p>
     * 
     * @return 工作流状态视图
     */
    public WorkflowStateView toView() {
        WorkflowStateView view = new WorkflowStateView();
        view.setWorkflowId(plan.getWorkflowId());
        view.setCurrentNodeId(currentNodeId);
        view.setStatus(workflowStatus);
        view.getVariables().putAll(variables);
        view.getNodeStatuses().putAll(nodeStatuses);
        return view;
    }
}

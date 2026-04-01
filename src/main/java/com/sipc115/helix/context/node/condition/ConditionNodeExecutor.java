/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.condition;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.runtime.*;

/**
 * 条件节点执行器
 * <p>
 * 负责执行工作流的条件节点，根据条件表达式的结果确定执行路径。
 * 实现了 WorkflowNodeExecutor 接口，支持 CONDITION 类型的节点。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class ConditionNodeExecutor implements WorkflowNodeExecutor {
    /**
     * 转换解析器
     * <p>
     * 用于根据条件结果计算下一个要执行的节点。
     */
    private final TransitionResolver transitionResolver;

    /**
     * 构造函数
     * <p>
     * 初始化条件节点执行器，注入转换解析器。
     * 
     * @param transitionResolver 转换解析器
     */
    public ConditionNodeExecutor(TransitionResolver transitionResolver) {
        this.transitionResolver = transitionResolver;
    }

    /**
     * 检查是否支持指定类型的节点
     * <p>
     * 只支持 CONDITION 类型的节点。
     * 
     * @param type 节点类型
     * @return 是否支持
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.CONDITION.name().equals(type);
    }

    /**
     * 执行条件节点
     * <p>
     * 执行条件节点，根据节点配置中的 defaultBranch 确定分支键，返回执行结果。
     * 
     * @param node 编译后的节点
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接
     * @return 节点执行结果，包含分支键
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        // TODO 这里应该接入确定性的表达式求值。推荐把编译后的表达式 DSL 存进 node.config。
        // 从节点配置中获取默认分支，默认为 "true"
        Object value = node.getConfig().getOrDefault("defaultBranch", "true");
        
        // 创建完成状态的执行结果
        NodeExecutionResult result = NodeExecutionResult.completed();
        
        // 设置分支键，用于后续的转换解析
        result.setBranchKey(String.valueOf(value));
        
        return result;
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.temporal.workflow;

import com.sipc115.helix.integration.workflow.runtime.*;

import java.util.List;

/**
 * 工作流执行器管理类
 * <p>
 * 提供共享的执行器注册表和转换解析器，避免每次工作流执行时重复创建实例，提高性能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public final class WorkflowExecutors {
    
    /**
     * 共享的无状态转换解析器
     * <p>
     * 用于根据当前节点和分支结果计算下一个要执行的节点。
     */
    public static final TransitionResolver TRANSITION_RESOLVER = new TransitionResolver();
    
    /**
     * 静态执行器注册表
     * <p>
     * 应用启动时初始化一次，包含所有类型的工作流节点执行器。
     */
    public static final NodeExecutorRegistry REGISTRY = new NodeExecutorRegistry(
        List.of(
            new StartNodeExecutor(),           // 起始节点执行器
            new EndNodeExecutor(),             // 结束节点执行器
            new ActivityNodeExecutor(),        // 活动节点执行器（调用外部服务）
            new ConditionNodeExecutor(TRANSITION_RESOLVER),  // 条件判断节点执行器
            new DelayNodeExecutor(),           // 延迟/定时节点执行器
            new HumanInputNodeExecutor(),      // 人工输入节点执行器
            new ChildWorkflowNodeExecutor(),   // 子工作流节点执行器
            new TransformNodeExecutor()        // 数据转换节点执行器
        )
    );
    
    /**
     * 私有构造函数
     * <p>
     * 防止实例化该工具类。
     */
    private WorkflowExecutors() {}
}

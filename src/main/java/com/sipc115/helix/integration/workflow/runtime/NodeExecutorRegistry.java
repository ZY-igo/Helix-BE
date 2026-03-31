/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器注册表
 * <p>
 * 负责管理和注册不同类型的工作流节点执行器，根据节点类型获取对应的执行器。
 * 支持通过类名和节点类型两种方式注册和获取执行器。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class NodeExecutorRegistry {
    /**
     * 执行器映射
     * <p>
     * 存储节点类型或执行器类名到执行器实例的映射。
     */
    private final Map<String, WorkflowNodeExecutor> delegate = new HashMap<>();

    /**
     * 构造函数
     * <p>
     * 初始化执行器注册表，注册所有提供的执行器实例。
     * 
     * @param executors 工作流节点执行器列表
     */
    public NodeExecutorRegistry(List<WorkflowNodeExecutor> executors) {
        // 首先按执行器类名注册
        for (WorkflowNodeExecutor executor : executors) {
            delegate.put(executor.getClass().getSimpleName(), executor);
        }
        
        // 然后按节点类型注册，避免在 Workflow 中依赖 Spring Bean 名称
        for (WorkflowNodeExecutor executor : executors) {
            for (String candidate : new String[]{"START", "END", "ACTIVITY", "CONDITION", "DELAY", "HUMAN_INPUT", "CHILD_WORKFLOW", "TRANSFORM"}) {
                if (executor.supports(candidate)) {
                    delegate.put(candidate, executor);
                }
            }
        }
    }

    /**
     * 获取节点执行器
     * <p>
     * 根据节点类型获取对应的执行器实例。
     * 
     * @param type 节点类型
     * @return 对应的工作流节点执行器
     * @throws IllegalStateException 当找不到对应类型的执行器时抛出
     */
    public WorkflowNodeExecutor get(String type) {
        WorkflowNodeExecutor executor = delegate.get(type);
        if (executor == null) {
            throw new IllegalStateException("No executor registered for node type=" + type);
        }
        return executor;
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.DslNodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器注册表
 * <p>
 * 负责管理和注册不同类型的工作流节点执行器，根据节点类型获取对应的执行器。
 * 支持通过类名和节点类型两种方式注册和获取执行器。
 * 使用 Spring 依赖注入自动收集所有执行器实现。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
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
     * 使用 Spring 依赖注入自动收集所有 WorkflowNodeExecutor 实现。
     *
     * @param executors 工作流节点执行器列表
     */
    @Autowired
    public NodeExecutorRegistry(List<WorkflowNodeExecutor> executors) {
        // 首先按执行器类名注册
        for (WorkflowNodeExecutor executor : executors) {
            delegate.put(executor.getClass().getSimpleName(), executor);
        }

        // 然后让每个执行器自己注册支持的类型（动态推导，不再硬编码）
        for (WorkflowNodeExecutor executor : executors) {
            registerExecutor(executor);
        }
    }

    /**
     * 注册执行器支持的类型
     * <p>
     * 通过执行器的 supports 方法动态发现其支持的类型，避免硬编码。
     *
     * @param executor 工作流节点执行器
     */
    private void registerExecutor(WorkflowNodeExecutor executor) {
        // 遍历所有可能的节点类型（从 DslNodeType 枚举获取）
        for (DslNodeType type : DslNodeType.values()) {
            String typeName = type.name();
            if (executor.supports(typeName)) {
                delegate.put(typeName, executor);
                break; // 一个执行器只支持一种主要类型
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

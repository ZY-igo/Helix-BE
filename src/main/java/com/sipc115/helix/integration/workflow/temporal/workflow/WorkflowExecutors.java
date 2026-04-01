/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.temporal.workflow;

import com.sipc115.helix.integration.workflow.runtime.NodeExecutorRegistry;
import com.sipc115.helix.integration.workflow.runtime.TransitionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 工作流执行器管理类
 * <p>
 * 提供共享的执行器注册表和转换解析器，避免每次工作流执行时重复创建实例，提高性能。
 * 使用 Spring 依赖注入管理所有执行器实例，同时提供静态访问方式。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class WorkflowExecutors {
    
    /**
     * 共享的无状态转换解析器
     * <p>
     * 用于根据当前节点和分支结果计算下一个要执行的节点。
     */
    public static TransitionResolver TRANSITION_RESOLVER;
    
    /**
     * 执行器注册表
     * <p>
     * 包含所有类型的工作流节点执行器。
     */
    public static NodeExecutorRegistry REGISTRY;
    
    /**
     * 构造函数
     * <p>
     * 初始化工作流执行器管理类，注入转换解析器和执行器注册表，并设置为静态字段。
     * 
     * @param transitionResolver 转换解析器
     * @param registry 执行器注册表
     */
    @Autowired
    public WorkflowExecutors(TransitionResolver transitionResolver, NodeExecutorRegistry registry) {
        TRANSITION_RESOLVER = transitionResolver;
        REGISTRY = registry;
    }
}

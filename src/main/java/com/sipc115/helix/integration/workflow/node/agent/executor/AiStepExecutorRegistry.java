/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 步骤执行器注册表
 * <p>
 * 管理和注册所有的 AI 步骤执行器，根据步骤类型分发执行器。
 * 使用 Spring 依赖注入自动收集所有 AiStepExecutor 实现。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiStepExecutorRegistry {
    private final Map<String, AiStepExecutor> executors = new HashMap<>();

    /**
     * 构造函数
     * <p>
     * 初始化执行器注册表，注册所有提供的执行器实例。
     * 使用 Spring 依赖注入自动收集所有 AiStepExecutor 实现。
     *
     * @param executors AI 步骤执行器列表
     */
    @Autowired
    public AiStepExecutorRegistry(List<AiStepExecutor> executors) {
        // 注册所有执行器
        for (AiStepExecutor executor : executors) {
            String supportedType = executor.getSupportedType();
            if (supportedType != null && !supportedType.isEmpty()) {
                this.executors.put(supportedType, executor);
            }
        }
    }

    /**
     * 获取执行器
     * <p>
     * 根据步骤类型获取对应的执行器。
     *
     * @param stepType 步骤类型
     * @return 对应的执行器
     * @throws IllegalArgumentException 如果步骤类型不支持
     */
    public AiStepExecutor getExecutor(String stepType) {
        AiStepExecutor executor = executors.get(stepType);
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported step type: " + stepType);
        }
        return executor;
    }

    /**
     * 注册执行器
     * <p>
     * 注册自定义执行器。
     *
     * @param stepType 步骤类型
     * @param executor 执行器
     */
    public void registerExecutor(String stepType, AiStepExecutor executor) {
        executors.put(stepType, executor);
    }
}

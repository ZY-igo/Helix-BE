/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

/**
 * Temporal Activity 工厂实现
 * <p>
 * 基于 Temporal SDK 实现 ActivityFactory 接口，
 * 使用 {@link Workflow#newActivityStub(Class, ActivityOptions)} 创建 Activity 存根。
 * <p>
 * 此实现负责将 ActivityInvocationSpec 转换为 Temporal 的 ActivityOptions，
 * 并在当前 Workflow 上下文中创建 Activity 实例。
 *
 * @author Helix Team
 * @since 2.0.0
 */
public class TemporalActivityFactory implements ActivityFactory {

    /**
     * 获取 Activity 实例（使用默认配置）
     * <p>
     * 使用 {@link ActivityInvocationSpec#defaultSpec()} 作为默认配置创建 Activity 实例。
     *
     * @param activityClass Activity 接口类
     * @param <T> Activity 接口类型
     * @return Activity 实例
     */
    @Override
    public <T> T getActivity(Class<T> activityClass) {
        return getActivity(activityClass, ActivityInvocationSpec.defaultSpec());
    }

    /**
     * 获取 Activity 实例（指定配置）
     * <p>
     * 根据提供的 ActivityInvocationSpec 构建 ActivityOptions，
     * 并在当前 Workflow 上下文中创建 Activity 存根。
     * <p>
     * 配置转换逻辑：
     * <ul>
     *   <li>startToCloseTimeout: 转换为 ActivityOptions 的超时时间</li>
     *   <li>taskQueue: 如果指定，设置 Activity 的任务队列</li>
     *   <li>retryOptions: 如果指定，设置 Activity 的重试策略</li>
     * </ul>
     *
     * @param activityClass Activity 接口类
     * @param spec Activity 调用规格
     * @param <T> Activity 接口类型
     * @return Activity 实例
     * @throws IllegalStateException 如果不在 Workflow 上下文中调用
     */
    @Override
    public <T> T getActivity(Class<T> activityClass, ActivityInvocationSpec spec) {
        // 构建 Activity 选项
        ActivityOptions.Builder optionsBuilder = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(spec.getStartToCloseTimeout());

        // 设置任务队列（如果指定）
        if (spec.getTaskQueue() != null && !spec.getTaskQueue().isEmpty()) {
            optionsBuilder.setTaskQueue(spec.getTaskQueue());
        }

        // 设置重试选项（如果指定）
        if (spec.getRetryOptions() != null) {
            optionsBuilder.setRetryOptions(spec.getRetryOptions());
        }

        // 在当前 Workflow 上下文中创建 Activity 存根
        return Workflow.newActivityStub(activityClass, optionsBuilder.build());
    }
}

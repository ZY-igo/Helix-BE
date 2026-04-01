/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

/**
 * Activity 工厂接口
 * <p>
 * 用于在 Workflow 上下文中创建 typed activity stub。
 * 提供两种创建 Activity 实例的方式：使用默认配置或指定自定义配置。
 * <p>
 * 此接口是工作流执行器与 Temporal Activity 之间的桥梁，
 * 允许节点执行器按需创建具有特定配置的 Activity 实例。
 *
 * @author Helix Team
 * @since 2.0.0
 */
public interface ActivityFactory {

    /**
     * 获取 Activity 实例（使用默认配置）
     * <p>
     * 使用默认的 Activity 调用规格创建 Activity 实例。
     * 适用于不需要特殊配置的标准 Activity 调用。
     *
     * @param activityClass Activity 接口类
     * @param <T> Activity 接口类型
     * @return Activity 实例，可用于调用 Activity 方法
     */
    <T> T getActivity(Class<T> activityClass);

    /**
     * 获取 Activity 实例（指定配置）
     * <p>
     * 使用指定的 Activity 调用规格创建 Activity 实例。
     * 允许为特定 Activity 调用配置超时时间、任务队列和重试策略等参数。
     * <p>
     * 使用示例：
     * <pre>
     * ActivityInvocationSpec spec = ActivityInvocationSpec.fromNodeConfig(nodeConfig);
     * FeishuNotificationActivity activity = factory.getActivity(FeishuNotificationActivity.class, spec);
     * </pre>
     *
     * @param activityClass Activity 接口类
     * @param spec Activity 调用规格，包含超时、队列、重试等配置
     * @param <T> Activity 接口类型
     * @return Activity 实例，可用于调用 Activity 方法
     */
    <T> T getActivity(Class<T> activityClass, ActivityInvocationSpec spec);
}

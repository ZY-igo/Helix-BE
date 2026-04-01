/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

import io.temporal.common.RetryOptions;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Map;

/**
 * Activity 调用规格
 * <p>
 * 封装单个 Activity 调用的运行时配置，由节点配置编译而来。
 * 支持从节点配置中解析超时时间、任务队列和重试策略等参数。
 * <p>
 * 使用 Builder 模式构建，提供默认配置和从节点配置解析的能力。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@Builder
public class ActivityInvocationSpec {

    /**
     * 任务队列（可选，默认使用工作流的队列）
     * <p>
     * 指定 Activity 执行的任务队列，用于负载均衡和优先级控制。
     * 如果未指定，则使用工作流默认的任务队列。
     */
    private String taskQueue;

    /**
     * 超时时间
     * <p>
     * Activity 从开始执行到完成的超时时间。
     * 如果 Activity 在此时间内未完成，将被标记为失败。
     * 默认值为 2 分钟。
     */
    private Duration startToCloseTimeout;

    /**
     * 重试配置
     * <p>
     * Activity 失败时的重试策略配置，包括初始间隔、最大间隔和最大重试次数。
     * 如果未指定，则使用 Temporal 默认的重试策略。
     */
    private RetryOptions retryOptions;

    /**
     * 从节点配置构建规格
     * <p>
     * 解析节点配置中的 activityConfig 部分，构建 Activity 调用规格。
     * 支持的配置项包括：
     * <ul>
     *   <li>taskQueue: 任务队列名称</li>
     *   <li>timeout: 超时时间（ISO-8601 格式，如 PT2M）</li>
     *   <li>retry: 重试配置，包含 initialInterval、maxInterval、maxAttempts</li>
     * </ul>
     *
     * @param nodeConfig 节点配置映射
     * @return Activity 调用规格
     */
    public static ActivityInvocationSpec fromNodeConfig(Map<String, Object> nodeConfig) {
        if (nodeConfig == null) {
            return defaultSpec();
        }

        // 解析活动配置（编译时已预处理）
        Object activityConfigObj = nodeConfig.get("activityConfig");
        if (!(activityConfigObj instanceof Map)) {
            return defaultSpec();
        }

        Map<?, ?> activityConfig = (Map<?, ?>) activityConfigObj;

        // 构建重试选项
        RetryOptions retryOptions = null;
        Object retryObj = activityConfig.get("retry");
        if (retryObj instanceof Map) {
            Map<?, ?> retryConfig = (Map<?, ?>) retryObj;
            retryOptions = RetryOptions.newBuilder()
                .setInitialInterval(Duration.parse(getString(retryConfig, "initialInterval", "PT1S")))
                .setMaximumInterval(Duration.parse(getString(retryConfig, "maxInterval", "PT1M")))
                .setMaximumAttempts(getInteger(retryConfig, "maxAttempts", 3))
                .build();
        }

        return ActivityInvocationSpec.builder()
            .taskQueue(getString(activityConfig, "taskQueue", null))
            .startToCloseTimeout(Duration.parse(
                getString(activityConfig, "timeout", "PT2M")
            ))
            .retryOptions(retryOptions)
            .build();
    }

    /**
     * 默认规格
     * <p>
     * 创建默认的 Activity 调用规格：
     * <ul>
     *   <li>超时时间：2 分钟</li>
     *   <li>初始重试间隔：1 秒</li>
     *   <li>最大重试间隔：1 分钟</li>
     *   <li>最大重试次数：3 次</li>
     * </ul>
     *
     * @return 默认的 Activity 调用规格
     */
    public static ActivityInvocationSpec defaultSpec() {
        return ActivityInvocationSpec.builder()
            .startToCloseTimeout(Duration.ofMinutes(2))
            .retryOptions(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(1))
                .setMaximumInterval(Duration.ofMinutes(1))
                .setMaximumAttempts(3)
                .build())
            .build();
    }

    /**
     * 从映射中获取字符串值
     *
     * @param map 配置映射
     * @param key 键
     * @param defaultValue 默认值
     * @return 字符串值，如果键不存在则返回默认值
     */
    private static String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 从映射中获取整数值
     *
     * @param map 配置映射
     * @param key 键
     * @param defaultValue 默认值
     * @return 整数值，如果键不存在或解析失败则返回默认值
     */
    private static Integer getInteger(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        return value != null ? Integer.parseInt(value.toString()) : defaultValue;
    }
}

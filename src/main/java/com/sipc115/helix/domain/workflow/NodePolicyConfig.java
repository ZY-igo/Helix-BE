/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点策略配置类
 * <p>
 * 定义工作流节点的执行策略，包括重试、超时等配置。
 * 这些配置由 DSL 控制，由 Temporal 的 Activity 具体执行。
 *
 * <p>策略配置示例：
 * <pre>
 * {
 *   "retryPolicy": {
 *     "maxAttempts": 3,
 *     "initialInterval": "1s",
 *     "maxInterval": "10s",
 *     "backoffCoefficient": 2.0,
 *     "retryableExceptions": ["IOException", "TimeoutException"]
 *   },
 *   "timeout": {
 *     "executionTimeout": "5m",
 *     "scheduleToCloseTimeout": "10m",
 *     "scheduleToStartTimeout": "30s",
 *     "startToCloseTimeout": "5m"
 *   }
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class NodePolicyConfig {

    /**
     * 重试策略
     * <p>
     * 配置节点执行失败时的重试行为。
     */
    private RetryPolicy retryPolicy = new RetryPolicy();

    /**
     * 超时配置
     * <p>
     * 配置节点执行的各种超时限制。
     */
    private TimeoutConfig timeout = new TimeoutConfig();

    /**
     * 重试策略配置
     */
    @Data
    public static class RetryPolicy {

        /**
         * 最大重试次数
         * <p>
         * 节点执行失败后最多重试的次数。
         * 默认值为 3 次。
         */
        private Integer maxAttempts = 3;

        /**
         * 初始重试间隔
         * <p>
         * 第一次重试前的等待时间。
         * 默认值为 1 秒。
         */
        private Duration initialInterval = Duration.ofSeconds(1);

        /**
         * 最大重试间隔
         * <p>
         * 重试间隔的最大值。
         * 默认值为 10 秒。
         */
        private Duration maxInterval = Duration.ofSeconds(10);

        /**
         * 重试间隔乘数
         * <p>
         * 每次重试后间隔的增长倍数（指数退避）。
         * 默认值为 2.0。
         */
        private Double backoffCoefficient = 2.0;

        /**
         * 可重试的异常类型
         * <p>
         * 哪些异常类型应该触发重试。
         * 为空时表示所有异常都可重试。
         */
        private List<String> retryableExceptions = new ArrayList<>();

        /**
         * 不可重试的异常类型
         * <p>
         * 哪些异常类型不应该触发重试（立即失败）。
         */
        private List<String> nonRetryableExceptions = new ArrayList<>();
    }

    /**
     * 超时配置
     */
    @Data
    public static class TimeoutConfig {

        /**
         * 执行超时
         * <p>
         * Activity 执行的最长时间限制。
         * 默认值为 5 分钟。
         */
        private Duration executionTimeout = Duration.ofMinutes(5);

        /**
         * 调度到关闭超时
         * <p>
         * 从任务被调度到完成的总时间限制。
         * 默认值为 10 分钟。
         */
        private Duration scheduleToCloseTimeout = Duration.ofMinutes(10);

        /**
         * 调度到开始超时
         * <p>
         * 任务被调度到开始执行的时间限制。
         * 默认值为 30 秒。
         */
        private Duration scheduleToStartTimeout = Duration.ofSeconds(30);

        /**
         * 开始到关闭超时
         * <p>
         * Activity 开始执行到完成的时间限制。
         * 默认值为 5 分钟。
         */
        private Duration startToCloseTimeout = Duration.ofMinutes(5);
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点策略配置类
 * <p>
 * 定义工作流节点的执行策略，包括重试、超时、错误处理等配置。
 * 这些配置由 DSL 中的用户定义，在编译时转换为 Temporal Activity 的执行参数。
 *
 * <h3>配置层级：</h3>
 * <ul>
 *   <li>节点级别策略 - 每个节点可以有自己的策略配置</li>
 *   <li>工作流级别默认策略 - 如果节点没有配置，使用工作流的默认策略</li>
 *   <li>系统级别策略 - 最高优先级的全局限制策略</li>
 * </ul>
 *
 * <h3>策略配置示例（JSON 格式）：</h3>
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
 *   },
 *   "errorHandling": "FAIL_FAST"
 * }
 * </pre>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>配置节点执行失败时的重试次数和间隔</li>
 *   <li>配置节点执行的超时时间</li>
 *   <li>配置节点执行失败时的错误处理策略</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see RetryPolicy
 * @see TimeoutConfig
 */
@Data
public class NodePolicyConfig {

    /**
     * 错误处理策略枚举
     * <p>
     * 定义当节点执行失败时，工作流引擎应该采取的行为。
     * 不同的策略决定了工作流的容错能力和恢复方式。
     *
     * <h3>策略对比：</h3>
     * <table border="1">
     *   <tr><th>策略</th><th>描述</th><th>适用场景</th></tr>
     *   <tr>
     *     <td>FAIL_FAST</td>
     *     <td>快速失败，遇到第一个错误立即停止</td>
     *     <td>关键业务流程，不允许部分成功</td>
     *   </tr>
     *   <tr>
     *     <td>FAIL_AFTER_COMPLETION</td>
     *     <td>等待所有节点执行完毕后报告失败</td>
     *     <td>需要完整执行报告的分析场景</td>
     *   </tr>
     *   <tr>
     *     <td>CONTINUE_WITH_ERRORS</td>
     *     <td>忽略错误，继续执行下游节点</td>
     *     <td>非关键路径，可接受部分失败</td>
     *   </tr>
     *   <tr>
     *     <td>COMPENSATE</td>
     *     <td>执行补偿逻辑后报告失败</td>
     *     <td>需要保证数据一致性的场景</td>
     *   </tr>
     * </table>
     */
    public enum ErrorHandlingStrategy {

        /**
         * 快速失败模式（默认）
         * <p>
         * 当节点执行失败时，立即停止工作流执行。
         * 不执行任何重试或补偿逻辑。
         *
         * <h3>行为描述：</h3>
         * <ol>
         *   <li>节点执行失败</li>
         *   <li>检查重试策略，如果没有重试次数则直接失败</li>
         *   <li>工作流状态标记为 FAILED</li>
         *   <li>已执行节点的输出保留，已失败节点记录错误</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>关键业务审批流程</li>
         *   <li>数据写入类操作（不允许部分成功）</li>
         *   <li>财务相关的业务流程</li>
         * </ul>
         */
        FAIL_FAST,

        /**
         * 完成后再失败模式
         * <p>
         * 即使遇到错误，也继续执行所有可执行的节点。
         * 在所有节点执行完毕后，才将工作流状态标记为 FAILED。
         *
         * <h3>行为描述：</h3>
         * <ol>
         *   <li>节点执行失败</li>
         *   <li>标记节点状态为 FAILED，继续执行其他分支</li>
         *   <li>等所有节点执行完毕（或无法继续）</li>
         *   <li>工作流状态标记为 PARTIAL_FAILED</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>批量数据处理任务</li>
         *   <li>需要完整执行报告的统计场景</li>
         *   <li>离线分析类任务</li>
         * </ul>
         */
        FAIL_AFTER_COMPLETION,

        /**
         * 带错误继续执行模式
         * <p>
         * 当节点执行失败时，继续执行所有下游节点。
         * 不等待，不阻塞，将失败的节点视为已跳过。
         *
         * <h3>行为描述：</h3>
         * <ol>
         *   <li>节点 A 执行失败</li>
         *   <li>节点 B 依赖 A，但 B 有其他有效前驱，继续执行 B</li>
         *   <li>节点 C 只依赖 A，A 失败导致 C 被跳过</li>
         *   <li>工作流最终状态可能是 SUCCESS（如果关键节点成功）</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>通知类任务（发送失败不影响主流程）</li>
         *   <li>日志记录类任务</li>
         *   <li>可选的优化步骤</li>
         * </ul>
         */
        CONTINUE_WITH_ERRORS,

        /**
         * 补偿模式
         * <p>
         * 当节点执行失败时，先执行已成功节点的补偿逻辑，
         * 然后再报告失败。这是一种分布式事务的解决方案。
         *
         * <h3>行为描述：</h3>
         * <ol>
         *   <li>节点 A、B、C 依次执行成功</li>
         *   <li>节点 D 执行失败</li>
         *   <li>按逆序执行 C、B、A 的补偿逻辑</li>
         *   <li>工作流状态标记为 COMPENSATED_FAILED</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>订单处理流程（需要取消已付款订单）</li>
         *   <li>资源分配场景（需要释放已分配资源）</li>
         *   <li>跨系统数据同步（需要回滚已同步数据）</li>
         * </ul>
         *
         * <h3>注意事项：</h3>
         * <ul>
         *   <li>需要节点实现 CompensatableAction 接口</li>
         *   <li>补偿逻辑可能也会失败，需要有降级策略</li>
         *   <li>补偿会增加执行时间和复杂度</li>
         * </ul>
         */
        COMPENSATE
    }

    /**
     * 重试策略配置
     * <p>
     * 定义节点执行失败时的重试行为。
     * 包括最大重试次数、重试间隔、退避策略等。
     *
     * <h3>重试流程：</h3>
     * <pre>
     * 执行失败
     *    │
     *    ▼
     * 检查剩余重试次数
     *    │
     *    ├─── 次数 > 0 ──→ 等待间隔时间 ──→ 重试执行
     *    │                       │
     *    │                       └─── 重复上述流程
     *    │
     *    └─── 次数 = 0 ──→ 报告失败
     * </pre>
     *
     * <h3>退避策略：</h3>
     * <ul>
     *   <li>固定间隔：每次重试间隔相同</li>
     *   <li>指数退避：间隔时间指数增长（推荐）</li>
     *   <li>抖动：在间隔基础上添加随机抖动，避免惊群效应</li>
     * </ul>
     *
     * @see RetryPolicy
     */
    private RetryPolicy retryPolicy = new RetryPolicy();

    /**
     * 超时配置
     * <p>
     * 定义节点执行的各种超时限制。
     * Temporal Activity 支持多种超时配置，用于控制不同阶段的执行时间。
     *
     * <h3>超时类型：</h3>
     * <ul>
     *   <li>ScheduleToStartTimeout: 任务从队列到开始执行的时间</li>
     *   <li>StartToCloseTimeout: 任务开始到完成的时间</li>
     *   <li>ScheduleToCloseTimeout: 任务入队到完成的总时间</li>
     *   <li>HeartbeatTimeout: 两次心跳之间的最大间隔</li>
     * </ul>
     *
     * @see TimeoutConfig
     */
    private TimeoutConfig timeout = new TimeoutConfig();

    /**
     * 错误处理策略
     * <p>
     * 指定当节点执行失败时采取的策略。
     * 默认为 FAIL_FAST（快速失败）。
     *
     * <h3>配置示例：</h3>
     * <pre>
     * {@code
     * NodePolicyConfig config = new NodePolicyConfig();
     * config.setErrorHandling(ErrorHandlingStrategy.CONTINUE_WITH_ERRORS);
     * }
     * </pre>
     *
     * @see ErrorHandlingStrategy
     */
    private ErrorHandlingStrategy errorHandling = ErrorHandlingStrategy.FAIL_FAST;

    /**
     * 重试策略配置
     * <p>
     * 定义节点执行失败时的重试行为。
     * 包括最大重试次数、重试间隔、退避策略等。
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
     * <p>
     * 定义节点执行的各种超时限制。
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
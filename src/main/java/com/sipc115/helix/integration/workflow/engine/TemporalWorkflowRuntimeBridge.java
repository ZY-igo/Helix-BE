/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Temporal 工作流运行时实现（纯原语版本）
 * <p>
 * 基于 Temporal 框架实现工作流运行时桥接，只提供 Temporal 特有的核心能力：
 * <ul>
 *   <li>人工信号等待：支持工作流暂停等待外部人工输入</li>
 *   <li>持久化睡眠：支持工作流在持久化状态下休眠指定时间</li>
 *   <li>Activity 工厂：提供创建 Activity 实例的能力</li>
 * </ul>
 * <p>
 * 此实现不包含任何业务语义，所有业务逻辑（如飞书通知）通过 Activity 工厂
 * 创建的 Activity 实例来执行。
 *
 * @author Helix Team
 * @since 2.0.0
 */
public class TemporalWorkflowRuntimeBridge implements WorkflowRuntimeBridge {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(TemporalWorkflowRuntimeBridge.class);

    /**
     * 缓冲的人工输入信号队列
     * <p>
     * 用于存储在工作流等待期间到达的信号，确保信号不会丢失。
     * 当工作流调用 awaitHumanSignal 时，会从此队列中查找匹配的信号。
     */
    private final List<HumanSignalPayload> bufferedSignals;

    /**
     * Activity 工厂实例
     * <p>
     * 用于在 Workflow 上下文中创建 Activity 存根。
     */
    private final ActivityFactory activityFactory;

    /**
     * 构造函数
     * <p>
     * 初始化 TemporalWorkflowRuntimeBridge，创建 Activity 工厂实例。
     *
     * @param bufferedSignals 缓冲的人工输入信号队列
     */
    public TemporalWorkflowRuntimeBridge(List<HumanSignalPayload> bufferedSignals) {
        this.bufferedSignals = bufferedSignals;
        // 创建工厂实例
        this.activityFactory = new TemporalActivityFactory();
    }

    /**
     * 获取 Activity 工厂
     * <p>
     * 返回 Activity 工厂实例，用于在 Workflow 上下文中创建 Activity 存根。
     * 节点执行器可以通过此工厂创建具有特定配置的 Activity 实例。
     *
     * @return Activity 工厂实例
     */
    @Override
    public ActivityFactory activities() {
        return activityFactory;
    }

    /**
     * 等待人工信号
     * <p>
     * 暂停工作流执行，直到接收到指定节点的人工输入信号。
     * 使用 Temporal 的 {@link Workflow#await(java.util.function.Supplier)} 机制实现非阻塞等待。
     * <p>
     * 工作流程：
     * <ol>
     *   <li>使用 Workflow.await 等待 bufferedSignals 中出现匹配的信号</li>
     *   <li>找到匹配信号后，从队列中移除并返回</li>
     * </ol>
     *
     * @param expectedNodeId 期望接收信号的节点 ID
     * @return 人工输入的有效载荷
     * @throws IllegalStateException 如果信号到达但找不到匹配的负载
     */
    @Override
    public HumanSignalPayload awaitHumanSignal(String expectedNodeId) {
        logger.info("Awaiting human signal for node: {}", expectedNodeId);

        // 使用 Temporal 的 await 机制等待信号
        // 此调用是非阻塞的，工作流会持久化状态并等待条件满足
        Workflow.await(() -> bufferedSignals.stream()
                .anyMatch(signal -> expectedNodeId.equals(signal.getNodeId())));

        // 查找并返回匹配的信号
        for (HumanSignalPayload payload : bufferedSignals) {
            if (expectedNodeId.equals(payload.getNodeId())) {
                bufferedSignals.remove(payload);
                logger.info("Received human signal for node: {}", expectedNodeId);
                return payload;
            }
        }

        // 理论上不会到达这里，因为 await 已经确保信号存在
        logger.error("Signal arrived but matching payload not found for node: {}", expectedNodeId);
        throw new IllegalStateException("Signal arrived but matching payload not found");
    }

    /**
     * 持久化睡眠
     * <p>
     * 使工作流在持久化状态下休眠指定的时间。
     * 使用 Temporal 的 {@link Workflow#sleep(Duration)} 实现，确保：
     * <ul>
     *   <li>工作流状态被持久化</li>
     *   <li>工作流进程可以退出以节省资源</li>
     *   <li>到期后 Temporal 自动恢复工作流执行</li>
     * </ul>
     *
     * @param duration 休眠时间
     */
    @Override
    public void durableSleep(Duration duration) {
        logger.info("Starting durable sleep for {}", duration);
        Workflow.sleep(duration);
        logger.info("Durable sleep completed");
    }
}

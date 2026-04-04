/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.integration.workflow.engine.ActivityFactory;

import java.time.Duration;

/**
 * 工作流运行时接口（纯 Temporal 原语）
 * <p>
 * 定义工作流运行时与底层执行引擎（如 Temporal）之间的桥接方法。
 * 只提供 Temporal 特有的核心能力，不包含任何业务语义。
 * <p>
 * 核心能力：
 * <ul>
 *   <li>人工信号等待：支持工作流暂停等待外部人工输入</li>
 *   <li>持久化睡眠：支持工作流在持久化状态下休眠指定时间</li>
 *   <li>Activity 工厂：提供创建 Activity 实例的能力，用于执行业务逻辑</li>
 * </ul>
 * <p>
 * 此接口隔离了工作流执行逻辑与具体的执行引擎实现，便于替换不同的执行引擎。
 *
 * @author Helix Team
 * @since 2.0.0
 */
public interface WorkflowRuntimeBridge {

    /**
     * 等待人工信号（无超时）
     * <p>
     * 暂停工作流执行，直到接收到指定节点的人工输入信号。
     * 此方法会无限等待信号到达。
     *
     * @param expectedNodeId 期望接收信号的节点 ID
     * @return 人工输入的有效载荷，包含节点 ID 和输入数据
     */
    HumanSignalPayload awaitHumanSignal(String expectedNodeId);

    /**
     * 等待人工信号（带超时）
     * <p>
     * 暂停工作流执行，直到接收到指定节点的人工输入信号或超过指定超时时间。
     * 如果超时，将返回 null 调用方需要处理超时情况。
     *
     * @param expectedNodeId 期望接收信号的节点 ID
     * @param timeout 最大等待时间，如果为 null 则表示无限等待
     * @return 人工输入的有效载荷，超时返回 null
     */
    HumanSignalPayload awaitHumanSignal(String expectedNodeId, Duration timeout);

    /**
     * 持久化睡眠
     * <p>
     * 使工作流在持久化状态下休眠指定的时间。
     * 工作流状态会被持久化，工作流进程可以退出，到期后自动恢复执行。
     *
     * @param duration 休眠时间
     */
    void durableSleep(Duration duration);

    /**
     * 获取 Activity 工厂
     * <p>
     * 用于在 Workflow 上下文中创建 Activity Stub。
     * 节点执行器可以通过此工厂创建具有特定配置的 Activity 实例来执行业务逻辑。
     *
     * @return Activity 工厂实例
     */
    ActivityFactory activities();
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;

import java.time.Duration;
import java.util.Map;

/**
 * 工作流运行时桥接接口
 * <p>
 * 定义工作流运行时与底层执行引擎（如 Temporal）之间的桥接方法，
 * 提供活动任务执行、子工作流调用、持久化睡眠和人工信号等待等功能。
 * <p>
 * 此接口隔离了工作流执行逻辑与具体的执行引擎实现，便于替换不同的执行引擎。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface WorkflowRuntimeBridge {

    /**
     * 等待人工信号
     * <p>
     * 暂停工作流执行，直到接收到指定节点的人工输入信号。
     * 
     * @param expectedNodeId 期望接收信号的节点 ID
     * @return 人工输入的有效载荷，包含节点 ID 和输入数据
     */
    HumanSignalPayload awaitHumanSignal(String expectedNodeId);
}

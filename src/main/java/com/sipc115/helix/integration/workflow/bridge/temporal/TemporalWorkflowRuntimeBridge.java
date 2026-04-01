/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.bridge.temporal;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.bridge.temporal.activity.ActivityTaskRouterActivity;
import com.sipc115.helix.domain.workflow.ActivityTaskRequest;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Temporal 工作流运行时桥接实现
 * <p>
 * 为节点执行器提供 Temporal 特定的底层运行时能力，
 * 如调用 Activity、子工作流、等待信号等。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class TemporalWorkflowRuntimeBridge implements WorkflowRuntimeBridge {

    /**
     * 缓冲的人工输入信号队列
     * <p>
     * 用于存储在工作流等待期间到达的信号，确保信号不会丢失。
     */
    private final List<HumanSignalPayload> bufferedSignals;

    /**
     * 构造函数
     * <p>
     * 初始化 Temporal 工作流运行时桥接。
     *
     * @param bufferedSignals 人工输入信号缓冲队列
     */
    public TemporalWorkflowRuntimeBridge(List<HumanSignalPayload> bufferedSignals) {
        this.bufferedSignals = bufferedSignals;
    }

    /**
     * 等待人工输入信号
     * <p>
     * 阻塞当前工作流直到收到匹配的 Signal 信号。
     * 
     * @param expectedNodeId 期望接收信号的节点 ID
     * @return 人工输入的有效载荷
     * @throws IllegalStateException 当信号到达但找不到匹配的有效载荷时抛出
     */
    @Override
    public HumanSignalPayload awaitHumanSignal(String expectedNodeId) {
        // 使用 Temporal 的 await 机制等待，直到有匹配的信号到达
        Workflow.await(() -> bufferedSignals.stream().anyMatch(signal -> expectedNodeId.equals(signal.getNodeId())));

        // 查找并移除匹配的信号
        for (HumanSignalPayload payload : bufferedSignals) {
            if (expectedNodeId.equals(payload.getNodeId())) {
                bufferedSignals.remove(payload);
                return payload;
            }
        }
        throw new IllegalStateException("Signal arrived but matching payload not found");
    }
}

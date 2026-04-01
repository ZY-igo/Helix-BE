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
     * 活动任务路由器活动
     * <p>
     * 用于路由和执行外部活动任务。
     */
    private final ActivityTaskRouterActivity activityTaskRouterActivity;

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
     * @param activityTaskRouterActivity 活动任务路由器活动
     * @param bufferedSignals 人工输入信号缓冲队列
     */
    public TemporalWorkflowRuntimeBridge(ActivityTaskRouterActivity activityTaskRouterActivity, List<HumanSignalPayload> bufferedSignals) {
        this.activityTaskRouterActivity = activityTaskRouterActivity;
        this.bufferedSignals = bufferedSignals;
    }

    /**
     * 调用外部活动任务
     * <p>
     * 构建活动任务请求并通过活动任务路由器执行。
     * 
     * @param node 当前节点的编译后定义
     * @param input 活动的输入参数
     * @return 活动执行结果
     */
    @Override
    public Map<String, Object> invokeActivityTask(CompiledNode node, Map<String, Object> input) {
        ActivityTaskRequest request = new ActivityTaskRequest();
        request.setAction(node.getAction());      // 设置活动动作标识
        request.setInput(input);                   // 传递输入参数
        request.setConfig(node.getConfig());       // 传递节点配置
        return activityTaskRouterActivity.dispatch(request);
    }

    /**
     * 调用子工作流
     * <p>
     * 目前为占位实现，后续需要根据节点配置创建和执行子工作流。
     * 
     * @param node 当前节点的编译后定义
     * @param input 子工作流的输入参数
     * @return 子工作流执行结果
     */
    @Override
    public String invokeChildWorkflow(CompiledNode node, Map<String, Object> input) {
        // TODO 这里可以按 node.config 中的 workflowType / taskQueue / childOptions 创建子工作流 stub。
        // 建议把不同子流程抽象成注册表，再由 DSL 节点选择。
        return "TODO_CHILD_WORKFLOW_RESULT";
    }

    /**
     * 持久化休眠
     * <p>
     * 使用 Temporal 提供的可靠定时功能，即使 Worker 重启，休眠也会在恢复后继续计时。
     * 
     * @param duration 休眠时长
     */
    @Override
    public void durableSleep(Duration duration) {
        Workflow.sleep(duration);
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

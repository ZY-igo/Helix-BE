package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;

import java.time.Duration;

/**
 * 延迟节点执行器
 * <p>
 * 负责执行延迟类型的节点，通过WorkflowRuntimeBridge进行持久化睡眠
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public class DelayNodeExecutor implements WorkflowNodeExecutor {
    /**
     * 检查当前执行器是否支持指定类型的节点
     * 
     * @param type 节点类型
     * @return 是否支持该类型的节点
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.DELAY.name().equals(type);
    }

    /**
     * 执行延迟节点
     * <p>
     * 从节点配置中获取延迟时间（秒），然后通过WorkflowRuntimeBridge进行持久化睡眠
     * </p>
     * 
     * @param node 编译后的节点
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接器
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        // 从节点配置中获取延迟时间，默认为1秒
        long seconds = Long.parseLong(String.valueOf(node.getConfig().getOrDefault("seconds", "1")));
        // 执行持久化睡眠
        bridge.durableSleep(Duration.ofSeconds(seconds));
        // 返回完成状态的执行结果
        return NodeExecutionResult.completed();
    }
}

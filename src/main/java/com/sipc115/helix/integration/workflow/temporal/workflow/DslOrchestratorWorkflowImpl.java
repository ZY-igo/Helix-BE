/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.temporal.workflow;

import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.workflow.runtime.*;
import com.sipc115.helix.domain.workflow.ActivityTaskRequest;
import com.sipc115.helix.integration.workflow.temporal.activity.ActivityTaskRouterActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DSL 编排工作流实现类
 * <p>
 * 作为工作流引擎的核心，负责根据编译后的执行计划（ExecutionPlan）依次执行各个节点。
 * 实现了 DslOrchestratorWorkflow 接口，提供工作流的运行、信号处理和状态查询功能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class DslOrchestratorWorkflowImpl implements DslOrchestratorWorkflow {

    /**
     * 缓冲的人工输入信号队列
     * <p>
     * 用于存储在工作流等待期间到达的 Signal 信号，确保信号不会丢失。
     */
    private final List<HumanSignalPayload> bufferedSignals = new ArrayList<>();

    /**
     * 工作流主执行方法
     * <p>
     * 根据执行计划中的节点定义和转换关系，依次执行每个节点直到工作流结束。
     * 
     * @param plan 编译后的执行计划，包含所有节点和转换关系
     * @param input 工作流的初始输入参数
     */
    @Override
    public void run(ExecutionPlan plan, Map<String, Object> input) {
        // 创建 Activity 存根，用于调用外部活动（如业务操作、HTTP 请求等）
        ActivityTaskRouterActivity activityStub = createActivityStub();

        // 创建工作流运行时桥接对象，提供节点执行时需要的底层能力
        WorkflowRuntimeBridge bridge = new TemporalWorkflowRuntimeBridge(activityStub);

        // 创建执行上下文，保存工作流运行时的所有状态信息
        ExecutionContext context = new ExecutionContext(plan, input);

        // 使用共享的执行器注册表
        NodeExecutorRegistry registry = WorkflowExecutors.REGISTRY;

        // 执行核心逻辑
        executePlan(plan, context, bridge, registry);
    }

    /**
     * 创建 Activity 存根
     * <p>
     * 配置活动的超时时间和重试策略。
     * 
     * @return ActivityTaskRouterActivity 存根
     */
    private ActivityTaskRouterActivity createActivityStub() {
        return Workflow.newActivityStub(
                ActivityTaskRouterActivity.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofMinutes(5))  // 活动超时时间：5 分钟
                        .setRetryOptions(RetryOptions.newBuilder()
                                .setMaximumAttempts(3)  // 最大重试次数：3 次
                                .build())
                        .build());
    }

    /**
     * 执行工作流计划
     * <p>
     * 沿着 DSL 定义的路径依次执行节点，直到工作流结束。
     * 
     * @param plan 编译后的执行计划
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接
     * @param registry 节点执行器注册表
     */
    private void executePlan(
            ExecutionPlan plan,
            ExecutionContext context,
            WorkflowRuntimeBridge bridge,
            NodeExecutorRegistry registry
    ) {
        // 从入口节点开始执行
        String currentNodeId = plan.getEntryNodeId();
        context.setWorkflowStatus(ExecutionStatus.RUNNING);

        // 核心执行循环：沿着 DSL 定义的路径依次执行节点，直到没有下一个节点
        while (currentNodeId != null) {
            // 更新当前执行的节点 ID
            context.setCurrentNodeId(currentNodeId);

            // 从执行计划中获取当前节点的编译后定义
            CompiledNode node = plan.getNodes().get(currentNodeId);

            // 标记当前节点状态为运行中
            context.getNodeStatuses().put(currentNodeId, ExecutionStatus.RUNNING);

            // 根据节点类型从注册表中获取对应的执行器
            WorkflowNodeExecutor executor = registry.get(node.getType().name());

            // 执行节点逻辑，获取执行结果
            NodeExecutionResult result = executor.execute(node, context, bridge);

            // 处理节点输出：如果有输出数据，更新上下文变量
            if (result.getOutput() != null && !result.getOutput().isEmpty()) {
                // 将输出数据同时存入以节点 ID 为键的变量和直接展开的变量中
                context.getVariables().put(node.getId(), result.getOutput());
                context.getVariables().putAll(result.getOutput());
            }

            // 根据执行结果更新工作流和节点状态
            updateStatus(context, currentNodeId, result);

            // 检查是否为结束节点，如果是则终止工作流
            if ("END".equals(node.getType().name())) {
                context.setWorkflowStatus(ExecutionStatus.COMPLETED);
                return;
            }

            // 计算下一个要执行的节点 ID
            // 优先使用执行结果中直接指定的 nextNodeId，否则通过 TransitionResolver 根据分支结果计算
            currentNodeId = result.getNextNodeId() != null
                    ? result.getNextNodeId()
                    : WorkflowExecutors.TRANSITION_RESOLVER.nextNode(plan, node.getId(), result.getBranchKey());
        }

        // 循环结束，工作流执行完成
        context.setWorkflowStatus(ExecutionStatus.COMPLETED);
    }

    /**
     * 更新工作流和节点状态
     * <p>
     * 根据节点执行结果更新工作流和节点的状态。
     * 
     * @param context 执行上下文
     * @param nodeId 当前节点 ID
     * @param result 节点执行结果
     */
    private void updateStatus(ExecutionContext context, String nodeId, NodeExecutionResult result) {
        if (result.getStatus() == ExecutionStatus.WAITING_SIGNAL) {
            // 需要等待外部信号（如人工输入），暂停工作流
            context.setWorkflowStatus(ExecutionStatus.WAITING_SIGNAL);
            context.getNodeStatuses().put(nodeId, ExecutionStatus.WAITING_SIGNAL);
        } else if (result.getStatus() == ExecutionStatus.COMPLETED) {
            // 节点执行完成
            context.getNodeStatuses().put(nodeId, ExecutionStatus.COMPLETED);
            context.setWorkflowStatus(ExecutionStatus.RUNNING);
        } else {
            // 其他状态（如运行中、失败等）
            context.getNodeStatuses().put(nodeId, result.getStatus());
            context.setWorkflowStatus(result.getStatus());
        }
    }

    /**
     * 提供人工输入信号
     * <p>
     * 该方法会被外部调用的 Signal 触发，将人工输入添加到缓冲队列。
     * 
     * @param payload 人工输入的有效载荷，包含节点 ID 和用户输入数据
     */
    @Override
    public void provideHumanInput(HumanSignalPayload payload) {
        bufferedSignals.add(payload);
    }

    /**
     * 查询工作流当前状态
     * <p>
     * 该方法会被外部调用的 Query 触发，返回工作流的状态快照。
     * 
     * @return 工作流状态视图对象
     */
    @Override
    public WorkflowStateView currentState() {
        // Query 方法不能依赖 Spring Bean，这里只返回 Workflow 内部状态快照。
        // TODO 如果要更丰富的调试视图，可在 Workflow 中维护额外的运行轨迹。
        WorkflowStateView view = new WorkflowStateView();
        // 由于 Query 可能在 run 前后调用，返回一个最小可用对象。
        return view;
    }

    /**
     * Temporal 工作流运行时桥接实现
     * <p>
     * 为节点执行器提供底层运行时能力，如调用 Activity、子工作流、等待信号等。
     */
    private class TemporalWorkflowRuntimeBridge implements WorkflowRuntimeBridge {
        /**
         * 活动任务路由器活动
         * <p>
         * 用于路由和执行外部活动任务。
         */
        private final ActivityTaskRouterActivity activityTaskRouterActivity;

        /**
         * 构造函数
         * <p>
         * 初始化 Temporal 工作流运行时桥接。
         * 
         * @param activityTaskRouterActivity 活动任务路由器活动
         */
        private TemporalWorkflowRuntimeBridge(ActivityTaskRouterActivity activityTaskRouterActivity) {
            this.activityTaskRouterActivity = activityTaskRouterActivity;
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
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.*;

import com.sipc115.helix.integration.workflow.engine.TemporalWorkflowRuntimeBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DSL 编排工作流实现类（Temporal 版本）
 * <p>
 * 实现了 DslOrchestratorWorkflow 接口，提供 Temporal 工作流的运行、信号处理和状态查询功能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class DslOrchestratorWorkflowImpl {

    /**
     * 缓冲的人工输入信号队列
     * <p>
     * 用于存储在工作流等待期间到达的信号，确保信号不会丢失。
     */
    private final List<HumanSignalPayload> bufferedSignals = new ArrayList<>();

    /**
     * 节点执行器注册表
     * <p>
     * 用于获取不同类型节点的执行器。
     */
    private final NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * 转换解析器
     * <p>
     * 用于根据当前节点和分支结果计算下一个要执行的节点。
     */
    private final TransitionResolver transitionResolver;

    /**
     * 无参构造函数
     * <p>
     * Temporal 框架需要无参构造函数来创建工作流实例。
     * 使用默认的注册表和解析器实例。
     */
    public DslOrchestratorWorkflowImpl() {
        this(NodeExecutorRegistryHolder.INSTANCE, TransitionResolverHolder.INSTANCE);
    }

    /**
     * 带依赖的构造函数
     * <p>
     * 允许注入节点执行器注册表和转换解析器，便于测试和自定义。
     * 
     * @param nodeExecutorRegistry 节点执行器注册表
     * @param transitionResolver 转换解析器
     */
    public DslOrchestratorWorkflowImpl(NodeExecutorRegistry nodeExecutorRegistry, TransitionResolver transitionResolver) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.transitionResolver = transitionResolver;
    }

    /**
     * 工作流主执行方法
     * <p>
     * 根据执行计划中的节点定义和转换关系，依次执行每个节点直到工作流结束。
     * 
     * @param plan 编译后的执行计划，包含所有节点和转换关系
     * @param input 工作流的初始输入参数
     */
    public void run(ExecutionPlan plan, Map<String, Object> input) {
        // 创建工作流运行时桥接对象，提供节点执行时需要的底层能力
        WorkflowRuntimeBridge bridge = createWorkflowRuntimeBridge();

        // 创建执行上下文，保存工作流运行时的所有状态信息
        ExecutionContext context = new ExecutionContext(plan, input);

        // 执行核心逻辑
        executePlan(plan, context, bridge);
    }

    /**
     * 创建工作流运行时桥接
     * <p>
     * 返回 Temporal 特定的运行时桥接实现。
     * 
     * @return Temporal 工作流运行时桥接
     */
    private WorkflowRuntimeBridge createWorkflowRuntimeBridge() {
        return new TemporalWorkflowRuntimeBridge(bufferedSignals);
    }

    /**
     * 执行工作流计划
     * <p>
     * 沿着执行计划定义的路径依次执行节点，直到工作流结束。
     * 
     * @param plan 编译后的执行计划
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接
     */
    private void executePlan(
            ExecutionPlan plan,
            ExecutionContext context,
            WorkflowRuntimeBridge bridge
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
            WorkflowNodeExecutor executor = nodeExecutorRegistry.get(node.getType().name());

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
                    : transitionResolver.nextNode(plan, node.getId(), result.getBranchKey());
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
    public WorkflowStateView currentState() {
        // Query 方法不能依赖 Spring Bean，这里只返回 Workflow 内部状态快照。
        // TODO 如果要更丰富的调试视图，可在 Workflow 中维护额外的运行轨迹。
        WorkflowStateView view = new WorkflowStateView();
        // 由于 Query 可能在 run 前后调用，返回一个最小可用对象。
        return view;
    }

    /**
     * 节点执行器注册表持有者
     * <p>
     * 使用单例模式持有 NodeExecutorRegistry 实例，
     * 避免在 Workflow 类中使用静态字段存储 Spring Bean。
     */
    private static class NodeExecutorRegistryHolder {
        static NodeExecutorRegistry INSTANCE = new NodeExecutorRegistry(List.of());
    }

    /**
     * 转换解析器持有者
     * <p>
     * 使用单例模式持有 TransitionResolver 实例，
     * 避免在 Workflow 类中使用静态字段存储 Spring Bean。
     */
    private static class TransitionResolverHolder {
        static TransitionResolver INSTANCE = new TransitionResolver();
    }
}

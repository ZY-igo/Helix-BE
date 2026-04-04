/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.*;

import com.sipc115.helix.integration.workflow.engine.TemporalWorkflowRuntimeBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DSL 工作流编排器实现
 * <p>
 * 负责解释执行编译后的 ExecutionPlan，是 Helix 工作流引擎的核心调度器。
 *
 * <h3>核心职责：</h3>
 * <ul>
 *   <li>按拓扑顺序执行工作流节点</li>
 *   <li>管理执行上下文（ExecutionContext）</li>
 *   <li>协调节点执行器（NodeExecutor）</li>
 *   <li>处理节点间的数据传递</li>
 *   <li>根据条件分支选择下一节点</li>
 * </ul>
 *
 * <h3>执行流程：</h3>
 * <ol>
 *   <li>从入口节点开始，依次执行每个节点</li>
 *   <li>每个节点执行完成后，将输出合并到上下文变量</li>
 *   <li>根据分支键（branchKey）通过 TransitionResolver 选择下一节点</li>
 *   <li>遇到 END 节点时工作流结束</li>
 * </ol>
 *
 * <h3>节点输出合并机制：</h3>
 * <p>
 * 节点执行完成后，其输出会自动合并到执行上下文的变量中：
 * <ul>
 *   <li>完整输出以节点ID为键存储</li>
 *   <li>输出中的每个键值对也会展开合并到变量中</li>
 * </ul>
 * 这样后续节点可以直接引用前序节点的输出。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see ExecutionContext
 * @see NodeExecutorRegistry
 * @see TransitionResolver
 */
public class DslOrchestratorWorkflowImpl {

    /**
     * 默认节点执行器注册表（静态配置）
     * <p>
     * 使用 volatile 保证多线程可见性。
     * 用于在没有显式传入注册表时作为默认值。
     */
    private static volatile NodeExecutorRegistry defaultNodeExecutorRegistry;

    /**
     * 默认转换解析器（静态配置）
     */
    private static volatile TransitionResolver defaultTransitionResolver;

    /**
     * 缓冲的人工输入信号队列
     * <p>
     * 存储在工作流等待期间到达的人工输入信号。
     * 当工作流调用 awaitHumanSignal 时从队列中获取信号。
     */
    private final List<HumanSignalPayload> bufferedSignals = new ArrayList<>();

    /**
     * 节点执行器注册表
     * <p>
     * 根据节点类型查找对应的执行器实例。
     */
    private final NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * 转换解析器
     * <p>
     * 根据当前节点和分支键确定下一节点。
     */
    private final TransitionResolver transitionResolver;

    /**
     * 当前执行上下文
     * <p>
     * 存储工作流执行过程中的所有状态信息。
     */
    private ExecutionContext currentContext;

    /**
     * 配置默认的注册表和解析器
     * <p>
     * 静态方法，用于在 Temporal Worker 初始化时配置默认组件。
     *
     * @param nodeExecutorRegistry 节点执行器注册表
     * @param transitionResolver 转换解析器
     */
    public static void configureDefaults(
            NodeExecutorRegistry nodeExecutorRegistry,
            TransitionResolver transitionResolver
    ) {
        defaultNodeExecutorRegistry = nodeExecutorRegistry;
        defaultTransitionResolver = transitionResolver;
    }

    /**
     * 默认构造函数
     * <p>
     * 使用默认的注册表和解析器实例。
     * 如果静态配置存在则使用配置，否则使用临时实例。
     */
    public DslOrchestratorWorkflowImpl() {
        this(
                defaultNodeExecutorRegistry != null ? defaultNodeExecutorRegistry : NodeExecutorRegistryHolder.INSTANCE,
                defaultTransitionResolver != null ? defaultTransitionResolver : TransitionResolverHolder.INSTANCE
        );
    }

    /**
     * 构造函数
     *
     * @param nodeExecutorRegistry 节点执行器注册表
     * @param transitionResolver 转换解析器
     */
    public DslOrchestratorWorkflowImpl(NodeExecutorRegistry nodeExecutorRegistry, TransitionResolver transitionResolver) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.transitionResolver = transitionResolver;
    }

    /**
     * 运行工作流
     * <p>
     * 工作流的入口方法，启动整个工作流的执行。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>创建工作流运行时桥接器</li>
     *   <li>初始化执行上下文</li>
     *   <li>提取并设置执行ID</li>
     *   <li>调用 executePlan 开始执行</li>
     * </ol>
     *
     * @param plan 编译后的执行计划
     * @param input 工作流输入参数
     */
    public void run(ExecutionPlan plan, Map<String, Object> input) {
        // 创建工作流运行时桥接器，提供 Temporal 等底层能力
        WorkflowRuntimeBridge bridge = createWorkflowRuntimeBridge();

        // 初始化执行上下文，包含计划、变量、状态等
        ExecutionContext context = new ExecutionContext(plan, input);
        this.currentContext = context;

        // 从输入中提取执行ID（用于追踪）
        Long executionId = extractExecutionId(input);
        if (executionId != null) {
            context.setExecutionId(executionId);
        }

        // 开始执行工作流
        executePlan(plan, context, bridge);
    }

    /**
     * 从输入中提取执行ID
     * <p>
     * 支持多种类型的 executionId：Long、Integer、String。
     *
     * @param input 工作流输入参数
     * @return 执行ID，如果不存在返回 null
     */
    private Long extractExecutionId(Map<String, Object> input) {
        if (input == null) {
            return null;
        }
        Object executionId = input.get("_executionId");
        if (executionId instanceof Long) {
            return (Long) executionId;
        } else if (executionId instanceof Integer) {
            return ((Integer) executionId).longValue();
        } else if (executionId instanceof String) {
            try {
                return Long.parseLong((String) executionId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 创建工作流运行时桥接器
     *
     * @return Temporal 实现的工作流运行时桥接器
     */
    private WorkflowRuntimeBridge createWorkflowRuntimeBridge() {
        return new TemporalWorkflowRuntimeBridge(bufferedSignals);
    }

    /**
     * 执行工作流计划
     * <p>
     * 核心调度逻辑，按拓扑顺序执行节点。
     *
     * <h3>执行循环：</h3>
     * <ol>
     *   <li>获取当前节点定义</li>
     *   <li>查找对应的节点执行器</li>
     *   <li>调用执行器执行节点</li>
     *   <li>将节点输出合并到上下文变量</li>
     *   <li>更新节点和工作流状态</li>
     *   <li>根据分支键选择下一节点</li>
     * </ol>
     *
     * <h3>节点输出合并说明：</h3>
     * <p>
     * 节点执行完成后，其输出会自动合并到执行上下文的变量中：
     * <ul>
     *   <li>完整输出以节点ID为键存储 - 便于追溯来源</li>
     *   <li>输出中的每个键值对也会展开合并 - 便于后续节点直接引用</li>
     * </ul>
     *
     * @param plan 执行计划
     * @param context 执行上下文
     * @param bridge 工作流运行时桥接器
     */
    private void executePlan(
            ExecutionPlan plan,
            ExecutionContext context,
            WorkflowRuntimeBridge bridge
    ) {
        // 从入口节点开始
        String currentNodeId = plan.getEntryNodeId();
        context.setWorkflowStatus(ExecutionStatus.RUNNING);

        // 主执行循环
        while (currentNodeId != null) {
            // 设置当前节点ID
            context.setCurrentNodeId(currentNodeId);

            // 获取节点定义
            CompiledNode node = plan.getNodes().get(currentNodeId);

            // 更新节点状态为运行中
            context.getNodeStatuses().put(currentNodeId, ExecutionStatus.RUNNING);

            // 递增执行顺序号
            context.incrementExecutionOrder();

            // 查找并调用节点执行器
            WorkflowNodeExecutor executor = nodeExecutorRegistry.get(node.getType().name());
            NodeExecutionResult result = executor.execute(node, context, bridge);

            // 将节点输出合并到上下文变量
            // 这是关键的上下文传递机制
            if (result.getOutput() != null && !result.getOutput().isEmpty()) {
                // 1. 以节点ID为键存储完整输出
                context.getVariables().put(node.getId(), result.getOutput());
                // 2. 将输出中的每个键值对展开合并到变量
                // 这样后续节点可以直接通过变量名访问
                context.getVariables().putAll(result.getOutput());
            }

            // 更新节点和工作流状态
            updateStatus(context, currentNodeId, result);

            // 检查是否是结束节点
            if ("END".equals(node.getType().name())) {
                context.setWorkflowStatus(ExecutionStatus.COMPLETED);
                return;
            }

            // 根据分支键选择下一节点
            // 优先级：result.getNextNodeId() > transitionResolver.nextNode()
            currentNodeId = result.getNextNodeId() != null
                    ? result.getNextNodeId()
                    : transitionResolver.nextNode(plan, node.getId(), result.getBranchKey());
        }

        // 正常结束（没有显式的END节点）
        context.setWorkflowStatus(ExecutionStatus.COMPLETED);
    }

    /**
     * 更新执行状态
     * <p>
     * 根据节点执行结果更新节点状态和工作流状态。
     *
     * <h3>状态转换规则：</h3>
     * <ul>
     *   <li>WAITING_SIGNAL：节点等待信号，工作流状态也设为等待</li>
     *   <li>COMPLETED：节点完成，工作流状态保持 RUNNING（继续执行）</li>
     *   <li>其他状态：直接透传到工作流状态</li>
     * </ul>
     *
     * @param context 执行上下文
     * @param nodeId 节点ID
     * @param result 节点执行结果
     */
    private void updateStatus(ExecutionContext context, String nodeId, NodeExecutionResult result) {
        if (result.getStatus() == ExecutionStatus.WAITING_SIGNAL) {
            context.setWorkflowStatus(ExecutionStatus.WAITING_SIGNAL);
            context.getNodeStatuses().put(nodeId, ExecutionStatus.WAITING_SIGNAL);
        } else if (result.getStatus() == ExecutionStatus.COMPLETED) {
            context.getNodeStatuses().put(nodeId, ExecutionStatus.COMPLETED);
            context.setWorkflowStatus(ExecutionStatus.RUNNING);
        } else {
            context.getNodeStatuses().put(nodeId, result.getStatus());
            context.setWorkflowStatus(result.getStatus());
        }
    }

    /**
     * 提供人工输入
     * <p>
     * 外部系统（如 API、前端）调用此方法提交人工输入。
     * 提交后，工作流中等待该信号的节点会恢复执行。
     *
     * @param payload 人工输入信号载荷
     */
    public void provideHumanInput(HumanSignalPayload payload) {
        bufferedSignals.add(payload);
    }

    /**
     * 获取当前工作流状态
     * <p>
     * 用于查询工作流执行的实时状态。
     *
     * @return 工作流状态视图
     */
    public WorkflowStateView currentState() {
        return currentContext != null ? currentContext.toView() : new WorkflowStateView();
    }

    /**
     * 节点执行器注册表持有者
     * <p>
     * 提供默认的空注册表实例，避免 NPE。
     */
    private static class NodeExecutorRegistryHolder {
        static NodeExecutorRegistry INSTANCE = new NodeExecutorRegistry(List.of());
    }

    /**
     * 转换解析器持有者
     */
    private static class TransitionResolverHolder {
        static TransitionResolver INSTANCE = new TransitionResolver();
    }
}

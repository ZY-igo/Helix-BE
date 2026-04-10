/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.common.constant.NodeRoleConstants;
import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.*;

import com.sipc115.helix.integration.workflow.engine.TemporalWorkflowRuntimeBridge;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@Slf4j
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
        Object executionId = input.get(WorkflowConstants.EXECUTION_ID_KEY);
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
     * 核心调度逻辑，按拓扑顺序执行节点。支持多上游节点汇聚、重试机制和条件分支。
     *
     * <h3>执行流程图：</h3>
     * <pre>
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │                        executePlan 循环开始                           │
     * └─────────────────────────────────────────────────────────────────────┘
     *                                    │
     *                                    ▼
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │  canExecuteNode(node)? ──否──▶ findNextReadyNode ──找到──▶ 继续循环    │
     * └─────────────────────────────────────────────────────────────────────┘
     *                  │是
     *                  ▼
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │                      执行节点 (executor.execute)                      │
     * │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
     * │   │  节点重试     │  │   成功       │  │   失败      │                  │
     * │   │  机制        │  │             │  │             │                 │
     * │   └─────────────┘  └─────────────┘  └─────────────┘                 │
     * └─────────────────────────────────────────────────────────────────────┘
     *                                    │
     *                                    ▼
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │              节点输出合并到 context.variables                          │
     * │   context.variables[nodeId] = result.output                         │
     * │   context.variables.putAll(result.output)                           │
     * └─────────────────────────────────────────────────────────────────────┘
     *                                    │
     *                                    ▼
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │                   选择下一节点（三级降级策略）                            │
     * │   1. result.getNextNodeId() 优先使用                                  │
     * │   2. transitionResolver.nextNode() 根据分支键选择                      │
     * │   3. findNextReadyNode() 全局扫描兜底                                  │
     * └─────────────────────────────────────────────────────────────────────┘
     *                                    │
     *                                    ▼
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │                      循环直到 END 节点或无节点可执行                     │
     * └─────────────────────────────────────────────────────────────────────┘
     * </pre>
     *
     * <h3>多上游节点汇聚（Join/Barrier）说明：</h3>
     * <pre>
     * 节点A ──┐
     *         ├──→ 节点D（只有A、B都完成才执行）
     * 节点B ──┘
     *
     * 执行流程：
     * 1. A完成 → markNodeCompleted(A) → D.completedPredecessors={A}
     * 2. canExecuteNode(D) 检查D的所有前驱：
     *    - A: COMPLETED ✓
     *    - B: 尚未执行，status=null，递归检查B的有效状态
     *    - B的有效状态取决于其前驱，如果B的前驱都完成则B视为SKIPPED
     * 3. canExecuteNode(D) 返回 true → D可执行
     * </pre>
     *
     * <h3>防死锁机制：</h3>
     * <ul>
     *   <li>maxIterations = 节点数 * 2，防止无限循环</li>
     *   <li>findNextReadyNode 跳过 COMPLETED 和 RUNNING 状态的节点</li>
     *   <li>SKIPPED 状态的前驱节点不阻塞下游执行</li>
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
        context.setWorkflowStatus(ExecutionStatus.RUNNING);

        // 获取需要保存的参数变量的名称集合
        Set<String> neededNodeOutputs = computeNeededNodeOutputs(plan);
        context.getNeededNodeOutputs().addAll(neededNodeOutputs);

        String currentNodeId = plan.getEntryNodeId();
        int maxIterations = plan.getNodes().size() * 2;
        int iteration = 0;

        while (currentNodeId != null && iteration < maxIterations) {
            iteration++;

            if (!canExecuteNode(plan, currentNodeId, context)) {
                currentNodeId = findNextReadyNode(plan, context);
                if (currentNodeId == null) {
                    break;
                }
                continue;
            }

            context.setCurrentNodeId(currentNodeId);
            CompiledNode node = plan.getNodes().get(currentNodeId);
            context.getNodeStatuses().put(currentNodeId, ExecutionStatus.RUNNING);
            context.incrementExecutionOrder();

            NodeExecutionResult result;
            try {
                WorkflowNodeExecutor executor = nodeExecutorRegistry.get(node.getType().name());
                result = executor.execute(node, context, bridge);
            } catch (Exception e) {
                log.error("节点 {} 执行失败: {}", currentNodeId, e.getMessage());
                throw new RuntimeException("节点 " + currentNodeId + " 执行失败: " + e.getMessage(), e);
            }

            if (result.getOutput() != null && !result.getOutput().isEmpty()) {
                context.getVariables().put(node.getId(), result.getOutput());
                context.getVariables().putAll(result.getOutput());
            }

            updateStatus(context, currentNodeId, result);
            markNodeCompleted(plan, currentNodeId);

            if (!neededNodeOutputs.contains(node.getId())) {
                context.cleanupVariablesForNode(node.getId());
                result.setOutput(null);
            }

            if (NodeRoleConstants.END.equals(node.getType().name())) {
                context.setWorkflowStatus(ExecutionStatus.COMPLETED);
                return;
            }

            String nextFromResult = result.getNextNodeId();
            if (nextFromResult != null) {
                currentNodeId = nextFromResult;
            } else {
                String nextFromTransition = transitionResolver.nextNode(plan, node.getId(), result.getBranchKey());
                if (nextFromTransition != null && canExecuteNode(plan, nextFromTransition, context)) {
                    currentNodeId = nextFromTransition;
                } else {
                    currentNodeId = findNextReadyNode(plan, context);
                }
            }
        }

        if (iteration >= maxIterations) {
            throw new IllegalStateException("工作流执行超过最大迭代次数(" + maxIterations + ")，可能存在死锁或未完成的节点");
        }

        context.setWorkflowStatus(ExecutionStatus.COMPLETED);
    }

    /**
     * 计算哪些节点的输出是后续节点需要的
     * <p>
     * 通过扫描所有节点的配置，检查是否包含 ${} 表达式引用来确定。
     * 如果某个节点的输出被其他节点引用，则该节点的输出需要保留。
     * 用于内存优化：不被引用的节点执行后可以清理其输出。
     *
     * <h3>示例：</h3>
     * <pre>
     * 节点A 输出: {result: "xxx"}
     * 节点B 配置: {text: "${A.result}"}  // 引用了A的输出
     * 节点C 配置: {text: "static text"}  // 没有引用
     *
     * 结果：neededNodeOutputs = {A}  // 只有A的输出需要保留
     * </pre>
     *
     * @param plan 执行计划
     * @return 需要保留输出的节点ID集合
     */
    private Set<String> computeNeededNodeOutputs(ExecutionPlan plan) {
        Set<String> needed = new TreeSet<>();
        Pattern pattern = Pattern.compile("\\$\\{([^.]+)\\.");
        for (CompiledNode node : plan.getNodes().values()) {
            Map<String, Object> config = node.getConfig();
            if (config != null) {
                for (Object value : config.values()) {
                    if (value instanceof String) {
                        String str = (String) value;
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            needed.add(matcher.group(1));
                        }
                    }
                }
            }
        }
        return needed;
    }

    /**
     * 判断节点是否可以执行
     * <p>
     * 检查节点的所有上游节点是否都已完成（COMPLETED）或跳过（SKIPPED）。
     * 只有所有非SKIPPED的前驱都完成后，节点才能执行。
     *
     * <h3>判断逻辑：</h3>
     * <pre>
     * for (前驱节点 in predecessors) {
     *     if (前驱状态 == SKIPPED) continue;  // 跳过的节点不阻塞
     *     if (前驱状态 == COMPLETED) continue;  // 已完成的节点通过
     *     if (前驱状态 == null) {
     *         effectiveStatus = getEffectiveNodeStatus(节点);  // 递归检查
     *         if (effectiveStatus == SKIPPED) continue;  // 视为跳过
     *         if (effectiveStatus != COMPLETED) return false;  // 未完成
     *     } else {
     *         return false;  // 其他状态（RUNNING/PENDING等）阻塞
     *     }
     * }
     * return true;  // 所有前驱都满足条件
     * </pre>
     *
     * <h3>条件分支场景处理：</h3>
     * <pre>
     * A → CONDITION → B
     *             → C → D
     *
     * 如果走了B分支，C从未被执行，status=null。
     * getEffectiveNodeStatus(C) 会检查C的所有前驱：
     * - CONDITION 的状态是 COMPLETED
     * - 所以 C 被视为 SKIPPED，不阻塞 D 的执行
     * </pre>
     *
     * @param plan 执行计划
     * @param nodeId 节点ID
     * @param context 执行上下文
     * @return true 表示节点可以执行，false 表示不能执行
     */
    private boolean canExecuteNode(ExecutionPlan plan, String nodeId, ExecutionContext context) {
        Set<String> preds = plan.getPredecessors().get(nodeId);
        if (preds == null || preds.isEmpty()) {
            return true;
        }

        for (String pred : preds) {
            ExecutionStatus status = context.getNodeStatuses().get(pred);
            if (status == ExecutionStatus.SKIPPED || status == ExecutionStatus.COMPLETED) {
                continue;
            }
            if (status == null) {
                ExecutionStatus effectiveStatus = getEffectiveNodeStatus(plan, pred, context);
                if (effectiveStatus == ExecutionStatus.SKIPPED) {
                    context.getNodeStatuses().put(pred, ExecutionStatus.SKIPPED);
                    continue;
                }
                if (effectiveStatus != ExecutionStatus.COMPLETED) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取节点的有效状态
     * <p>
     * 递归检查节点的状态。如果节点从未被执行（status=null），
     * 通过递归检查其所有前驱来判断其有效状态。
     *
     * <h3>判断规则：</h3>
     * <ul>
     *   <li>如果节点有直接状态（COMPLETED/SKIPPED等），直接返回</li>
     *   <li>如果节点是入口节点（无前驱），视为 COMPLETED</li>
     *   <li>如果节点的所有前驱都完成了，视为 SKIPPED（条件分支互斥）</li>
     *   <li>否则视为 PENDING</li>
     * </ul>
     *
     * <h3>递归终止条件：</h3>
     * <pre>
     * 1. 找到节点有直接状态
     * 2. 找到节点的前驱有非COMPLETED/非SKIPPED状态
     * 3. 递归到入口节点（无前驱）
     * </pre>
     *
     * @param plan 执行计划
     * @param nodeId 节点ID
     * @param context 执行上下文
     * @return 节点的有效状态
     */
    private ExecutionStatus getEffectiveNodeStatus(ExecutionPlan plan, String nodeId, ExecutionContext context) {
        ExecutionStatus directStatus = context.getNodeStatuses().get(nodeId);
        if (directStatus != null) {
            return directStatus;
        }

        Set<String> preds = plan.getPredecessors().get(nodeId);
        if (preds == null || preds.isEmpty()) {
            return ExecutionStatus.COMPLETED;
        }

        for (String pred : preds) {
            ExecutionStatus predStatus = context.getNodeStatuses().get(pred);
            if (predStatus == null) {
                ExecutionStatus effectivePredStatus = getEffectiveNodeStatus(plan, pred, context);
                if (effectivePredStatus != ExecutionStatus.COMPLETED && effectivePredStatus != ExecutionStatus.SKIPPED) {
                    return ExecutionStatus.PENDING;
                }
            } else if (predStatus != ExecutionStatus.COMPLETED && predStatus != ExecutionStatus.SKIPPED) {
                return ExecutionStatus.PENDING;
            }
        }
        return ExecutionStatus.SKIPPED;
    }

    /**
     * 标记节点完成并通知下游节点
     * <p>
     * 当节点执行成功后，调用此方法更新其所有下游节点的已完成前驱集合。
     * 这是 Join/Barrier 机制的关键：只有当一个节点的所有前驱都完成后，
     * 该节点才能被执行。
     *
     * <h3>工作流程：</h3>
     * <pre>
     * 节点A ──┬──→ 节点C
     *         └──→ 节点D
     *
     * A执行完成后：
     * markNodeCompleted(A) 会遍历A的所有后继（B、C、D）
     * 并将A添加到它们各自的 completedPredecessors 集合中
     *
     * C.completedPredecessors = {A}
     * D.completedPredecessors = {A}
     * </pre>
     *
     * <h3>线程安全说明：</h3>
     * <p>
     * 使用 TreeSet 保证在 Temporal 重放时的确定性。
     * 使用 computeIfAbsent 避免空指针并保证原子性。
     *
     * @param plan 执行计划
     * @param nodeId 刚完成执行的节点ID
     */
    private void markNodeCompleted(ExecutionPlan plan, String nodeId) {
        Set<String> succs = plan.getSuccessors().get(nodeId);
        if (succs != null) {
            for (String successor : succs) {
                plan.getCompletedPredecessors()
                        .computeIfAbsent(successor, k -> new TreeSet<>())
                        .add(nodeId);
            }
        }
    }

    /**
     * 查找下一个可执行的节点
     * <p>
     * 当当前节点不可执行时（如等待上游节点），扫描所有节点找到第一个可执行的节点。
     * 跳过已经完成（COMPLETED）或正在运行（RUNNING）的节点。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>多上游节点汇聚时，需要等待所有上游完成</li>
     *   <li>条件分支场景下，选择未走的分支</li>
     *   <li>节点执行完成后，下一节点不可执行时的兜底策略</li>
     * </ul>
     *
     * <h3>查找顺序：</h3>
     * <pre>
     * 按节点ID顺序遍历，返回第一个满足以下条件的节点：
     * 1. 状态不是 COMPLETED
     * 2. 状态不是 RUNNING
     * 3. canExecuteNode() 返回 true
     * </pre>
     *
     * <h3>返回 null 的情况：</h3>
     * <ul>
     *   <li>所有节点都已完成</li>
     *   <li>所有节点都不可执行（可能存在未解决的依赖环）</li>
     * </ul>
     *
     * @param plan 执行计划
     * @param context 执行上下文
     * @return 下一个可执行的节点ID，如果不存在返回 null
     */
    private String findNextReadyNode(ExecutionPlan plan, ExecutionContext context) {
        for (String nodeId : plan.getNodes().keySet()) {
            ExecutionStatus status = context.getNodeStatuses().get(nodeId);
            if (status == ExecutionStatus.COMPLETED || status == ExecutionStatus.RUNNING) {
                continue;
            }
            if (canExecuteNode(plan, nodeId, context)) {
                return nodeId;
            }
        }
        return null;
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

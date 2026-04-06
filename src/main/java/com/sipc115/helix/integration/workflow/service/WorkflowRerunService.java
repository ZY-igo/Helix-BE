/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.workflow.compiler.DefaultDslCompiler;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import com.sipc115.helix.repository.jpa.NodeExecutionTraceRepository;
import com.sipc115.helix.repository.jpa.WorkflowExecutionRepository;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 工作流节点重新执行服务
 * <p>
 * 提供工作流节点的重新执行能力，支持多种重跑策略。
 * 当工作流执行过程中某个节点失败，或者用户需要重新执行某个节点时，可以使用此服务。
 *
 * <h3>核心功能：</h3>
 * <ul>
 *   <li>节点级别重新执行 - 重新执行单个节点及其下游</li>
 *   <li>继承上游输出 - 新执行可以继承之前节点的输出作为输入</li>
 *   <li>执行计划克隆 - 支持创建新的执行计划副本</li>
 *   <li>追踪记录管理 - 管理节点执行追踪记录的创建和更新</li>
 * </ul>
 *
 * <h3>支持的重新执行策略：</h3>
 * <ul>
 *   <li>RERUN_NODE_ONLY - 仅重新执行目标节点</li>
 *   <li>RERUN_NODE_AND_DOWNSTREAM - 重新执行目标节点及其所有下游节点</li>
 *   <li>RERUN_FROM_NODE_NEW_EXECUTION - 从目标节点开始创建全新的工作流执行</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>节点执行失败后的重试</li>
 *   <li>用户手动要求重新执行某个审批节点</li>
 *   <li>数据更新后需要重新计算下游结果</li>
 *   <li>调试时需要重新执行特定节点</li>
 * </ul>
 *
 * <h3>策略对比：</h3>
 * <table border="1">
 *   <tr><th>策略</th><th>描述</th><th>适用场景</th></tr>
 *   <tr>
 *     <td>RERUN_NODE_ONLY</td>
 *     <td>仅重跑指定节点，使用新的 retryCount</td>
 *     <td>临时性失败，希望重试节点</td>
 *   </tr>
 *   <tr>
 *     <td>RERUN_NODE_AND_DOWNSTREAM</td>
 *     <td>重跑节点及其下游，清理下游追踪记录</td>
 *     <td>节点失败影响了下游，需要重新执行</td>
 *   </tr>
 *   <tr>
 *     <td>RERUN_FROM_NODE_NEW_EXECUTION</td>
 *     <td>创建新执行，继承上游节点的输出</td>
 *     <td>需要从某个节点重新开始完整流程</td>
 *   </tr>
 * </table>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowTraceService
 * @see ExecutionPlanRepository
 */
@Service
public class WorkflowRerunService {

    /**
     * 日志记录器
     * <p>
     * 用于记录重新执行服务的关键操作和调试信息。
     */
    private static final Logger log = LoggerFactory.getLogger(WorkflowRerunService.class);

    /**
     * 工作流执行仓储
     * <p>
     * 负责 WorkflowExecutionEntity 的持久化操作。
     * 用于查询原始执行记录和创建新的执行记录。
     */
    private final WorkflowExecutionRepository executionRepo;

    /**
     * 节点执行追踪仓储
     * <p>
     * 负责 NodeExecutionTraceEntity 的持久化操作。
     * 用于查询现有追踪记录和创建新的追踪记录。
     */
    private final NodeExecutionTraceRepository nodeTraceRepo;

    /**
     * 执行计划仓储
     * <p>
     * 负责 ExecutionPlan 的持久化操作。
     * 用于获取工作流的执行计划。
     */
    private final ExecutionPlanRepository planRepo;

    /**
     * 工作流追踪服务
     * <p>
     * 负责记录节点执行追踪信息。
     * 在重新执行时创建新的追踪记录。
     */
    private final WorkflowTraceService traceService;

    /**
     * DSL 编译器
     * <p>
     * 负责将 DSL 编译为执行计划。
     * 在创建新执行时可能需要重新编译。
     */
    private final DefaultDslCompiler dslCompiler;

    /**
     * 构造函数
     * <p>
     * 通过构造器注入的方式初始化所有依赖。
     *
     * @param executionRepo 工作流执行仓储
     * @param nodeTraceRepo 节点执行追踪仓储
     * @param planRepo 执行计划仓储
     * @param traceService 工作流追踪服务
     * @param dslCompiler DSL 编译器
     */
    public WorkflowRerunService(
            WorkflowExecutionRepository executionRepo,
            NodeExecutionTraceRepository nodeTraceRepo,
            ExecutionPlanRepository planRepo,
            WorkflowTraceService traceService,
            DefaultDslCompiler dslCompiler) {
        this.executionRepo = executionRepo;
        this.nodeTraceRepo = nodeTraceRepo;
        this.planRepo = planRepo;
        this.traceService = traceService;
        this.dslCompiler = dslCompiler;
    }

    /**
     * 重新执行策略枚举
     * <p>
     * 定义了三种不同的重新执行策略，适用于不同的业务场景。
     */
    public enum RerunStrategy {

        /**
         * 仅重新执行目标节点
         * <p>
         * 在同一个工作流执行中，使用新的重试计数重新执行指定节点。
         * 不会影响其他节点的执行记录。
         *
         * <h3>执行流程：</h3>
         * <ol>
         *   <li>查询该节点现有的追踪记录</li>
         *   <li>创建新的追踪记录（retryCount + 1）</li>
         *   <li>下游节点状态保持不变</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>临时性网络故障导致节点执行失败</li>
         *   <li>外部服务暂时不可用</li>
         *   <li>用户希望重试验证节点</li>
         * </ul>
         */
        RERUN_NODE_ONLY,

        /**
         * 重新执行目标节点及所有下游节点
         * <p>
         * 清理目标节点及其所有下游节点的追踪记录，
         * 允许它们重新执行。
         *
         * <h3>执行流程：</h3>
         * <ol>
         *   <li>查找目标节点的所有下游节点</li>
         *   <li>将目标节点和下游节点的追踪记录标记为失败</li>
         *   <li>下游节点可以重新执行</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>节点失败后，需要重新执行以修复问题</li>
         *   <li>上游数据变更，需要重新计算下游结果</li>
         *   <li>人工审批拒绝后，需要重新执行</li>
         * </ul>
         */
        RERUN_NODE_AND_DOWNSTREAM,

        /**
         * 从目标节点创建全新的工作流执行
         * <p>
         * 创建一个新的工作流执行实例，
         * 继承目标节点之前所有节点的输出作为输入。
         *
         * <h3>执行流程：</h3>
         * <ol>
         *   <li>克隆执行计划，只包含目标节点及其下游</li>
         *   <li>收集目标节点之前所有成功节点的输出</li>
         *   <li>创建新的工作流执行记录</li>
         *   <li>新执行的输入继承自历史输出</li>
         * </ol>
         *
         * <h3>适用场景：</h3>
         * <ul>
         *   <li>工作流已经进入终态，但需要从中间节点重新开始</li>
         *   <li>业务逻辑变更，需要用新版本执行后续节点</li>
         *   <li>需要在不影响原始执行的情况下测试新的执行路径</li>
         * </ul>
         */
        RERUN_FROM_NODE_NEW_EXECUTION
    }

    /**
     * 重新执行节点
     * <p>
     * 根据指定的策略，重新执行工作流中的某个节点。
     * 这是服务的主入口方法，会根据策略分发到不同的处理方法。
     *
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>验证执行记录是否存在</li>
     *   <li>获取对应的执行计划</li>
     *   <li>验证目标节点是否存在</li>
     *   <li>根据策略执行相应的重新执行逻辑</li>
     * </ol>
     *
     * <h3>事务管理：</h3>
     * <ul>
     *   <li>此方法带有 @Transactional 注解</li>
     *   <li>所有数据库操作在同一个事务中完成</li>
     *   <li>如果任何操作失败，整个事务回滚</li>
     * </ul>
     *
     * @param executionId 工作流执行记录 ID
     * @param nodeId 要重新执行的节点 ID
     * @param strategy 重新执行策略
     * @return 重新执行结果，包含成功/失败状态和相关信息
     * @throws IllegalArgumentException 如果执行记录或节点不存在
     */
    @Transactional
    public RerunResult rerunNode(Long executionId, String nodeId, RerunStrategy strategy) {
        log.info("节点重跑请求: executionId={}, nodeId={}, strategy={}", executionId, nodeId, strategy);

        // 1. 验证执行记录是否存在
        Optional<WorkflowExecutionEntity> executionOpt = executionRepo.findById(executionId);
        if (executionOpt.isEmpty()) {
            return RerunResult.failure("Execution not found: " + executionId);
        }

        WorkflowExecutionEntity originalExecution = executionOpt.get();

        // 2. 获取对应的执行计划
        ExecutionPlan originalPlan = planRepo.findByWorkflowIdAndVersion(
                originalExecution.getWorkflowId(), originalExecution.getVersion())
                .orElse(null);

        if (originalPlan == null) {
            return RerunResult.failure("Execution plan not found");
        }

        // 3. 验证目标节点是否存在
        CompiledNode targetNode = originalPlan.getNodes().get(nodeId);
        if (targetNode == null) {
            return RerunResult.failure("Node not found: " + nodeId);
        }

        // 4. 根据策略执行相应的处理逻辑
        switch (strategy) {
            case RERUN_NODE_ONLY:
                return rerunNodeOnly(executionId, nodeId, originalPlan);
            case RERUN_NODE_AND_DOWNSTREAM:
                return rerunNodeAndDownstream(executionId, nodeId, originalPlan);
            case RERUN_FROM_NODE_NEW_EXECUTION:
                return rerunFromNodeNewExecution(originalExecution, nodeId, originalPlan);
            default:
                return RerunResult.failure("Unknown strategy: " + strategy);
        }
    }

    /**
     * 仅重新执行目标节点
     * <p>
     * 使用新的重试计数，在同一执行中重新运行目标节点。
     * 下游节点的状态和输出保持不变。
     *
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>查询该节点现有的所有追踪记录</li>
     *   <li>计算新的重试计数（现有记录数）</li>
     *   <li>创建新的追踪记录开始节点执行</li>
     * </ol>
     *
     * <h3>重试计数规则：</h3>
     * <pre>
     * 首次执行: retryCount = 0
     * 第一次重跑: retryCount = 1
     * 第二次重跑: retryCount = 2
     * ...
     * </pre>
     *
     * <h3>追踪记录示例：</h3>
     * <pre>
     * | traceId | nodeId    | retryCount | status |
     * |---------|-----------|-------------|--------|
     * | 1       | sendNotify| 0           | FAILED |
     * | 2       | sendNotify| 1           | ...    |  ← 新创建
     * </pre>
     *
     * @param executionId 工作流执行 ID
     * @param nodeId 节点 ID
     * @param plan 执行计划
     * @return 重新执行结果
     */
    private RerunResult rerunNodeOnly(Long executionId, String nodeId, ExecutionPlan plan) {
        log.info("执行策略 RERUN_NODE_ONLY: executionId={}, nodeId={}", executionId, nodeId);

        // 查询该节点现有的所有追踪记录
        List<NodeExecutionTraceEntity> existingTraces = nodeTraceRepo.findByExecutionIdAndNodeId(executionId, nodeId);

        // 计算新的重试计数 = 现有记录数
        // 例如：已有 0 条记录 → retryCount = 0（首次）
        //       已有 1 条记录 → retryCount = 1（第一次重试）
        int currentRetryCount = existingTraces.size();

        // 将现有追踪记录标记为失败状态（可选，用于标记历史）
        for (NodeExecutionTraceEntity trace : existingTraces) {
            if (trace.getRetryCount() == currentRetryCount) {
                traceService.markNodeFailed(trace.getId(), "Manually rerun", null);
            }
        }

        // 获取节点定义
        CompiledNode node = plan.getNodes().get(nodeId);

        // 获取节点的输入参数（使用最后一次执行的输入）
        Map<String, Object> input = getNodeInput(existingTraces.isEmpty() ? null : existingTraces.get(existingTraces.size() - 1));

        // 创建新的追踪记录
        NodeExecutionTraceEntity newTrace = traceService.startNodeExecution(
                executionId, nodeId, node.getType().name(), "NORMAL",
                plan.getNodes().size() + 1, input, currentRetryCount + 1);

        log.info("节点重跑记录已创建: traceId={}, retryCount={}", newTrace.getId(), currentRetryCount + 1);

        // 返回成功结果，包含新的 attemptId
        return RerunResult.success(
                "Node marked for rerun with retryCount=" + (currentRetryCount + 1),
                newTrace.getAttemptId()
        );
    }

    /**
     * 重新执行目标节点及所有下游节点
     * <p>
     * 清理目标节点及其所有下游节点的追踪记录，
     * 使它们可以在下次工作流执行时被重新执行。
     *
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>使用 BFS 算法查找目标节点的所有下游节点</li>
     *   <li>将目标节点和所有下游节点的追踪记录标记为失败</li>
     *   <li>返回被清理的节点列表</li>
     * </ol>
     *
     * <h3>下游节点计算示例：</h3>
     * <pre>
     *     A → B → C
     *     ↓   ↓
     *     D → E → F
     *
     * 从节点 B 开始，下游节点包括：
     * - C（直接下游）
     * - E（B 的直接下游）
     * - F（E 的下游）
     * 不包括 D（D 是 B 的上游，不是下游）
     * </pre>
     *
     * @param executionId 工作流执行 ID
     * @param nodeId 节点 ID
     * @param plan 执行计划
     * @return 重新执行结果
     */
    private RerunResult rerunNodeAndDownstream(Long executionId, String nodeId, ExecutionPlan plan) {
        log.info("执行策略 RERUN_NODE_AND_DOWNSTREAM: executionId={}, nodeId={}", executionId, nodeId);

        // 1. 计算所有下游节点（使用 BFS 遍历）
        Set<String> downstreamNodes = getDownstreamNodes(plan, nodeId);
        // 将目标节点也加入清理列表
        downstreamNodes.add(nodeId);

        List<String> clearedNodeIds = new ArrayList<>();

        // 2. 遍历所有需要清理的节点
        for (String downstreamNodeId : downstreamNodes) {
            // 查询该节点的追踪记录
            List<NodeExecutionTraceEntity> traces = nodeTraceRepo.findByExecutionIdAndNodeId(executionId, downstreamNodeId);

            // 将所有追踪记录标记为失败
            for (NodeExecutionTraceEntity trace : traces) {
                traceService.markNodeFailed(trace.getId(), "Manually rerun - downstream cleared", null);
            }
            clearedNodeIds.add(downstreamNodeId);
        }

        log.info("下游节点已清理: clearedNodes={}", clearedNodeIds);

        // 返回成功结果
        return RerunResult.success(
                "Downstream nodes cleared for rerun: " + downstreamNodes,
                null
        );
    }

    /**
     * 从目标节点创建全新的工作流执行
     * <p>
     * 创建一个新的工作流执行实例，
     * 继承目标节点之前所有成功节点的输出作为输入。
     *
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>克隆执行计划，只包含目标节点及其下游</li>
     *   <li>收集目标节点之前所有成功节点的输出</li>
     *   <li>创建新的工作流执行记录</li>
     *   <li>设置新执行的触发来源为 MANUAL_RERUN</li>
     * </ol>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>工作流已完成后，发现某个中间节点需要重新执行</li>
     *   <li>需要用新的业务逻辑执行后续流程</li>
     *   <li>不影响原始执行的情况下测试新的执行路径</li>
     * </ul>
     *
     * <h3>继承输入示例：</h3>
     * <pre>
     * 原始执行：
     * A(success) → B(success) → C(failed)
     *                           ↓
     *                          创建新执行
     *                           ↓
     * 新执行的输入 = A的输出 + B的输出
     *
     * 新执行计划：
     * B(使用B的输出作为输入) → C(重新执行)
     * </pre>
     *
     * @param originalExecution 原始工作流执行记录
     * @param nodeId 节点 ID
     * @param plan 执行计划
     * @return 重新执行结果
     */
    private RerunResult rerunFromNodeNewExecution(WorkflowExecutionEntity originalExecution, String nodeId, ExecutionPlan plan) {
        log.info("执行策略 RERUN_FROM_NODE_NEW_EXECUTION: originalExecutionId={}, nodeId={}",
                originalExecution.getId(), nodeId);

        // 1. 克隆执行计划，只包含目标节点及其下游
        ExecutionPlan clonedPlan = clonePlanFromNode(plan, nodeId);

        // 2. 收集目标节点之前所有成功节点的输出
        Map<String, Object> inheritedOutputs = getNodeOutputsUpTo(originalExecution.getId(), nodeId, plan);

        // 3. 创建新的工作流执行记录
        WorkflowExecutionEntity newExecution = new WorkflowExecutionEntity();
        newExecution.setWorkflowId(originalExecution.getWorkflowId());
        // 版本号添加 rerun 标记，便于识别
        newExecution.setVersion(originalExecution.getVersion() + "-rerun-" + System.currentTimeMillis());
        // 继承上游节点的输出作为新执行的输入
        newExecution.setInput(inheritedOutputs);
        newExecution.setStatus("RUNNING");
        // 标记触发来源为手动重新执行
        newExecution.setTriggeredBy("MANUAL_RERUN");
        newExecution.setStartedAt(java.time.Instant.now());

        // 4. 保存新的执行记录
        WorkflowExecutionEntity saved = executionRepo.save(newExecution);

        log.info("新执行已创建: newExecutionId={}, inheritedOutputs={}", saved.getId(), inheritedOutputs.keySet());

        // 5. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("newExecutionId", saved.getId());
        result.put("newWorkflowVersion", saved.getVersion());
        result.put("entryNodeId", clonedPlan.getEntryNodeId());
        result.put("inheritedOutputKeys", inheritedOutputs.keySet());

        return RerunResult.success("New execution created", result);
    }

    /**
     * 从指定节点克隆执行计划
     * <p>
     * 创建一个新的执行计划，包含指定节点及其所有下游节点。
     * 用于创建新的工作流执行。
     *
     * <h3>克隆过程：</h3>
     * <ol>
     *   <li>确定需要包含的节点集合（指定节点及其下游）</li>
     *   <li>复制节点定义</li>
     *   <li>复制并裁剪前驱后继关系</li>
     *   <li>设置新的入口节点</li>
     * </ol>
     *
     * <h3>节点关系裁剪示例：</h3>
     * <pre>
     * 原始计划：
     * A → B → C → D
     * 裁剪从 B 开始：
     * B → C → D
     * A 不在克隆计划中（A 的后继 B 仍然指向 C，但 A 本身被移除）
     * </pre>
     *
     * @param original 原始执行计划
     * @param startNodeId 起始节点 ID
     * @return 克隆后的执行计划
     */
    private ExecutionPlan clonePlanFromNode(ExecutionPlan original, String startNodeId) {
        ExecutionPlan cloned = new ExecutionPlan();
        cloned.setWorkflowId(original.getWorkflowId());
        cloned.setWorkflowVersion(original.getWorkflowVersion() + "-rerun");
        cloned.setEntryNodeId(startNodeId);

        // 使用 LinkedHashMap 保持节点顺序
        Map<String, CompiledNode> clonedNodes = new LinkedHashMap<>();
        Map<String, Set<String>> clonedSuccessors = new LinkedHashMap<>();
        Map<String, Set<String>> clonedPredecessors = new LinkedHashMap<>();

        // 计算需要包含的节点（起始节点及其所有下游）
        Set<String> nodesToInclude = getDownstreamNodes(original, startNodeId);
        nodesToInclude.add(startNodeId);

        // 复制节点定义
        for (String nodeId : nodesToInclude) {
            clonedNodes.put(nodeId, original.getNodes().get(nodeId));
        }

        // 复制并裁剪前驱后继关系
        for (String nodeId : nodesToInclude) {
            Set<String> succs = original.getSuccessors().get(nodeId);
            if (succs != null) {
                // 只保留也包含在克隆计划中的后继
                succs.stream().filter(nodesToInclude::contains).forEach(s -> {
                    // 添加到后继列表
                    clonedSuccessors.computeIfAbsent(nodeId, k -> new TreeSet<>()).add(s);
                    // 添加到前驱列表
                    clonedPredecessors.computeIfAbsent(s, k -> new TreeSet<>()).add(nodeId);
                });
            }
        }

        cloned.setNodes(clonedNodes);
        cloned.setSuccessors(clonedSuccessors);
        cloned.setPredecessors(clonedPredecessors);

        return cloned;
    }

    /**
     * 计算指定节点的所有下游节点（BFS 遍历）
     * <p>
     * 使用广度优先搜索（BFS）算法，
     * 找出从起始节点开始可以到达的所有节点。
     *
     * <h3>算法说明：</h3>
     * <ul>
     *   <li>使用队列实现 BFS</li>
     *   <li>使用 Set 避免重复访问</li>
     *   <li>保证节点按层级顺序访问</li>
     * </ul>
     *
     * <h3>遍历示例：</h3>
     * <pre>
     *     A → B → C
     *     ↓   ↓
     *     D → E
     *
     * 从 A 开始遍历：
     * 1. 队列: [A]，已访问: []
     * 2. 取出 A，访问后继 B,D，队列: [B,D]，已访问: [A]
     * 3. 取出 B，访问后继 C,E，队列: [D,C,E]，已访问: [A,B]
     * 4. 取出 D，无后继，队列: [C,E]，已访问: [A,B,D]
     * 5. 取出 C，无后继，队列: [E]，已访问: [A,B,D,C]
     * 6. 取出 E，无后继，队列: []，已访问: [A,B,D,C,E]
     *
     * 结果: {B, D, C, E}
     * </pre>
     *
     * @param plan 执行计划
     * @param startNodeId 起始节点 ID
     * @return 下游节点 ID 集合（不包含起始节点）
     */
    private Set<String> getDownstreamNodes(ExecutionPlan plan, String startNodeId) {
        Set<String> downstream = new TreeSet<>();
        Queue<String> queue = new LinkedList<>();

        // 将起始节点加入队列
        queue.add(startNodeId);

        // BFS 遍历
        while (!queue.isEmpty()) {
            // 取出当前节点
            String current = queue.poll();

            // 获取后继节点
            Set<String> succs = plan.getSuccessors().get(current);
            if (succs != null) {
                for (String succ : succs) {
                    // 如果还未访问过，加入队列
                    if (downstream.add(succ)) {
                        queue.add(succ);
                    }
                }
            }
        }

        return downstream;
    }

    /**
     * 获取节点的输入参数
     * <p>
     * 从追踪记录中提取节点的输入参数。
     *
     * <h3>返回值说明：</h3>
     * <ul>
     *   <li>如果追踪记录为 null，返回空 Map</li>
     *   <li>如果追踪记录的 input 为 null，返回空 Map</li>
     *   <li>否则返回 input 本身</li>
     * </ul>
     *
     * @param trace 节点追踪记录（可为 null）
     * @return 输入参数 Map
     */
    private Map<String, Object> getNodeInput(NodeExecutionTraceEntity trace) {
        if (trace == null) {
            return new HashMap<>();
        }
        return trace.getInput() != null ? trace.getInput() : new HashMap<>();
    }

    /**
     * 获取指定节点及其上游所有成功节点的输出
     * <p>
     * 用于在创建新执行时，继承历史执行的输出作为新执行的输入。
     *
     * <h3>收集过程：</h3>
     * <ol>
     *   <li>计算指定节点的所有上游节点</li>
     *   <li>遍历上游节点，收集状态为 SUCCESS 的节点的输出</li>
     *   <li>将输出合并到一个 Map 中</li>
     * </ol>
     *
     * <h3>输出合并规则：</h3>
     * <ul>
     *   <li>按节点 ID 分组存储：key = 节点 ID，value = 节点输出</li>
     *   <li>同时将输出合并到顶层：key = 输出中的每个键，value = 对应的值</li>
     *   <li>后出现的同名键会覆盖前面的值</li>
     * </ul>
     *
     * @param executionId 工作流执行 ID
     * @param nodeId 目标节点 ID
     * @param plan 执行计划
     * @return 合并后的输出 Map
     */
    private Map<String, Object> getNodeOutputsUpTo(Long executionId, String nodeId, ExecutionPlan plan) {
        Map<String, Object> outputs = new HashMap<>();

        // 计算目标节点的所有上游节点
        Set<String> upstreamNodes = getUpstreamNodes(plan, nodeId);
        // 将目标节点也加入（因为目标节点之前可能已执行成功）
        upstreamNodes.add(nodeId);

        // 遍历上游节点，收集成功节点的输出
        for (String upstreamId : upstreamNodes) {
            List<NodeExecutionTraceEntity> traces = nodeTraceRepo.findByExecutionIdAndNodeId(executionId, upstreamId);
            for (NodeExecutionTraceEntity trace : traces) {
                // 只收集成功执行的输出
                if ("SUCCESS".equals(trace.getStatus()) && trace.getOutput() != null) {
                    // 按节点 ID 分组存储
                    outputs.put(upstreamId, trace.getOutput());
                    // 同时合并到顶层（供表达式引用）
                    outputs.putAll(trace.getOutput());
                }
            }
        }

        return outputs;
    }

    /**
     * 计算指定节点的所有上游节点（BFS 遍历）
     * <p>
     * 使用广度优先搜索（BFS）算法，
     * 找出所有可以到达指定节点的节点。
     *
     * <h3>遍历示例：</h3>
     * <pre>
     *     A → B → C
     *     ↓   ↓
     *     D → E
     *
     * 从 E 开始遍历上游：
     * 1. 队列: [E]，已访问: []
     * 2. 取出 E，访问前驱 B,D，队列: [B,D]，已访问: [E]
     * 3. 取出 B，访问前驱 A，队列: [D,A]，已访问: [E,B]
     * 4. 取出 D，访问前驱 A，队列: [A,A]，已访问: [E,B,D]
     * 5. 取出 A，无前驱，队列: [A]，已访问: [E,B,D,A]
     * 6. 取出 A（重复，不处理），队列: []，已访问: [E,B,D,A]
     *
     * 结果: {B, D, A}
     * </pre>
     *
     * @param plan 执行计划
     * @param endNodeId 目标节点 ID
     * @return 上游节点 ID 集合（不包含目标节点）
     */
    private Set<String> getUpstreamNodes(ExecutionPlan plan, String endNodeId) {
        Set<String> upstream = new TreeSet<>();
        Queue<String> queue = new LinkedList<>();

        // 将目标节点加入队列
        queue.add(endNodeId);

        // BFS 遍历
        while (!queue.isEmpty()) {
            String current = queue.poll();

            // 获取前驱节点
            Set<String> preds = plan.getPredecessors().get(current);
            if (preds != null) {
                for (String pred : preds) {
                    // 如果还未访问过，加入队列
                    if (upstream.add(pred)) {
                        queue.add(pred);
                    }
                }
            }
        }

        return upstream;
    }

    /**
     * 重新执行结果类
     * <p>
     * 封装重新执行操作的返回结果，
     * 包含执行状态、消息和可选的数据。
     *
     * <h3>使用示例：</h3>
     * <pre>
     * {@code
     * RerunResult result = rerunService.rerunNode(executionId, nodeId, strategy);
     * if (result.isSuccess()) {
     *     // 处理成功
     *     Object data = result.getData();
     * } else {
     *     // 处理失败
     *     String message = result.getMessage();
     * }
     * }
     * </pre>
     */
    @lombok.Data
    public static class RerunResult {

        /**
         * 是否成功
         */
        private final boolean success;

        /**
         * 结果消息
         * <p>
         * 成功时包含操作描述，
         * 失败时包含错误原因。
         */
        private final String message;

        /**
         * 附加数据
         * <p>
         * 根据不同的重新执行策略，包含不同的数据：
         * <ul>
         *   <li>RERUN_NODE_ONLY: 新的 attemptId</li>
         *   <li>RERUN_NODE_AND_DOWNSTREAM: 被清理的节点列表</li>
         *   <li>RERUN_FROM_NODE_NEW_EXECUTION: 新执行的详细信息</li>
         * </ul>
         */
        private final Object data;

        /**
         * 创建成功结果
         *
         * @param message 成功消息
         * @param data 附加数据
         * @return 成功结果
         */
        public static RerunResult success(String message, Object data) {
            return new RerunResult(true, message, data);
        }

        /**
         * 创建失败结果
         *
         * @param message 错误消息
         * @return 失败结果
         */
        public static RerunResult failure(String message) {
            return new RerunResult(false, message, null);
        }
    }
}
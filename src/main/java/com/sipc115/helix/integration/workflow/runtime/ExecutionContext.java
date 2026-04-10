package com.sipc115.helix.integration.workflow.runtime;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 工作流执行上下文
 * <p>
 * 存储工作流执行过程中的所有状态信息，包括：
 * <ul>
 *   <li>当前执行计划（ExecutionPlan）</li>
 *   <li>工作流变量（variables）- 节点间传递数据</li>
 *   <li>节点状态（nodeStatuses）- 每个节点的执行状态</li>
 *   <li>表达式缓存（expressionCache）- 避免重复求值</li>
 *   <li>被引用的节点输出（neededNodeOutputs）- 内存优化用</li>
 * </ul>
 *
 * <h3>变量命名约定：</h3>
 * <pre>
 * context.variables["node-id"]           = 完整输出（Map）
 * context.variables["node-id.key"]      = 展开的输出字段
 * context.variables["global-var"]       = 全局变量
 * </pre>
 *
 * <h3>状态流转：</h3>
 * <pre>
 * PENDING → RUNNING → COMPLETED
 *                 ↓
 *           WAITING_SIGNAL (等待人工输入)
 *                 ↓
 *           WAITING_RETRY (等待重试)
 *                 ↓
 *              FAILED (最终失败)
 * </pre>
 *
 * @author Helix Team
 * @see ExecutionPlan
 * @see ExecutionStatus
 * @since 2.0.0
 */
@Data
public class ExecutionContext {

    /**
     * 执行计划
     * <p>
     * 包含工作流的拓扑结构（节点、边、前后继关系）。
     */
    private ExecutionPlan plan;

    /**
     * 工作流变量
     * <p>
     * 存储所有节点的输出和全局变量。
     * 使用 TreeMap 保证遍历顺序一致性（Temporal 重放安全）。
     *
     * <h3>存储结构：</h3>
     * <pre>
     * variables = {
     *   "node-A": {                           // 节点完整输出
     *     "result": "xxx",
     *     "status": "success"
     *   },
     *   "node-A.result": "xxx",               // 展开的字段
     *   "node-A.status": "success",
     *   "global-var": "value"                 // 全局变量
     * }
     * </pre>
     */
    private Map<String, Object> variables = new TreeMap<>();

    /**
     * 节点状态映射
     * <p>
     * 记录每个节点的执行状态。
     * key: 节点ID, value: 节点状态
     */
    private Map<String, ExecutionStatus> nodeStatuses = new TreeMap<>();

    /**
     * 当前正在执行的节点ID
     */
    private String currentNodeId;

    /**
     * 工作流整体状态
     */
    private ExecutionStatus workflowStatus = ExecutionStatus.PENDING;

    /**
     * 执行记录ID（用于追踪）
     */
    private Long executionId;

    /**
     * 当前执行顺序号
     * <p>
     * 每次执行节点前递增，用于记录节点执行顺序。
     */
    private Integer executionOrder = 0;

    /**
     * 当前节点追踪记录ID
     */
    private Long currentNodeTraceId;

    /**
     * 表达式缓存
     * <p>
     * 缓存表达式求值结果，避免重复计算。
     * key: 表达式字符串, value: 求值结果
     *
     * <h3>缓存命中场景：</h3>
     * <pre>
     * 节点A 输出: {result: "xxx"}
     * 节点B 配置: ${A.result}  // 第一次求值，缓存
     * 节点C 配置: ${A.result}  // 第二次求值，命中缓存
     * </pre>
     */
    private Map<String, Object> expressionCache = new TreeMap<>();

    /**
     * 需要保留输出的节点集合
     * <p>
     * 通过分析 DSL 配置，确定哪些节点的输出被后续节点引用。
     * 不被引用的节点执行后可以清理其输出，节省内存。
     */
    private Set<String> neededNodeOutputs = new TreeSet<>();

    public ExecutionContext() {
    }

    public ExecutionContext(ExecutionPlan plan, Map<String, Object> input) {
        this.plan = plan;
        if (input != null) {
            this.variables.putAll(input);
        }
    }

    /**
     * 递增执行顺序号
     */
    public void incrementExecutionOrder() {
        this.executionOrder++;
    }

    /**
     * 清空表达式缓存
     */
    public void clearExpressionCache() {
        this.expressionCache.clear();
    }

    /**
     * 获取缓存的表达式结果
     *
     * @param expression 表达式字符串
     * @return 缓存的结果，如果不存在返回 null
     */
    public Object getCachedExpression(String expression) {
        return expressionCache.get(expression);
    }

    /**
     * 缓存表达式求值结果
     *
     * @param expression 表达式字符串
     * @param result     求值结果
     */
    public void cacheExpressionResult(String expression, Object result) {
        expressionCache.put(expression, result);
    }

    /**
     * 如果节点输出不需要则清理
     * <p>
     * 内存优化用：如果节点的输出没有被后续节点引用，则清空输出。
     *
     * @param nodeId 节点ID
     * @param output 节点输出
     */
    public void cleanupNodeOutputIfNotNeeded(String nodeId, Map<String, Object> output) {
        if (!neededNodeOutputs.contains(nodeId)) {
            if (output != null) {
                output.clear();
            }
        }
    }

    /**
     * 清理节点的展开变量
     * <p>
     * 当节点执行完成后，如果其输出不被需要，清理以 nodeId.xxx 形式存储的展开变量。
     *
     * <h3>示例：</h3>
     * <pre>
     * 清理前: variables = {"A.result": "xxx", "A.status": "ok", "B.output": "yyy"}
     * 清理 A:  variables = {"B.output": "yyy"}
     * </pre>
     *
     * @param nodeId 节点ID
     */
    public void cleanupVariablesForNode(String nodeId) {
        Set<String> keysToRemove = new TreeSet<>();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key.equals(nodeId) || key.startsWith(nodeId + ".")) {
                keysToRemove.add(key);
            }
        }
        keysToRemove.forEach(variables::remove);
    }

    /**
     * 转换为工作流状态视图
     * <p>
     * 用于查询工作流执行状态和返回给调用方。
     *
     * @return 工作流状态视图对象
     */
    public WorkflowStateView toView() {
        WorkflowStateView view = new WorkflowStateView();
        if (plan != null) {
            view.setWorkflowId(plan.getWorkflowId());
        }
        view.setCurrentNodeId(currentNodeId);
        view.setStatus(workflowStatus);
        view.getVariables().putAll(variables);
        view.getNodeStatuses().putAll(nodeStatuses);
        return view;
    }
}

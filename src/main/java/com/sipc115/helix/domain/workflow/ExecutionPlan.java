package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.*;

/**
 * 工作流执行计划
 * <p>
 * 包含工作流编译后的所有信息，用于执行工作流。
 * 由 DefaultDslCompiler 根据 WorkflowDsl 编译生成。
 *
 * <h3>编译流程：</h3>
 * <pre>
 * WorkflowDsl (用户编写)
 *        ↓
 * DefaultDslCompiler.compile()
 *        ↓
 * ExecutionPlan (可执行的计划)
 * </pre>
 *
 * <h3>数据结构：</h3>
 * <pre>
 * ExecutionPlan {
 *     workflowId: "workflow-001"
 *     entryNodeId: "node-A"           // 入口节点
 *     nodes: {                         // 所有节点
 *         "node-A": CompiledNode,
 *         "node-B": CompiledNode,
 *         ...
 *     }
 *     transitions: [...],              // 所有边
 *     predecessors: {                 // 前驱关系（入度）
 *         "node-C": {"node-A", "node-B"},
 *         "node-D": {"node-C"}
 *     }
 *     successors: {                   // 后继关系（出度）
 *         "node-A": {"node-C", "node-B"},
 *         "node-B": {"node-C"}
 *     }
 *     completedPredecessors: {        // 已完成的前驱（运行时更新）
 *         "node-C": {"node-A"},       // 只有 A 完成时
 *         "node-C": {"node-A", "node-B"}  // A、B 都完成时
 *     }
 * }
 * </pre>
 *
 * <h3>拓扑结构示例：</h3>
 * <pre>
 *     ┌→ node-B ─┐
 * node-A ┤         ├→ node-D
 *     └→ node-C ─┘
 *
 * predecessors:  {"node-B": {"node-A"}, "node-C": {"node-A"}, "node-D": {"node-B", "node-C"}}
 * successors:    {"node-A": {"node-B", "node-C"}, "node-B": {"node-D"}, "node-C": {"node-D"}}
 * </pre>
 *
 * <h3> Temporal 重放安全性：</h3>
 * <p>
 * 使用 TreeMap/TreeSet 保证遍历顺序一致性。
 * 这对于 Temporal Workflow 的重放机制至关重要。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see CompiledNode
 * @see Transition
 */
@Data
public class ExecutionPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流 ID
     */
    private String workflowId;

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 入口节点 ID
     * <p>
     * 工作流开始执行的第一个节点。
     * 通常是 START 类型的节点。
     */
    private String entryNodeId;

    /**
     * 所有节点映射
     * <p>
     * key: 节点ID, value: 编译后的节点
     */
    private Map<String, CompiledNode> nodes = new TreeMap<>();

    /**
     * 工作流中的所有边（转换关系）
     */
    private List<Transition> transitions;

    /**
     * 执行计划 ID（唯一标识）
     */
    private String planId;

    /**
     * 计划元数据
     */
    private PlanMetadata metadata;

    /**
     * 调度规格（如果是定时工作流）
     */
    private ScheduleSpec schedule;

    /**
     * 节点前驱关系（入度）
     * <p>
     * key: 节点ID, value: 该节点的所有前驱节点ID集合
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>判断节点是否可以执行（canExecuteNode）</li>
     *   <li>检测循环依赖</li>
     *   <li>计算执行顺序</li>
     * </ul>
     *
     * <h3>示例：</h3>
     * <pre>
     * A → B → C
     * predecessors: {"B": {"A"}, "C": {"B"}}
     * </pre>
     */
    private Map<String, Set<String>> predecessors = new TreeMap<>();

    /**
 * 节点后继关系（出度）
     * <p>
     * key: 节点ID, value: 该节点的所有后继节点ID集合
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>节点完成时通知下游节点（markNodeCompleted）</li>
     *   <li>构建执行路径</li>
     * </ul>
     *
     * <h3>示例：</h3>
     * <pre>
     * A → B
     * A → C
     * successors: {"A": {"B", "C"}, "B": {}, "C": {}}
     * </pre>
     */
    private Map<String, Set<String>> successors = new TreeMap<>();

    /**
     * 变量引用依赖图（编译期构建）
     * <p>
     * 记录每个节点引用了哪些上游节点的输出。
     * 用于精准内存清理：只有当一个变量的引用计数降为 0 时，才从上下文中删除。
     *
     * <h3>数据结构：</h3>
     * <pre>
     * variableDependencies: {
     *     "node-B": {"node-A": 1},        // B 引用了 A 的输出，引用计数 1
     *     "node-C": {"node-A": 1},        // C 也引用了 A
     *     "node-D": {"node-B": 1, "node-C": 1}  // D 引用了 B 和 C
     * }
     * </pre>
     *
     * <h3>构建时机：</h3>
     * <ul>
     *   <li>编译期：DefaultDslCompiler.compile() 中构建</li>
     *   <li>运行时：只读，用于判断何时可以清理变量</li>
     * </ul>
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>精准内存清理：引用计数归零才删除</li>
     *   <li>避免误删：下游节点仍在使用的变量不会被删除</li>
     * </ul>
     */
    private Map<String, Map<String, Integer>> variableDependencies = new TreeMap<>();

    /**
     * 已完成的前驱节点集合（运行时）
     * <p> 实际并没有用该属性判断节点是否可以执行, 因此该属性目前处于保留状态
     * <p>
     * 记录每个节点的哪些前驱已经完成执行。
     * 当 completedPredecessors[nodeId] 包含所有 predecessors[nodeId] 时，节点可以执行。
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>实现 Join/Barrier 机制</li>
     *   <li>判断多上游节点汇聚场景</li>
     * </ul>
     *
     * <h3>工作流程：</h3>
     * <pre>
     * A ─→ C ←── B
     *
     * 初始: completedPredecessors = {"C": {}}
     *
     * A完成: markNodeCompleted(A)
     *      → completedPredecessors = {"C": {"A"}}
     *
     * B完成: markNodeCompleted(B)
     *      → completedPredecessors = {"C": {"A", "B"}}
     *
     * canExecuteNode(C): predecessors["C"] = {"A", "B"}
     *                   completedPredecessors["C"] = {"A", "B"}
     *                   包含所有前驱 → C 可以执行
     * </pre>
     *
     * <h3>线程安全：</h3>
     * <p>
     * 使用 TreeSet 保证 Temporal 重放时的确定性。
     */
    private Map<String, Set<String>> completedPredecessors = new TreeMap<>();
}

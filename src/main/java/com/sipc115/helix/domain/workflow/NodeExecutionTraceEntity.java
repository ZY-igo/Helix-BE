/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 节点执行轨迹实体
 * <p>
 * 用于记录工作流中每个节点的执行情况，是工作流追踪体系的重要组成部分。
 * 通过记录每个节点的输入、输出、状态变更等信息，实现工作流执行的可追溯性。
 *
 * <h3>核心功能：</h3>
 * <ul>
 *   <li>节点级别执行追踪：记录每个节点的开始时间、结束时间、执行状态</li>
 *   <li>输入输出记录：保存节点的输入参数和执行结果</li>
 *   <li>异常信息记录：保存节点执行失败时的错误信息和堆栈</li>
 *   <li>重试次数追踪：记录节点被重试的次数</li>
 *   <li>幂等键支持：通过 attemptId 实现 Activity 级别的幂等性保证</li>
 * </ul>
 *
 * <h3>数据库索引设计：</h3>
 * <ul>
 *   <li>idx_exec_id_node_id：支持按 executionId 和 nodeId 快速查询</li>
 *   <li>idx_exec_order：支持按 executionId 和 executionOrder 排序查询</li>
 *   <li>idx_attempt_id：唯一索引，用于幂等性保证</li>
 * </ul>
 *
 * <h3>唯一约束：</h3>
 * <ul>
 *   <li>uk_exec_node_retry：(executionId, nodeId, retryCount) 组合唯一</li>
 *   <li>确保同一节点的同一次执行（retryCount相同）不会被重复插入</li>
 * </ul>
 *
 * <h3>状态流转：</h3>
 * <pre>
 * PENDING（待执行）
 *    ↓
 * RUNNING（执行中）
 *    ↓
 * SUCCESS / FAILED / SKIPPED（终态）
 *
 * 重试时的状态流转：
 * RUNNING → FAILED → PENDING → RUNNING → SUCCESS
 *                              ↑ retryCount + 1
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowExecutionEntity
 */
@Data
@Entity
@Table(name = "node_execution_trace", indexes = {
    @Index(name = "idx_exec_id_node_id", columnList = "executionId, nodeId"),
    @Index(name = "idx_exec_order", columnList = "executionId, executionOrder"),
    @Index(name = "idx_attempt_id", columnList = "attemptId", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_exec_node_retry", columnNames = {"executionId", "nodeId", "retryCount"})
})
public class NodeExecutionTraceEntity {

    /**
     * 主键 ID
     * <p>
     * 使用数据库自增策略生成。
     * JPA/Hibernate 会自动管理该字段的映射。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的工作流执行 ID
     * <p>
     * 指向 workflow_execution 表的主键。
     * 通过此字段可以将节点执行记录关联到具体的工作流执行实例。
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>关联查询：SELECT * FROM node_execution_trace WHERE executionId = ?</li>
     *   <li>数据隔离：不同工作流执行的追踪数据通过此字段区分</li>
     * </ul>
     */
    @Column(nullable = false)
    private Long executionId;

    /**
     * 节点 ID（来自 DSL 定义）
     * <p>
     * 节点在 DSL 中的唯一标识符。
     * 例如：start、fetchData、sendNotify、end 等。
     *
     * <h3>注意：</h3>
     * <ul>
     *   <li>此字段是 DSL 级别的 ID，不是数据库主键</li>
     *   <li>同一个工作流中 nodeId 必须唯一</li>
     *   <li>不同工作流可以拥有相同 nodeId 的节点</li>
     * </ul>
     */
    @Column(nullable = false, length = 128)
    private String nodeId;

    /**
     * 节点类型
     * <p>
     * 标识节点的功能类型，对应 DslNodeType 枚举值。
     * 例如：START、END、TASK、CONDITION、LOOP、FEISHU_SEND_TEXT 等。
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>节点分类统计</li>
     *   <li>根据类型执行不同的处理逻辑</li>
     *   <li>日志和监控按类型聚合</li>
     * </ul>
     */
    @Column(nullable = false, length = 64)
    private String nodeType;

    /**
     * 节点角色
     * <p>
     * 标识节点在流程中的角色分类。
     * 可选值（来自 NodeRoleConstants）：
     * <ul>
     *   <li>START：开始节点，流程入口</li>
     *   <li>END：结束节点，流程出口</li>
     *   <li>NORMAL：普通节点，常规执行单元</li>
     * </ul>
     *
     * <h3>与 nodeType 的区别：</h3>
     * <ul>
     *   <li>nodeType 关注节点的功能类型（如 FEISHU_SEND_TEXT）</li>
     *   <li>nodeRole 关注节点在流程中的角色（如 START/END/NORMAL）</li>
     * </ul>
     */
    @Column(length = 32)
    private String nodeRole;

    /**
     * 执行顺序号
     * <p>
     * 标识节点在工作流执行中的顺序。
     * 由工作流引擎在执行前分配，同一工作流中不同时刻的节点会有不同的执行顺序。
     *
     * <h3>特性：</h3>
     * <ul>
     *   <li>顺序号不一定连续（因为有并行分支）</li>
     *   <li>主要用于调试和追踪，不用于控制流程</li>
     *   <li>在 WorkflowTraceService.executePlan() 的循环中递增</li>
     * </ul>
     */
    @Column(nullable = false)
    private Integer executionOrder;

    /**
     * 执行状态
     * <p>
     * 记录节点的当前执行状态。
     * 可选值（来自 ExecutionStatusConstants 或 ExecutionStatus 枚举）：
     * <ul>
     *   <li>PENDING：待执行，节点还未开始</li>
     *   <li>RUNNING：执行中，节点正在运行</li>
     *   <li>SUCCESS/COMPLETED：执行成功</li>
     *   <li>FAILED：执行失败</li>
     *   <li>SKIPPED：被跳过（条件分支未走）</li>
     *   <li>TIMED_OUT：执行超时</li>
     *   <li>WAITING_SIGNAL：等待人工信号</li>
     *   <li>WAITING_RETRY：等待重试</li>
     * </ul>
     *
     * <h3>状态转换图：</h3>
     * <pre>
     * 正常流程：
     * PENDING → RUNNING → COMPLETED
     *
     * 失败流程：
     * PENDING → RUNNING → FAILED
     *
     * 跳过流程：
     * PENDING → SKIPPED（前置条件不满足）
     *
     * 重试流程：
     * RUNNING → FAILED → PENDING → RUNNING → COMPLETED
     *                   (重试次数+1)
     * </pre>
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 节点执行开始时间
     * <p>
     * 记录节点开始执行的时间戳（UTC）。
     * 在 WorkflowTraceService.startNodeExecution() 被调用时设置。
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>计算执行耗时：durationMs = endedAt - startedAt</li>
     *   <li>性能分析：慢节点识别</li>
     *   <li>时间范围查询</li>
     * </ul>
     */
    private Instant startedAt;

    /**
     * 节点执行结束时间
     * <p>
     * 记录节点执行完成的时间戳（UTC）。
     * 在 WorkflowTraceService.markNodeSuccess() 或 markNodeFailed() 被调用时设置。
     */
    private Instant endedAt;

    /**
     * 执行耗时（毫秒）
     * <p>
     * 节点从开始到结束的毫秒数。
     * 计算公式：durationMs = endedAt - startedAt
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>性能监控：识别慢节点</li>
     *   <li>SLA 考核：节点是否在规定时间内完成</li>
     *   <li>优化依据：耗时长的节点优先优化</li>
     * </ul>
     */
    private Long durationMs;

    /**
     * 输入数据（JSON 格式）
     * <p>
     * 记录节点执行时的输入参数，存储为 JSONB 格式。
     *
     * <h3>内容示例：</h3>
     * <pre>
     * {
     *   "connectionId": 123,
     *   "chatId": "oc_xxxxx",
     *   "text": "Hello ${user.name}",
     *   "_executionId": 456
     * }
     * </pre>
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>问题排查：复现节点执行时的输入</li>
     *   <li>审计追踪：了解节点决策的依据</li>
     *   <li>重试依据：重试时使用相同的输入</li>
     * </ul>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> input;

    /**
     * 输出数据（JSON 格式）
     * <p>
     * 记录节点执行成功后的输出结果，存储为 JSONB 格式。
     *
     * <h3>内容示例（飞书发消息节点）：</h3>
     * <pre>
     * {
     *   "success": true,
     *   "messageId": "om_xxxxx",
     *   "timestamp": 1699999999999
     * }
     * </pre>
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>下游节点引用：${sendNotify.messageId}</li>
     *   <li>结果查询：获取节点执行结果</li>
     *   <li>审计追踪：记录业务操作结果</li>
     * </ul>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> output;

    /**
     * 错误信息
     * <p>
     * 节点执行失败时记录的错误描述。
     * 最多存储 2048 个字符，超出部分会被截断。
     *
     * <h3>内容示例：</h3>
     * <pre>
     * "飞书发送消息失败: connection timeout"
     * </pre>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>只保存简要的错误描述，不保存完整堆栈</li>
     *   <li>详细堆栈信息存储在 errorStack 字段</li>
     *   <li>成功执行的节点此字段为空</li>
     * </ul>
     */
    @Column(length = 2048)
    private String errorMessage;

    /**
     * 错误堆栈
     * <p>
     * 节点执行失败时的完整异常堆栈信息。
     * 以 TEXT 格式存储，可以保存完整的 StackTrace。
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>详细问题排查</li>
     *   <li>开发调试</li>
     *   <li>错误分析</li>
     * </ul>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>TEXT 类型在 PostgreSQL 中最大 1GB</li>
     *   <li>超长堆栈可能被数据库截断</li>
     *   <li>成功执行的节点此字段为空</li>
     * </ul>
     */
    @Column(columnDefinition = "TEXT")
    private String errorStack;

    /**
     * 重试次数
     * <p>
     * 记录节点被重试的次数。
     * 初始值为 0，表示首次执行。
     * 每次重试时递增 1。
     *
     * <h3>重试计数规则：</h3>
     * <ul>
     *   <li>0 = 首次执行</li>
     *   <li>1 = 第一次重试</li>
     *   <li>2 = 第二次重试</li>
     *   <li>以此类推...</li>
     * </ul>
     *
     * <h3>与 attemptId 的关系：</h3>
     * <ul>
     *   <li>attemptId = {executionId}_{nodeId}_{retryCount}</li>
     *   <li>retryCount 变化时，会生成新的 attemptId</li>
     *   <li>通过 (executionId, nodeId, retryCount) 唯一约束防止重复</li>
     * </ul>
     */
    @Column(nullable = false)
    private Integer retryCount = 0;

    /**
     * 幂等键
     * <p>
     * 用于防止 Activity 重复执行的唯一标识符。
     * 在 Temporal 等工作流引擎中，相同的 Activity 可能会被重试执行，
     * 通过 attemptId 可以识别并跳过重复的执行。
     *
     * <h3>格式定义：</h3>
     * <pre>
     * attemptId = {executionId}_{nodeId}_{retryCount}
     *
     * 示例：
     * - 12345_sendNotify_0  （首次执行）
     * - 12345_sendNotify_1  （第一次重试）
     * - 12345_sendNotify_2  （第二次重试）
     * </pre>
     *
     * <h3>唯一性保证：</h3>
     * <ul>
     *   <li>数据库层面：idx_attempt_id 唯一索引确保不重复</li>
     *   <li>业务层面：在 WorkflowTraceService.startNodeExecution() 中生成</li>
     * </ul>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>Temporal Activity 的幂等键</li>
     *   <li>消息队列消费的幂等键</li>
     *   <li>外部 API 调用的幂等键</li>
     * </ul>
     */
    @Column(length = 256, unique = true)
    private String attemptId;

    /**
     * 元数据（JSON 格式）
     * <p>
     * 用于存储特定节点的额外信息，存储为 JSONB 格式。
     *
     * <h3>使用示例：</h3>
     * <pre>
     * {
     *   "loopCount": 5,
     *   "exitCondition": "count >= 10",
     *   "customField": "any value"
     * }
     * </pre>
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>节点特定信息的存储</li>
     *   <li>扩展字段，避免频繁修改表结构</li>
     *   <li>特定业务场景的定制化数据</li>
     * </ul>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    /**
     * 创建时间
     * <p>
     * 记录追踪记录被创建的时间戳。
     * 在数据首次插入数据库时自动设置。
     *
     * <h3>特性：</h3>
     * <ul>
     *   <li>updatable = false，创建后不可修改</li>
     *   <li>由 @PrePersist 回调自动设置</li>
     *   <li>使用 Instant（UTC 时间）</li>
     * </ul>
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA 持久化前回调
     * <p>
     * 在实体第一次被持久化到数据库之前自动调用。
     * 用于设置创建时间等自动填充字段。
     *
     * <h3>执行时机：</h3>
     * <ul>
     *   <li>INSERT 操作时自动触发</li>
     *   <li>UPDATE 操作不触发</li>
     * </ul>
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
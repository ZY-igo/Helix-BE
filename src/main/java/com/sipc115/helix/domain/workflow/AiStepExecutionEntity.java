/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * AI 步骤执行记录实体
 * <p>
 * 专门用于记录 AI_TASK 节点内部的微流程步骤执行情况。
 * 用于追踪 AI 模型调用、对话轮次、Token 消耗等详细执行信息。
 *
 * <h3>主要用途：</h3>
 * <ul>
 *   <li>记录 AI 多轮对话的每次交互详情</li>
 *   <li>追踪 AI 生成、验证、修复等步骤</li>
 *   <li>分析 Token 消耗和模型响应时间</li>
 *   <li>复现 AI 执行过程进行调试</li>
 * </ul>
 *
 * <h3>与其他实体关系：</h3>
 * <ul>
 *   <li>通过 {@link #nodeTraceId} 关联 {@link NodeExecutionTraceEntity}</li>
 *   <li>通过 {@link #executionId} 可直接查询某次工作流执行的所有 AI 步骤</li>
 * </ul>
 *
 * <h3>索引说明：</h3>
 * <ul>
 *   <li>idx_node_trace_round: 用于查询某个节点追踪的所有 AI 步骤（按轮次排序）</li>
 *   <li>idx_ai_step_execution_id: 用于查询某次工作流执行的所有 AI 步骤</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see NodeExecutionTraceEntity
 * @see WorkflowTraceEvent
 */
@Data
@Entity
@Table(name = "ai_step_execution", indexes = {
    @Index(name = "idx_node_trace_round", columnList = "nodeTraceId, round"),
    @Index(name = "idx_ai_step_execution_id", columnList = "executionId")
})
public class AiStepExecutionEntity {

    // ==================== 主键 ====================

    /**
     * 主键 ID
     * <p>
     * 自增主键，唯一标识每条 AI 步骤执行记录。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== 关联字段 ====================

    /**
     * 关联的节点执行追踪 ID
     * <p>
     * 指向 {@link NodeExecutionTraceEntity} 的外键，
     * 用于关联到具体的节点执行记录。
     * 通过此字段可以查询该节点下的所有 AI 步骤。
     */
    @Column(nullable = false)
    private Long nodeTraceId;

    /**
     * 工作流执行 ID
     * <p>
     * 指向 {@link WorkflowExecutionEntity} 的外键，
     * 用于直接查询某次工作流执行的所有 AI 步骤，
     * 无需通过节点追踪间接查询。
     * 添加此字段是为了优化跨节点查询性能。
     */
    @Column(nullable = false)
    private Long executionId;

    // ==================== 步骤标识 ====================

    /**
     * 步骤 ID
     * <p>
     * AI 步骤的唯一标识符，格式如 "step_001"、"generate_1" 等。
     * 用于区分同一轮次中的不同步骤。
     */
    @Column(nullable = false, length = 128)
    private String stepId;

    /**
     * 步骤类型
     * <p>
     * 标识 AI 步骤的具体类型，用于细分 AI 执行过程：
     * <ul>
     *   <li>GENERATE - 内容生成步骤</li>
     *   <li>VALIDATE - 验证步骤</li>
     *   <li>REPAIR - 修复步骤</li>
     *   <li>CHAT - 对话步骤</li>
     *   <li>OTHER - 其他类型步骤</li>
     * </ul>
     */
    @Column(nullable = false, length = 64)
    private String stepType;

    // ==================== 执行状态 ====================

    /**
     * 轮次号
     * <p>
     * 用于标识多轮对话中的轮次序号，从 1 开始递增。
     * 每次新的对话交互轮次递增，同一轮次内可以有多个步骤。
     */
    @Column(nullable = false)
    private Integer round;

    /**
     * 执行状态
     * <p>
     * 标识当前步骤的执行状态：
     * <ul>
     *   <li>RUNNING - 执行中</li>
     *   <li>SUCCESS - 执行成功</li>
     *   <li>FAILED - 执行失败</li>
     * </ul>
     */
    @Column(nullable = false, length = 32)
    private String status;

    // ==================== 时间相关 ====================

    /**
     * 开始时间
     * <p>
     * 记录 AI 步骤开始执行的时刻。
     * 与 {@link #endedAt} 配合使用计算执行耗时。
     */
    private Instant startedAt;

    /**
     * 结束时间
     * <p>
     * 记录 AI 步骤完成执行的时刻。
     * 如果状态为 RUNNING，则此字段可能为 null。
     */
    private Instant endedAt;

    /**
     * 执行耗时（毫秒）
     * <p>
     * 记录 AI 步骤执行的持续时间。
     * 可以通过 {@link #startedAt} 和 {@link #endedAt} 计算得出，
     * 也可以由调用方直接设置。
     */
    private Long durationMs;

    // ==================== AI 模型信息 ====================

    /**
     * 模型名称
     * <p>
     * 记录实际使用的 AI 模型名称，如 "glm-4"、"gpt-4"、"claude-3" 等。
     * 用于分析不同模型的性能和质量差异。
     */
    @Column(length = 64)
    private String modelName;

    /**
     * Token 消耗统计
     * <p>
     * 记录本次 AI 调用消耗的 Token 数量。
     * 通常为输入 Token + 输出 Token 的总和。
     * 用于成本分析和性能优化。
     */
    @Column
    private Integer tokenUsage;

    // ==================== 内容数据 ====================

    /**
     * 输入内容快照
     * <p>
     * 记录发送给 AI 模型的完整 Prompt 内容。
     * 以 TEXT 格式存储，支持长文本。
     * 用于复现和调试 AI 执行过程。
     */
    @Column(columnDefinition = "TEXT")
    private String inputContent;

    /**
     * 输出内容快照
     * <p>
     * 记录 AI 模型生成的完整回复内容。
     * 以 TEXT 格式存储，支持长文本。
     * 用于分析 AI 输出质量和进行后处理。
     */
    @Column(columnDefinition = "TEXT")
    private String outputContent;

    /**
     * 变量状态快照（JSON）
     * <p>
     * 以 JSON 格式存储执行时刻的变量状态快照。
     * 包含工作流上下文中的关键变量，用于：
     * <ul>
     *   <li>复现执行过程</li>
     *   <li>分析变量变化趋势</li>
     *   <li>调试变量相关问题</li>
     * </ul>
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> varsSnapshot;

    // ==================== 错误信息 ====================

    /**
     * 错误信息
     * <p>
     * 当执行状态为 FAILED 时，记录具体的错误描述信息。
     * 长度限制为 2048 字符。
     */
    @Column(length = 2048)
    private String errorMessage;

    // ==================== 系统字段 ====================

    /**
     * 创建时间
     * <p>
     * 记录本条记录创建的时间戳。
     * 由 JPA 的 @PrePersist 回调自动设置。
     * 不可更新，用于审计追踪。
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA 持久化前回调
     * <p>
     * 在记录首次保存到数据库前自动设置创建时间。
     * 确保创建时间准确且不可篡改。
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

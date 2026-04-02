/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * AI 步骤执行记录
 * <p>
 * 专门用于记录 AI_TASK 节点内部的微流程步骤执行情况
 */
@Data
@Entity
@Table(name = "ai_step_execution", indexes = {
    @Index(name = "idx_node_trace_round", columnList = "nodeTraceId, round")
})
public class AiStepExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的节点执行轨迹 ID
     */
    @Column(nullable = false)
    private Long nodeTraceId;

    /**
     * 步骤 ID
     */
    @Column(nullable = false, length = 128)
    private String stepId;

    /**
     * 步骤类型：GENERATE, VALIDATE, REPAIR, IF, LOOP_WHILE, RETURN
     */
    @Column(nullable = false, length = 64)
    private String stepType;

    /**
     * 所属轮次（用于多轮迭代）
     */
    @Column(nullable = false)
    private Integer round;

    /**
     * 执行状态
     */
    @Column(nullable = false, length = 32)
    private String status;

    /**
     * 开始时间
     */
    private Instant startedAt;

    /**
     * 结束时间
     */
    private Instant endedAt;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 使用的模型名称（如 glm-4, gpt-4）
     */
    @Column(length = 64)
    private String modelName;

    /**
     * Token 消耗统计
     */
    @Column
    private Integer tokenUsage;

    /**
     * 输入内容快照
     */
    @Column(columnDefinition = "TEXT")
    private String inputContent;

    /**
     * 输出内容快照
     */
    @Column(columnDefinition = "TEXT")
    private String outputContent;

    /**
     * 变量状态快照（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> varsSnapshot;

    /**
     * 错误信息
     */
    @Column(length = 2048)
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}

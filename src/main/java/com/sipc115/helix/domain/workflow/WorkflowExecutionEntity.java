/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * 工作流执行实例实体
 * <p>
 * 记录每次工作流执行的完整信息
 */
@Data
@Entity
@Table(name = "workflow_execution")
public class WorkflowExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 工作流 ID
     */
    @Column(nullable = false, length = 64)
    private String workflowId;

    /**
     * DSL 版本号
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * 执行状态：RUNNING, SUCCESS, FAILED, CANCELLED
     */
    @Column(length = 32)
    private String status;

    /**
     * Temporal 执行 ID（用于关联 Temporal 的执行）
     */
    @Column(length = 128)
    private String temporalExecutionId;

    /**
     * 开始时间
     */
    private Instant startedAt;

    /**
     * 结束时间
     */
    private Instant endedAt;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDurationMs;

    /**
     * 触发人/系统
     */
    private String triggeredBy;

    /**
     * 输入参数（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> input;

    /**
     * 最终输出（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> output;

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

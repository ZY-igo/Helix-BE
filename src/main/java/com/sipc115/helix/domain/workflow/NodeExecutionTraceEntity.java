/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * 节点执行轨迹实体
 * <p>
 * 记录工作流中每个节点的执行情况
 */
@Data
@Entity
@Table(name = "node_execution_trace", indexes = {
    @Index(name = "idx_exec_id_node_id", columnList = "executionId, nodeId"),
    @Index(name = "idx_exec_order", columnList = "executionId, executionOrder")
})
public class NodeExecutionTraceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的工作流执行 ID
     */
    @Column(nullable = false)
    private Long executionId;

    /**
     * 节点 ID（来自 DSL 定义）
     */
    @Column(nullable = false, length = 128)
    private String nodeId;

    /**
     * 节点类型
     */
    @Column(nullable = false, length = 64)
    private String nodeType;

    /**
     * 节点角色：START, END, NORMAL
     */
    @Column(length = 32)
    private String nodeRole;

    /**
     * 执行顺序号（用于追踪执行顺序）
     */
    @Column(nullable = false)
    private Integer executionOrder;

    /**
     * 执行状态：PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
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
     * 输入数据（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> input;

    /**
     * 输出数据（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> output;

    /**
     * 错误信息
     */
    @Column(length = 2048)
    private String errorMessage;

    /**
     * 错误堆栈（详细异常信息）
     */
    @Column(columnDefinition = "TEXT")
    private String errorStack;

    /**
     * 重试次数
     */
    @Column(nullable = false)
    private Integer retryCount = 0;

    /**
     * 元数据（用于存储特定节点的额外信息）
     */
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * 索引优化查询
     */
}

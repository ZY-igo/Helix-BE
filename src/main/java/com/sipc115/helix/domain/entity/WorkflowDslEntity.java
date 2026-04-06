/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 工作流 DSL 实体类
 * <p>
 * 对应数据库表：workflow_dsl
 */
@Data
@Entity
@Table(name = "workflow_dsl")
public class WorkflowDslEntity {

    /**
     * 主键 ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 工作流 ID（业务主键）
     */
    @Column(name = "workflow_id", nullable = false, length = 64)
    private String workflowId;

    /**
     * 版本号
     * <p>
     * 支持语义化版本，如 "v1.0.0", "v2.0-beta", "1.0.1"
     */
    @Column(name = "version", nullable = false, length = 32)
    private String version;

    /**
     * DSL 内容（JSON 格式）
     */
    @Column(name = "dsl_content", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String dslContent;

    /**
     * 状态：DRAFT, PUBLISHED, DEPRECATED
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 工作流状态
     * <p>
     * 可选值：
     * - INITIALIZING: 初始化
     * - COMPILING: 编译中
     * - COMPILE_FAILED: 编译失败
     * - COMPILED: 编译成功
     * - RUNNING: 运行中
     * - STOPPED: 已停用
     */
    @Column(name = "state", length = 20)
    private String state;

    /**
     * 状态详情（用于存储编译失败原因等）
     */
    @Column(name = "state_detail", length = 500)
    private String stateDetail;

    /**
     * 元数据（可选）
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 64)
    private String createdBy;

    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;
}

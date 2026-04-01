/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 执行计划实体类
 * <p>
 * 对应数据库表：execution_plan
 */
@Data
@Entity
@Table(name = "execution_plan")
public class ExecutionPlanEntity {

    /**
     * 主键 ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 计划 ID（业务主键，唯一标识）
     */
    @Column(name = "plan_id", nullable = false, unique = true, length = 64)
    private String planId;

    /**
     * 关联的工作流 ID
     */
    @Column(name = "workflow_id", nullable = false, length = 64)
    private String workflowId;

    /**
     * 关联的版本号
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * 执行计划内容（JSON 格式）
     */
    @Column(name = "plan_content", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String planContent;

    /**
     * 编译器版本
     */
    @Column(name = "compiler_version", length = 32)
    private String compilerVersion;

    /**
     * 编译时间
     */
    @Column(name = "compiled_at", nullable = false)
    private Instant compiledAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 64)
    private String createdBy;
}

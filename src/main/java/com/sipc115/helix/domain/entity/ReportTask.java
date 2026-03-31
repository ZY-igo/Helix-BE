/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报告任务实体类
 * <p>
 * 用于存储报告任务的信息，对应数据库中的 task 表。
 * 包含任务 ID、任务介绍、会话 ID、执行周期、密码等信息。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Entity
@Table(name = "task")
@Data
public class ReportTask {

    /**
     * 任务 ID
     * <p>
     * 任务的唯一标识符，作为主键。
     */
    @Id
    @Column(name = "task_id")
    private String taskId;

    /**
     * 任务介绍
     * <p>
     * 任务的详细介绍，不能为空，最大长度为 1000。
     */
    @Column(name = "task_intro", nullable = false, length = 1000)
    private String taskIntro;

    /**
     * 会话 ID 列表
     * <p>
     * 存储 JSON 格式的会话 ID 列表，不能为空，使用 jsonb 类型存储。
     */
    @Column(name = "session_ids", nullable = false, columnDefinition = "jsonb")
    private String sessionIds;

    /**
     * 执行周期
     * <p>
     * 任务的执行周期，不能为空，最大长度为 255。
     */
    @Column(name = "execution_cycle", nullable = false, length = 255)
    private String executionCycle;

    /**
     * 密码
     * <p>
     * 任务的密码，最大长度为 1000。
     */
    @Column(name = "password", length = 1000)
    private String password;

    /**
     * 创建时间
     * <p>
     * 任务的创建时间，不能为空。
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * <p>
     * 任务的更新时间，不能为空。
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 持久化前的回调方法
     * <p>
     * 在实体被持久化前设置创建时间和更新时间。
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新前的回调方法
     * <p>
     * 在实体被更新前设置更新时间。
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

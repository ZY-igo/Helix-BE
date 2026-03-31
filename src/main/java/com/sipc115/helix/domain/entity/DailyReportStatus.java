/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日报告状态实体类
 * <p>
 * 用于存储每日报告的执行状态，对应数据库中的 daily_report_status 表。
 * 包含任务 ID、报告日期、发送状态、错误信息等信息。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Entity
@Table(name = "daily_report_status")
@Data
public class DailyReportStatus {

    /**
     * 主键 ID
     * <p>
     * 自动生成的唯一标识符，使用自增策略。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务 ID
     * <p>
     * 关联的任务标识符，不能为空。
     */
    @Column(name = "task_id", nullable = false)
    private String taskId;

    /**
     * 报告日期
     * <p>
     * 报告的日期，不能为空。
     */
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    /**
     * 是否发送
     * <p>
     * 报告是否已发送，不能为空。
     */
    @Column(nullable = false)
    private boolean sent;

    /**
     * ES 文档 ID
     * <p>
     * 存储在 Elasticsearch 中的文档 ID。
     */
    @Column(name = "es_document_id")
    private String esDocumentId;

    /**
     * 摘要消息 ID
     * <p>
     * 发送的摘要消息 ID。
     */
    @Column(name = "summary_message_id")
    private String summaryMessageId;

    /**
     * 详细消息 ID
     * <p>
     * 发送的详细消息 ID。
     */
    @Column(name = "detail_message_id")
    private String detailMessageId;

    /**
     * 发送时间
     * <p>
     * 报告的发送时间。
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 错误消息
     * <p>
     * 执行过程中出现的错误消息，最大长度为 1200。
     */
    @Column(name = "error_message", length = 1200)
    private String errorMessage;

    /**
     * 更新时间
     * <p>
     * 记录的更新时间，不能为空。
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 持久化和更新前的回调方法
     * <p>
     * 在实体被持久化或更新前设置更新时间。
     */
    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 任务提示实体类
 * <p>
 * 用于存储任务相关的提示信息，对应数据库中的 prompt 表。
 * 包含任务 ID、标签和提示内容等信息，用于生成任务执行所需的提示。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Entity
@Table(name = "prompt")
@Data
public class TaskPrompt {

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
     * 标签
     * <p>
     * 提示的标签，用于分类和识别不同类型的提示，不能为空。
     */
    @Column(name = "tag", nullable = false)
    private String tag;

    /**
     * 提示内容
     * <p>
     * 提示的具体内容，使用 text 类型存储，不能为空。
     */
    @Column(name = "prompt_content", nullable = false, columnDefinition = "text")
    private String promptContent;
}

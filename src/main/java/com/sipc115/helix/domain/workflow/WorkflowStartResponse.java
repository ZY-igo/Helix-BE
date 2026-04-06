/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

/**
 * 工作流启动响应
 * <p>
 * 包含工作流启动后的关键信息，用于前端追踪和查询。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class WorkflowStartResponse {
    /**
     * 数据库执行记录ID（用于查询状态）
     */
    private Long executionId;

    /**
     * Temporal 工作流 ID（用于取消操作）
     */
    private String temporalWorkflowId;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 提示信息
     */
    private String message;
}

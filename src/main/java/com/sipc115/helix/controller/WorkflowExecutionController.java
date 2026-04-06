/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.workflow.WorkflowExecutionRequest;
import com.sipc115.helix.domain.workflow.WorkflowStartResponse;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import com.sipc115.helix.integration.workflow.service.WorkflowExecutionApplicationService;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流执行控制器
 * <p>
 * 提供工作流的启动、状态查询、取消等 REST API。
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>POST /api/workflows/{workflowId}/execute - 启动工作流执行</li>
 *   <li>GET /api/workflows/executions/{executionId}/status - 查询执行状态</li>
 *   <li>POST /api/workflows/executions/{workflowId}/cancel - 取消执行</li>
 * </ul>
 *
 * <h3>重要说明：</h3>
 * <ul>
 *   <li>启动接口返回的 executionId 用于后续状态查询</li>
 *   <li>temporalWorkflowId 用于取消操作</li>
 *   <li>workflowVersion 必须指定，否则会报错</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowExecutionApplicationService
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowExecutionController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionController.class);

    @Autowired
    private WorkflowExecutionApplicationService workflowExecutionService;

    @Autowired
    private WorkflowTraceService traceService;


    /**
     * 启动工作流执行
     *
     * @param workflowId 工作流ID
     * @param request 包含版本和输入参数的请求体
     * @return 执行响应，包含 executionId（用于查询）和 temporalWorkflowId（用于取消）
     */
    @PostMapping("/{workflowId}/execute")
    public ResponseEntity<WorkflowStartResponse> executeWorkflow(
            @PathVariable String workflowId,
            @RequestBody WorkflowExecutionRequest request
    ) {
        log.info("收到工作流执行请求: workflowId={}, version={}", workflowId, request.getWorkflowVersion());

        try {
            request.setWorkflowId(workflowId);
            WorkflowStartResponse response = workflowExecutionService.start(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("工作流执行参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("工作流执行异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 查询执行状态
     *
     * @param executionId 数据库中的执行记录ID (Long)
     * @return 工作流状态视图
     */
    @GetMapping("/executions/{executionId}/status")
    public ResponseEntity<WorkflowStateView> getExecutionStatus(@PathVariable Long executionId) {
        log.info("查询执行状态: executionId={}", executionId);

        try {
            WorkflowStateView state = traceService.getExecutionStateView(executionId);
            if (state == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            log.error("查询执行状态异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 取消工作流执行
     *
     * @param workflowId Temporal 工作流 ID
     * @return 取消结果
     */
    @PostMapping("/executions/{workflowId}/cancel")
    public ResponseEntity<Void> cancelExecution(@PathVariable String workflowId) {
        log.info("收到取消执行请求: workflowId={}", workflowId);

        try {
            workflowExecutionService.cancel(workflowId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("取消执行失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("取消执行异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

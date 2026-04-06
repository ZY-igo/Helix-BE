/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.workflow.HumanSignalPayload;
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
 *   <li>执行计划未找到时会自动编译最新 DSL</li>
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
     * <p>
     * 自动查找或编译执行计划，然后启动工作流。
     * 1. 优先查找已发布的执行计划
     * 2. 如果没有已发布的执行计划，查找最新版本并自动编译保存
     * 3. 如果没有任何版本，抛出异常
     *
     * @param workflowId 工作流 ID
     * @param request    包含输入参数的请求体
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
     * @param executionId 数据库中的执行记录 ID
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

    /**
     * 向工作流发送人工输入信号
     * <p>
     * 用于唤醒处于 HUMAN_INPUT 节点的工作流实例。
     *
     * @param workflowId Temporal 工作流 ID（即启动时返回的 temporalWorkflowId）
     * @param payload 包含节点 ID 和用户输入数据的载荷
     * @return 操作结果
     */
    @PostMapping("/executions/{workflowId}/signal")
    public ResponseEntity<Void> sendHumanSignal(
            @PathVariable String workflowId,
            @RequestBody HumanSignalPayload payload
    ) {
        log.info("收到人工输入信号请求: workflowId={}, nodeId={}", workflowId, payload.getNodeId());

        try {
            workflowExecutionService.sendHumanSignal(workflowId, payload);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("发送信号参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("发送信号异常: workflowId={}, error={}", workflowId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

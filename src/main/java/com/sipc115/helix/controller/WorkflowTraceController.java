/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import com.sipc115.helix.integration.workflow.service.WorkflowRerunService;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流追踪控制器
 * <p>
 * 提供工作流执行追踪相关的 REST API，用于查询工作流和节点的执行历史、状态和统计信息。
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>查询节点执行轨迹列表 - 获取某次执行的所有节点追踪记录</li>
 *   <li>查询工作流执行历史 - 获取某工作流的所有执行记录</li>
 *   <li>查询执行统计信息 - 获取执行次数、成功率、平均耗时等</li>
 *   <li>节点重新执行 - 支持多种重跑策略</li>
 * </ul>
 *
 * <h3>API 列表：</h3>
 * <ul>
 *   <li>GET /api/workflows/{workflowId}/executions - 获取执行历史</li>
 *   <li>GET /api/workflows/executions/{executionId}/traces - 获取节点轨迹</li>
 *   <li>GET /api/workflows/{workflowId}/stats - 获取执行统计</li>
 *   <li>POST /api/workflows/executions/{executionId}/nodes/{nodeId}/rerun - 节点重跑</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowTraceService
 * @see WorkflowRerunService
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowTraceController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTraceController.class);

    @Autowired
    private WorkflowTraceService traceService;

    @Autowired
    private WorkflowRerunService rerunService;

    /**
     * 获取工作流执行历史
     * <p>
     * 查询指定工作流的所有执行记录，按创建时间倒序排列。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>查看工作流的历史执行记录</li>
     *   <li>追踪工作流的执行情况</li>
     *   <li>分析工作流的执行趋势</li>
     * </ul>
     *
     * <h3>返回数据示例：</h3>
     * <pre>
     * [
     *   {
     *     "id": 123,
     *     "workflowId": "daily-report",
     *     "version": "v1.0.0",
     *     "status": "SUCCESS",
     *     "startedAt": "2024-01-15T10:30:00Z",
     *     "endedAt": "2024-01-15T10:35:00Z",
     *     "totalDurationMs": 300000
     *   },
     *   {
     *     "id": 122,
     *     "workflowId": "daily-report",
     *     "version": "v1.0.0",
     *     "status": "FAILED",
     *     "startedAt": "2024-01-15T09:00:00Z",
     *     "endedAt": "2024-01-15T09:05:00Z",
     *     "errorMessage": "Connection timeout"
     *   }
     * ]
     * </pre>
     *
     * @param workflowId 工作流 ID
     * @param limit 返回记录数量限制（默认 20，最大 100）
     * @return 工作流执行记录列表
     */
    @GetMapping("/{workflowId}/executions")
    public ResponseEntity<List<WorkflowExecutionEntity>> getExecutionHistory(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("查询执行历史: workflowId={}, limit={}", workflowId, limit);

        // 限制最大返回数量，防止查询过多
        if (limit > 100) {
            limit = 100;
        }

        try {
            List<WorkflowExecutionEntity> executions = traceService.getExecutions(workflowId, limit);
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            log.error("查询执行历史异常: workflowId={}, error={}", workflowId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取节点执行轨迹列表
     * <p>
     * 获取指定工作流执行的所有节点追踪记录，按执行顺序排列。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>查看工作流执行的详细步骤</li>
     *   <li>分析节点执行顺序和时间</li>
     *   <li>排查节点执行问题</li>
     * </ul>
     *
     * <h3>返回数据示例：</h3>
     * <pre>
     * [
     *   {
     *     "id": 1001,
     *     "nodeId": "start",
     *     "nodeType": "START",
     *     "nodeRole": "START",
     *     "status": "SUCCESS",
     *     "executionOrder": 1,
     *     "startedAt": "2024-01-15T10:30:00Z",
     *     "endedAt": "2024-01-15T10:30:01Z",
     *     "durationMs": 1000
     *   },
     *   {
     *     "id": 1002,
     *     "nodeId": "sendNotify",
     *     "nodeType": "FEISHU_SEND_TEXT",
     *     "nodeRole": "NORMAL",
     *     "status": "SUCCESS",
     *     "executionOrder": 2,
     *     "startedAt": "2024-01-15T10:30:01Z",
     *     "endedAt": "2024-01-15T10:30:05Z",
     *     "durationMs": 4000,
     *     "output": {
     *       "success": true,
     *       "messageId": "om_xxxxx"
     *     }
     *   }
     * ]
     * </pre>
     *
     * @param executionId 工作流执行记录 ID
     * @return 节点追踪记录列表
     */
    @GetMapping("/executions/{executionId}/traces")
    public ResponseEntity<List<NodeExecutionTraceEntity>> getNodeTraces(@PathVariable Long executionId) {
        log.info("查询节点轨迹: executionId={}", executionId);

        try {
            List<NodeExecutionTraceEntity> traces = traceService.getNodeTraces(executionId);
            return ResponseEntity.ok(traces);
        } catch (Exception e) {
            log.error("查询节点轨迹异常: executionId={}, error={}", executionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取工作流执行统计信息
     * <p>
     * 统计指定工作流的执行情况，包括总次数、成功次数、失败次数和平均耗时。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>工作流健康度监控</li>
     *   <li>SLA 指标统计</li>
     *   <li>性能趋势分析</li>
     * </ul>
     *
     * <h3>返回数据示例：</h3>
     * <pre>
     * {
     *   "total": 150,
     *   "success": 142,
     *   "failed": 8,
     *   "avgDurationMs": 45000.5
     * }
     * </pre>
     *
     * @param workflowId 工作流 ID
     * @return 执行统计信息
     */
    @GetMapping("/{workflowId}/stats")
    public ResponseEntity<WorkflowTraceService.ExecutionStats> getStats(@PathVariable String workflowId) {
        log.info("查询执行统计: workflowId={}", workflowId);

        try {
            WorkflowTraceService.ExecutionStats stats = traceService.getStats(workflowId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("查询执行统计异常: workflowId={}, error={}", workflowId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重新执行节点
     * <p>
     * 支持三种重新执行策略：
     * <ul>
     *   <li>RERUN_NODE_ONLY - 仅重新执行目标节点</li>
     *   <li>RERUN_NODE_AND_DOWNSTREAM - 重新执行目标节点及所有下游节点</li>
     *   <li>RERUN_FROM_NODE_NEW_EXECUTION - 从目标节点创建全新的工作流执行</li>
     * </ul>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>节点执行失败后的手动重试</li>
     *   <li>人工审批拒绝后的重新执行</li>
     *   <li>数据更新后需要重新计算下游结果</li>
     * </ul>
     *
     * <h3>请求示例：</h3>
     * <pre>
     * POST /api/workflows/executions/123/nodes/sendNotify/rerun
     * Content-Type: application/json
     *
     * {
     *   "strategy": "RERUN_NODE_ONLY"
     * }
     * </pre>
     *
     * <h3>响应示例（成功）：</h3>
     * <pre>
     * {
     *   "success": true,
     *   "message": "Node marked for rerun with retryCount=1",
     *   "data": "12345_sendNotify_1"
     * }
     * </pre>
     *
     * @param executionId 工作流执行记录 ID
     * @param nodeId 要重新执行的节点 ID
     * @param request 重跑策略请求
     * @return 重跑结果
     */
    @PostMapping("/executions/{executionId}/nodes/{nodeId}/rerun")
    public ResponseEntity<WorkflowRerunService.RerunResult> rerunNode(
            @PathVariable Long executionId,
            @PathVariable String nodeId,
            @RequestBody RerunNodeRequest request) {

        log.info("收到节点重跑请求: executionId={}, nodeId={}, strategy={}",
                executionId, nodeId, request.getStrategy());

        try {
            WorkflowRerunService.RerunStrategy strategy =
                    WorkflowRerunService.RerunStrategy.valueOf(request.getStrategy());

            WorkflowRerunService.RerunResult result = rerunService.rerunNode(
                    executionId, nodeId, strategy);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (IllegalArgumentException e) {
            log.error("节点重跑参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    WorkflowRerunService.RerunResult.failure("Invalid strategy: " + request.getStrategy())
            );
        } catch (Exception e) {
            log.error("节点重跑异常: executionId={}, nodeId={}, error={}",
                    executionId, nodeId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 节点重跑请求
     * <p>
     * 用于 POST /api/workflows/executions/{executionId}/nodes/{nodeId}/rerun 接口。
     */
    public static class RerunNodeRequest {
        /**
         * 重跑策略
         * <p>
         * 可选值：
         * <ul>
         *   <li>RERUN_NODE_ONLY - 仅重新执行目标节点</li>
         *   <li>RERUN_NODE_AND_DOWNSTREAM - 重新执行目标节点及所有下游节点</li>
         *   <li>RERUN_FROM_NODE_NEW_EXECUTION - 从目标节点创建全新的工作流执行</li>
         * </ul>
         */
        private String strategy = "RERUN_NODE_ONLY";

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }
}
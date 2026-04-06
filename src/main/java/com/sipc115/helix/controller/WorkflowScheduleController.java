/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.integration.workflow.service.WorkflowExecutionApplicationService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工作流调度控制器
 * <p>
 * 提供 Temporal Schedule 的创建、暂停、恢复、删除等 REST API。
 * 调度表达式从 DSL 的 schedule 配置中自动读取，无需单独传递。
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>POST /api/schedules/{workflowId} - 为工作流创建调度（自动读取 DSL 中的 cron）</li>
 *   <li>POST /api/schedules/{scheduleId}/pause - 暂停调度</li>
 *   <li>POST /api/schedules/{scheduleId}/resume - 恢复调度</li>
 *   <li>DELETE /api/schedules/{scheduleId} - 删除调度</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowExecutionApplicationService
 */
@RestController
@RequestMapping("/api/schedules")
public class WorkflowScheduleController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduleController.class);

    // 修复：使用构造函数注入代替字段注入
    private final WorkflowExecutionApplicationService executionService;

    public WorkflowScheduleController(WorkflowExecutionApplicationService executionService) {
        this.executionService = executionService;
    }

    /**
     * 为工作流创建定时调度
     * <p>
     * 自动从已发布的 DSL 中读取 schedule 配置（Cron 表达式或间隔），
     * 并创建 Temporal Schedule。
     *
     * @param workflowId 工作流 ID
     * @return 调度响应
     */
    @PostMapping("/{workflowId}")
    public ResponseEntity<ScheduleResponse> runWorkflow(@PathVariable String workflowId) {
        log.info("收到创建调度请求: workflowId={}", workflowId);

        try {
            String scheduleId = executionService.runWorkflow(workflowId);

            ScheduleResponse response = new ScheduleResponse();
            response.setScheduleId(scheduleId);
            response.setWorkflowId(workflowId);
            response.setStatus("ACTIVE");
            response.setMessage("调度创建成功");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("创建调度参数错误: {}", e.getMessage());
            // 修复 XSS：禁止在错误响应中直接反射用户输入的 workflowId
            return ResponseEntity.badRequest().body(new ScheduleResponse("ERROR", "工作流不存在或执行计划未找到"));
        } catch (IllegalStateException e) {
            log.error("工作流状态错误: {}", e.getMessage());
            // 修复 XSS：禁止在错误响应中直接反射用户输入的 workflowId
            return ResponseEntity.badRequest().body(new ScheduleResponse("ERROR", "工作流未启用调度配置"));
        } catch (Exception e) {
            log.error("创建调度异常: {}", e.getMessage(), e);
            // 修复 XSS：禁止在错误响应中直接反射用户输入的 workflowId
            return ResponseEntity.internalServerError().body(new ScheduleResponse("ERROR", "创建调度失败，请稍后重试"));
        }
    }

    /**
     * 暂停调度
     */
    @PostMapping("/{scheduleId}/pause")
    public ResponseEntity<Void> pauseSchedule(@PathVariable String scheduleId) {
        log.info("收到暂停调度请求: scheduleId={}", scheduleId);

        try {
            executionService.pauseSchedule(scheduleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("暂停调度失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 恢复调度
     */
    @PostMapping("/{scheduleId}/resume")
    public ResponseEntity<Void> resumeSchedule(@PathVariable String scheduleId) {
        log.info("收到恢复调度请求: scheduleId={}", scheduleId);

        try {
            executionService.resumeSchedule(scheduleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("恢复调度失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除调度
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String scheduleId) {
        log.info("收到删除调度请求: scheduleId={}", scheduleId);

        try {
            executionService.deleteSchedule(scheduleId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("删除调度失败: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 调度响应
     */
    @Data
    public static class ScheduleResponse {
        private String scheduleId;
        private String workflowId;
        private String status;
        private String message;

        // 增加快捷构造函数，用于返回固定格式的错误信息，避免 tainted data 污染
        public ScheduleResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public ScheduleResponse() {
        }
    }
}

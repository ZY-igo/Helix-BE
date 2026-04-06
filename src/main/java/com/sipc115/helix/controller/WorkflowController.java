package com.sipc115.helix.controller;

import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.service.WorkflowApplicationService;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流管理控制器
 * <p>
 * 提供工作流 DSL 的创建、保存、版本控制、发布以及编译等 REST API。
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>POST /api/workflows/create - 创建工作流</li>
 *   <li>POST /api/workflows/save - 保存工作流修改</li>
 *   <li>POST /api/workflows/copy - 复制工作流</li>
 *   <li>GET /api/workflows - 获取指定版本工作流</li>
 *   <li>GET /api/workflows/latest-published - 获取最新已发布版本</li>
 *   <li>GET /api/workflows/versions - 获取所有历史版本</li>
 *   <li>POST /api/workflows/publish - 发布工作流</li>
 *   <li>POST /api/workflows/save-and-compile - 保存并编译</li>
 * </ul>
 *
 * <h3>重要说明：</h3>
 * <ul>
 *   <li>工作流发布后，旧版本自动标记为 DEPRECATED</li>
 *   <li>save-and-compile 会先保存 DSL 再编译为 ExecutionPlan</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowApplicationService
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowApplicationService workflowService;

    public WorkflowController(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * 创建工作流
     *
     * @param request 包含工作流名称和创建人信息的请求体
     * @return 初始化后的工作流实体（状态为 DRAFT）
     */
    @PostMapping("/create")
    public ResponseEntity<WorkflowDslEntity> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        log.info("REST: Create workflow. name={}", request.getName());

        String createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : "system";
        WorkflowDslEntity created = workflowService.createWorkflow(request.getName(), createdBy);

        return ResponseEntity.ok(created);
    }

    /**
     * 保存工作流修改
     *
     * @param request 包含更新后的 DSL 对象和更新人信息的请求体
     * @return 保存后的工作流实体
     */
    @PostMapping("/save")
    public ResponseEntity<WorkflowDslEntity> saveWorkflow(@RequestBody SaveWorkflowRequest request) {
        log.info("REST: Save workflow. workflowId={}, version={}",
                request.getDsl().getWorkflowId(), request.getDsl().getVersion());

        String updatedBy = request.getUpdatedBy() != null ? request.getUpdatedBy() : "system";
        WorkflowDslEntity saved = workflowService.saveWorkflow(request.getDsl(), updatedBy);

        return ResponseEntity.ok(saved);
    }

    /**
     * 复制工作流
     * <p>
     * 支持两种模式：
     * 1. 同一工作流 ID 下的版本迭代（newWorkflowId 为空）
     * 2. 基于现有工作流创建全新的工作流 ID（newWorkflowId 不为空）
     *
     * @param request 包含源工作流信息和新版本/新 ID 的请求体
     * @return 复制后的新工作流实体
     */
    @PostMapping("/copy")
    public ResponseEntity<WorkflowDslEntity> copyWorkflow(@RequestBody CopyWorkflowRequest request) {
        log.info("REST: Copy workflow. source={} v{}, newVersion={}",
                request.getSourceWorkflowId(), request.getSourceVersion(), request.getNewVersion());

        String createdBy = request.getCreatedBy() != null ? request.getCreatedBy() : "system";
        WorkflowDslEntity copied;

        if (request.getNewWorkflowId() != null) {
            copied = workflowService.createCopyAsNewWorkflow(
                    request.getSourceWorkflowId(),
                    request.getSourceVersion(),
                    request.getNewWorkflowId(),
                    request.getNewVersion(),
                    createdBy
            );
        } else {
            copied = workflowService.createCopy(
                    request.getSourceWorkflowId(),
                    request.getSourceVersion(),
                    request.getNewVersion(),
                    createdBy
            );
        }

        return ResponseEntity.ok(copied);
    }

    /**
     * 获取指定版本的工作流
     *
     * @param workflowId 工作流唯一标识
     * @param version    版本号
     * @return 工作流实体
     */
    @GetMapping
    public ResponseEntity<WorkflowDslEntity> getWorkflow(
            @RequestParam String workflowId,
            @RequestParam String version) {
        log.debug("REST: Get workflow. workflowId={}, version={}", workflowId, version);

        WorkflowDslEntity workflow = workflowService.getWorkflow(workflowId, version);
        return ResponseEntity.ok(workflow);
    }

    /**
     * 获取最新已发布的工作流版本
     *
     * @param workflowId 工作流唯一标识
     * @return 最近一次发布的工作流实体
     */
    @GetMapping("/latest-published")
    public ResponseEntity<WorkflowDslEntity> getLatestPublishedWorkflow(@RequestParam String workflowId) {
        log.debug("REST: Get latest published workflow. workflowId={}", workflowId);

        WorkflowDslEntity workflow = workflowService.getLatestPublishedWorkflow(workflowId);
        return ResponseEntity.ok(workflow);
    }

    /**
     * 获取所有历史版本列表
     *
     * @param workflowId 工作流唯一标识
     * @return 工作流实体列表（按创建时间倒序）
     */
    @GetMapping("/versions")
    public ResponseEntity<List<WorkflowDslEntity>> getAllVersions(@RequestParam String workflowId) {
        log.debug("REST: Get all versions. workflowId={}", workflowId);

        List<WorkflowDslEntity> versions = workflowService.getAllVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    /**
     * 发布工作流
     * <p>
     * 发布操作会将该工作流下其他已发布的版本标记为 DEPRECATED，
     * 确保同一时间只有一个生效版本。
     *
     * @param workflowId 工作流唯一标识
     * @param version    待发布的版本号
     * @param updatedBy  操作人标识（可选）
     * @return 发布后的工作流实体
     */
    @PostMapping("/publish")
    public ResponseEntity<WorkflowDslEntity> publishWorkflow(
            @RequestParam String workflowId,
            @RequestParam String version,
            @RequestParam(required = false) String updatedBy) {
        log.info("REST: Publish workflow. workflowId={}, version={}", workflowId, version);

        String user = updatedBy != null ? updatedBy : "system";
        WorkflowDslEntity published = workflowService.publish(workflowId, version, user);

        return ResponseEntity.ok(published);
    }

    /**
     * 保存并编译工作流
     * <p>
     * 组合接口：先持久化 DSL 定义，再编译生成扁平化的 ExecutionPlan。
     * 用于前端在保存后立即验证工作流逻辑的正确性。
     *
     * @param request 包含 DSL 对象和编译人信息的请求体
     * @return 包含 DSL 实体和执行计划实体的映射
     */
    @PostMapping("/save-and-compile")
    public ResponseEntity<Map<String, Object>> saveAndCompile(@RequestBody SaveAndCompileRequest request) {
        log.info("REST: Save and compile workflow. workflowId={}, version={}",
                request.getDsl().getWorkflowId(), request.getDsl().getVersion());

        String compiledBy = request.getCompiledBy() != null ? request.getCompiledBy() : "system";

        WorkflowDslEntity dslSaved = workflowService.saveWorkflow(request.getDsl(), compiledBy);

        ExecutionPlanEntity planSaved = workflowService.saveAndCompile(request.getDsl(), compiledBy);

        Map<String, Object> result = new HashMap<>();
        result.put("dsl", dslSaved);
        result.put("executionPlan", planSaved);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * 创建工作流的请求参数封装
     */
    @Data
    public static class CreateWorkflowRequest {
        private String name;
        private String createdBy;
    }

    /**
     * 保存工作流的请求参数封装
     */
    @Data
    public static class SaveWorkflowRequest {
        private WorkflowDsl dsl;
        private String updatedBy;
    }

    /**
     * 复制工作流的请求参数封装
     */
    @Data
    public static class CopyWorkflowRequest {
        private String sourceWorkflowId;
        private String sourceVersion;
        private String newWorkflowId;
        private String newVersion;
        private String createdBy;
    }

    /**
     * 保存并编译工作流的请求参数封装
     */
    @Data
    public static class SaveAndCompileRequest {
        private WorkflowDsl dsl;
        private String compiledBy;
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.service.WorkflowDslService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 工作流 DSL 控制器
 * <p>
 * 提供工作流领域特定语言（DSL）定义的 RESTful API 接口。
 * 工作流 DSL 是工作流的定义描述，包含节点、边和元数据，用于描述工作流的结构和逻辑。
 * </p>
 *
 * <p>主要功能包括：</p>
 * <ul>
 *   <li>保存工作流 DSL 草稿</li>
 *   <li>发布工作流 DSL</li>
 *   <li>下线（弃用）工作流 DSL</li>
 *   <li>查询工作流 DSL（按 ID 或 工作流ID+版本）</li>
 *   <li>解析 DSL 内容</li>
 *   <li>查询工作流的所有版本</li>
 *   <li>删除工作流 DSL</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/workflow-dsl")
public class WorkflowDslController {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(WorkflowDslController.class);

    /**
     * 工作流 DSL 服务
     */
    @Autowired
    private WorkflowDslService workflowDslService;

    /**
     * 保存工作流 DSL 草稿
     * <p>
     * 创建一个新的工作流 DSL 草稿或更新现有草稿。
     * 如果是新的 workflowId，会创建 version=1 的草稿。
     * 如果已存在，会在最高版本基础上 +1 创建新版本。
     * </p>
     *
     * @param request 请求体，包含 workflowId、name、dsl 和 createdBy
     * @return 保存后的 DSL 实体，包含自动生成的版本号
     */
    @PostMapping("/drafts")
    public ResponseEntity<WorkflowDslEntity> saveDraft(@RequestBody SaveDraftRequest request) {
        log.info("Saving workflow DSL draft. workflowId={}, name={}",
                request.getWorkflowId(), request.getName());

        WorkflowDslEntity saved = workflowDslService.saveDraft(
                request.getWorkflowId(),
                request.getName(),
                request.getDsl(),
                request.getCreatedBy()
        );

        log.info("Workflow DSL draft saved successfully. id={}, version={}",
                saved.getId(), saved.getVersion());

        return ResponseEntity.ok(saved);
    }

    /**
     * 发布工作流 DSL
     * <p>
     * 将指定版本的草稿 DSL 状态设置为已发布。
     * 发布后会自动将同一工作流的其他已发布版本标记为 DEPRECATED。
     * 发布后的 DSL 可以用于创建执行计划和工作流实例。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @param updatedBy 更新人
     * @return 发布后的 DSL 实体
     */
    @PostMapping("/publish")
    public ResponseEntity<WorkflowDslEntity> publish(
            @RequestParam String workflowId,
            @RequestParam String version,
            @RequestParam String updatedBy) {
        log.info("Publishing workflow DSL. workflowId={}, version={}", workflowId, version);

        try {
            WorkflowDslEntity published = workflowDslService.publish(workflowId, version, updatedBy);
            log.info("Workflow DSL published successfully. id={}", published.getId());
            return ResponseEntity.ok(published);
        } catch (IllegalArgumentException e) {
            log.warn("Workflow DSL not found for publish. workflowId={}, version={}", workflowId, version);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 回滚工作流 DSL
     * <p>
     * 将工作流回滚到指定版本。
     * 会将当前已发布版本标记为 DEPRECATED，并将目标版本标记为 PUBLISHED。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param targetVersion 目标版本号
     * @param updatedBy 更新人
     * @return 回滚后的 DSL 实体
     */
    @PostMapping("/rollback")
    public ResponseEntity<WorkflowDslEntity> rollback(
            @RequestParam String workflowId,
            @RequestParam String targetVersion,
            @RequestParam String updatedBy) {
        log.info("Rolling back workflow DSL. workflowId={}, targetVersion={}", workflowId, targetVersion);

        try {
            WorkflowDslEntity rolledBack = workflowDslService.rollback(workflowId, targetVersion, updatedBy);
            log.info("Workflow DSL rolled back successfully. id={}", rolledBack.getId());
            return ResponseEntity.ok(rolledBack);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Failed to rollback workflow DSL. workflowId={}, targetVersion={}, error={}",
                    workflowId, targetVersion, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 下线（弃用）工作流 DSL
     * <p>
     * 将指定版本的已发布 DSL 状态设置为已下线。
     * 下线后的 DSL 不再可用于创建新的工作流实例，但已有实例不受影响。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @param updatedBy 更新人
     * @return 下线后的 DSL 实体
     */
    @PostMapping("/deprecate")
    public ResponseEntity<WorkflowDslEntity> deprecate(
            @RequestParam String workflowId,
            @RequestParam String version,
            @RequestParam String updatedBy) {
        log.info("Deprecating workflow DSL. workflowId={}, version={}", workflowId, version);

        try {
            WorkflowDslEntity deprecated = workflowDslService.deprecate(workflowId, version, updatedBy);
            log.info("Workflow DSL deprecated successfully. id={}", deprecated.getId());
            return ResponseEntity.ok(deprecated);
        } catch (IllegalArgumentException e) {
            log.warn("Workflow DSL not found for deprecate. workflowId={}, version={}", workflowId, version);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据主键 ID 查询工作流 DSL
     * <p>
     * 通过数据库主键 ID 获取 DSL 实体。
     * </p>
     *
     * @param id DSL 主键 ID
     * @return DSL 实体，如果不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDslEntity> findById(@PathVariable Long id) {
        log.debug("Finding workflow DSL by id={}", id);

        Optional<WorkflowDslEntity> result = workflowDslService.findById(id);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 根据工作流 ID 和版本查询 DSL
     * <p>
     * 通过工作流 ID 和版本号的组合查询对应的 DSL。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return DSL 实体，如果不存在返回 404
     */
    @GetMapping("/by-workflow")
    public ResponseEntity<WorkflowDslEntity> findByWorkflowIdAndVersion(
            @RequestParam String workflowId,
            @RequestParam String version) {
        log.debug("Finding workflow DSL by workflowId={}, version={}", workflowId, version);

        Optional<WorkflowDslEntity> result = workflowDslService.findByWorkflowIdAndVersion(workflowId, version);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 查询工作流最新发布的版本
     * <p>
     * 获取指定工作流最新发布的 DSL 版本。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @return 最新发布的 DSL 实体，如果不存在返回 404
     */
    @GetMapping("/latest-published")
    public ResponseEntity<WorkflowDslEntity> findLatestPublished(@RequestParam String workflowId) {
        log.debug("Finding latest published DSL for workflowId={}", workflowId);

        Optional<WorkflowDslEntity> result = workflowDslService.findLatestPublished(workflowId);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 解析 DSL 内容
     * <p>
     * 将存储的 JSON 字符串解析为可读的 WorkflowDsl 对象。
     * 返回工作流的节点、边和元数据等详细信息。
     * </p>
     *
     * @param id DSL 主键 ID
     * @return 解析后的 WorkflowDsl 对象
     */
    @GetMapping("/{id}/parsed")
    public ResponseEntity<WorkflowDsl> parseDsl(@PathVariable Long id) {
        log.debug("Parsing DSL content for id={}", id);

        Optional<WorkflowDslEntity> entityOpt = workflowDslService.findById(id);
        if (entityOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            WorkflowDsl dsl = workflowDslService.parseDsl(entityOpt.get());
            return ResponseEntity.ok(dsl);
        } catch (Exception e) {
            log.error("Failed to parse DSL. id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询工作流的所有版本
     * <p>
     * 获取指定工作流的所有历史版本 DSL。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @return 该工作流的所有 DSL 版本列表
     */
    @GetMapping("/versions")
    public ResponseEntity<List<WorkflowDslEntity>> findAllVersions(@RequestParam String workflowId) {
        log.debug("Finding all DSL versions for workflowId={}", workflowId);

        List<WorkflowDslEntity> versions = workflowDslService.findAllVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    /**
     * 删除工作流 DSL
     * <p>
     * 根据主键 ID 删除指定的 DSL。
     * 注意：此操作不可逆，且可能影响关联的执行计划。
     * </p>
     *
     * @param id DSL 主键 ID
     * @return 删除成功返回 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        log.info("Deleting workflow DSL. id={}", id);

        workflowDslService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 保存草稿请求对象
     * <p>
     * 封装保存草稿所需的参数。
     * </p>
     */
    public static class SaveDraftRequest {
        /**
         * 工作流 ID
         */
        private String workflowId;

        /**
         * 工作流名称
         */
        private String name;

        /**
         * 工作流 DSL 对象
         */
        private WorkflowDsl dsl;

        /**
         * 创建人
         */
        private String createdBy;

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public WorkflowDsl getDsl() {
            return dsl;
        }

        public void setDsl(WorkflowDsl dsl) {
            this.dsl = dsl;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.service.ExecutionPlanService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 执行计划控制器
 * <p>
 * 提供执行计划的编译、保存、查询等 RESTful API 接口。
 * 执行计划是将工作流 DSL 编译后生成的可执行对象，包含节点的编译结果和转换规则。
 * </p>
 *
 * <p>主要功能包括：</p>
 * <ul>
 *   <li>编译并保存工作流 DSL 为执行计划</li>
 *   <li>根据 ID、planId 或工作流 ID+版本查询执行计划</li>
 *   <li>解析执行计划内容</li>
 *   <li>删除执行计划</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/execution-plans")
public class ExecutionPlanController {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ExecutionPlanController.class);

    /**
     * 执行计划服务
     */
    @Autowired
    private ExecutionPlanService executionPlanService;

    /**
     * 编译并保存执行计划
     * <p>
     * 接收工作流 DSL 对象，编译为执行计划并持久化到数据库。
     * 这是创建可执行工作流的核心接口。
     * </p>
     *
     * @param request 请求体，包含 workflowDsl（工作流DSL对象）和 compiledBy（编译人）
     * @return 保存后的执行计划实体
     */
    @PostMapping("/compile")
    public ResponseEntity<ExecutionPlanEntity> compileAndSave(@RequestBody CompileRequest request) {
        log.info("Received compile request for workflowId={}, version={}",
                request.getWorkflowDsl().getWorkflowId(), request.getWorkflowDsl().getVersion());

        ExecutionPlanEntity saved = executionPlanService.compileAndSave(
                request.getWorkflowDsl(),
                request.getCompiledBy()
        );

        log.info("Execution plan compiled and saved successfully. planId={}", saved.getPlanId());
        return ResponseEntity.ok(saved);
    }

    /**
     * 根据主键 ID 查询执行计划
     * <p>
     * 通过数据库主键 ID 获取执行计划实体。
     * </p>
     *
     * @param id 执行计划主键 ID
     * @return 执行计划实体，如果不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionPlanEntity> findById(@PathVariable Long id) {
        log.debug("Finding execution plan by id={}", id);

        Optional<ExecutionPlanEntity> result = executionPlanService.findById(id);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 根据计划 ID 查询执行计划
     * <p>
     * 通过唯一的 planId 查询执行计划。
     * planId 通常格式为：{workflowId}-v{version}-{timestamp}
     * </p>
     *
     * @param planId 执行计划唯一标识
     * @return 执行计划实体，如果不存在返回 404
     */
    @GetMapping("/by-plan-id/{planId}")
    public ResponseEntity<ExecutionPlanEntity> findByPlanId(@PathVariable String planId) {
        log.debug("Finding execution plan by planId={}", planId);

        Optional<ExecutionPlanEntity> result = executionPlanService.findByPlanId(planId);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 根据工作流 ID 和版本查询执行计划
     * <p>
     * 通过工作流 ID 和版本号的组合查询对应的执行计划。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 执行计划实体，如果不存在返回 404
     */
    @GetMapping("/by-workflow")
    public ResponseEntity<ExecutionPlanEntity> findByWorkflowIdAndVersion(
            @RequestParam String workflowId,
            @RequestParam String version) {
        log.debug("Finding execution plan by workflowId={}, version={}", workflowId, version);

        Optional<ExecutionPlanEntity> result = executionPlanService.findByWorkflowIdAndVersion(workflowId, version);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 解析执行计划内容
     * <p>
     * 将存储的 JSON 字符串解析为可读的 ExecutionPlan 对象。
     * 返回编译后的节点映射、转换规则等详细信息。
     * </p>
     *
     * @param id 执行计划主键 ID
     * @return 解析后的执行计划对象，包含节点定义和转换规则
     */
    @GetMapping("/{id}/parsed")
    public ResponseEntity<ExecutionPlan> parsePlan(@PathVariable Long id) {
        log.debug("Parsing execution plan content for id={}", id);

        Optional<ExecutionPlanEntity> entityOpt = executionPlanService.findById(id);
        if (entityOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            ExecutionPlan plan = executionPlanService.parsePlan(entityOpt.get());
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            log.error("Failed to parse execution plan. id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询工作流的所有执行计划版本
     * <p>
     * 获取指定工作流的所有历史版本执行计划。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @return 该工作流的所有执行计划版本列表
     */
    @GetMapping("/versions")
    public ResponseEntity<List<ExecutionPlanEntity>> findAllVersions(@RequestParam String workflowId) {
        log.debug("Finding all execution plan versions for workflowId={}", workflowId);

        List<ExecutionPlanEntity> versions = executionPlanService.findAllVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    /**
     * 删除执行计划
     * <p>
     * 根据主键 ID 删除指定的执行计划。
     * 注意：此操作不可逆，请谨慎使用。
     * </p>
     *
     * @param id 执行计划主键 ID
     * @return 删除成功返回 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        log.info("Deleting execution plan. id={}", id);

        executionPlanService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除指定工作流版本的执行计划
     * <p>
     * 通过工作流 ID 和版本号的组合删除对应的执行计划。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 删除成功返回 204 No Content
     */
    @DeleteMapping("/by-workflow")
    public ResponseEntity<Void> deleteByWorkflowIdAndVersion(
            @RequestParam String workflowId,
            @RequestParam String version) {
        log.info("Deleting execution plan by workflowId={}, version={}", workflowId, version);

        executionPlanService.deleteByWorkflowIdAndVersion(workflowId, version);
        return ResponseEntity.noContent().build();
    }

    /**
     * 编译请求对象
     * <p>
     * 封装编译执行计划所需的参数。
     * </p>
     */
    @Data
    public static class CompileRequest {
        /**
         * 工作流 DSL 对象
         */
        private WorkflowDsl workflowDsl;

        /**
         * 编译人
         */
        private String compiledBy;
    }
}

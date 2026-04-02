/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import com.sipc115.helix.repository.jpa.JpaExecutionPlanRepository;
import com.sipc115.helix.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 执行计划服务类
 * <p>
 * 负责工作流执行计划的编译、保存和查询
 * </p>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class ExecutionPlanService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ExecutionPlanService.class);

    /**
     * 执行计划仓库
     */
    private final JpaExecutionPlanRepository planRepository;

    /**
     * DSL 编译器
     */
    private final DslCompiler dslCompiler;

    /**
     * Jackson 对象映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 雪花 ID 生成器
     */
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 构造函数
     *
     * @param planRepository 执行计划仓库
     * @param dslCompiler DSL 编译器
     * @param objectMapper Jackson 对象映射器
     * @param idGenerator 雪花 ID 生成器
     */
    public ExecutionPlanService(
            JpaExecutionPlanRepository planRepository,
            DslCompiler dslCompiler,
            ObjectMapper objectMapper,
            SnowflakeIdGenerator idGenerator) {
        this.planRepository = planRepository;
        this.dslCompiler = dslCompiler;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 编译并保存执行计划
     * <p>
     * 将 DSL 编译为执行计划并持久化到数据库
     * </p>
     *
     * @param dsl 工作流 DSL 对象
     * @param compiledBy 编译人
     * @return 编译后的执行计划实体
     */
    @Transactional
    public ExecutionPlanEntity compileAndSave(WorkflowDsl dsl, String compiledBy) {
        log.info("Compiling and saving execution plan. workflowId={}, version={}",
                dsl.getWorkflowId(), dsl.getVersion());

        try {
            // 1. 编译 DSL
            ExecutionPlan plan = dslCompiler.compile(dsl);

            // 2. 生成唯一的计划 ID
            String planId = String.valueOf(idGenerator.nextId());
            plan.setPlanId(planId);

            // 3. 创建实体
            ExecutionPlanEntity entity = new ExecutionPlanEntity();
            entity.setPlanId(planId);
            entity.setWorkflowId(dsl.getWorkflowId());
            entity.setVersion(dsl.getVersion());
            entity.setPlanContent(objectMapper.writeValueAsString(plan));
            entity.setCompilerVersion("1.0.0"); // TODO: 从配置或常量获取
            entity.setCompiledAt(Instant.now());
            entity.setCreatedAt(Instant.now());
            entity.setCreatedBy(compiledBy);

            ExecutionPlanEntity saved = planRepository.save(entity);
            log.info("Execution plan compiled and saved successfully. planId={}, workflowId={}",
                    saved.getPlanId(), saved.getWorkflowId());

            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize execution plan. workflowId={}", dsl.getWorkflowId(), e);
            throw new RuntimeException("Failed to serialize execution plan: " + e.getMessage(), e);
        }
    }

    /**
     * 根据主键 ID 查询执行计划
     *
     * @param id 主键 ID
     * @return 执行计划实体，不存在返回 Optional.empty()
     */
    public Optional<ExecutionPlanEntity> findById(Long id) {
        return planRepository.findById(id);
    }

    /**
     * 根据计划 ID 查询执行计划
     *
     * @param planId 计划 ID
     * @return 执行计划实体，不存在返回 Optional.empty()
     */
    public Optional<ExecutionPlanEntity> findByPlanId(String planId) {
        return planRepository.findByPlanId(planId);
    }

    /**
     * 根据工作流 ID 和版本查询执行计划
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 执行计划实体，不存在返回 Optional.empty()
     */
    public Optional<ExecutionPlanEntity> findByWorkflowIdAndVersion(String workflowId, Integer version) {
        return planRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    /**
     * 解析执行计划内容
     * <p>
     * 将存储的 JSON 字符串转换为 ExecutionPlan 对象
     * </p>
     *
     * @param entity 执行计划实体
     * @return ExecutionPlan 对象
     */
    public ExecutionPlan parsePlan(ExecutionPlanEntity entity) {
        try {
            return objectMapper.readValue(entity.getPlanContent(), ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse execution plan content. entityId={}", entity.getId(), e);
            throw new RuntimeException("Failed to parse execution plan: " + e.getMessage(), e);
        }
    }

    /**
     * 查询工作流的所有执行计划版本
     *
     * @param workflowId 工作流 ID
     * @return 该工作流的所有执行计划版本列表
     */
    public List<ExecutionPlanEntity> findAllVersions(String workflowId) {
        // TODO: 需要在 Repository 中添加自定义查询方法
        return planRepository.findAll();
    }

    /**
     * 删除执行计划
     *
     * @param id 主键 ID
     */
    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting execution plan. id={}", id);
        planRepository.deleteById(id);
        log.info("Execution plan deleted successfully. id={}", id);
    }

    /**
     * 删除指定工作流的执行计划
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     */
    @Transactional
    public void deleteByWorkflowIdAndVersion(String workflowId, Integer version) {
        log.info("Deleting execution plan. workflowId={}, version={}", workflowId, version);

        findByWorkflowIdAndVersion(workflowId, version)
                .ifPresent(entity -> {
                    planRepository.deleteById(entity.getId());
                    log.info("Execution plan deleted successfully. workflowId={}, version={}", workflowId, version);
                });
    }
}

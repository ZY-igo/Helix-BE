/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.repository.jpa.JpaExecutionPlanRepository;
import com.sipc115.helix.repository.jpa.JpaWorkflowDslRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 工作流持久化服务
 * <p>
 * 负责工作流 DSL 和执行计划的持久化存储与读取。
 * 该服务是独立的，不耦合到具体的业务流程中。
 */
@Service
public class WorkflowPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowPersistenceService.class);

    private final JpaWorkflowDslRepository dslRepository;
    private final JpaExecutionPlanRepository planRepository;
    private final ObjectMapper objectMapper;

    public WorkflowPersistenceService(
            JpaWorkflowDslRepository dslRepository,
            JpaExecutionPlanRepository planRepository,
            ObjectMapper objectMapper) {
        this.dslRepository = dslRepository;
        this.planRepository = planRepository;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // DSL 持久化方法
    // =====================================================

    /**
     * 保存工作流 DSL 到数据库
     *
     * @param dsl 工作流 DSL 对象
     * @return 保存后的 DSL 对象
     */
    @Transactional
    public WorkflowDsl saveDsl(WorkflowDsl dsl) {
        try {
            WorkflowDslEntity entity = new WorkflowDslEntity();
            entity.setWorkflowId(dsl.getWorkflowId());
            entity.setVersion(dsl.getVersion());
            entity.setDslContent(objectMapper.writeValueAsString(dsl));
            entity.setStatus("PUBLISHED");
            entity.setMetadata(objectMapper.writeValueAsString(dsl.getMetadata()));
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            entity.setCreatedBy("system");
            entity.setUpdatedBy("system");

            WorkflowDslEntity saved = dslRepository.save(entity);
            logger.info("✅ DSL 已保存到数据库。workflowId={}, version={}",
                    dsl.getWorkflowId(), dsl.getVersion());

            return toDomain(saved);
        } catch (JsonProcessingException e) {
            logger.error("❌ DSL 序列化失败", e);
            throw new RuntimeException("DSL 序列化失败", e);
        }
    }

    /**
     * 从数据库读取工作流 DSL
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return DSL 对象（如果不存在返回 Optional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowDsl> findDsl(String workflowId, Integer version) {
        return dslRepository.findByWorkflowIdAndVersion(workflowId, String.valueOf(version))
                .map(this::toDomain);
    }

    // =====================================================
    // 执行计划持久化方法
    // =====================================================

    /**
     * 保存执行计划到数据库
     *
     * @param plan 执行计划对象
     * @return 保存后的执行计划对象
     */
    @Transactional
    public ExecutionPlan saveExecutionPlan(ExecutionPlan plan) {
        try {
            ExecutionPlanEntity entity = new ExecutionPlanEntity();
            entity.setPlanId(plan.getPlanId() != null ? plan.getPlanId() : java.util.UUID.randomUUID().toString());
            entity.setWorkflowId(plan.getWorkflowId());
            entity.setVersion(plan.getWorkflowVersion());
            entity.setPlanContent(objectMapper.writeValueAsString(plan));
            entity.setCompilerVersion("2.0.0");
            entity.setCompiledAt(Instant.now());
            entity.setCreatedAt(Instant.now());
            entity.setCreatedBy("system");

            planRepository.save(entity);
            logger.info("✅ 执行计划已保存到数据库。workflowId={}, version={}, planId={}",
                    plan.getWorkflowId(), plan.getWorkflowVersion(), entity.getPlanId());

            return plan;
        } catch (JsonProcessingException e) {
            logger.error("❌ 执行计划序列化失败", e);
            throw new RuntimeException("执行计划序列化失败", e);
        }
    }

    /**
     * 从数据库读取执行计划
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 执行计划对象（如果不存在返回 Optional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<ExecutionPlan> findExecutionPlan(String workflowId, String version) {
        return planRepository.findByWorkflowIdAndVersion(workflowId, version)
                .map(this::toDomain);
    }

    /**
     * 从数据库读取执行计划（通过 planId）
     *
     * @param planId 计划 ID
     * @return 执行计划对象（如果不存在返回 Optional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<ExecutionPlan> findExecutionPlanByPlanId(String planId) {
        return planRepository.findByPlanId(planId)
                .map(this::toDomain);
    }

    /**
     * 获取工作流最新已发布的执行计划
     * <p>
     * 通过查找最新已发布的 DSL 版本，然后获取对应的执行计划。
     *
     * @param workflowId 工作流 ID
     * @return 最新发布的执行计划（如果不存在返回 Optional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<ExecutionPlan> findLatestPublishedExecutionPlan(String workflowId) {
        Optional<WorkflowDslEntity> publishedDsl = dslRepository
                .findFirstByWorkflowIdAndStatusOrderByCreatedAtDesc(workflowId, "PUBLISHED");

        if (publishedDsl.isEmpty()) {
            logger.warn("未找到已发布的工作流。workflowId={}", workflowId);
            return Optional.empty();
        }

        String version = publishedDsl.get().getVersion();
        logger.info("找到最新发布的 DSL 版本。workflowId={}, version={}", workflowId, version);

        return findExecutionPlan(workflowId, version);
    }

    /**
     * 获取工作流最新版本（不限状态）
     * <p>
     * 查找该工作流最新创建的一个版本，不关心其发布状态。
     *
     * @param workflowId 工作流 ID
     * @return 最新版本的 DSL（如果不存在返回 Optional.empty()）
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowDsl> findLatestDsl(String workflowId) {
        List<WorkflowDslEntity> dsls = dslRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        if (dsls.isEmpty()) {
            logger.warn("未找到任何工作流版本。workflowId={}", workflowId);
            return Optional.empty();
        }
        WorkflowDslEntity latest = dsls.get(0);
        logger.info("找到最新版本的 DSL。workflowId={}, version={}", workflowId, latest.getVersion());
        return Optional.of(toDomain(latest));
    }

    // =====================================================
    // 转换方法（Entity → Domain）
    // =====================================================

    private WorkflowDsl toDomain(WorkflowDslEntity entity) {
        try {
            return objectMapper.readValue(entity.getDslContent(), WorkflowDsl.class);
        } catch (JsonProcessingException e) {
            logger.error("❌ DSL 反序列化失败", e);
            throw new RuntimeException("DSL 反序列化失败", e);
        }
    }

    private ExecutionPlan toDomain(ExecutionPlanEntity entity) {
        try {
            return objectMapper.readValue(entity.getPlanContent(), ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            logger.error("❌ 执行计划反序列化失败", e);
            throw new RuntimeException("执行计划反序列化失败", e);
        }
    }
}

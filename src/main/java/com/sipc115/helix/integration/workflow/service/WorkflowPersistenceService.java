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
            // 先查询是否存在，存在则只更新内容，不存在则新建
            Optional<WorkflowDslEntity> existingOpt = dslRepository.findByWorkflowIdAndVersion(dsl.getWorkflowId(), dsl.getVersion());
            WorkflowDslEntity entity;
            
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.setDslContent(objectMapper.writeValueAsString(dsl));
                entity.setMetadata(objectMapper.writeValueAsString(dsl.getMetadata()));
                entity.setUpdatedAt(Instant.now());
                // 保持原有状态不变，除非显式调用 updateDslState
            } else {
                entity = new WorkflowDslEntity();
                entity.setWorkflowId(dsl.getWorkflowId());
                entity.setVersion(dsl.getVersion());
                entity.setDslContent(objectMapper.writeValueAsString(dsl));
                entity.setStatus("PUBLISHED");
                entity.setMetadata(objectMapper.writeValueAsString(dsl.getMetadata()));
                entity.setState(com.sipc115.helix.domain.workflow.WorkflowState.INITIALIZING.name());
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                entity.setCreatedBy("system");
                entity.setUpdatedBy("system");
            }

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

    /**
     * 更新工作流版本状态
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @param state 新状态
     * @param stateDetail 状态详情（可选）
     */
    @Transactional
    public void updateDslState(String workflowId, String version, String state, String stateDetail) {
        Optional<WorkflowDslEntity> entityOpt = dslRepository.findByWorkflowIdAndVersion(workflowId, version);
        if (entityOpt.isEmpty()) {
            logger.warn("无法更新状态，未找到工作流。workflowId={}, version={}", workflowId, version);
            return;
        }
        WorkflowDslEntity entity = entityOpt.get();
        entity.setState(state);
        entity.setStateDetail(stateDetail);
        entity.setUpdatedAt(Instant.now());
        dslRepository.save(entity);
        logger.info("工作流状态已更新。workflowId={}, version={}, state={}", workflowId, version, state);
    }

    /**
     * 获取工作流版本的当前状态
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 状态值（如果不存在返回 null）
     */
    @Transactional(readOnly = true)
    public String getDslState(String workflowId, String version) {
        Optional<WorkflowDslEntity> entityOpt = dslRepository.findByWorkflowIdAndVersion(workflowId, version);
        return entityOpt.map(WorkflowDslEntity::getState).orElse(null);
    }

    /**
     * 获取工作流版本的最新版本（状态为 RUNNING）
     * <p>
     * 由于 scheduleId = workflowId（不含版本），同一时间只有一个版本处于 RUNNING 状态。
     * 该方法查找创建时间最新的 RUNNING 版本。
     *
     * @param workflowId 工作流 ID
     * @return 状态为 RUNNING 的版本号（如果不存在返回 null）
     */
    @Transactional(readOnly = true)
    public String findRunningVersion(String workflowId) {
        List<WorkflowDslEntity> dsls = dslRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        for (WorkflowDslEntity entity : dsls) {
            if (com.sipc115.helix.domain.workflow.WorkflowState.RUNNING.name().equals(entity.getState())) {
                logger.info("找到运行中的工作流版本。workflowId={}, version={}", workflowId, entity.getVersion());
                return entity.getVersion();
            }
        }
        return null;
    }

    /**
     * 获取工作流版本的最新版本（状态为 RUNNING），按更新时间排序
     * <p>
     * 适用于更精确地查找最近一次被设置为 RUNNING 的版本。
     *
     * @param workflowId 工作流 ID
     * @return 状态为 RUNNING 的版本号（如果不存在返回 null）
     */
    @Transactional(readOnly = true)
    public String findRunningVersionByUpdatedAt(String workflowId) {
        List<WorkflowDslEntity> dsls = dslRepository.findByWorkflowIdOrderByUpdatedAtDesc(workflowId);
        for (WorkflowDslEntity entity : dsls) {
            if (com.sipc115.helix.domain.workflow.WorkflowState.RUNNING.name().equals(entity.getState())) {
                logger.info("找到运行中的工作流版本（按更新时间）。workflowId={}, version={}", workflowId, entity.getVersion());
                return entity.getVersion();
            }
        }
        return null;
    }

    /**
     * 获取工作流版本的状态详情
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 状态详情（如果不存在返回 null）
     */
    @Transactional(readOnly = true)
    public String getDslStateDetail(String workflowId, String version) {
        Optional<WorkflowDslEntity> entityOpt = dslRepository.findByWorkflowIdAndVersion(workflowId, version);
        return entityOpt.map(WorkflowDslEntity::getStateDetail).orElse(null);
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

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

@Service
public class ExecutionPlanService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionPlanService.class);

    private final JpaExecutionPlanRepository planRepository;
    private final DslCompiler dslCompiler;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator idGenerator;

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

    @Transactional
    public ExecutionPlanEntity compileAndSave(WorkflowDsl dsl, String compiledBy) {
        log.info("Compiling and saving execution plan. workflowId={}, version={}",
                dsl.getWorkflowId(), dsl.getVersion());

        try {
            ExecutionPlan plan = dslCompiler.compile(dsl);

            String planId = String.valueOf(idGenerator.nextId());
            plan.setPlanId(planId);

            ExecutionPlanEntity entity = new ExecutionPlanEntity();
            entity.setPlanId(planId);
            entity.setWorkflowId(dsl.getWorkflowId());
            entity.setVersion(dsl.getVersion());
            entity.setPlanContent(objectMapper.writeValueAsString(plan));
            entity.setCompilerVersion("1.0.0");
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

    public Optional<ExecutionPlanEntity> findById(Long id) {
        return planRepository.findById(id);
    }

    public Optional<ExecutionPlanEntity> findByPlanId(String planId) {
        return planRepository.findByPlanId(planId);
    }

    public Optional<ExecutionPlanEntity> findByWorkflowIdAndVersion(String workflowId, String version) {
        return planRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    public Optional<ExecutionPlanEntity> findLatestVersion(String workflowId) {
        return planRepository.findFirstByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    public ExecutionPlan parsePlan(ExecutionPlanEntity entity) {
        try {
            return objectMapper.readValue(entity.getPlanContent(), ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse execution plan content. entityId={}", entity.getId(), e);
            throw new RuntimeException("Failed to parse execution plan: " + e.getMessage(), e);
        }
    }

    public List<ExecutionPlanEntity> findAllVersions(String workflowId) {
        return planRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting execution plan. id={}", id);
        planRepository.deleteById(id);
        log.info("Execution plan deleted successfully. id={}", id);
    }

    @Transactional
    public void deleteByWorkflowIdAndVersion(String workflowId, String version) {
        log.info("Deleting execution plan. workflowId={}, version={}", workflowId, version);

        findByWorkflowIdAndVersion(workflowId, version)
                .ifPresent(entity -> {
                    planRepository.deleteById(entity.getId());
                    log.info("Execution plan deleted successfully. workflowId={}, version={}", workflowId, version);
                });
    }
}
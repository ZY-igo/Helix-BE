package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowCompileDomainService;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.domain.workflow.WorkflowDslDomainService;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import com.sipc115.helix.repository.jpa.JpaExecutionPlanRepository;
import com.sipc115.helix.repository.jpa.JpaWorkflowDslRepository;
import com.sipc115.helix.utils.SnowflakeIdGenerator;
import com.sipc115.helix.utils.WorkflowVersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowApplicationService.class);

    private final JpaWorkflowDslRepository dslRepository;
    private final JpaExecutionPlanRepository planRepository;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final DslCompiler dslCompiler;

    public WorkflowApplicationService(
            JpaWorkflowDslRepository dslRepository,
            JpaExecutionPlanRepository planRepository,
            ObjectMapper objectMapper,
            SnowflakeIdGenerator idGenerator,
            DslCompiler dslCompiler) {
        this.dslRepository = dslRepository;
        this.planRepository = planRepository;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.dslCompiler = dslCompiler;
    }

    @Transactional
    public WorkflowDslEntity createWorkflow(String name, String createdBy) {
        log.info("Creating new workflow. name={}, createdBy={}", name, createdBy);

        WorkflowDsl dsl = WorkflowDslDomainService.createNew(name, createdBy);

        WorkflowDslEntity entity = toEntity(dsl, createdBy);
        WorkflowDslEntity saved = dslRepository.save(entity);

        log.info("Workflow created successfully. id={}, workflowId={}", saved.getId(), saved.getWorkflowId());
        return saved;
    }

    @Transactional
    public WorkflowDslEntity saveWorkflow(WorkflowDsl dsl, String updatedBy) {
        log.info("Saving workflow. workflowId={}, version={}", dsl.getWorkflowId(), dsl.getVersion());

        WorkflowDslEntity existing = dslRepository
                .findByWorkflowIdAndVersion(dsl.getWorkflowId(), dsl.getVersion())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + dsl.getWorkflowId() + " v" + dsl.getVersion()));

        existing.setDslContent(toJson(dsl));
        existing.setMetadata(toJson(dsl.getMetadata()));
        existing.setStatus("DRAFT");
        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedBy(updatedBy);

        WorkflowDslEntity saved = dslRepository.save(existing);

        log.info("Workflow saved successfully. id={}", saved.getId());
        return saved;
    }

    @Transactional
    public WorkflowDslEntity createCopy(String sourceWorkflowId, String sourceVersion,
                                        String newVersion, String createdBy) {
        log.info("Creating copy of workflow. source={} v{}, newVersion={}",
                sourceWorkflowId, sourceVersion, newVersion);

        WorkflowDslEntity source = dslRepository
                .findByWorkflowIdAndVersion(sourceWorkflowId, sourceVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source workflow not found: " + sourceWorkflowId + " v" + sourceVersion));

        WorkflowDsl sourceDsl = parseDsl(source);

        String targetVersion = newVersion != null ? newVersion : WorkflowVersionUtils.nextMinorVersion(sourceVersion);
        WorkflowDsl copiedDsl = WorkflowDslDomainService.copyAsNewVersion(sourceDsl, targetVersion);

        WorkflowDslEntity newEntity = toEntity(copiedDsl, createdBy);
        newEntity.setWorkflowId(sourceWorkflowId);

        WorkflowDslEntity saved = dslRepository.save(newEntity);

        log.info("Workflow copy created successfully. id={}, version={}", saved.getId(), saved.getVersion());
        return saved;
    }

    @Transactional
    public WorkflowDslEntity createCopyAsNewWorkflow(String sourceWorkflowId, String sourceVersion,
                                                     String newWorkflowId, String newVersion,
                                                     String createdBy) {
        log.info("Creating copy as new workflow. source={} v{}, newWorkflowId={}, newVersion={}",
                sourceWorkflowId, sourceVersion, newWorkflowId, newVersion);

        WorkflowDslEntity source = dslRepository
                .findByWorkflowIdAndVersion(sourceWorkflowId, sourceVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source workflow not found: " + sourceWorkflowId + " v" + sourceVersion));

        WorkflowDsl sourceDsl = parseDsl(source);

        String targetVersion = newVersion != null ? newVersion : WorkflowVersionUtils.initialVersion();
        WorkflowDsl copiedDsl = WorkflowDslDomainService.copyAsNewWorkflow(sourceDsl, newWorkflowId, targetVersion);

        WorkflowDslEntity newEntity = toEntity(copiedDsl, createdBy);

        WorkflowDslEntity saved = dslRepository.save(newEntity);

        log.info("Workflow copy created successfully. id={}, newWorkflowId={}, version={}",
                saved.getId(), newWorkflowId, targetVersion);
        return saved;
    }

    public WorkflowDslEntity getWorkflow(String workflowId, String version) {
        return dslRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + workflowId + " v" + version));
    }

    public WorkflowDslEntity getLatestPublishedWorkflow(String workflowId) {
        return dslRepository.findFirstByWorkflowIdAndStatusOrderByCreatedAtDesc(workflowId, "PUBLISHED")
                .orElseThrow(() -> new IllegalArgumentException(
                        "No published workflow found: " + workflowId));
    }

    public List<WorkflowDslEntity> getAllVersions(String workflowId) {
        return dslRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    @Transactional
    public WorkflowDslEntity publish(String workflowId, String version, String updatedBy) {
        log.info("Publishing workflow. workflowId={}, version={}", workflowId, version);

        WorkflowDslEntity entity = dslRepository
                .findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + workflowId + " v" + version));

        dslRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId).stream()
                .filter(e -> "PUBLISHED".equals(e.getStatus()))
                .forEach(e -> {
                    e.setStatus("DEPRECATED");
                    e.setUpdatedAt(Instant.now());
                    e.setUpdatedBy(updatedBy);
                    dslRepository.save(e);
                });

        entity.setStatus("PUBLISHED");
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(updatedBy);

        WorkflowDslEntity saved = dslRepository.save(entity);
        log.info("Workflow published successfully. id={}", saved.getId());
        return saved;
    }


    @Transactional
    public ExecutionPlanEntity saveAndCompile(WorkflowDsl dsl, String compiledBy) {
        log.info("Saving and compiling workflow. workflowId={}, version={}",
                dsl.getWorkflowId(), dsl.getVersion());

        WorkflowDslEntity entity = saveWorkflow(dsl, compiledBy);

        WorkflowCompileDomainService compileService = new WorkflowCompileDomainService(dslCompiler);
        ExecutionPlan plan = compileService.compile(dsl);

        String planId = String.valueOf(idGenerator.nextId());
        plan.setPlanId(planId);

        ExecutionPlanEntity planEntity = new ExecutionPlanEntity();
        planEntity.setPlanId(planId);
        planEntity.setWorkflowId(dsl.getWorkflowId());
        planEntity.setVersion(dsl.getVersion());
        planEntity.setPlanContent(toJson(plan));
        planEntity.setCompilerVersion("2.0.0");
        planEntity.setCompiledAt(Instant.now());
        planEntity.setCreatedAt(Instant.now());
        planEntity.setCreatedBy(compiledBy);

        ExecutionPlanEntity saved = planRepository.save(planEntity);
        log.info("Execution plan saved. planId={}", planId);
        return saved;
    }

    public ExecutionPlan getExecutionPlan(String workflowId, String version) {
        ExecutionPlanEntity entity = planRepository
                .findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Execution plan not found: " + workflowId + " v" + version));

        return parsePlan(entity);
    }

    private WorkflowDslEntity toEntity(WorkflowDsl dsl, String createdBy) {
        WorkflowDslEntity entity = new WorkflowDslEntity();
        entity.setWorkflowId(dsl.getWorkflowId());
        entity.setVersion(dsl.getVersion());
        entity.setDslContent(toJson(dsl));
        entity.setMetadata(toJson(dsl.getMetadata()));
        entity.setStatus("DRAFT");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        return entity;
    }

    private WorkflowDsl parseDsl(WorkflowDslEntity entity) {
        try {
            return objectMapper.readValue(entity.getDslContent(), WorkflowDsl.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse DSL", e);
        }
    }

    private ExecutionPlan parsePlan(ExecutionPlanEntity entity) {
        try {
            return objectMapper.readValue(entity.getPlanContent(), ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse ExecutionPlan", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}

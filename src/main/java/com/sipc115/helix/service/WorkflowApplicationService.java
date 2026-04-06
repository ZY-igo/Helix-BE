package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.entity.ExecutionPlanEntity;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.workflow.engine.DslRuntimeWorkflow;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import com.sipc115.helix.repository.jpa.JpaExecutionPlanRepository;
import com.sipc115.helix.repository.jpa.JpaWorkflowDslRepository;
import com.sipc115.helix.utils.SnowflakeIdGenerator;
import com.sipc115.helix.utils.WorkflowVersionUtils;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.sipc115.helix.integration.workflow.service.WorkflowExecutionApplicationService.TASK_QUEUE;

/**
 * 工作流应用服务层
 *
 * <p>负责工作流 DSL 和执行计划（Execution Plan）的生命周期管理，包括创建、版本控制、发布以及编译。</p>
 *
 * @author Helix Team
 * @since 2.0.0
 */
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

    /**
     * 创建一个新的工作流定义。
     *
     * @param name      工作流显示名称
     * @param createdBy 创建人标识
     * @return 保存后的工作流实体
     */
    @Transactional
    public WorkflowDslEntity createWorkflow(String name, String createdBy) {
        log.info("Creating new workflow. name={}, createdBy={}", name, createdBy);

        // 调用领域服务生成初始的 DSL 对象
        WorkflowDsl dsl = WorkflowDslDomainService.createNew(name, createdBy);

        // 转换为持久化实体并保存
        WorkflowDslEntity entity = toEntity(dsl, createdBy);
        WorkflowDslEntity saved = dslRepository.save(entity);

        log.info("Workflow created successfully. id={}, workflowId={}", saved.getId(), saved.getWorkflowId());
        return saved;
    }

    /**
     * 保存工作流的修改内容。
     *
     * @param dsl       更新后的工作流 DSL 对象
     * @param updatedBy 更新人标识
     * @return 保存后的工作流实体
     */
    @Transactional
    public WorkflowDslEntity saveWorkflow(WorkflowDsl dsl, String updatedBy) {
        log.info("Saving workflow. workflowId={}, version={}", dsl.getWorkflowId(), dsl.getVersion());

        // 确保数据库中已存在该版本的工作流
        WorkflowDslEntity existing = dslRepository
                .findByWorkflowIdAndVersion(dsl.getWorkflowId(), dsl.getVersion())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + dsl.getWorkflowId() + " v" + dsl.getVersion()));

        // 更新 JSON 内容和元数据，并将状态重置为草稿
        existing.setDslContent(toJson(dsl));
        existing.setMetadata(toJson(dsl.getMetadata()));
        existing.setStatus("DRAFT");
        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedBy(updatedBy);

        WorkflowDslEntity saved = dslRepository.save(existing);

        log.info("Workflow saved successfully. id={}", saved.getId());
        return saved;
    }

    /**
     * 基于现有工作流创建一个新版本（复制）。
     *
     * @param sourceWorkflowId 源工作流 ID
     * @param sourceVersion    源版本号
     * @param newVersion       新目标版本号（若为空则自动递增）
     * @param createdBy        创建人标识
     * @return 新版本的工作流实体
     */
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

        // 确定目标版本号并执行领域层的复制逻辑
        String targetVersion = newVersion != null ? newVersion : WorkflowVersionUtils.nextMinorVersion(sourceVersion);
        WorkflowDsl copiedDsl = WorkflowDslDomainService.copyAsNewVersion(sourceDsl, targetVersion);

        WorkflowDslEntity newEntity = toEntity(copiedDsl, createdBy);
        newEntity.setWorkflowId(sourceWorkflowId);

        WorkflowDslEntity saved = dslRepository.save(newEntity);

        log.info("Workflow copy created successfully. id={}, version={}", saved.getId(), saved.getVersion());
        return saved;
    }

    /**
     * 将现有工作流复制为一个全新的工作流 ID。
     *
     * @param sourceWorkflowId 源工作流 ID
     * @param sourceVersion    源版本号
     * @param newWorkflowId    新工作流 ID
     * @param newVersion       新初始版本号
     * @param createdBy        创建人标识
     * @return 新工作流实体
     */
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

    /**
     * 获取指定版本的工作流详情。
     */
    public WorkflowDslEntity getWorkflow(String workflowId, String version) {
        return dslRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + workflowId + " v" + version));
    }

    /**
     * 获取指定工作流最新已发布的版本。
     */
    public WorkflowDslEntity getLatestPublishedWorkflow(String workflowId) {
        return dslRepository.findFirstByWorkflowIdAndStatusOrderByCreatedAtDesc(workflowId, "PUBLISHED")
                .orElseThrow(() -> new IllegalArgumentException(
                        "No published workflow found: " + workflowId));
    }

    /**
     * 获取指定工作流的所有历史版本。
     */
    public List<WorkflowDslEntity> getAllVersions(String workflowId) {
        return dslRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    /**
     * 发布工作流。
     *
     * <p>发布时会将该工作流下其他已发布的版本标记为“DEPRECATED”（废弃），确保同一时间只有一个生效版本。</p>
     *
     * @param workflowId 工作流 ID
     * @param version    待发布的版本号
     * @param updatedBy  操作人标识
     * @return 发布后的工作流实体
     */
    @Transactional
    public WorkflowDslEntity publish(String workflowId, String version, String updatedBy) {
        log.info("Publishing workflow. workflowId={}, version={}", workflowId, version);

        WorkflowDslEntity entity = dslRepository
                .findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow not found: " + workflowId + " v" + version));

        // 废弃旧的已发布版本
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


    /**
     * 保存工作流并立即触发编译，生成可执行的 ExecutionPlan。
     *
     * @param dsl        工作流 DSL 对象
     * @param compiledBy 编译人标识
     * @return 生成的执行计划实体
     */
    @Transactional
    public ExecutionPlanEntity saveAndCompile(WorkflowDsl dsl, String compiledBy) {
        log.info("Saving and compiling workflow. workflowId={}, version={}",
                dsl.getWorkflowId(), dsl.getVersion());

        // 先持久化 DSL 定义
        WorkflowDslEntity entity = saveWorkflow(dsl, compiledBy);

        // 调用领域服务进行编译，将 DSL 转换为扁平化的执行计划
        WorkflowCompileDomainService compileService = new WorkflowCompileDomainService(dslCompiler);
        ExecutionPlan plan = compileService.compile(dsl);

        // 使用雪花算法生成唯一的计划 ID
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

    /**
     * 获取指定版本的执行计划。
     */
    public ExecutionPlan getExecutionPlan(String workflowId, String version) {
        ExecutionPlanEntity entity = planRepository
                .findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Execution plan not found: " + workflowId + " v" + version));

        return parsePlan(entity);
    }

    /**
     * 将领域对象转换为数据库实体。
     */
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

    /**
     * 将数据库实体解析为 DSL 领域对象。
     */
    private WorkflowDsl parseDsl(WorkflowDslEntity entity) {
        try {
            return objectMapper.readValue(entity.getDslContent(), WorkflowDsl.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse DSL", e);
        }
    }

    /**
     * 将数据库实体解析为执行计划对象。
     */
    private ExecutionPlan parsePlan(ExecutionPlanEntity entity) {
        try {
            return objectMapper.readValue(entity.getPlanContent(), ExecutionPlan.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse ExecutionPlan", e);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}

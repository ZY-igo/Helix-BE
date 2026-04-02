/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.entity.WorkflowDslEntity;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import com.sipc115.helix.repository.jpa.JpaWorkflowDslRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 工作流 DSL 服务类
 * <p>
 * 负责工作流 DSL 定义的增删改查和版本管理
 * </p>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class WorkflowDslService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(WorkflowDslService.class);

    /**
     * 工作流 DSL 仓库
     */
    private final JpaWorkflowDslRepository dslRepository;

    /**
     * DSL 编译器
     */
    private final DslCompiler dslCompiler;

    /**
     * Jackson 对象映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param dslRepository 工作流 DSL 仓库
     * @param dslCompiler DSL 编译器
     * @param objectMapper Jackson 对象映射器
     */
    public WorkflowDslService(
            JpaWorkflowDslRepository dslRepository,
            DslCompiler dslCompiler,
            ObjectMapper objectMapper) {
        this.dslRepository = dslRepository;
        this.dslCompiler = dslCompiler;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存工作流 DSL（草稿）
     * <p>
     * 如果是新的 workflowId，会创建新版本（version=1）。
     * 如果已存在，会在最高版本基础上 +1。
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param name 工作流名称
     * @param dsl 工作流 DSL 对象
     * @param createdBy 创建人
     * @return 保存后的实体
     */
    @Transactional
    public WorkflowDslEntity saveDraft(String workflowId, String name, WorkflowDsl dsl, String createdBy) {
        log.info("Saving workflow DSL draft. workflowId={}, name={}", workflowId, name);

        try {
            // 设置 DSL 基本信息
            dsl.setWorkflowId(workflowId);
            dsl.setName(name);

            // 计算新版本号
            Integer nextVersion = calculateNextVersion(workflowId);
            dsl.setVersion(nextVersion);

            // 创建实体
            WorkflowDslEntity entity = new WorkflowDslEntity();
            entity.setWorkflowId(workflowId);
            entity.setVersion(nextVersion);
            entity.setDslContent(objectMapper.writeValueAsString(dsl));
            entity.setStatus("DRAFT"); // 草稿状态
            entity.setMetadata(objectMapper.writeValueAsString(dsl.getMetadata()));
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            entity.setCreatedBy(createdBy);
            entity.setUpdatedBy(createdBy);

            WorkflowDslEntity saved = dslRepository.save(entity);
            log.info("Workflow DSL draft saved successfully. id={}, version={}", saved.getId(), saved.getVersion());

            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DSL to JSON. workflowId={}", workflowId, e);
            throw new RuntimeException("Failed to serialize DSL: " + e.getMessage(), e);
        }
    }

    /**
     * 发布工作流 DSL
     * <p>
     * 将草稿状态的 DSL 标记为已发布
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @param updatedBy 更新人
     * @return 发布后的实体
     */
    @Transactional
    public WorkflowDslEntity publish(String workflowId, Integer version, String updatedBy) {
        log.info("Publishing workflow DSL. workflowId={}, version={}", workflowId, version);

        WorkflowDslEntity entity = dslRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow DSL not found: workflowId=" + workflowId + ", version=" + version));

        entity.setStatus("PUBLISHED");
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(updatedBy);

        WorkflowDslEntity published = dslRepository.save(entity);
        log.info("Workflow DSL published successfully. id={}, status={}", published.getId(), published.getStatus());

        return published;
    }

    /**
     * 下线工作流 DSL
     * <p>
     * 将已发布的 DSL 标记为已下线
     * </p>
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @param updatedBy 更新人
     * @return 下线后的实体
     */
    @Transactional
    public WorkflowDslEntity deprecate(String workflowId, Integer version, String updatedBy) {
        log.info("Deprecating workflow DSL. workflowId={}, version={}", workflowId, version);

        WorkflowDslEntity entity = dslRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow DSL not found: workflowId=" + workflowId + ", version=" + version));

        entity.setStatus("DEPRECATED");
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(updatedBy);

        WorkflowDslEntity deprecated = dslRepository.save(entity);
        log.info("Workflow DSL deprecated successfully. id={}, status={}", deprecated.getId(), deprecated.getStatus());

        return deprecated;
    }

    /**
     * 根据 ID 查询工作流 DSL
     *
     * @param id 主键 ID
     * @return 工作流 DSL 实体，不存在返回 Optional.empty()
     */
    public Optional<WorkflowDslEntity> findById(Long id) {
        return dslRepository.findById(id);
    }

    /**
     * 根据工作流 ID 和版本查询
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 工作流 DSL 实体，不存在返回 Optional.empty()
     */
    public Optional<WorkflowDslEntity> findByWorkflowIdAndVersion(String workflowId, Integer version) {
        return dslRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    /**
     * 解析 DSL 内容
     * <p>
     * 将存储的 JSON 字符串转换为 WorkflowDsl 对象
     * </p>
     *
     * @param entity DSL 实体
     * @return WorkflowDsl 对象
     */
    public WorkflowDsl parseDsl(WorkflowDslEntity entity) {
        try {
            return objectMapper.readValue(entity.getDslContent(), WorkflowDsl.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DSL content. entityId={}", entity.getId(), e);
            throw new RuntimeException("Failed to parse DSL: " + e.getMessage(), e);
        }
    }

    /**
     * 查询工作流的所有版本
     *
     * @param workflowId 工作流 ID
     * @return 该工作流的所有版本列表
     */
    public List<WorkflowDslEntity> findAllVersions(String workflowId) {
        // TODO: 需要在 Repository 中添加自定义查询方法
        return dslRepository.findAll();
    }

    /**
     * 删除工作流 DSL
     *
     * @param id 主键 ID
     */
    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting workflow DSL. id={}", id);
        dslRepository.deleteById(id);
        log.info("Workflow DSL deleted successfully. id={}", id);
    }

    /**
     * 计算下一个版本号
     */
    private Integer calculateNextVersion(String workflowId) {
        // 简单实现：固定从 1 开始
        // TODO: 实际应该查询当前最大版本号 +1
        return 1;
    }
}

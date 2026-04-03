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
 * 工作流 DSL 服务
 * <p>
 * 负责工作流 DSL 的完整生命周期管理，包括：
 * <ul>
 *   <li>保存草稿（Save Draft）</li>
 *   <li>发布（Publish）</li>
 *   <li>废弃（Deprecate）</li>
 *   <li>回滚（Rollback）</li>
 *   <li>查询（Query）</li>
 * </ul>
 *
 * <p>工作流状态流转：
 * <pre>
 * DRAFT → PUBLISHED → DEPRECATED
 *           ↑              ↑
 *           └────ROLLBACK──┘
 * </pre>
 *
 * <p>状态说明：
 * <ul>
 *   <li>DRAFT：草稿状态，还未发布，可以继续修改</li>
 *   <li>PUBLISHED：已发布状态，用于生产环境</li>
 *   <li>DEPRECATED：已废弃状态，不再使用</li>
 * </ul>
 *
 * <p>版本管理策略：
 * <ul>
 *   <li>每次保存草稿都会自动分配新版本号</li>
 *   <li>发布时自动将旧发布版本标记为 DEPRECATED</li>
 *   <li>回滚可以将 DEPRECATED 版本重新设为 PUBLISHED</li>
 *   <li>Temporal 的工作流定义不可变，所以修改必须发布新版本</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowDsl 工作流 DSL 定义
 * @see WorkflowDslEntity 工作流 DSL 实体
 */
@Service
public class WorkflowDslService {

    /**
     * 日志记录器
     * <p>
     * 用于记录 DSL 服务相关的日志信息。
     */
    private static final Logger log = LoggerFactory.getLogger(WorkflowDslService.class);

    /**
     * 工作流 DSL JPA 仓储
     * <p>
     * 负责工作流 DSL 实体的数据库持久化操作。
     * 提供 CRUD 操作方法。
     */
    private final JpaWorkflowDslRepository dslRepository;

    /**
     * DSL 编译器
     * <p>
     * 用于将 DSL 编译为可执行的 ExecutionPlan。
     * 当前服务中未直接使用，但保留用于未来扩展。
     */
    private final DslCompiler dslCompiler;

    /**
     * JSON 对象映射器
     * <p>
     * 用于：
     * <ul>
     *   <li>将 WorkflowDsl 对象序列化为 JSON 字符串</li>
     *   <li>将 JSON 字符串反序列化为 WorkflowDsl 对象</li>
     * </ul>
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所有必需组件。
     *
     * @param dslRepository 工作流 DSL 仓储
     * @param dslCompiler DSL 编译器
     * @param objectMapper JSON 映射器
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
     * 保存工作流 DSL 草稿
     * <p>
     * 创建一个新的草稿版本工作流。
     * 系统会自动分配新的版本号。
     *
     * <p>版本号分配规则：
     * <ul>
     *   <li>首次创建：v1.0.0</li>
     *   <li>后续创建：自动递增 minor 版本，如 v1.1.0, v1.2.0</li>
     * </ul>
     *
     * <p>执行流程：
     * <ol>
     *   <li>设置 workflowId 和 name</li>
     *   <li>计算下一个版本号</li>
     *   <li>创建实体并设置初始状态为 DRAFT</li>
     *   <li>序列化为 JSON 并保存到数据库</li>
     * </ol>
     *
     * <p>注意：
     * 草稿状态的工作流不会自动执行，也不会被调度服务扫描到。
     *
     * @param workflowId 工作流 ID
     * @param name 工作流名称
     * @param dsl 工作流 DSL 对象
     * @param createdBy 创建人
     * @return 保存后的工作流 DSL 实体
     * @throws RuntimeException 如果 JSON 序列化失败
     */
    @Transactional
    public WorkflowDslEntity saveDraft(String workflowId, String name, WorkflowDsl dsl, String createdBy) {
        log.info("保存工作流 DSL 草稿: workflowId={}, name={}", workflowId, name);

        try {
            // 设置工作流基本信息
            dsl.setWorkflowId(workflowId);
            dsl.setName(name);

            // 计算并设置新版本号
            String nextVersion = calculateNextVersion(workflowId);
            dsl.setVersion(nextVersion);

            // 创建实体对象
            WorkflowDslEntity entity = new WorkflowDslEntity();
            entity.setWorkflowId(workflowId);
            entity.setVersion(nextVersion);

            // 序列化 DSL 为 JSON 字符串存储
            entity.setDslContent(objectMapper.writeValueAsString(dsl));

            // 设置初始状态为草稿
            entity.setStatus("DRAFT");

            // 序列化元数据
            entity.setMetadata(objectMapper.writeValueAsString(dsl.getMetadata()));

            // 设置时间戳和审计信息
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            entity.setCreatedBy(createdBy);
            entity.setUpdatedBy(createdBy);

            // 保存到数据库
            WorkflowDslEntity saved = dslRepository.save(entity);
            log.info("工作流 DSL 草稿保存成功: id={}, version={}", saved.getId(), saved.getVersion());

            return saved;

        } catch (JsonProcessingException e) {
            log.error("DSL JSON 序列化失败: workflowId={}", workflowId, e);
            throw new RuntimeException("DSL 序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发布工作流 DSL
     * <p>
     * 将草稿状态的工作流发布为正式版本。
     * 发布后工作流可以被调度服务扫描到并自动执行。
     *
     * <p>发布影响：
     * <ol>
     *   <li>将目标版本状态从 DRAFT 改为 PUBLISHED</li>
     *   <li>自动将同一工作流的其他已发布版本标记为 DEPRECATED</li>
     *   <li>更新发布时间和更新人</li>
     * </ol>
     *
     * <p>重要说明：
     * <ul>
     *   <li>发布不会影响已运行中的旧版本工作流实例</li>
     *   <li>Temporal 的工作流定义不可变，所以新启动的实例会使用新版本</li>
     *   <li>旧版本实例会继续使用启动时的版本逻辑执行完成</li>
     * </ul>
     *
     * @param workflowId 工作流 ID
     * @param version 要发布的版本号
     * @param updatedBy 更新人
     * @return 发布后的工作流 DSL 实体
     * @throws IllegalArgumentException 如果工作流不存在
     */
    @Transactional
    public WorkflowDslEntity publish(String workflowId, String version, String updatedBy) {
        log.info("发布工作流 DSL: workflowId={}, version={}", workflowId, version);

        // 查找要发布的工作流
        WorkflowDslEntity entity = dslRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "工作流 DSL 不存在: workflowId=" + workflowId + ", version=" + version));

        // 废弃旧版本（同一工作流的已发布版本）
        deprecateOldPublishedVersions(workflowId, updatedBy);

        // 设置新版本状态为已发布
        entity.setStatus("PUBLISHED");
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(updatedBy);

        // 保存更改
        WorkflowDslEntity published = dslRepository.save(entity);
        log.info("工作流 DSL 发布成功: id={}, status={}", published.getId(), published.getStatus());

        return published;
    }

    /**
     * 废弃旧版本
     * <p>
     * 将同一工作流的所有已发布版本标记为 DEPRECATED。
     * 这个方法在发布新版本时自动调用。
     *
     * <p>执行逻辑：
     * <ol>
     *   <li>查询该工作流的所有已发布版本</li>
     *   <li>遍历并设置状态为 DEPRECATED</li>
     *   <li>更新审计字段</li>
     * </ol>
     *
     * @param workflowId 工作流 ID
     * @param updatedBy 更新人
     */
    private void deprecateOldPublishedVersions(String workflowId, String updatedBy) {
        // 查询该工作流的所有版本
        List<WorkflowDslEntity> publishedVersions = dslRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId).stream()
                // 筛选出已发布状态
                .filter(e -> "PUBLISHED".equals(e.getStatus()))
                .toList();

        // 遍历并废弃
        for (WorkflowDslEntity old : publishedVersions) {
            old.setStatus("DEPRECATED");
            old.setUpdatedAt(Instant.now());
            old.setUpdatedBy(updatedBy);
            dslRepository.save(old);

            log.info("已废弃旧版本: workflowId={}, oldVersion={}", workflowId, old.getVersion());
        }
    }

    /**
     * 回滚工作流 DSL
     * <p>
     * 将当前已发布版本回滚到指定的历史版本。
     * 回滚后，新指定的版本会变为 PUBLISHED 状态。
     *
     * <p>回滚影响：
     * <ol>
     *   <li>将当前 PUBLISHED 版本标记为 DEPRECATED</li>
     *   <li>将目标版本标记为 PUBLISHED</li>
     * </ol>
     *
     * <p>使用场景：
     * <ul>
     *   <li>新版本发现严重 bug，需要退回旧版本</li>
     *   <li>临时需要使用旧版本功能</li>
     * </ul>
     *
     * <p>注意：
     * 回滚不会影响已经在 Temporal 中运行的工作流实例。
     *
     * @param workflowId 工作流 ID
     * @param targetVersion 目标版本号（要回滚到的版本）
     * @param updatedBy 更新人
     * @return 回滚后的工作流 DSL 实体
     * @throws IllegalStateException 如果没有可回滚的版本
     * @throws IllegalArgumentException 如果目标版本不存在
     */
    @Transactional
    public WorkflowDslEntity rollback(String workflowId, String targetVersion, String updatedBy) {
        log.info("回滚工作流 DSL: workflowId={}, targetVersion={}", workflowId, targetVersion);

        // 查找当前已发布的版本
        WorkflowDslEntity currentPublished = dslRepository
                .findFirstByWorkflowIdAndStatusOrderByCreatedAtDesc(workflowId, "PUBLISHED")
                .orElseThrow(() -> new IllegalStateException("没有可回滚的已发布版本"));

        // 查找目标版本
        WorkflowDslEntity targetEntity = dslRepository.findByWorkflowIdAndVersion(workflowId, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "目标版本不存在: workflowId=" + workflowId + ", version=" + targetVersion));

        // 将当前版本标记为废弃
        currentPublished.setStatus("DEPRECATED");
        currentPublished.setUpdatedAt(Instant.now());
        currentPublished.setUpdatedBy(updatedBy);
        dslRepository.save(currentPublished);

        // 将目标版本设为已发布
        targetEntity.setStatus("PUBLISHED");
        targetEntity.setUpdatedAt(Instant.now());
        targetEntity.setUpdatedBy(updatedBy);
        WorkflowDslEntity rolledBack = dslRepository.save(targetEntity);

        log.info("工作流 DSL 回滚成功: workflowId={}, rolledBackVersion={}",
                workflowId, targetVersion);

        return rolledBack;
    }

    /**
     * 废弃工作流 DSL
     * <p>
     * 将指定版本的工作流标记为已废弃。
     * 废弃后该版本不能再被发布或使用。
     *
     * <p>与回滚的区别：
     * <ul>
     *   <li>废弃（Deprecate）：单方面停止使用某个版本</li>
     *   <li>回滚（Rollback）：废弃当前版本并激活历史版本</li>
     * </ul>
     *
     * @param workflowId 工作流 ID
     * @param version 要废弃的版本号
     * @param updatedBy 更新人
     * @return 废弃后的工作流 DSL 实体
     * @throws IllegalArgumentException 如果工作流不存在
     */
    @Transactional
    public WorkflowDslEntity deprecate(String workflowId, String version, String updatedBy) {
        log.info("废弃工作流 DSL: workflowId={}, version={}", workflowId, version);

        // 查找要废弃的工作流
        WorkflowDslEntity entity = dslRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "工作流 DSL 不存在: workflowId=" + workflowId + ", version=" + version));

        // 设置状态为废弃
        entity.setStatus("DEPRECATED");
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(updatedBy);

        // 保存更改
        WorkflowDslEntity deprecated = dslRepository.save(entity);
        log.info("工作流 DSL 废弃成功: id={}, status={}", deprecated.getId(), deprecated.getStatus());

        return deprecated;
    }

    /**
     * 根据 ID 查询工作流 DSL
     *
     * @param id 工作流 DSL 实体 ID
     * @return 包含工作流 DSL 实体的 Optional
     */
    public Optional<WorkflowDslEntity> findById(Long id) {
        return dslRepository.findById(id);
    }

    /**
     * 根据工作流 ID 和版本查询
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 包含工作流 DSL 实体的 Optional
     */
    public Optional<WorkflowDslEntity> findByWorkflowIdAndVersion(String workflowId, String version) {
        return dslRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    /**
     * 查询工作流最新发布的版本
     * <p>
     * 用于获取当前正在使用的正式版本。
     *
     * @param workflowId 工作流 ID
     * @return 包含最新发布版本的 Optional，如果没有则为空
     */
    public Optional<WorkflowDslEntity> findLatestPublished(String workflowId) {
        return dslRepository.findFirstByWorkflowIdAndStatusOrderByCreatedAtDesc(workflowId, "PUBLISHED");
    }

    /**
     * 解析 DSL 内容
     * <p>
     * 将存储的 JSON 字符串反序列化为 WorkflowDsl 对象。
     *
     * @param entity 工作流 DSL 实体
     * @return 反序列化后的 WorkflowDsl 对象
     * @throws RuntimeException 如果 JSON 反序列化失败
     */
    public WorkflowDsl parseDsl(WorkflowDslEntity entity) {
        try {
            return objectMapper.readValue(entity.getDslContent(), WorkflowDsl.class);
        } catch (JsonProcessingException e) {
            log.error("DSL 内容解析失败: entityId={}", entity.getId(), e);
            throw new RuntimeException("DSL 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询工作流所有版本
     * <p>
     * 返回该工作流的所有版本，按创建时间倒序排列。
     *
     * @param workflowId 工作流 ID
     * @return 所有版本的工作流 DSL 实体列表
     */
    public List<WorkflowDslEntity> findAllVersions(String workflowId) {
        return dslRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    /**
     * 删除工作流 DSL
     *
     * @param id 工作流 DSL 实体 ID
     */
    @Transactional
    public void deleteById(Long id) {
        log.info("删除工作流 DSL: id={}", id);
        dslRepository.deleteById(id);
        log.info("工作流 DSL 删除成功: id={}", id);
    }

    /**
     * 计算下一个版本号
     * <p>
     * 根据已存在的版本自动计算下一个版本号。
     *
     * <p>版本递增规则：
     * <ul>
     *   <li>无版本时：v1.0.0</li>
     *   <li>有版本时：递增 minor 版本，如 v1.0.0 → v1.1.0</li>
     * </ul>
     *
     * @param workflowId 工作流 ID
     * @return 下一个版本号字符串
     */
    private String calculateNextVersion(String workflowId) {
        // 查询已存在的版本
        List<WorkflowDslEntity> existingVersions = dslRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId);

        // 如果没有版本，返回初始版本
        if (existingVersions.isEmpty()) {
            return "v1.0.0";
        }

        // 获取最新版本并递增
        String latestVersion = existingVersions.get(0).getVersion();
        return incrementVersion(latestVersion);
    }

    /**
     * 递增版本号
     * <p>
     * 将版本号的 minor 版本递增，major 版本保持不变。
     *
     * <p>示例：
     * <ul>
     *   <li>v1.0.0 → v1.1.0</li>
     *   <li>v2.3.0 → v2.4.0</li>
     *   <li>v1.0 → v1.1（兼容性处理）</li>
     * </ul>
     *
     * @param version 当前版本号
     * @return 递增后的版本号
     */
    private String incrementVersion(String version) {
        // 边界情况处理
        if (version == null || version.isEmpty()) {
            return "v1.0.0";
        }

        // 去除 v 前缀
        String cleanVersion = version.startsWith("v") ? version.substring(1) : version;

        // 按点分割版本号
        String[] parts = cleanVersion.split("\\.");

        // 如果至少有两位版本号，尝试递增 minor
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return "v" + major + "." + (minor + 1) + ".0";
            } catch (NumberFormatException e) {
                // 解析失败，追加 patch
                return version + "-patch1";
            }
        }

        // 无法解析，直接追加
        return version + ".1";
    }
}
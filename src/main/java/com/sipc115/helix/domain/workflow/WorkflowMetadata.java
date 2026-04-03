/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流元数据类
 * <p>
 * 定义工作流的元数据信息，包括权限范围、状态、描述、标签等。
 * 用于存储与工作流相关的附加信息，不影响工作流的执行逻辑。
 *
 * <p>元数据示例：
 * <pre>
 * {
 *   "scope": "PUBLIC",                    // 权限范围
 *   "state": "PUBLISHED",                 // 工作流状态
 *   "description": "每日报告生成工作流",    // 描述
 *   "owner": "data-team",                 // 负责人
 *   "tags": ["report", "daily"],          // 标签
 *   "createdBy": "admin",                 // 创建人
 *   "createdAt": "2024-01-01T00:00:00Z",  // 创建时间
 *   "updatedBy": "admin",                 // 更新人
 *   "updatedAt": "2024-01-01T00:00:00Z"   // 更新时间
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class WorkflowMetadata {

    /**
     * 权限范围
     * <p>
     * 定义工作流的访问权限范围，控制谁可以查看和使用此工作流。
     *
     * <p>可选值：
     * <ul>
     *   <li>PUBLIC - 公开，所有用户可见</li>
     *   <li>PRIVATE - 私有，仅创建者可见</li>
     *   <li>TEAM - 团队，指定团队成员可见</li>
     *   <li>ORGANIZATION - 组织，整个组织可见</li>
     * </ul>
     */
    private WorkflowScope scope = WorkflowScope.PRIVATE;

    /**
     * 工作流状态
     * <p>
     * 标识工作流的当前生命周期状态。
     *
     * <p>可选值：
     * <ul>
     *   <li>DRAFT - 草稿，开发中</li>
     *   <li>PUBLISHED - 已发布，可执行</li>
     *   <li>DEPRECATED - 已弃用，不再推荐使用</li>
     *   <li>ARCHIVED - 已归档，仅保留历史记录</li>
     * </ul>
     */
    private WorkflowState state = WorkflowState.DRAFT;

    /**
     * 工作流描述
     * <p>
     * 工作流的详细描述信息，用于说明工作流的用途和功能。
     */
    private String description;

    /**
     * 负责人
     * <p>
     * 工作流的责任人，负责工作流的维护和更新。
     */
    private String owner;

    /**
     * 标签列表
     * <p>
     * 用于对工作流进行分类和检索的标签。
     */
    private java.util.List<String> tags = new java.util.ArrayList<>();

    /**
     * 创建人
     * <p>
     * 创建工作流的用户标识。
     */
    private String createdBy;

    /**
     * 创建时间
     * <p>
     * 工作流的创建时间戳。
     */
    private Instant createdAt;

    /**
     * 更新人
     * <p>
     * 最后更新工作流的用户标识。
     */
    private String updatedBy;

    /**
     * 更新时间
     * <p>
     * 工作流的最后更新时间戳。
     */
    private Instant updatedAt;

    /**
     * 扩展属性
     * <p>
     * 用于存储额外的自定义元数据。
     * 可以存储任意键值对。
     */
    private Map<String, Object> extra = new HashMap<>();

    /**
     * 工作流权限范围枚举
     */
    public enum WorkflowScope {
        /**
         * 公开，所有用户可见
         */
        PUBLIC,

        /**
         * 私有，仅创建者可见
         */
        PRIVATE,

        /**
         * 团队，指定团队成员可见
         */
        TEAM,

        /**
         * 组织，整个组织可见
         */
        ORGANIZATION
    }

    /**
     * 工作流状态枚举
     */
    public enum WorkflowState {
        /**
         * 草稿，开发中
         */
        DRAFT,

        /**
         * 已发布，可执行
         */
        PUBLISHED,

        /**
         * 已弃用，不再推荐使用
         */
        DEPRECATED,

        /**
         * 已归档，仅保留历史记录
         */
        ARCHIVED
    }
}

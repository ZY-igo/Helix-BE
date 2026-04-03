/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流领域特定语言（DSL）定义
 * <p>
 * 工作流 DSL 是 Helix 系统中工作流的核心定义，描述了工作流的完整结构。
 * 它采用 JSON 格式存储，包含工作流的基本信息、节点定义、边定义、
 * 元数据以及调度配置。
 *
 * <p>DSL 结构示例：
 * <pre>
 * {
 *   "workflowId": "daily-report",          // 工作流唯一标识
 *   "name": "每日报告生成工作流",            // 工作流名称
 *   "version": "v1.0.0",                   // 工作流版本
 *   "schedule": {                          // 调度配置（可选）
 *     "cron": "0 8 * * *",
 *     "timezone": "Asia/Shanghai",
 *     "enabled": true
 *   },
 *   "nodes": [                             // 节点定义
 *     {
 *       "id": "start",
 *       "type": "START",
 *       "name": "开始",
 *       "category": "CONTROL_FLOW"
 *     },
 *     {
 *       "id": "task1",
 *       "type": "TASK",
 *       "name": "获取数据",
 *       "category": "DATA_PROCESSING",
 *       "policy": {
 *         "retryPolicy": {
 *           "maxAttempts": 3,
 *           "initialInterval": "1s"
 *         },
 *         "timeout": {
 *           "executionTimeout": "5m"
 *         }
 *       },
 *       "config": {...}
 *     }
 *   ],
 *   "edges": [                             // 边定义（节点之间的连接）
 *     {
 *       "from": "start",
 *       "to": "task1",
 *       "condition": null
 *     }
 *   ],
 *   "metadata": {                          // 元数据（可选）
 *     "scope": "PUBLIC",                   // 权限范围
 *     "state": "PUBLISHED",                // 工作流状态
 *     "description": "用于生成每日数据报告",
 *     "owner": "data-team",
 *     "tags": ["report", "daily"]
 *   }
 * }
 * </pre>
 *
 * <p>核心概念：
 * <ul>
 *   <li>节点（Node）：工作流中的基本执行单元，包含策略配置和分区信息</li>
 *   <li>边（Edge）：节点之间的连接，定义执行顺序和条件</li>
 *   <li>调度（Schedule）：工作流的自动触发规则</li>
 *   <li>元数据（Metadata）：附加信息，包括权限范围、状态等</li>
 * </ul>
 *
 * <p>版本管理：
 * <ul>
 *   <li>每个工作流可以有多个版本</li>
 *   <li>版本号采用语义化版本格式，如 "v1.0.0"、"v2.0-beta"</li>
 *   <li>发布新版本不会影响已运行中的旧版本实例</li>
 *   <li>旧版本可以标记为 DEPRECATED 状态</li>
 * </ul>
 *
 * <p>权限控制：
 * <ul>
 *   <li>通过 metadata.scope 控制访问权限（PUBLIC/PRIVATE/TEAM/ORGANIZATION）</li>
 *   <li>通过 metadata.state 控制生命周期状态（DRAFT/PUBLISHED/DEPRECATED/ARCHIVED）</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see ScheduleSpec 调度配置
 * @see DslNodeSpec 节点定义
 * @see DslEdgeSpec 边定义
 * @see WorkflowMetadata 工作流元数据
 * @see ExecutionPlan 编译后的执行计划
 */
@Data
public class WorkflowDsl {

    /**
     * 工作流唯一标识符
     * <p>
     * 用于在整个系统中唯一标识一个工作流。
     * 同一 workflowId 可以有多个版本。
     *
     * <p>命名规范：
     * <ul>
     *   <li>建议使用 kebab-case 或 camelCase</li>
     *   <li>应具有描述性，如 "daily-report"、"user-onboarding"</li>
     *   <li>在系统中应保持唯一</li>
     * </ul>
     */
    private String workflowId;

    /**
     * 工作流名称
     * <p>
     * 用于展示给用户的友好名称。
     * 与 workflowId 不同，name 可以是非英文的自然语言描述。
     *
     * <p>示例：
     * <ul>
     *   <li>"每日报告生成工作流"</li>
     *   <li>"用户入职处理流程"</li>
     *   <li>"订单处理流水线"</li>
     * </ul>
     */
    private String name;

    /**
     * 工作流版本号
     * <p>
     * 采用语义化版本格式，支持灵活的版本命名策略。
     *
     * <p>支持的版本格式：
     * <ul>
     *   <li>"v1.0.0" - 正式版本</li>
     *   <li>"v2.0-beta" - 测试版本</li>
     *   <li>"1.0.1" - 不带 v 前缀</li>
     *   <li>"v3.0.0-rc1" - 发布候选版本</li>
     * </ul>
     *
     * <p>版本递增规则（由系统自动计算）：
     * <ul>
     *   <li>首次创建：v1.0.0</li>
     *   <li>后续创建：自动递增 minor 版本</li>
     * </ul>
     *
     * <p>重要说明：
     * Temporal 的工作流定义是不可变的。
     * 修改工作流 DSL 后，需要发布新版本才能生效。
     */
    private String version;

    /**
     * 工作流节点列表
     * <p>
     * 定义工作流中的所有节点。
     * 节点是工作流的基本执行单元，包含策略配置和分区信息。
     *
     * <p>节点属性：
     * <ul>
     *   <li>id - 节点唯一标识</li>
     *   <li>type - 节点类型（START、END、TASK、CONDITION 等）</li>
     *   <li>name - 节点显示名称</li>
     *   <li>category - 节点分区（CONTROL_FLOW、AI_TASK、DATA_PROCESSING 等）</li>
     *   <li>policy - 节点策略配置（重试、超时、熔断、限流）</li>
     *   <li>config - 节点特定配置</li>
     * </ul>
     *
     * @see DslNodeSpec
     * @see NodeCategory
     * @see NodePolicyConfig
     */
    private List<DslNodeSpec> nodes = new ArrayList<>();

    /**
     * 工作流边列表
     * <p>
     * 定义节点之间的连接关系和有向执行流。
     * 边表示节点之间的执行顺序和数据流向。
     *
     * <p>边的属性：
     * <ul>
     *   <li>from - 起始节点 ID</li>
     *   <li>to - 目标节点 ID</li>
     *   <li>condition - 触发条件（可选，用于条件边）</li>
     * </ul>
     *
     * <p>约束：
     * <ul>
     *   <li>边连接的节点必须存在于 nodes 列表中</li>
     *   <li>工作流是有向无环图（DAG），不支持循环</li>
     *   <li>每个非 END 节点至少有一条出边</li>
     *   <li>每个非 START 节点至少有一条入边</li>
     * </ul>
     *
     * @see DslEdgeSpec
     */
    private List<DslEdgeSpec> edges = new ArrayList<>();

    /**
     * 工作流元数据
     * <p>
     * 存储与工作流相关的附加信息，包括权限范围、状态、描述、标签等。
     * 元数据不影响工作流的执行逻辑，但控制访问权限和生命周期状态。
     *
     * <p>核心属性：
     * <ul>
     *   <li>scope - 权限范围（PUBLIC/PRIVATE/TEAM/ORGANIZATION）</li>
     *   <li>state - 工作流状态（DRAFT/PUBLISHED/DEPRECATED/ARCHIVED）</li>
     *   <li>description - 工作流描述</li>
     *   <li>owner - 负责人</li>
     *   <li>tags - 标签列表</li>
     *   <li>createdBy/createdAt - 创建信息</li>
     *   <li>updatedBy/updatedAt - 更新信息</li>
     * </ul>
     *
     * @see WorkflowMetadata
     */
    private WorkflowMetadata metadata = new WorkflowMetadata();

    /**
     * 工作流调度配置
     * <p>
     * 定义工作流的自动调度规则。
     * 支持 Cron 表达式和固定间隔两种调度方式。
     *
     * <p>调度类型：
     * <ul>
     *   <li>Cron 调度：基于 Cron 表达式的定时调度</li>
     *   <li>间隔调度：基于固定时间间隔的轮询调度</li>
     * </ul>
     *
     * <p>配置示例（Cron）：
     * <pre>
     * "schedule": {
     *   "cron": "0 8 * * *",           // 每天 8:00 执行
     *   "timezone": "Asia/Shanghai",   // 使用中国时区
     *   "enabled": true                 // 启用调度
     * }
     * </pre>
     *
     * <p>配置示例（间隔）：
     * <pre>
     * "schedule": {
     *   "intervalMs": 3600000,         // 每小时执行一次
     *   "enabled": true
     * }
     * </pre>
     *
     * <p>说明：
     * 如果不配置 schedule 或 enabled 为 false，工作流不会自动执行，
     * 但可以通过 API 手动触发。
     *
     * @see ScheduleSpec
     */
    private ScheduleSpec schedule;

    /**
     * 获取工作流元数据
     * <p>
     * 如果 metadata 为 null，则创建一个新的默认元数据对象。
     *
     * @return 工作流元数据，不会返回 null
     */
    public WorkflowMetadata getMetadata() {
        if (metadata == null) {
            metadata = new WorkflowMetadata();
        }
        return metadata;
    }

    /**
     * 获取权限范围
     * <p>
     * 从元数据中获取权限范围，如果未设置则返回默认值 PRIVATE。
     *
     * @return 权限范围
     * @see WorkflowMetadata.WorkflowScope
     */
    public WorkflowMetadata.WorkflowScope getScope() {
        return getMetadata().getScope();
    }

    /**
     * 设置权限范围
     * <p>
     * 设置元数据中的权限范围。
     *
     * @param scope 权限范围
     * @see WorkflowMetadata.WorkflowScope
     */
    public void setScope(WorkflowMetadata.WorkflowScope scope) {
        getMetadata().setScope(scope);
    }

    /**
     * 获取工作流状态
     * <p>
     * 从元数据中获取工作流状态，如果未设置则返回默认值 DRAFT。
     *
     * @return 工作流状态
     * @see WorkflowMetadata.WorkflowState
     */
    public WorkflowMetadata.WorkflowState getState() {
        return getMetadata().getState();
    }

    /**
     * 设置工作流状态
     * <p>
     * 设置元数据中的工作流状态。
     *
     * @param state 工作流状态
     * @see WorkflowMetadata.WorkflowState
     */
    public void setState(WorkflowMetadata.WorkflowState state) {
        getMetadata().setState(state);
    }
}

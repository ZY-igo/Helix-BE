/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流 DSL 节点规范类
 * <p>
 * 用于定义工作流 DSL 中的节点（node）规范，表示工作流中的一个执行步骤。
 * 包含节点 ID、类型、名称、分区、策略配置和节点配置信息等，用于构建工作流的执行逻辑。
 *
 * <p>节点规范示例：
 * <pre>
 * {
 *   "id": "data-fetch",
 *   "type": "TASK",
 *   "name": "获取数据",
 *   "category": "DATA_PROCESSING",
 *   "policy": {
 *     "retryPolicy": {
 *       "maxAttempts": 3,
 *       "initialInterval": "1s"
 *     },
 *     "timeout": {
 *       "executionTimeout": "5m"
 *     }
 *   },
 *   "config": {
 *     "activityType": "DataFetchActivity",
 *     "parameters": {...}
 *   }
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see NodeCategory 节点分区
 * @see NodePolicyConfig 节点策略配置
 */
@Data
public class DslNodeSpec {

    /**
     * 节点 ID
     * <p>
     * 节点的唯一标识符，用于在工作流中引用此节点。
     * 在同一个工作流中必须唯一。
     *
     * <p>命名规范：
     * <ul>
     *   <li>建议使用 kebab-case 或 camelCase</li>
     *   <li>应具有描述性，如 "fetch-data", "validate-input"</li>
     *   <li>避免使用特殊字符</li>
     * </ul>
     */
    private String id;

    /**
     * 节点类型
     * <p>
     * 节点的类型，决定了节点的执行逻辑和行为。
     *
     * <p>支持的类型：
     * <ul>
     *   <li>START - 开始节点，每个工作流必须有且仅有一个</li>
     *   <li>END - 结束节点，可以有多个</li>
     *   <li>TASK - 任务节点，执行具体的 Activity</li>
     *   <li>CONDITION - 条件节点，用于条件分支</li>
     *   <li>FORK - 并行分支起始节点</li>
     *   <li>JOIN - 并行分支汇合节点</li>
     *   <li>AI_TASK - AI 任务节点</li>
     *   <li>HUMAN_INPUT - 人工输入节点</li>
     *   <li>FEISHU - 飞书通知节点</li>
     * </ul>
     *
     * @see DslNodeType
     */
    private DslNodeType type;

    /**
     * 节点名称
     * <p>
     * 节点的显示名称，用于在界面上展示节点。
     * 可以是自然语言描述，如 "获取用户数据"、"发送通知" 等。
     */
    private String name;

    /**
     * 节点分区
     * <p>
     * 用于对节点进行分类分区，便于管理和组织不同类型的节点。
     * 分区信息用于前端展示、权限控制和逻辑分组。
     *
     * <p>默认值为 UTILITY（工具节点）。
     *
     * <p>可选分区：
     * <ul>
     *   <li>CONTROL_FLOW - 控制流节点</li>
     *   <li>AI_TASK - AI 任务节点</li>
     *   <li>HUMAN_INTERACTION - 人机交互节点</li>
     *   <li>INTEGRATION - 系统集成节点</li>
     *   <li>DATA_PROCESSING - 数据处理节点</li>
     *   <li>NOTIFICATION - 消息通知节点</li>
     *   <li>UTILITY - 工具节点</li>
     *   <li>CUSTOM - 自定义节点</li>
     * </ul>
     *
     * @see NodeCategory
     */
    private NodeCategory category = NodeCategory.UTILITY;

    /**
     * 节点策略配置
     * <p>
     * 定义节点的执行策略，包括重试、超时、熔断、限流等配置。
     * 这些配置由 DSL 控制，由 Temporal 的 Activity 具体执行。
     *
     * <p>策略配置包括：
     * <ul>
     *   <li>retryPolicy - 重试策略（最大重试次数、退避间隔等）</li>
     *   <li>timeout - 超时配置（执行超时、调度超时等）</li>
     *   <li>circuitBreaker - 熔断器配置</li>
     *   <li>rateLimit - 限流配置</li>
     * </ul>
     *
     * @see NodePolicyConfig
     */
    private NodePolicyConfig policy = new NodePolicyConfig();

    /**
     * 节点配置
     * <p>
     * 节点的配置参数，以键值对形式存储。
     * 具体的配置项取决于节点类型。
     *
     * <p>示例配置（TASK 节点）：
     * <pre>
     * {
     *   "activityType": "DataFetchActivity",
     *   "parameters": {
     *     "source": "database",
     *     "table": "users"
     *   }
     * }
     * </pre>
     *
     * <p>示例配置（CONDITION 节点）：
     * <pre>
     * {
     *   "expression": "input.age >= 18",
     *   "trueBranch": "adult-process",
     *   "falseBranch": "minor-process"
     * }
     * </pre>
     */
    private Map<String, Object> config = new HashMap<>();

    /**
     * 获取节点策略配置
     * <p>
     * 如果 policy 为 null，则创建一个新的默认策略配置。
     *
     * @return 节点策略配置，不会返回 null
     */
    public NodePolicyConfig getPolicy() {
        if (policy == null) {
            policy = new NodePolicyConfig();
        }
        return policy;
    }

    /**
     * 获取节点分区
     * <p>
     * 如果 category 为 null，则返回默认分区 UTILITY。
     *
     * @return 节点分区，不会返回 null
     */
    public NodeCategory getCategory() {
        if (category == null) {
            category = NodeCategory.UTILITY;
        }
        return category;
    }
}

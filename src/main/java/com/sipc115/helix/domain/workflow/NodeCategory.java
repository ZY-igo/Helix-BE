/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

/**
 * 节点分区枚举类
 * <p>
 * 用于对 DSL 节点进行分类分区，便于管理和组织不同类型的节点。
 * 分区信息存储在 DSL 中，用于前端展示、权限控制和逻辑分组。
 *
 * <p>分区设计原则：
 * <ul>
 *   <li>CONTROL_FLOW - 控制流节点，控制工作流的执行流程</li>
 *   <li>AI_TASK - AI 任务节点，涉及 AI 模型调用和智能处理</li>
 *   <li>HUMAN_INTERACTION - 人机交互节点，需要人工介入的节点</li>
 *   <li>INTEGRATION - 集成节点，与外部系统集成的节点</li>
 *   <li>DATA_PROCESSING - 数据处理节点，数据转换和处理</li>
 *   <li>NOTIFICATION - 通知节点，消息通知和提醒</li>
 *   <li>UTILITY - 工具节点，通用工具类节点</li>
 *   <li>CUSTOM - 自定义节点，用户自定义的特殊节点</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
public enum NodeCategory {

    /**
     * 控制流节点
     * <p>
     * 控制工作流的执行流程，包括：
     * <ul>
     *   <li>START - 开始节点</li>
     *   <li>END - 结束节点</li>
     *   <li>CONDITION - 条件分支节点</li>
     *   <li>FORK - 并行分支起始节点</li>
     *   <li>JOIN - 并行分支汇合节点</li>
     *   <li>LOOP - 循环节点</li>
     * </ul>
     */
    CONTROL_FLOW("控制流", "Control the execution flow of workflow"),

    /**
     * AI 任务节点
     * <p>
     * 涉及 AI 模型调用和智能处理的节点，包括：
     * <ul>
     *   <li>AI_TASK - AI 任务执行</li>
     *   <li>GENERATE - 内容生成</li>
     *   <li>VALIDATE - AI 验证</li>
     *   <li>REPAIR - 自动修复</li>
     * </ul>
     */
    AI_TASK("AI任务", "AI model invocation and intelligent processing"),

    /**
     * 人机交互节点
     * <p>
     * 需要人工介入的节点，包括：
     * <ul>
     *   <li>HUMAN_INPUT - 人工输入</li>
     *   <li>HUMAN_APPROVAL - 人工审批</li>
     *   <li>HUMAN_TASK - 人工任务</li>
     * </ul>
     */
    HUMAN_INTERACTION("人机交互", "Require human intervention"),

    /**
     * 集成节点
     * <p>
     * 与外部系统集成的节点，包括：
     * <ul>
     *   <li>FEISHU - 飞书集成</li>
     *   <li>WEBHOOK - Webhook 调用</li>
     *   <li>API_CALL - API 调用</li>
     *   <li>DATABASE - 数据库操作</li>
     * </ul>
     */
    INTEGRATION("系统集成", "Integration with external systems"),

    /**
     * 数据处理节点
     * <p>
     * 数据转换和处理的节点，包括：
     * <ul>
     *   <li>TRANSFORM - 数据转换</li>
     *   <li>FILTER - 数据过滤</li>
     *   <li>AGGREGATE - 数据聚合</li>
     *   <li>SCRIPT - 脚本执行</li>
     * </ul>
     */
    DATA_PROCESSING("数据处理", "Data transformation and processing"),

    /**
     * 通知节点
     * <p>
     * 消息通知和提醒的节点，包括：
     * <ul>
     *   <li>NOTIFICATION - 通用通知</li>
     *   <li>EMAIL - 邮件通知</li>
     *   <li>SMS - 短信通知</li>
     *   <li>PUSH - 推送通知</li>
     * </ul>
     */
    NOTIFICATION("消息通知", "Message notification and alerts"),

    /**
     * 工具节点
     * <p>
     * 通用工具类节点，包括：
     * <ul>
     *   <li>DELAY - 延迟执行</li>
     *   <li>LOG - 日志记录</li>
     *   <li>VARIABLE - 变量操作</li>
     *   <li>CONDITION_EVAL - 条件求值</li>
     * </ul>
     */
    UTILITY("工具", "General utility nodes"),

    /**
     * 自定义节点
     * <p>
     * 用户自定义的特殊节点，用于扩展系统功能。
     */
    CUSTOM("自定义", "User-defined custom nodes");

    /**
     * 分区显示名称（中文）
     */
    private final String displayName;

    /**
     * 分区描述（英文）
     */
    private final String description;

    NodeCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 获取分区显示名称
     *
     * @return 中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取分区描述
     *
     * @return 英文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据名称获取分区枚举
     *
     * @param name 分区名称
     * @return 分区枚举，如果不存在返回 null
     */
    public static NodeCategory fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /**
     * 根据显示名称获取分区枚举
     *
     * @param displayName 显示名称
     * @return 分区枚举，如果不存在返回 null
     */
    public static NodeCategory fromDisplayName(String displayName) {
        for (NodeCategory category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        return null;
    }
}

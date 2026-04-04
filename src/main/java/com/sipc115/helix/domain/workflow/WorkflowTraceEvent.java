/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 工作流追踪事件模型
 * <p>
 * 用于在 RocketMQ 中传输工作流执行追踪事件，采用事件溯源模式记录工作流执行过程中的关键节点。
 * 支持两种级别的追踪事件：工作流级(L1)、节点级(L2)。
 *
 * <h3>事件类型说明：</h3>
 * <ul>
 *   <li>{@link #EVENT_WORKFLOW_START} - 工作流启动事件</li>
 *   <li>{@link #EVENT_WORKFLOW_COMPLETE} - 工作流完成事件</li>
 *   <li>{@link #EVENT_NODE_START} - 节点开始执行事件</li>
 *   <li>{@link #EVENT_NODE_COMPLETE} - 节点完成执行事件</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * WorkflowTraceEvent event = WorkflowTraceEvent.builder()
 *     .eventType(WorkflowTraceEvent.EVENT_NODE_START)
 *     .executionId(12345L)
 *     .nodeId("node_001")
 *     .status("RUNNING")
 *     .timestamp(Instant.now())
 *     .build();
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTraceEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 事件类型常量 ====================

    /**
     * 工作流启动事件
     * <p>
     * 当工作流实例开始执行时触发，记录工作流启动时的输入参数。
     */
    public static final String EVENT_WORKFLOW_START = "WORKFLOW_START";

    /**
     * 工作流完成事件
     * <p>
     * 当工作流执行完成时触发（包括成功和失败），记录工作流的最终输出或错误信息。
     */
    public static final String EVENT_WORKFLOW_COMPLETE = "WORKFLOW_COMPLETE";

    /**
     * 节点开始执行事件
     * <p>
     * 当工作流中的某个节点开始执行时触发，记录节点类型、角色和输入参数。
     */
    public static final String EVENT_NODE_START = "NODE_START";

    /**
     * 节点完成执行事件
     * <p>
     * 当节点执行完成时触发（包括成功、失败、跳过），记录节点输出或错误信息。
     */
    public static final String EVENT_NODE_COMPLETE = "NODE_COMPLETE";

    // ==================== 事件字段 ====================

    /**
     * 事件类型
     * <p>
     * 标识事件的种类，取值为上述事件类型常量之一。
     * 用于 Consumer 区分不同类型的事件并进行相应处理。
     */
    private String eventType;

    /**
     * 工作流执行ID
     * <p>
     * 关联的工作流执行记录的唯一标识，用于追踪整个工作流执行链路。
     * 可通过此字段关联查询所有相关的事件记录。
     */
    private Long executionId;

    /**
     * 节点ID
     * <p>
     * 触发事件的节点唯一标识，来源于工作流 DSL 定义。
     * 格式通常为 "node_001"、"startNode" 等有意义的名称。
     */
    private String nodeId;

    /**
     * 节点执行追踪ID
     * <p>
     * 关联的节点执行轨迹记录ID，用于关联节点级别的事件和具体执行记录。
     * 通过此字段可以查询节点执行的具体详情。
     */
    private Long nodeTraceId;

    /**
     * 执行状态
     * <p>
     * 表示事件关联操作的当前状态，常见值包括：
     * <ul>
     *   <li>RUNNING - 执行中</li>
     *   <li>SUCCESS - 执行成功</li>
     *   <li>FAILED - 执行失败</li>
     *   <li>SKIPPED - 被跳过</li>
     * </ul>
     */
    private String status;

    /**
     * 执行顺序号
     * <p>
     * 标识节点在工作流执行序列中的顺序，从1开始递增。
     * 用于分析工作流执行路径和性能瓶颈。
     */
    private Integer executionOrder;

    /**
     * 输入数据
     * <p>
     * 以键值对形式存储的输入参数快照，JSON格式。
     * 用于回溯和复现执行过程。
     */
    private Map<String, Object> inputData;

    /**
     * 输出数据
     * <p>
     * 以键值对形式存储的输出结果快照，JSON格式。
     * 包含AI模型的回复、工具调用结果等信息。
     */
    private Map<String, Object> outputData;

    /**
     * 事件时间戳
     * <p>
     * 记录事件发生的精确时间，采用UTC时间。
     * 用于计算执行耗时和分析时间序列。
     */
    private Instant timestamp;

    /**
     * 元数据
     * <p>
     * 存储与特定事件类型相关的额外信息，格式为键值对。
     * 例如：
     * <ul>
     *   <li>WORKFLOW_COMPLETE: 包含 errorMessage 错误信息</li>
     *   <li>NODE_COMPLETE(FAILED): 包含 errorStack 完整堆栈</li>
     *   <li>NODE_COMPLETE(SKIPPED): 包含 reason 跳过原因</li>
     * </ul>
     */
    private Map<String, Object> metadata;
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流状态视图类
 * <p>
 * 用于表示工作流的当前执行状态，包含工作流 ID、当前节点、执行状态、变量信息和节点状态等。
 * 提供工作流执行状态的快照，用于查询和展示。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class WorkflowStateView implements Serializable {
    /**
     * 工作流 ID
     * <p>
     * 标识当前工作流实例的唯一 ID。
     */
    private String workflowId;
    
    /**
     * 当前节点 ID
     * <p>
     * 工作流当前正在执行的节点 ID。
     */
    private String currentNodeId;
    
    /**
     * 执行状态
     * <p>
     * 工作流的整体执行状态，如 RUNNING、COMPLETED、FAILED 等。
     */
    private ExecutionStatus status;
    
    /**
     * 变量信息
     * <p>
     * 工作流执行过程中的变量，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> variables = new HashMap<>();
    
    /**
     * 节点状态映射
     * <p>
     * 记录工作流中各个节点的执行状态，以节点 ID 为键，执行状态为值。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, ExecutionStatus> nodeStatuses = new HashMap<>();
}

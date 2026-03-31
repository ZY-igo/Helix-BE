/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流 DSL 类
 * <p>
 * 用于表示工作流的领域特定语言（DSL）定义，包含工作流的基本信息、节点和边的定义，以及元数据。
 * 是工作流定义的核心数据结构，用于描述工作流的结构和逻辑。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class WorkflowDsl {
    /**
     * 工作流 ID
     * <p>
     * 工作流的唯一标识符，用于区分不同的工作流定义。
     */
    private String workflowId;
    
    /**
     * 工作流名称
     * <p>
     * 工作流的显示名称，用于在界面上展示。
     */
    private String name;
    
    /**
     * 工作流版本
     * <p>
     * 工作流的版本号，用于区分同一工作流的不同版本。
     */
    private Integer version;
    
    /**
     * 节点列表
     * <p>
     * 工作流中的节点定义，包含各种类型的节点（如开始、结束、活动、条件等）。
     * 默认为空 ArrayList，确保始终可用。
     */
    private List<DslNodeSpec> nodes = new ArrayList<>();
    
    /**
     * 边列表
     * <p>
     * 工作流中的边定义，表示节点之间的连接关系。
     * 默认为空 ArrayList，确保始终可用。
     */
    private List<DslEdgeSpec> edges = new ArrayList<>();
    
    /**
     * 元数据
     * <p>
     * 工作流的元数据信息，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> metadata = new HashMap<>();
}
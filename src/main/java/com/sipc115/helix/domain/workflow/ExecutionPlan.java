package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行计划类
 * <p>
 * 表示工作流的执行计划，包含工作流ID、版本、入口节点、编译后的节点和转换规则
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Data
public class ExecutionPlan implements Serializable {
    /**
     * 工作流ID
     */
    private String workflowId;
    
    /**
     * 工作流版本
     */
    private Integer workflowVersion;
    
    /**
     * 入口节点ID
     */
    private String entryNodeId;
    
    /**
     * 编译后的节点映射
     */
    private Map<String, CompiledNode> nodes = new HashMap<>();
    
    /**
     * 转换规则列表
     */
    private List<Transition> transitions;
}

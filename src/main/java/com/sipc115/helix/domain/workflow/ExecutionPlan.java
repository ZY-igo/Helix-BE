package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行计划类
 */
@Data
public class ExecutionPlan implements Serializable {

    /**
     * 工作流 ID
     */
    private String workflowId;

    /**
     * 工作流版本
     */
    private Integer workflowVersion;

    /**
     * 入口节点 ID
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

    /**
     * ⭐ 新增：计划 ID（唯一标识）
     */
    private String planId;

    /**
     * ⭐ 新增：计划元数据
     */
    private PlanMetadata metadata;
}

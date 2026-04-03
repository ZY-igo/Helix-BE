/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行请求类
 * <p>
 * 用于表示启动工作流实例的请求信息，包含工作流 ID、版本号和输入参数。
 * 作为工作流启动的载体，传递执行所需的基本信息。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class WorkflowExecutionRequest {
    /**
     * 工作流 ID
     * <p>
     * 标识要执行的工作流定义，用于查找对应的执行计划。
     */
    private String workflowId;
    
    /**
     * 工作流版本号
     * <p>
     * 标识工作流定义的版本，用于查找对应版本的执行计划。
     */
    private String workflowVersion;
    
    /**
     * 输入参数
     * <p>
     * 工作流执行所需的输入数据，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> input = new HashMap<>();
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.end;

import lombok.Data;

import java.util.Map;

/**
 * 结束节点配置类
 * <p>
 * 用于配置结束节点的执行参数和行为。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class EndNodeConfig {
    /**
     * 输出变量
     * <p>
     * 工作流结束时输出的变量，以键值对形式存储。
     */
    private Map<String, Object> outputVariables;
    
    /**
     * 工作流结果状态
     * <p>
     * 工作流结束时的结果状态，如 "SUCCESS"、"FAILED" 等。
     */
    private String resultStatus;
    
    /**
     * 工作流结果消息
     * <p>
     * 工作流结束时的结果消息。
     */
    private String resultMessage;
}

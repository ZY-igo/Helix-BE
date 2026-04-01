/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.start;

import lombok.Data;

import java.util.Map;

/**
 * 开始节点配置类
 * <p>
 * 用于配置开始节点的执行参数和行为。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class StartNodeConfig {
    /**
     * 初始化变量
     * <p>
     * 工作流开始时初始化的变量，以键值对形式存储。
     */
    private Map<String, Object> initVariables;
    
    /**
     * 工作流描述
     * <p>
     * 工作流的描述信息。
     */
    private String description;
}

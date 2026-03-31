/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

/**
 * 工作流 DSL 边规范类
 * <p>
 * 用于定义工作流 DSL 中的边（edge）规范，表示两个节点之间的连接关系。
 * 包含源节点、目标节点和条件键等信息，用于构建工作流的执行路径。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class DslEdgeSpec {
    /**
     * 源节点 ID
     * <p>
     * 边的起始节点，标识从哪个节点开始连接。
     */
    private String from;
    
    /**
     * 目标节点 ID
     * <p>
     * 边的目标节点，标识连接到哪个节点。
     */
    private String to;
    
    /**
     * 条件键
     * <p>
     * 边的条件标识符，用于判断是否执行此边的转换。
     * 当条件满足时，工作流将从源节点转换到目标节点。
     */
    private String conditionKey;
}

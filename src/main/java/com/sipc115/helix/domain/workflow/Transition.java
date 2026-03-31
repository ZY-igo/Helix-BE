/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;

/**
 * 工作流节点转换类
 * <p>
 * 表示工作流中两个节点之间的转换关系，包含源节点、目标节点和转换条件。
 * 用于定义工作流的执行路径和分支逻辑。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class Transition implements Serializable {
    /**
     * 源节点 ID
     * <p>
     * 转换的起始节点，标识从哪个节点开始转换。
     */
    private String from;
    
    /**
     * 目标节点 ID
     * <p>
     * 转换的目标节点，标识转换到哪个节点。
     */
    private String to;
    
    /**
     * 条件键
     * <p>
     * 转换的条件标识符，用于判断是否执行此转换。
     * 当条件满足时，工作流将从源节点转换到目标节点。
     */
    private String conditionKey;
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

/**
 * 工作流 DSL 节点类型枚举
 * <p>
 * 定义工作流中支持的节点类型，每种类型对应不同的执行逻辑：
 * <ul>
 *   <li>START - 工作流开始节点</li>
 *   <li>END - 工作流结束节点</li>
 *   <li>ACTIVITY - 活动节点，执行具体的业务逻辑</li>
 *   <li>CONDITION - 条件节点，根据条件决定执行路径</li>
 *   <li>DELAY - 延迟节点，暂停执行一段时间</li>
 *   <li>HUMAN_INPUT - 人工输入节点，需要人工干预</li>
 *   <li>CHILD_WORKFLOW - 子工作流节点，调用其他工作流</li>
 *   <li>TRANSFORM - 转换节点，处理数据转换</li>
 * </ul>
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public enum DslNodeType {
    /** 工作流开始节点 */
    START,
    
    /** 工作流结束节点 */
    END,
    
    /** 活动节点，执行具体的业务逻辑 */
    ACTIVITY,
    
    /** 条件节点，根据条件决定执行路径 */
    CONDITION,
    
    /** 延迟节点，暂停执行一段时间 */
    DELAY,
    
    /** 人工输入节点，需要人工干预 */
    HUMAN_INPUT,
    
    /** 子工作流节点，调用其他工作流 */
    CHILD_WORKFLOW,
    
    /** 转换节点，处理数据转换 */
    TRANSFORM
}

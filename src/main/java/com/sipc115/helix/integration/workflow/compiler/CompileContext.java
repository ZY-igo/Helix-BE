/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.expression.ExpressionEngine;
import com.sipc115.helix.domain.workflow.DslNodeSpec;

import java.util.Map;
import java.util.Set;

/**
 * 编译上下文
 * <p>
 * 封装编译过程中的共享依赖，提供给各个节点编译器使用。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class CompileContext {
    
    /**
     * 表达式引擎
     * <p>
     * 用于编译和执行条件表达式。
     */
    private final ExpressionEngine expressionEngine;
    
    /**
     * 工作流元数据
     * <p>
     * 包含工作流的全局配置和信息。
     */
    private final Map<String, Object> workflowMeta;
    
    /**
     * 节点索引
     * <p>
     * 节点 ID 到节点规范的映射，用于快速查找节点。
     */
    private final Map<String, DslNodeSpec> nodeIndex;
    
    /**
     * 已声明的变量
     * <p>
     * 工作流中已声明的所有变量，用于验证变量引用的合法性。
     */
    private final Set<String> declaredVariables;
    
    /**
     * 构造编译上下文
     * 
     * @param expressionEngine 表达式引擎
     * @param workflowMeta 工作流元数据
     * @param nodeIndex 节点索引
     * @param declaredVariables 已声明的变量
     */
    public CompileContext(
            ExpressionEngine expressionEngine,
            Map<String, Object> workflowMeta,
            Map<String, DslNodeSpec> nodeIndex,
            Set<String> declaredVariables) {
        this.expressionEngine = expressionEngine;
        this.workflowMeta = workflowMeta;
        this.nodeIndex = nodeIndex;
        this.declaredVariables = declaredVariables;
    }
    
    /**
     * 获取表达式引擎
     * 
     * @return 表达式引擎
     */
    public ExpressionEngine getExpressionEngine() {
        return expressionEngine;
    }
    
    /**
     * 获取工作流元数据
     * 
     * @return 工作流元数据
     */
    public Map<String, Object> getWorkflowMeta() {
        return workflowMeta;
    }
    
    /**
     * 获取节点索引
     * 
     * @return 节点索引
     */
    public Map<String, DslNodeSpec> getNodeIndex() {
        return nodeIndex;
    }
    
    /**
     * 获取已声明的变量
     * 
     * @return 已声明的变量集合
     */
    public Set<String> getDeclaredVariables() {
        return declaredVariables;
    }
    
    /**
     * 检查变量是否已声明
     * 
     * @param variable 变量名
     * @return 是否已声明
     */
    public boolean isVariableDeclared(String variable) {
        return declaredVariables.contains(variable);
    }
    
    /**
     * 根据节点 ID 获取节点规范
     * 
     * @param nodeId 节点 ID
     * @return 节点规范，如果不存在则返回 null
     */
    public DslNodeSpec getNodeById(String nodeId) {
        return nodeIndex.get(nodeId);
    }
}

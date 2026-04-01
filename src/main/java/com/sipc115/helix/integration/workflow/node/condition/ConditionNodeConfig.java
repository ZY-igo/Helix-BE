/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.condition;

import com.sipc115.helix.expression.CompiledExpression;
import lombok.Data;

/**
 * 条件节点配置类
 * <p>
 * 用于配置条件节点的执行参数和行为。
 * 包含原始表达式和编译后的表达式，运行时直接使用编译后的表达式以提升性能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class ConditionNodeConfig {
    /**
     * 条件表达式
     * <p>
     * 用于判断执行路径的原始表达式，由用户在 DSL 中编写。
     */
    private String condition;
    
    /**
     * 表达式语言类型
     * <p>
     * 条件表达式使用的语言类型，如 "aviator"、"spel" 等。
     */
    private String expressionLanguage;
    
    /**
     * 编译后的表达式
     * <p>
     * 由编译器编译后的表达式对象，运行时直接使用以避免重复编译。
     * 使用 CompiledExpression 接口，支持不同的表达式引擎实现。
     */
    private CompiledExpression compiledExpression;
    
    /**
     * 表达式编译状态
     * <p>
     * 标识表达式是否已编译，用于运行时检查。
     */
    private boolean compiled = false;
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.expression;

import java.util.Map;

/**
 * 表达式引擎接口
 * <p>
 * 定义表达式引擎的核心操作，包括表达式的编译、执行和验证。
 * 该接口抽象了不同表达式引擎的实现细节，允许在运行时切换不同的引擎。
 * 
 * <p>支持的操作：</p>
 * <ul>
 *   <li>执行表达式字符串</li>
 *   <li>编译表达式为可重用对象</li>
 *   <li>执行编译后的表达式</li>
 *   <li>验证表达式语法</li>
 * </ul>
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface ExpressionEngine {
    
    /**
     * 获取引擎名称
     * <p>
     * 返回表达式引擎的标识名称，如 "aviator"、"spel" 等。
     * 
     * @return 引擎名称
     */
    String getEngineName();
    
    /**
     * 执行表达式
     * <p>
     * 直接执行表达式字符串，返回执行结果。
     * 
     * @param expression 表达式字符串
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果
     * @throws ExpressionException 当表达式执行失败时抛出
     */
    Object execute(String expression, Map<String, Object> env);
    
    /**
     * 编译表达式
     * <p>
     * 编译表达式为可重用的 CompiledExpression 对象，提高执行性能。
     * 编译后的表达式可以多次执行，避免重复编译的开销。
     * 
     * @param expression 表达式字符串
     * @return 编译后的表达式对象
     * @throws ExpressionException 当表达式编译失败时抛出
     */
    CompiledExpression compile(String expression);
    
    /**
     * 执行编译后的表达式
     * <p>
     * 执行编译后的 CompiledExpression 对象，返回执行结果。
     * 
     * @param compiledExpression 编译后的表达式对象
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果
     * @throws ExpressionException 当表达式执行失败时抛出
     */
    Object executeCompiled(CompiledExpression compiledExpression, Map<String, Object> env);
    
    /**
     * 执行表达式并返回布尔结果
     * <p>
     * 执行表达式并将结果转换为布尔值，适用于条件判断场景。
     * 
     * @param expression 表达式字符串
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果的布尔值
     * @throws ExpressionException 当表达式执行失败时抛出
     */
    boolean executeBoolean(String expression, Map<String, Object> env);
    
    /**
     * 执行表达式并返回数值结果
     * <p>
     * 执行表达式并将结果转换为数值，适用于计算场景。
     * 
     * @param expression 表达式字符串
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果的数值，如果结果不是数值则返回 null
     * @throws ExpressionException 当表达式执行失败时抛出
     */
    Number executeNumber(String expression, Map<String, Object> env);
    
    /**
     * 验证表达式语法
     * <p>
     * 验证表达式的语法是否正确，若语法错误则抛出异常。
     * 
     * @param expression 表达式字符串
     * @throws ExpressionException 当表达式语法错误时抛出
     */
    void validateExpression(String expression);
}

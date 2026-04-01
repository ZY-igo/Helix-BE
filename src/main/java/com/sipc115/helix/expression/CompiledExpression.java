/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.expression;

import java.util.Map;

/**
 * 编译后的表达式接口
 * <p>
 * 封装编译后的表达式对象，提供统一的执行接口。
 * 该接口隐藏了底层表达式引擎的具体实现细节。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface CompiledExpression {
    
    /**
     * 获取原始表达式字符串
     * <p>
     * 返回编译前的原始表达式字符串。
     * 
     * @return 原始表达式字符串
     */
    String getOriginalExpression();
    
    /**
     * 执行表达式
     * <p>
     * 在指定的执行环境中执行编译后的表达式。
     * 
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果
     * @throws ExpressionException 当表达式执行失败时抛出
     */
    Object execute(Map<String, Object> env);
    
    /**
     * 获取表达式引擎类型
     * <p>
     * 返回创建该编译后表达式的引擎类型标识。
     * 
     * @return 引擎类型标识，如 "aviator"、"spel" 等
     */
    String getEngineType();
}

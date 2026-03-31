/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.aviator;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Aviator 表达式引擎服务
 * <p>
 * 封装 Aviator 表达式引擎的核心功能，提供表达式的编译、执行和管理。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class AviatorService {

    /**
     * 执行表达式
     * <p>
     * 直接执行表达式字符串，返回执行结果。
     * 
     * @param expression 表达式字符串
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果
     */
    public Object execute(String expression, Map<String, Object> env) {
        return AviatorEvaluator.execute(expression, env);
    }

    /**
     * 编译表达式
     * <p>
     * 编译表达式为可重用的 Expression 对象，提高执行性能。
     * 
     * @param expression 表达式字符串
     * @return 编译后的 Expression 对象
     */
    public Expression compile(String expression) {
        return AviatorEvaluator.compile(expression);
    }

    /**
     * 执行编译后的表达式
     * <p>
     * 执行编译后的 Expression 对象，返回执行结果。
     * 
     * @param compiledExpression 编译后的 Expression 对象
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果
     */
    public Object executeCompiled(Expression compiledExpression, Map<String, Object> env) {
        return compiledExpression.execute(env);
    }

    /**
     * 执行表达式并返回布尔结果
     * <p>
     * 执行表达式并将结果转换为布尔值，适用于条件判断场景。
     * 
     * @param expression 表达式字符串
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果的布尔值
     */
    public boolean executeBoolean(String expression, Map<String, Object> env) {
        Object result = execute(expression, env);
        return result instanceof Boolean ? (Boolean) result : false;
    }

    /**
     * 执行表达式并返回数值结果
     * <p>
     * 执行表达式并将结果转换为数值，适用于计算场景。
     * 
     * @param expression 表达式字符串
     * @param env 表达式执行环境（变量映射）
     * @return 表达式执行结果的数值
     */
    public Number executeNumber(String expression, Map<String, Object> env) {
        Object result = execute(expression, env);
        return result instanceof Number ? (Number) result : null;
    }
}

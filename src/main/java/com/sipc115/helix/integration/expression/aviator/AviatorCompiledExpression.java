/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.expression.aviator;

import com.googlecode.aviator.Expression;
import com.sipc115.helix.integration.expression.CompiledExpression;

import java.util.Map;

/**
 * Aviator 编译后的表达式包装类
 * <p>
 * 封装 Aviator 的 Expression 对象，实现 CompiledExpression 接口。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class AviatorCompiledExpression implements CompiledExpression {
    
    /**
     * Aviator 表达式对象
     */
    private final Expression expression;
    
    /**
     * 原始表达式字符串
     */
    private final String originalExpression;
    
    /**
     * 构造 Aviator 编译后的表达式
     * 
     * @param originalExpression 原始表达式字符串
     * @param expression Aviator 表达式对象
     */
    public AviatorCompiledExpression(String originalExpression, Expression expression) {
        this.originalExpression = originalExpression;
        this.expression = expression;
    }
    
    @Override
    public String getOriginalExpression() {
        return originalExpression;
    }
    
    @Override
    public Object execute(Map<String, Object> env) {
        return expression.execute(env);
    }
    
    @Override
    public String getEngineType() {
        return "aviator";
    }
    
    /**
     * 获取底层的 Aviator 表达式对象
     * <p>
     * 仅在需要访问 Aviator 特有功能时使用。
     * 
     * @return Aviator Expression 对象
     */
    public Expression getAviatorExpression() {
        return expression;
    }
}

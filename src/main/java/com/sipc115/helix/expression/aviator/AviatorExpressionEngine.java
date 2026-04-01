/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.expression.aviator;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.sipc115.helix.expression.CompiledExpression;
import com.sipc115.helix.expression.ExpressionEngine;
import com.sipc115.helix.expression.ExpressionException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Aviator 表达式引擎实现
 * <p>
 * 基于 Aviator 表达式引擎的实现，提供表达式的编译、执行和验证功能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class AviatorExpressionEngine implements ExpressionEngine {

    @Override
    public String getEngineName() {
        return "aviator";
    }

    @Override
    public Object execute(String expression, Map<String, Object> env) {
        try {
            return AviatorEvaluator.execute(expression, env);
        } catch (Exception e) {
            throw new ExpressionException(
                "Failed to execute expression: " + e.getMessage(),
                expression,
                ExpressionException.ExceptionType.EXECUTE_ERROR,
                e
            );
        }
    }

    @Override
    public CompiledExpression compile(String expression) {
        try {
            Expression aviatorExpr = AviatorEvaluator.compile(expression);
            return new AviatorCompiledExpression(expression, aviatorExpr);
        } catch (Exception e) {
            throw new ExpressionException(
                "Failed to compile expression: " + e.getMessage(),
                expression,
                ExpressionException.ExceptionType.COMPILE_ERROR,
                e
            );
        }
    }

    @Override
    public Object executeCompiled(CompiledExpression compiledExpression, Map<String, Object> env) {
        if (!(compiledExpression instanceof AviatorCompiledExpression)) {
            throw new IllegalArgumentException(
                "Invalid compiled expression type. Expected AviatorCompiledExpression but got: " 
                + compiledExpression.getClass().getName()
            );
        }
        
        try {
            return compiledExpression.execute(env);
        } catch (Exception e) {
            throw new ExpressionException(
                "Failed to execute compiled expression: " + e.getMessage(),
                compiledExpression.getOriginalExpression(),
                ExpressionException.ExceptionType.EXECUTE_ERROR,
                e
            );
        }
    }

    @Override
    public boolean executeBoolean(String expression, Map<String, Object> env) {
        Object result = execute(expression, env);
        return result instanceof Boolean ? (Boolean) result : false;
    }

    @Override
    public Number executeNumber(String expression, Map<String, Object> env) {
        Object result = execute(expression, env);
        return result instanceof Number ? (Number) result : null;
    }

    @Override
    public void validateExpression(String expression) {
        try {
            AviatorEvaluator.compile(expression);
        } catch (Exception e) {
            throw new ExpressionException(
                "Invalid expression syntax: " + e.getMessage(),
                expression,
                ExpressionException.ExceptionType.VALIDATE_ERROR,
                e
            );
        }
    }
}

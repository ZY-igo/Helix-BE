/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.expression;

/**
 * 表达式异常
 * <p>
 * 表达式引擎执行过程中抛出的运行时异常。
 * 用于封装表达式编译、执行过程中的各种错误。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class ExpressionException extends RuntimeException {
    
    /**
     * 表达式字符串
     * <p>
     * 发生异常的表达式。
     */
    private final String expression;
    
    /**
     * 异常类型
     * <p>
     * 标识是编译错误还是执行错误。
     */
    private final ExceptionType exceptionType;
    
    /**
     * 异常类型枚举
     */
    public enum ExceptionType {
        /** 编译错误 */
        COMPILE_ERROR,
        /** 执行错误 */
        EXECUTE_ERROR,
        /** 验证错误 */
        VALIDATE_ERROR,
        /** 其他错误 */
        OTHER_ERROR
    }
    
    /**
     * 构造表达式异常
     * 
     * @param message 异常消息
     */
    public ExpressionException(String message) {
        super(message);
        this.expression = null;
        this.exceptionType = ExceptionType.OTHER_ERROR;
    }
    
    /**
     * 构造表达式异常
     * 
     * @param message 异常消息
     * @param cause 原始异常
     */
    public ExpressionException(String message, Throwable cause) {
        super(message, cause);
        this.expression = null;
        this.exceptionType = ExceptionType.OTHER_ERROR;
    }
    
    /**
     * 构造表达式异常
     * 
     * @param message 异常消息
     * @param expression 发生异常的表达式
     * @param exceptionType 异常类型
     */
    public ExpressionException(String message, String expression, ExceptionType exceptionType) {
        super(message);
        this.expression = expression;
        this.exceptionType = exceptionType;
    }
    
    /**
     * 构造表达式异常
     * 
     * @param message 异常消息
     * @param expression 发生异常的表达式
     * @param exceptionType 异常类型
     * @param cause 原始异常
     */
    public ExpressionException(String message, String expression, ExceptionType exceptionType, Throwable cause) {
        super(message, cause);
        this.expression = expression;
        this.exceptionType = exceptionType;
    }
    
    /**
     * 获取发生异常的表达式
     * 
     * @return 表达式字符串
     */
    public String getExpression() {
        return expression;
    }
    
    /**
     * 获取异常类型
     * 
     * @return 异常类型
     */
    public ExceptionType getExceptionType() {
        return exceptionType;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(exceptionType).append("] ");
        sb.append(super.getMessage());
        if (expression != null) {
            sb.append(" | Expression: ").append(expression);
        }
        return sb.toString();
    }
}

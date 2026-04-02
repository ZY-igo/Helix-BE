/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.runtime;

import lombok.Getter;

/**
 * AI 任务执行异常
 * <p>
 * 封装 AI 任务执行过程中的异常信息，包含步骤 ID、错误类型等上下文。
 */
@Getter
public class AiTaskExecutionException extends RuntimeException {

    /**
     * 出错的步骤 ID
     */
    private final String stepId;

    /**
     * 步骤类型
     */
    private final String stepType;

    /**
     * 错误类型（如 MODEL_ERROR, VALIDATION_FAILED, TIMEOUT 等）
     */
    private final String errorType;

    /**
     * 原始错误信息
     */
    private final String originalMessage;

    public AiTaskExecutionException(String stepId, String stepType,
                                     String errorType, String message, Throwable cause) {
        super(String.format("AI task failed at step[%s](%s): %s",
            stepId, stepType, message), cause);
        this.stepId = stepId;
        this.stepType = stepType;
        this.errorType = errorType;
        this.originalMessage = message;
    }

    /**
     * 创建模型调用失败的异常
     */
    public static AiTaskExecutionException modelError(String stepId, Throwable cause) {
        return new AiTaskExecutionException(
            stepId,
            "UNKNOWN",
            "MODEL_ERROR",
            "AI 模型调用失败：" + cause.getMessage(),
            cause
        );
    }

    /**
     * 创建验证失败的异常
     */
    public static AiTaskExecutionException validationFailed(String stepId, String reason) {
        return new AiTaskExecutionException(
            stepId,
            "VALIDATE",
            "VALIDATION_FAILED",
            reason,
            null
        );
    }

    /**
     * 创建超时异常
     */
    public static AiTaskExecutionException timeout(String stepId, long timeoutMs) {
        return new AiTaskExecutionException(
            stepId,
            "UNKNOWN",
            "TIMEOUT",
            "步骤执行超时：" + timeoutMs + "ms",
            null
        );
    }
}

package com.sipc115.helix.domain.workflow;

/**
 * 执行状态枚举
 * <p>
 * 表示工作流或节点的执行状态
 * </p>
 *
 * @author system
 * @since 1.0.0
 */
public enum ExecutionStatus {
    /**
     * 待执行
     */
    PENDING,

    /**
     * 执行中
     */
    RUNNING,

    /**
     * 等待信号
     */
    WAITING_SIGNAL,

    /**
     * 等待重试
     */
    WAITING_RETRY,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 执行失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 已跳过
     */
    SKIPPED,

    /**
     * 超时
     */
    TIMED_OUT
}

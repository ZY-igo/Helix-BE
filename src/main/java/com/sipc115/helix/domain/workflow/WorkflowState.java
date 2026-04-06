package com.sipc115.helix.domain.workflow;

/**
 * 工作流版本状态枚举
 * <p>
 * 表示工作流 DSL 版本的状态，用于追踪工作流的生命周期
 */
public enum WorkflowState {
    /**
     * 初始化
     */
    INITIALIZING("初始化"),

    /**
     * 编译中
     */
    COMPILING("编译中"),

    /**
     * 编译失败
     */
    COMPILE_FAILED("编译失败"),

    /**
     * 编译成功
     */
    COMPILED("编译成功"),

    /**
     * 运行中
     */
    RUNNING("运行中"),

    /**
     * 已停用
     */
    STOPPED("已停用");

    private final String description;

    WorkflowState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
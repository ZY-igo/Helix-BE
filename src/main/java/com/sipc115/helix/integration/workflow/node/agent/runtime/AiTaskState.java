/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.runtime;

import com.sipc115.helix.integration.workflow.node.agent.step.LlmConfig;

import java.util.Map;

/**
 * AI 任务运行时状态类
 * <p>
 * 表示 AI 任务的运行时状态，包括输入、变量和元数据。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class AiTaskState {
    private Map<String, Object> input;
    private Map<String, Object> vars;
    private Map<String, Object> meta;
    private Map<String, Object> output;
    private LlmConfig llmConfig;

    private AiTaskState(Builder builder) {
        this.input = builder.input;
        this.vars = builder.vars;
        this.meta = builder.meta;
        this.output = builder.output;
        this.llmConfig = builder.llmConfig;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    public LlmConfig getLlmConfig() {
        return llmConfig;
    }

    /**
     * 创建建造者实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 建造者模式
     */
    public static class Builder {
        private Map<String, Object> input;
        private Map<String, Object> vars;
        private Map<String, Object> meta;
        private Map<String, Object> output;
        private LlmConfig llmConfig;

        public Builder input(Map<String, Object> input) {
            this.input = input;
            return this;
        }

        public Builder vars(Map<String, Object> vars) {
            this.vars = vars;
            return this;
        }

        public Builder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public Builder output(Map<String, Object> output) {
            this.output = output;
            return this;
        }

        public Builder llmConfig(LlmConfig llmConfig) {
            this.llmConfig = llmConfig;
            return this;
        }

        public AiTaskState build() {
            return new AiTaskState(this);
        }
    }
}

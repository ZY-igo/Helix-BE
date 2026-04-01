/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.ai.runtime;

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

    private AiTaskState(Builder builder) {
        this.input = builder.input;
        this.vars = builder.vars;
        this.meta = builder.meta;
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

    /**
     * 建造者模式
     */
    public static class Builder {
        private Map<String, Object> input;
        private Map<String, Object> vars;
        private Map<String, Object> meta;

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

        public AiTaskState build() {
            return new AiTaskState(this);
        }
    }
}

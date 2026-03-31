/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.node.ai.runtime;

import java.util.List;

/**
 * AI 任务执行轨迹类
 * <p>
 * 表示 AI 任务的执行轨迹，包括轮次轨迹、最终状态和停止原因。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class AiTaskTrace {
    private List<RoundTrace> rounds;
    private String finalStatus;
    private String stopReason;

    private AiTaskTrace(Builder builder) {
        this.rounds = builder.rounds;
        this.finalStatus = builder.finalStatus;
        this.stopReason = builder.stopReason;
    }

    // 为了方便使用，添加默认构造函数
    public AiTaskTrace() {
    }

    public List<RoundTrace> getRounds() {
        return rounds;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public String getStopReason() {
        return stopReason;
    }

    // 添加 setter 方法
    public void setRounds(List<RoundTrace> rounds) {
        this.rounds = rounds;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    /**
     * 轮次轨迹类
     * <p>
     * 表示 AI 任务的一个执行轮次。
     */
    public static class RoundTrace {
        private int round;
        private List<StepTrace> steps;

        public RoundTrace(int round, List<StepTrace> steps) {
            this.round = round;
            this.steps = steps;
        }

        public int getRound() {
            return round;
        }

        public List<StepTrace> getSteps() {
            return steps;
        }
    }

    /**
     * 步骤轨迹类
     * <p>
     * 表示 AI 任务的一个执行步骤。
     */
    public static class StepTrace {
        private String stepId;
        private String stepType;
        private String status;
        private Object inputSnapshot;
        private Object outputSnapshot;
        private long durationMs;

        public StepTrace(String stepId, String stepType, String status, Object inputSnapshot, Object outputSnapshot, long durationMs) {
            this.stepId = stepId;
            this.stepType = stepType;
            this.status = status;
            this.inputSnapshot = inputSnapshot;
            this.outputSnapshot = outputSnapshot;
            this.durationMs = durationMs;
        }

        public String getStepId() {
            return stepId;
        }

        public String getStepType() {
            return stepType;
        }

        public String getStatus() {
            return status;
        }

        public Object getInputSnapshot() {
            return inputSnapshot;
        }

        public Object getOutputSnapshot() {
            return outputSnapshot;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }

    /**
     * 建造者模式
     */
    public static class Builder {
        private List<RoundTrace> rounds;
        private String finalStatus;
        private String stopReason;

        public Builder rounds(List<RoundTrace> rounds) {
            this.rounds = rounds;
            return this;
        }

        public Builder finalStatus(String finalStatus) {
            this.finalStatus = finalStatus;
            return this;
        }

        public Builder stopReason(String stopReason) {
            this.stopReason = stopReason;
            return this;
        }

        public AiTaskTrace build() {
            return new AiTaskTrace(this);
        }
    }
}

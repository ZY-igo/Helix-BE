/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.runtime;

import java.util.List;

/**
 * AI 任务执行轨迹类
 */
public class AiTaskTrace {
    private List<RoundTrace> rounds;
    private String finalStatus;
    private String stopReason;

    // ⭐ 添加错误信息字段
    private String errorMessage;

    // 为了方便使用，添加默认构造函数
    public AiTaskTrace() {
    }

    private AiTaskTrace(Builder builder) {
        this.rounds = builder.rounds;
        this.finalStatus = builder.finalStatus;
        this.stopReason = builder.stopReason;
    }

    // ... existing code (getters, setters) ...

    public void setRounds(List<RoundTrace> rounds) {
        this.rounds = rounds;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 轮次轨迹类
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
     */
    public static class StepTrace {
        private String stepId;
        private String stepType;
        private String status;
        private Object inputSnapshot;
        private Object outputSnapshot;
        private long durationMs;

        // ⭐ 添加错误信息字段
        private String errorMessage;

        public StepTrace(String stepId, String stepType, String status,
                        Object inputSnapshot, Object outputSnapshot, long durationMs) {
            this.stepId = stepId;
            this.stepType = stepType;
            this.status = status;
            this.inputSnapshot = inputSnapshot;
            this.outputSnapshot = outputSnapshot;
            this.durationMs = durationMs;
        }

        // ⭐ 添加静态工厂方法
        public static StepTrace success(String stepId, String stepType,
                                       Object outputSnapshot, long durationMs) {
            return new StepTrace(stepId, stepType, "SUCCESS", null, outputSnapshot, durationMs);
        }

        public static StepTrace failed(String stepId, String stepType,
                                      String errorMessage, long durationMs) {
            StepTrace trace = new StepTrace(stepId, stepType, "FAILED", null, null, durationMs);
            trace.setErrorMessage(errorMessage);
            return trace;
        }

        // ⭐ 添加带错误信息的成功/失败方法
        public static StepTrace create(String stepId, String stepType, String status,
                                      Object outputSnapshot, long durationMs, String errorMessage) {
            StepTrace trace = new StepTrace(stepId, stepType, status, null, outputSnapshot, durationMs);
            trace.setErrorMessage(errorMessage);
            return trace;
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

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
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

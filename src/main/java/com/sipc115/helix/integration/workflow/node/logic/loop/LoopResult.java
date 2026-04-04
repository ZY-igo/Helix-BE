package com.sipc115.helix.integration.workflow.node.logic.loop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoopResult {

    private int iterations;

    private Object lastResult;

    private String exitReason;

    public Map<String, Object> toOutput() {
        return Map.of(
                "iterations", iterations,
                "lastResult", lastResult != null ? lastResult : "null",
                "exitReason", exitReason
        );
    }
}

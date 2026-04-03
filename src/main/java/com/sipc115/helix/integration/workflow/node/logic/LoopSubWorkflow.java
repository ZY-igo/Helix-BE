package com.sipc115.helix.integration.workflow.node.logic;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

@WorkflowInterface
public interface LoopSubWorkflow {

    @WorkflowMethod
    LoopResult execute(Integer maxRounds, Map<String, Object> input);
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.QueryMethod;

import java.util.Map;

/**
 * DSL 运行时工作流接口
 */
@WorkflowInterface
public interface DslRuntimeWorkflow {

    @WorkflowMethod
    void run(ExecutionPlan plan, Map<String, Object> input);

    @SignalMethod
    void provideHumanInput(HumanSignalPayload payload);

    @QueryMethod
    WorkflowStateView currentState();
}

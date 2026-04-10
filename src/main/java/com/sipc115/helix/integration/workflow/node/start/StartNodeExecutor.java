/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.start;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.springframework.stereotype.Component;

@Component
public class StartNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public boolean supports(String type) {
        return DslNodeType.START.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        return NodeExecutionResult.completed();
    }
}

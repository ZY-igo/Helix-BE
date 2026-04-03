/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.integration.expression.ExpressionEngine;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.WorkflowMetadata;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public class CompileContext {

    private final ExpressionEngine expressionEngine;
    private final WorkflowMetadata workflowMeta;
    private final Map<String, DslNodeSpec> nodeIndex;
    private final Set<String> declaredVariables;

    public CompileContext(
            ExpressionEngine expressionEngine,
            WorkflowMetadata workflowMeta,
            Map<String, DslNodeSpec> nodeIndex,
            Set<String> declaredVariables) {
        this.expressionEngine = expressionEngine;
        this.workflowMeta = workflowMeta;
        this.nodeIndex = nodeIndex;
        this.declaredVariables = declaredVariables;
    }

    public boolean isVariableDeclared(String variable) {
        return declaredVariables.contains(variable);
    }

    public DslNodeSpec getNodeById(String nodeId) {
        return nodeIndex.get(nodeId);
    }
}

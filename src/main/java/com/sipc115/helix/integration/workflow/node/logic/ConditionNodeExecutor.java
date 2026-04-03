package com.sipc115.helix.integration.workflow.node.logic;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {

    private final TransitionResolver transitionResolver;

    @Autowired
    public ConditionNodeExecutor(TransitionResolver transitionResolver) {
        this.transitionResolver = transitionResolver;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.CONDITION.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        String branchKey;

        Map<String, Object> config = node.getConfig();
        Object compiledExprObj = config.get("compiledExpression");

        if (compiledExprObj instanceof CompiledExpression) {
            try {
                CompiledExpression compiledExpr = (CompiledExpression) compiledExprObj;
                Boolean result = (Boolean) compiledExpr.execute(context.getVariables());
                branchKey = result ? "true" : "false";
            } catch (Exception e) {
                branchKey = String.valueOf(config.getOrDefault("defaultBranch", "true"));
            }
        } else {
            branchKey = String.valueOf(config.getOrDefault("defaultBranch", "true"));
        }

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(branchKey);

        return result;
    }
}

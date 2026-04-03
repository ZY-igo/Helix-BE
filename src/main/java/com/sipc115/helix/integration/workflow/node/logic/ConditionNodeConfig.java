package com.sipc115.helix.integration.workflow.node.logic;

import com.sipc115.helix.integration.expression.CompiledExpression;
import lombok.Data;

@Data
public class ConditionNodeConfig {

    private String condition;

    private String expressionLanguage;

    private CompiledExpression compiledExpression;

    private boolean compiled = false;

    private String defaultBranch = "true";
}

package com.sipc115.helix.integration.workflow.node.logic;

import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoopNodeCompiler implements NodeCompiler {

    private static final Logger log = LoggerFactory.getLogger(LoopNodeCompiler.class);

    @Override
    public DslNodeType supportType() {
        return DslNodeType.LOOP;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source.getConfig() == null) {
            throw new IllegalArgumentException("Loop node config cannot be null: " + source.getId());
        }

        Object maxRounds = source.getConfig().get("maxRounds");
        if (maxRounds != null && !(maxRounds instanceof Integer)) {
            throw new IllegalArgumentException("maxRounds must be an integer: " + source.getId());
        }

        Object exitCondition = source.getConfig().get("exitCondition");
        if (exitCondition == null || !(exitCondition instanceof String)) {
            throw new IllegalArgumentException("exitCondition must be a non-empty string: " + source.getId());
        }

        String conditionStr = (String) exitCondition;
        if (conditionStr.isEmpty()) {
            throw new IllegalArgumentException("exitCondition cannot be empty: " + source.getId());
        }
    }

    @Override
    public com.sipc115.helix.domain.workflow.CompiledNode compile(DslNodeSpec source, CompileContext context) {
        com.sipc115.helix.domain.workflow.CompiledNode node = new com.sipc115.helix.domain.workflow.CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setAction("LOOP");

        java.util.Map<String, Object> compiledConfig = source.getConfig() != null
            ? new java.util.HashMap<>(source.getConfig())
            : new java.util.HashMap<>();

        Object exitCondition = compiledConfig.get("exitCondition");
        if (exitCondition instanceof String) {
            try {
                String conditionStr = (String) exitCondition;

                context.getExpressionEngine().validateExpression(conditionStr);

                CompiledExpression compiledExpr = context.getExpressionEngine().compile(conditionStr);
                compiledConfig.put("compiledExitCondition", compiledExpr);
                compiledConfig.put("exitConditionCompiled", true);

                log.debug("Compiled exit condition for loop node {}: {}", source.getId(), conditionStr);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid exit condition for loop node " + source.getId() + ": " + e.getMessage(), e);
            }
        }

        Integer maxRounds = (Integer) compiledConfig.get("maxRounds");
        if (maxRounds == null) {
            compiledConfig.put("maxRounds", 10);
        }

        String loopVariable = (String) compiledConfig.get("loopVariable");
        if (loopVariable == null || loopVariable.isEmpty()) {
            compiledConfig.put("loopVariable", "iteration");
        }

        String resultVariable = (String) compiledConfig.get("resultVariable");
        if (resultVariable == null || resultVariable.isEmpty()) {
            compiledConfig.put("resultVariable", "loopResult");
        }

        node.setConfig(compiledConfig);
        return node;
    }
}

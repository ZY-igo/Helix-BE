package com.sipc115.helix.context.node.condition;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

@Component
public class ConditionNodeDefinition implements NodeDefinition<ConditionNodeConfig> {
    @Override
    public DslNodeType type() {
        return DslNodeType.CONDITION;
    }

    @Override
    public Class<ConditionNodeConfig> configClass() {
        return ConditionNodeConfig.class;
    }
}

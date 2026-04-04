package com.sipc115.helix.integration.workflow.node.logic.condition;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

@Component
public class ConditionNodeDefinition implements NodeDefinition<ConditionNodeConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.CONDITION;
    }

    @Override
    public NodeRole role() {
        return NodeRole.NORMAL;
    }

    @Override
    public Class<ConditionNodeConfig> configClass() {
        return ConditionNodeConfig.class;
    }
}

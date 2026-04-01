package com.sipc115.helix.context.node.start;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

@Component
public class StartNodeDefinition implements NodeDefinition<StartNodeConfig> {
    @Override
    public DslNodeType type() {
        return DslNodeType.START;
    }

    @Override
    public NodeRole role() {
        return NodeRole.ENTRY;
    }

    @Override
    public Class<StartNodeConfig> configClass() {
        return StartNodeConfig.class;
    }
}

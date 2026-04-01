package com.sipc115.helix.context.node.end;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

@Component
public class EndNodeDefinition implements NodeDefinition<EndNodeConfig> {
    @Override
    public DslNodeType type() {
        return DslNodeType.END;
    }

    @Override
    public NodeRole role() {
        return NodeRole.EXIT;
    }

    @Override
    public Class<EndNodeConfig> configClass() {
        return EndNodeConfig.class;
    }
}

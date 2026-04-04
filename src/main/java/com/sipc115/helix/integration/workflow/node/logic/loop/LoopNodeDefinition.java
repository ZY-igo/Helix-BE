package com.sipc115.helix.integration.workflow.node.logic.loop;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

@Component
public class LoopNodeDefinition implements NodeDefinition<LoopNodeConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.LOOP;
    }

    @Override
    public NodeRole role() {
        return NodeRole.NORMAL;
    }

    @Override
    public Class<LoopNodeConfig> configClass() {
        return LoopNodeConfig.class;
    }
}

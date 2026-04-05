package com.sipc115.helix.integration.workflow.node.agent.chat;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.compiler.NodeRole;
import org.springframework.stereotype.Component;

@Component
public class AiTaskNodeDefinition implements NodeDefinition<Void> {

    @Override
    public DslNodeType type() {
        return DslNodeType.AI_TASK;
    }

    @Override
    public NodeRole role() {
        return NodeRole.NORMAL;
    }

    @Override
    public Class<Void> configClass() {
        return Void.class;
    }
}

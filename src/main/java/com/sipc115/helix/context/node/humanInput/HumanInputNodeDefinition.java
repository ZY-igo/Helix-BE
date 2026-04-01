package com.sipc115.helix.context.node.humanInput;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

@Component
public class HumanInputNodeDefinition implements NodeDefinition<HumanInputNodeConfig> {
    @Override
    public DslNodeType type() {
        return DslNodeType.HUMAN_INPUT;
    }

    @Override
    public Class<HumanInputNodeConfig> configClass() {
        return HumanInputNodeConfig.class;
    }
}

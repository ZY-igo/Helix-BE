package com.sipc115.helix.integration.workflow.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;

import java.util.HashMap;
import java.util.Map;

import com.sipc115.helix.domain.workflow.DslNodeType;

public interface NodeDefinition<T> {
    DslNodeType type();

    default NodeRole role() {
        return NodeRole.NORMAL;
    }

    Class<T> configClass();

    default T readConfig(Map<String, Object> rawConfig, ObjectMapper objectMapper) {
        Map<String, Object> safeConfig = rawConfig == null ? Map.of() : rawConfig;
        return objectMapper.convertValue(safeConfig, configClass());
    }

    default CompiledNode compile(DslNodeSpec spec, T config) {
        CompiledNode node = new CompiledNode();
        node.setId(spec.getId());
        node.setType(spec.getType());
        node.setAction(resolveAction(spec.getType(), spec.getConfig()));
        node.setConfig(spec.getConfig() == null ? new HashMap<>() : new HashMap<>(spec.getConfig()));
        return node;
    }

    private static String resolveAction(com.sipc115.helix.domain.workflow.DslNodeType type, Map<String, Object> config) {
        if (config == null) {
            return type.getValue();
        }
        Object action = config.get("action");
        return action == null ? type.getValue() : String.valueOf(action);
    }
}

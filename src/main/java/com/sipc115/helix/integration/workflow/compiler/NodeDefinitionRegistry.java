package com.sipc115.helix.integration.workflow.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NodeDefinitionRegistry {
    private final Map<DslNodeType, NodeDefinition<?>> definitions;
    private final ObjectMapper objectMapper;

    public NodeDefinitionRegistry(List<NodeDefinition<?>> definitions, ObjectMapper objectMapper) {
        this.definitions = definitions.stream()
                .collect(Collectors.toUnmodifiableMap(NodeDefinition::type, Function.identity()));
        this.objectMapper = objectMapper;
    }

    public boolean hasDefinition(DslNodeType type) {
        return definitions.containsKey(type);
    }

    public NodeDefinition<?> getDefinition(DslNodeType type) {
        NodeDefinition<?> definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported node type: " + type);
        }
        return definition;
    }

    public NodeRole getRole(DslNodeType type) {
        return getDefinition(type).role();
    }

    public Object convertConfig(DslNodeType type, Map<String, Object> rawConfig) {
        return readConfig(getDefinition(type), rawConfig);
    }

    public CompiledNode compile(DslNodeSpec spec) {
        NodeDefinition<?> definition = getDefinition(spec.getType());
        Object config = readConfig(definition, spec.getConfig());
        return compile(definition, spec, config);
    }

    private Object readConfig(NodeDefinition<?> definition, Map<String, Object> rawConfig) {
        return definition.readConfig(rawConfig, objectMapper);
    }

    @SuppressWarnings("unchecked")
    private CompiledNode compile(NodeDefinition<?> definition, DslNodeSpec spec, Object config) {
        return ((NodeDefinition<Object>) definition).compile(spec, config);
    }
}

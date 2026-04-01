/*-*- coding: UTF-8 -*-*/
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

/**
 * 节点定义注册中心
 * <p>
 * 管理所有节点定义，提供节点定义的查找、配置转换和编译功能。
 * 采用不可变映射存储节点定义，确保线程安全和性能优化。
 * <p>
 * 通过 Spring 自动注入所有 NodeDefinition 实现，实现零配置注册。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class NodeDefinitionRegistry {
    /**
     * 节点定义映射
     * <p>
     * 以节点类型为键，节点定义为值的不可变映射，提供 O(1) 查找性能。
     */
    private final Map<DslNodeType, NodeDefinition<?>> definitions;
    
    /**
     * Jackson 对象映射器
     * <p>
     * 用于将原始配置映射转换为类型安全的配置对象。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * <p>
     * 通过 Spring 自动注入所有 NodeDefinition 实现，并构建不可变映射。
     * 
     * @param definitions 所有节点定义实现的列表
     * @param objectMapper Jackson 对象映射器
     */
    public NodeDefinitionRegistry(List<NodeDefinition<?>> definitions, ObjectMapper objectMapper) {
        this.definitions = definitions.stream()
                .collect(Collectors.toUnmodifiableMap(NodeDefinition::type, Function.identity()));
        this.objectMapper = objectMapper;
    }

    /**
     * 检查是否存在指定类型的节点定义
     * <p>
     * 根据节点类型检查是否存在对应的节点定义。
     * 
     * @param type 节点类型
     * @return 如果存在定义返回 true，否则返回 false
     */
    public boolean hasDefinition(DslNodeType type) {
        return definitions.containsKey(type);
    }

    /**
     * 获取指定类型的节点定义
     * <p>
     * 根据节点类型获取对应的节点定义。如果不存在，抛出异常。
     * 
     * @param type 节点类型
     * @return 节点定义
     * @throws IllegalArgumentException 当节点类型不支持时抛出
     */
    public NodeDefinition<?> getDefinition(DslNodeType type) {
        NodeDefinition<?> definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported node type: " + type);
        }
        return definition;
    }

    /**
     * 获取节点角色
     * <p>
     * 根据节点类型获取对应的节点角色。
     * 
     * @param type 节点类型
     * @return 节点角色
     * @throws IllegalArgumentException 当节点类型不支持时抛出
     */
    public NodeRole getRole(DslNodeType type) {
        return getDefinition(type).role();
    }

    /**
     * 转换配置
     * <p>
     * 将原始配置映射转换为类型安全的配置对象。
     * 
     * @param type 节点类型
     * @param rawConfig 原始配置映射
     * @return 类型安全的配置对象
     * @throws IllegalArgumentException 当节点类型不支持时抛出
     */
    public Object convertConfig(DslNodeType type, Map<String, Object> rawConfig) {
        return readConfig(getDefinition(type), rawConfig);
    }

    /**
     * 编译节点
     * <p>
     * 将 DSL 节点规范编译为编译后的节点对象。
     * 首先获取对应的节点定义，然后转换配置，最后编译为 CompiledNode。
     * 
     * @param spec DSL 节点规范
     * @return 编译后的节点
     * @throws IllegalArgumentException 当节点类型不支持时抛出
     */
    public CompiledNode compile(DslNodeSpec spec) {
        NodeDefinition<?> definition = getDefinition(spec.getType());
        Object config = readConfig(definition, spec.getConfig());
        return compile(definition, spec, config);
    }

    /**
     * 读取配置
     * <p>
     * 使用节点定义和对象映射器读取配置。
     * 
     * @param definition 节点定义
     * @param rawConfig 原始配置映射
     * @return 类型安全的配置对象
     */
    private Object readConfig(NodeDefinition<?> definition, Map<String, Object> rawConfig) {
        return definition.readConfig(rawConfig, objectMapper);
    }

    /**
     * 编译节点（内部方法）
     * <p>
     * 使用节点定义编译节点规范和配置。
     * 由于泛型擦除，需要进行类型转换。
     * 
     * @param definition 节点定义
     * @param spec DSL 节点规范
     * @param config 类型安全的配置对象
     * @return 编译后的节点
     */
    @SuppressWarnings("unchecked")
    private CompiledNode compile(NodeDefinition<?> definition, DslNodeSpec spec, Object config) {
        return ((NodeDefinition<Object>) definition).compile(spec, config);
    }
}

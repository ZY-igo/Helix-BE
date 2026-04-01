/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点定义接口
 * <p>
 * 为每种节点类型提供定义和编译逻辑，支持强类型配置转换和节点编译。
 * 每种节点类型都应该实现此接口，提供特定的配置类型和编译逻辑。
 * <p>
 * 泛型 T 表示节点的配置类型，确保类型安全的配置处理。
 * 
 * @param <T> 节点配置类型
 * @author Helix Team
 * @since 2.0.0
 */
public interface NodeDefinition<T> {
    /**
     * 获取节点类型
     * <p>
     * 返回此定义支持的节点类型枚举值。
     * 
     * @return 节点类型枚举
     */
    DslNodeType type();

    /**
     * 获取节点角色
     * <p>
     * 返回节点的角色，默认为 NORMAL 角色。
     * 可以根据需要重写此方法以返回特定的角色。
     * 
     * @return 节点角色
     */
    default NodeRole role() {
        return NodeRole.NORMAL;
    }

    /**
     * 获取配置类
     * <p>
     * 返回此节点类型对应的配置类，用于类型安全的配置转换。
     * 
     * @return 配置类
     */
    Class<T> configClass();

    /**
     * 读取配置
     * <p>
     * 将原始配置映射转换为类型安全的配置对象。
     * 如果原始配置为 null，则使用空映射。
     * 
     * @param rawConfig 原始配置映射
     * @param objectMapper Jackson 对象映射器
     * @return 类型安全的配置对象
     */
    default T readConfig(Map<String, Object> rawConfig, ObjectMapper objectMapper) {
        Map<String, Object> safeConfig = rawConfig == null ? Map.of() : rawConfig;
        return objectMapper.convertValue(safeConfig, configClass());
    }

    /**
     * 编译节点
     * <p>
     * 将 DSL 节点规范编译为编译后的节点对象。
     * 默认实现创建基本的 CompiledNode 对象，设置 ID、类型、操作和配置。
     * 可以根据需要重写此方法以提供更复杂的编译逻辑。
     * 
     * @param spec DSL 节点规范
     * @param config 类型安全的配置对象
     * @return 编译后的节点
     */
    default CompiledNode compile(DslNodeSpec spec, T config) {
        CompiledNode node = new CompiledNode();
        node.setId(spec.getId());
        node.setType(spec.getType());
        node.setAction(resolveAction(spec.getType(), spec.getConfig()));
        node.setConfig(spec.getConfig() == null ? new HashMap<>() : new HashMap<>(spec.getConfig()));
        return node;
    }

    /**
     * 解析操作名称
     * <p>
     * 根据节点类型和配置解析操作名称。
     * 如果配置为 null 或配置中没有指定 action，则使用节点类型的默认值。
     * 
     * @param type 节点类型
     * @param config 节点配置
     * @return 操作名称
     */
    private static String resolveAction(DslNodeType type, Map<String, Object> config) {
        if (config == null) {
            return type.getValue();
        }
        Object action = config.get("action");
        return action == null ? type.getValue() : String.valueOf(action);
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 节点注册表服务类
 * <p>
 * 负责管理和查询已注册的工作流节点类型、执行器和配置信息
 * </p>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class NodeRegistryService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(NodeRegistryService.class);

    /**
     * 所有节点定义的列表
     */
    private final List<NodeDefinition<?>> nodeDefinitions;

    /**
     * Jackson 对象映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param nodeDefinitions 所有节点定义的列表（Spring 自动注入）
     * @param objectMapper Jackson 对象映射器
     */
    public NodeRegistryService(
            List<NodeDefinition<?>> nodeDefinitions,
            ObjectMapper objectMapper) {
        this.nodeDefinitions = nodeDefinitions;
        this.objectMapper = objectMapper;

        log.info("NodeRegistryService initialized with {} node definitions", nodeDefinitions.size());
    }

    /**
     * 获取所有已注册的节点类型
     *
     * @return 节点类型列表
     */
    public List<String> getAllRegisteredTypes() {
        List<String> types = nodeDefinitions.stream()
                .map(NodeDefinition::type)
                .map(DslNodeType::name)
                .collect(Collectors.toList());

        log.debug("Retrieved {} registered node types", types.size());
        return types;
    }

    /**
     * 获取所有已注册的节点定义信息
     *
     * @return 节点定义信息列表
     */
    public List<NodeDefinitionInfo> getAllNodeDefinitions() {
        List<NodeDefinitionInfo> definitions = nodeDefinitions.stream()
                .map(this::toNodeDefinitionInfo)
                .collect(Collectors.toList());

        log.debug("Retrieved {} node definitions", definitions.size());
        return definitions;
    }

    /**
     * 根据节点类型获取节点定义信息
     *
     * @param type 节点类型（如 "AI_TASK", "START" 等）
     * @return 节点定义信息，不存在返回 Optional.empty()
     */
    public Optional<NodeDefinitionInfo> getNodeDefinition(String type) {
        return nodeDefinitions.stream()
                .filter(def -> def.type().name().equals(type))
                .findFirst()
                .map(this::toNodeDefinitionInfo);
    }

    /**
     * 获取节点的配置类名称
     *
     * @param type 节点类型
     * @return 配置类全限定名，不存在返回 Optional.empty()
     */
    public Optional<String> getConfigClassName(String type) {
        return nodeDefinitions.stream()
                .filter(def -> def.type().name().equals(type))
                .findFirst()
                .map(def -> def.configClass().getName());
    }

    /**
     * 获取节点的示例配置
     * <p>
     * 返回一个空的配置对象，用于展示该节点需要哪些配置字段
     * </p>
     *
     * @param type 节点类型
     * @return 示例配置（JSON 格式），不存在返回 Optional.empty()
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getExampleConfig(String type) {
        return nodeDefinitions.stream()
                .filter(def -> def.type().name().equals(type))
                .findFirst()
                .map(def -> {
                    try {
                        // 创建一个空的配置对象
                        Class<?> configClass = def.configClass();
                        Object emptyConfig = objectMapper.convertValue(new HashMap<>(), configClass);

                        // 转换为 Map 返回
                        return objectMapper.convertValue(emptyConfig, Map.class);
                    } catch (Exception e) {
                        log.error("Failed to create example config for type={}", type, e);
                        return new HashMap<>();
                    }
                });
    }

    /**
     * 检查指定节点类型是否已注册
     *
     * @param type 节点类型
     * @return 是否已注册
     */
    public boolean isTypeRegistered(String type) {
        return nodeDefinitions.stream()
                .anyMatch(def -> def.type().name().equals(type));
    }

    /**
     * 获取已注册的节点数量
     *
     * @return 节点数量
     */
    public int getRegisteredCount() {
        return nodeDefinitions.size();
    }

    /**
     * 将 NodeDefinition 转换为 NodeDefinitionInfo
     *
     * @param definition 节点定义
     * @return 节点定义信息
     */
    private NodeDefinitionInfo toNodeDefinitionInfo(NodeDefinition<?> definition) {
        return new NodeDefinitionInfo(
                definition.type().name(),
                definition.type().getValue(),
                definition.role().name(),
                definition.configClass().getName(),
                definition.getClass().getName()
        );
    }

    /**
     * 节点定义信息 DTO
     * <p>
     * 封装节点定义的元数据信息
     * </p>
     */
    public static class NodeDefinitionInfo {

        /**
         * 节点类型名称（枚举名）
         */
        private final String type;

        /**
         * 节点类型值（数据库存储的值）
         */
        private final String typeValue;

        /**
         * 节点角色
         */
        private final String role;

        /**
         * 配置类全限定名
         */
        private final String configClass;

        /**
         * 定义类全限定名
         */
        private final String definitionClass;

        public NodeDefinitionInfo(String type, String typeValue, String role,
                                  String configClass, String definitionClass) {
            this.type = type;
            this.typeValue = typeValue;
            this.role = role;
            this.configClass = configClass;
            this.definitionClass = definitionClass;
        }

        public String getType() {
            return type;
        }

        public String getTypeValue() {
            return typeValue;
        }

        public String getRole() {
            return role;
        }

        public String getConfigClass() {
            return configClass;
        }

        public String getDefinitionClass() {
            return definitionClass;
        }

        @Override
        public String toString() {
            return String.format("NodeDefinitionInfo{type='%s', role='%s', configClass='%s'}",
                    type, role, configClass);
        }
    }
}

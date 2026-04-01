/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.node.condition.ConditionNodeConfig;
import com.sipc115.helix.integration.workflow.node.end.EndNodeConfig;
import com.sipc115.helix.integration.workflow.node.humanInput.HumanInputNodeConfig;
import com.sipc115.helix.integration.workflow.node.start.StartNodeConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 节点配置转换器
 * <p>
 * 用于将 Map<String, Object> 配置转换为类型安全的配置类对象。
 * 支持所有节点类型的配置转换。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class NodeConfigConverter {
    
    private final ObjectMapper objectMapper;
    
    public NodeConfigConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    public NodeConfigConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 转换节点配置
     * <p>
     * 根据节点类型将 Map 配置转换为对应的配置类对象。
     * 
     * @param nodeType 节点类型
     * @param config Map 格式的配置
     * @return 转换后的配置类对象
     * @throws IllegalArgumentException 当节点类型不支持或配置转换失败时抛出
     */
    public Object convert(DslNodeType nodeType, Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        
        return switch (nodeType) {
            case START -> convertToStartNodeConfig(config);
            case END -> convertToEndNodeConfig(config);
            case CONDITION -> convertToConditionNodeConfig(config);
            case HUMAN_INPUT -> convertToHumanInputNodeConfig(config);
            default -> throw new IllegalArgumentException("Unsupported node type: " + nodeType);
        };
    }
    
    /**
     * 转换为开始节点配置
     * 
     * @param config Map 格式的配置
     * @return 开始节点配置对象
     */
    public StartNodeConfig convertToStartNodeConfig(Map<String, Object> config) {
        return objectMapper.convertValue(config, StartNodeConfig.class);
    }
    
    /**
     * 转换为结束节点配置
     * 
     * @param config Map 格式的配置
     * @return 结束节点配置对象
     */
    public EndNodeConfig convertToEndNodeConfig(Map<String, Object> config) {
        return objectMapper.convertValue(config, EndNodeConfig.class);
    }
    
    /**
     * 转换为条件节点配置
     * 
     * @param config Map 格式的配置
     * @return 条件节点配置对象
     */
    public ConditionNodeConfig convertToConditionNodeConfig(Map<String, Object> config) {
        return objectMapper.convertValue(config, ConditionNodeConfig.class);
    }
    
    /**
     * 转换为人工输入节点配置
     * 
     * @param config Map 格式的配置
     * @return 人工输入节点配置对象
     */
    public HumanInputNodeConfig convertToHumanInputNodeConfig(Map<String, Object> config) {
        return objectMapper.convertValue(config, HumanInputNodeConfig.class);
    }
}

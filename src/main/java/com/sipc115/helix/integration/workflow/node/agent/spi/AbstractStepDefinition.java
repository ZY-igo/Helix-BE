/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.spi;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 抽象步骤定义类
 * <p>
 * 提供通用的步骤定义方法，减少重复代码
 *
 * @param <T> 步骤配置类型
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public abstract class AbstractStepDefinition<T extends AiFlowStepConfig> implements StepDefinition<T> {

    /**
     * 从配置中获取字符串值
     *
     * @param map 配置映射
     * @param key 键
     * @param defaultValue 默认值
     * @return 字符串值
     */
    protected String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * 从配置中获取整数值
     *
     * @param map 配置映射
     * @param key 键
     * @param defaultValue 默认值
     * @return 整数值
     */
    protected int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * 从配置中获取长整数值
     *
     * @param map 配置映射
     * @param key 键
     * @param defaultValue 默认值
     * @return 长整数值
     */
    protected long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * 从配置中获取双精度浮点数值
     *
     * @param map 配置映射
     * @param key 键
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    protected double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * 验证步骤基本信息
     *
     * @param stepConfig 步骤配置
     * @param stepIndex 步骤索引
     * @param nodeId 节点 ID
     */
    protected void validateBasicInfo(Map<String, Object> stepConfig, int stepIndex, String nodeId) {
        String id = (String) stepConfig.get("id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Step " + stepIndex + " must have an id: " + nodeId);
        }
    }
}

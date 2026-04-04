/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQ常量测试类
 * <p>
 * 测试 MqConstants 中定义的 RocketMQ 相关常量是否正确。
 * 这些常量用于消息队列的消息发送和接收配置。
 *
 * @author Helix Team
 * @since 2.0.0
 */
class MqConstantsTest {

    /**
     * 测试消息 Topic 常量
     * <p>
     * Topic 是 RocketMQ 中消息的逻辑分类单位。
     * workflow-trace-topic 用于传输工作流追踪事件。
     *
     * @see MqConstants#TOPIC_TRACE
     */
    @Test
    void testTopicConstant() {
        // 验证追踪消息的 Topic 名称
        assertEquals("workflow-trace-topic", MqConstants.TOPIC_TRACE);
    }

    /**
     * 测试消息 Tag 常量
     * <p>
     * Tag 是 Topic 下的子分类，用于更细粒度的消息过滤：
     * - TAG_WORKFLOW: 工作流级别的事件标签
     * - TAG_NODE: 节点级别的事件标签
     *
     * @see MqConstants#TAG_WORKFLOW
     * @see MqConstants#TAG_NODE
     */
    @Test
    void testTagConstants() {
        // 验证工作流事件标签
        assertEquals("workflow", MqConstants.TAG_WORKFLOW);
        // 验证节点事件标签
        assertEquals("node", MqConstants.TAG_NODE);
    }

    /**
     * 测试 MQ 常量值不为 null
     */
    @Test
    void testMqValuesAreNotNull() {
        assertNotNull(MqConstants.TOPIC_TRACE);
        assertNotNull(MqConstants.TAG_WORKFLOW);
        assertNotNull(MqConstants.TAG_NODE);
    }

    /**
     * 测试 MQ 常量值不为空
     */
    @Test
    void testMqValuesAreNotEmpty() {
        assertFalse(MqConstants.TOPIC_TRACE.isEmpty());
        assertFalse(MqConstants.TAG_WORKFLOW.isEmpty());
        assertFalse(MqConstants.TAG_NODE.isEmpty());
    }
}
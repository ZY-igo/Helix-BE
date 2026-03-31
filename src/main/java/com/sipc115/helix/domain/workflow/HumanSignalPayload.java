/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 人工信号有效载荷类
 * <p>
 * 用于表示向工作流发送的人工输入信号的数据结构，包含节点 ID 和人工输入数据。
 * 当工作流执行到需要人工干预的节点时，通过此数据结构传递用户输入。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class HumanSignalPayload implements Serializable {
    /**
     * 节点 ID
     * <p>
     * 标识需要人工输入的节点，确保信号被正确路由到对应的节点。
     */
    private String nodeId;
    
    /**
     * 人工输入数据
     * <p>
     * 包含用户输入的具体数据，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> payload = new HashMap<>();
}

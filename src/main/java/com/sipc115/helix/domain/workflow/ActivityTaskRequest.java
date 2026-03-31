/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 活动任务请求类
 * <p>
 * 用于表示 Temporal 活动任务的请求信息，包含活动操作类型、输入参数和配置信息。
 * 作为活动执行的载体，传递执行所需的所有数据。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class ActivityTaskRequest implements Serializable {
    /**
     * 活动操作类型
     * <p>
     * 标识要执行的具体活动类型，用于路由到对应的处理逻辑。
     */
    private String action;
    
    /**
     * 输入参数
     * <p>
     * 活动执行所需的输入数据，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> input = new HashMap<>();
    
    /**
     * 配置信息
     * <p>
     * 活动执行的配置参数，以键值对形式存储。
     * 默认为空 HashMap，确保始终可用。
     */
    private Map<String, Object> config = new HashMap<>();
}

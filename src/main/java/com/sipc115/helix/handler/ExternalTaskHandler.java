package com.sipc115.helix.handler;

import java.util.Map;

/**
 * 外部任务处理器接口
 * <p>
 * 定义了处理外部任务的方法，用于执行外部系统的任务并返回结果
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public interface ExternalTaskHandler {
    /**
     * 获取任务的动作名称
     * 
     * @return 动作名称
     */
    String action();
    
    /**
     * 处理外部任务
     * <p>
     * 接收输入参数和配置参数，执行任务并返回执行结果
     * </p>
     * 
     * @param input 输入参数
     * @param config 配置参数
     * @return 任务执行结果
     */
    Map<String, Object> handle(Map<String, Object> input, Map<String, Object> config);
}

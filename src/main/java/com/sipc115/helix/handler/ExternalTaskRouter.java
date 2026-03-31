/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.handler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部任务路由器
 * <p>
 * 负责根据操作类型分发外部任务到对应的处理器，实现任务的路由和分发逻辑。
 * 使用 ConcurrentHashMap 存储处理器映射，确保线程安全。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class ExternalTaskRouter {
    /**
     * 处理器映射
     * <p>
     * 存储操作类型到处理器的映射，使用 ConcurrentHashMap 确保线程安全。
     */
    private final Map<String, ExternalTaskHandler> handlers = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * <p>
     * 初始化处理器映射，将所有外部任务处理器注册到映射中。
     * 
     * @param handlers 外部任务处理器列表
     */
    public ExternalTaskRouter(List<ExternalTaskHandler> handlers) {
        // 遍历处理器列表，将每个处理器注册到映射中
        for (ExternalTaskHandler handler : handlers) {
            this.handlers.put(handler.action(), handler);
        }
    }

    /**
     * 分发任务
     * <p>
     * 根据操作类型查找对应的处理器，并调用其处理方法执行任务。
     * 
     * @param action 操作类型
     * @param input 输入参数
     * @param config 配置信息
     * @return 任务执行结果
     * @throws IllegalStateException 当找不到对应操作类型的处理器时抛出
     */
    public Map<String, Object> dispatch(String action, Map<String, Object> input, Map<String, Object> config) {
        // 根据操作类型查找处理器
        ExternalTaskHandler handler = handlers.get(action);
        
        // 如果找不到处理器，抛出异常
        if (handler == null) {
            throw new IllegalStateException("No external task handler for action=" + action);
        }
        
        // 调用处理器的处理方法执行任务
        return handler.handle(input, config);
    }
}

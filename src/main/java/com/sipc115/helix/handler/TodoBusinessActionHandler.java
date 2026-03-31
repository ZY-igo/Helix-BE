/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO 业务操作处理器
 * <p>
 * 实现了 ExternalTaskHandler 接口，用于处理 TODO_BUSINESS_ACTION 类型的外部任务。
 * 目前为占位实现，后续需要接入实际业务系统。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class TodoBusinessActionHandler implements ExternalTaskHandler {
    /**
     * 获取操作类型
     * <p>
     * 返回该处理器支持的操作类型。
     * 
     * @return 操作类型
     */
    @Override
    public String action() {
        return "TODO_BUSINESS_ACTION";
    }

    /**
     * 处理外部任务
     * <p>
     * 处理 TODO_BUSINESS_ACTION 类型的外部任务，返回处理结果。
     * 目前为占位实现，后续需要接入实际业务系统。
     * 
     * @param input 输入参数
     * @param config 配置信息
     * @return 处理结果
     */
    @Override
    public Map<String, Object> handle(Map<String, Object> input, Map<String, Object> config) {
        // TODO 接入实际业务系统，例如 HTTP / RPC / 插件 / LLM。
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("echoInput", input);
        result.put("echoConfig", config);
        return result;
    }
}

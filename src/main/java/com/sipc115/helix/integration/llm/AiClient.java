package com.sipc115.helix.integration.llm;

/**
 * AI 客户端统一接口
 * <p>
 * 支持不同 AI 提供商的切换
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
public interface AiClient {

    /**
     * 获取客户端标识（用于区分不同的 AI 服务）
     * 
     * @return 客户端名称，如 "zhipu", "azure-openai", "anthropic"
     */
    String getClientType();

    /**
     * 发送聊天请求
     * 
     * @param stage 处理阶段标识
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return AI 响应文本
     */
    String chat(String stage, String systemPrompt, String userPrompt);

    /**
     * 是否支持某个功能（可选）
     * 
     * @param feature 功能名称
     * @return 是否支持
     */
    default boolean supportsFeature(String feature) {
        return true;
    }
}

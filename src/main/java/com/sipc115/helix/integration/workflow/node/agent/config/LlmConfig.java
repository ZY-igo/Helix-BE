/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;
import java.util.Map;

/**
 * LLM 配置
 * <p>
 * 配置 AI 模型的参数，支持不同的 AI 提供商
 */
@Data
public class LlmConfig {
    
    /**
     * AI 客户端类型
     * <p>
     * 对应 AiClientFactory 中的客户端类型，如 "zhipu", "azure-openai", "anthropic"
     */
    private String clientType = "zhipu";
    
    /**
     * 模型名称
     * <p>
     * 如 "glm-4", "gpt-4o", "claude-3-opus"
     */
    private String model = "glm-4";
    
    /**
     * 温度参数
     * <p>
     * 控制生成文本的随机性，范围 0.0-1.0
     */
    private Double temperature = 0.7;
    
    /**
     * 最大生成长度
     * <p>
     * 控制生成文本的最大长度
     */
    private Integer maxTokens;
    
    /**
     * 系统提示词
     * <p>
     * 为 AI 模型提供背景信息和指令
     */
    private String systemPrompt;
    
    /**
     * 额外参数
     * <p>
     * 特定模型的额外参数
     */
    private Map<String, Object> extraParams;
}

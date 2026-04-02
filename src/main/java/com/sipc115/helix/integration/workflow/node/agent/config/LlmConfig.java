/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;
import java.util.Map;

/**
 * LLM 配置
 * <p>
 * 配置 AI 模型的参数，支持不同的 AI 提供商。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class LlmConfig {
    
    /**
     * AI 客户端类型
     * <p>
     * 对应 AiClientFactory 中的客户端类型，如 "zhipu", "azure-openai", "anthropic"
     * <p>
     * 默认值："zhipu"
     */
    private String clientType = "zhipu";
    
    /**
     * 模型名称
     * <p>
     * 如 "glm-4", "gpt-4o", "claude-3-opus"
     * <p>
     * 默认值："glm-4"
     */
    private String model = "glm-4";
    
    /**
     * 温度参数
     * <p>
     * 控制生成文本的随机性，范围 0.0-1.0
     * <p>
     * 默认值：0.7
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
     * <p>
     * 示例：
     * <pre>
     * "你是一个专业的助手，帮助用户解决问题。"
     * </pre>
     */
    private String systemPrompt;
    
    /**
     * 额外参数
     * <p>
     * 特定模型的额外参数
     * <p>
     * 示例：
     * <pre>
     * {
     *   "top_p": 0.9,
     *   "frequency_penalty": 0.1
     * }
     * </pre>
     */
    private Map<String, Object> extraParams;
}

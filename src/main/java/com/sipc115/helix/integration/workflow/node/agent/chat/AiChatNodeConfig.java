/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat;

import lombok.Data;

/**
 * AI 聊天节点配置类
 * <p>
 * 用于配置 AI 聊天节点的执行参数和行为。
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class AiChatNodeConfig {

    /**
     * 集成连接 ID
     * <p>
     * 指向 LLM 类型的集成连接，用于获取 API Key、Base URL、Model 等配置。
     */
    private Long connectionId;

    /**
     * 系统提示词
     * <p>
     * 定义 AI 助手的角色和行为。
     */
    private String systemPrompt;

    /**
     * 用户提示词
     * <p>
     * 支持表达式，可从工作流变量中动态获取。
     */
    private String userPrompt;

    /**
     * 温度参数（0.0 - 1.0）
     * <p>
     * 控制输出的随机性，值越高越有创造性。
     */
    private Double temperature;

    /**
     * 最大 Token 数
     * <p>
     * 限制 AI 响应的长度。
     */
    private Integer maxTokens;

    /**
     * 思考模式
     * <p>
     * 可选值：disabled（禁用）、enabled（启用），仅部分模型支持。
     */
    private String thinking;

    /**
     * 输出变量名
     * <p>
     * AI 响应结果存储的变量名。
     */
    private String outputVar;
}

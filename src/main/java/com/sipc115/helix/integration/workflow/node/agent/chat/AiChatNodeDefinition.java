package com.sipc115.helix.integration.workflow.node.agent.chat;

import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * AI 聊天节点定义
 * <p>
 * 定义 AI 聊天节点（AI_TASK）的元数据和配置类型。
 * <p>
 * DSL 配置结构：
 * <pre>
 * {
 *   "id": "ai-chat-node",
 *   "type": "AI_TASK",
 *   "config": {
 *     "connectionId": 1,
 *     "systemPrompt": "你是一个专业的助手",
 *     "userPrompt": "${input.question}",
 *     "temperature": 0.7,
 *     "maxTokens": 4096,
 *     "thinking": "disabled",
 *     "outputVar": "aiResponse"
 *   }
 * }
 * </pre>
 * <p>
 * 配置字段说明：
 * <ul>
 *   <li>connectionId: LLM 集成连接 ID</li>
 *   <li>systemPrompt: 系统提示词（可选，默认"You are a helpful AI assistant."）</li>
 *   <li>userPrompt: 用户提示词，支持表达式（必填）</li>
 *   <li>temperature: 温度参数 0.0-1.0（可选，默认 0.7）</li>
 *   <li>maxTokens: 最大 Token 数（可选，默认 4096）</li>
 *   <li>thinking: 思考模式 disabled/enabled（可选，默认 disabled）</li>
 *   <li>outputVar: 输出变量名（可选，默认 aiResponse）</li>
 * </ul>
 */
@Component
public class AiChatNodeDefinition implements NodeDefinition<AiChatNodeConfig> {

    @Override
    public DslNodeType type() {
        return DslNodeType.AI_TASK;
    }

    @Override
    public Class<AiChatNodeConfig> configClass() {
        return AiChatNodeConfig.class;
    }
}

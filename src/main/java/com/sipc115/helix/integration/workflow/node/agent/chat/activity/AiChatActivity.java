/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat.activity;

import io.temporal.activity.ActivityInterface;

/**
 * AI 聊天 Activity 接口
 * <p>
 * 定义了调用 LLM API 进行对话的 Activity 方法。
 * 使用 Temporal Activity 将外部 API 调用从 Workflow 线程分离出来，
 * 保证 Workflow 的确定性执行。
 *
 * <h3>设计目的：</h3>
 * <ul>
 *   <li>将非确定性操作（HTTP 调用）从 Workflow 中分离</li>
 *   <li>支持 Temporal 的重试和错误处理机制</li>
 *   <li>保证 Workflow 重放时的一致性</li>
 *   <li>实现幂等性保护，防止重复调用 LLM API</li>
 * </ul>
 *
 * <h3>幂等性保护：</h3>
 * <p>
 * 所有 chat 方法都包含 executionId、nodeId 和 retryCount 参数，
 * 用于实现幂等性检查。由于 LLM API 调用通常比较昂贵（费用和延迟），
 * 幂等性保护可以避免在 Temporal 重试时重复调用。
 *
 * <h3>执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ AiChatNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(AiChatActivity.class)
 *           └─ AiChatActivity.chat()  ← 在 Activity Worker 上执行
 *               └─ 幂等性检查
 *               └─ LlmAuthClient.chat()   ← 真正的 HTTP 调用
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see AiChatActivityImpl
 * @see io.temporal.activity.ActivityInterface
 */
@ActivityInterface
public interface AiChatActivity {

    /**
     * 调用 LLM 进行对话
     * <p>
     * 通过 LLM API 发送对话请求并获取响应。
     * 实现了幂等性保护。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionId LLM 连接配置 ID
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @param temperature 温度参数
     * @param maxTokens 最大 token 数
     * @param thinking 思考模式
     * @return LLM 的响应内容
     * @throws IllegalArgumentException 如果参数无效
     * @throws IllegalStateException 如果 API 调用失败
     */
    String chat(Long executionId, Integer retryCount, String nodeId,
                Long connectionId, String systemPrompt, String userPrompt,
                Double temperature, Integer maxTokens, String thinking);

    /**
     * 调用 LLM 进行对话（使用连接配置）
     * <p>
     * 重载方法，允许直接传入连接配置而非 connectionId。
     * 实现了幂等性保护。
     *
     * @param executionId 工作流执行 ID（用于幂等键）
     * @param retryCount 当前重试次数（用于幂等键）
     * @param nodeId 节点 ID（用于幂等键）
     * @param connectionConfig 连接配置对象
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户输入
     * @param temperature 温度参数
     * @param maxTokens 最大 token 数
     * @param thinking 思考模式
     * @return LLM 的响应内容
     */
    String chatWithConfig(Long executionId, Integer retryCount, String nodeId,
                          Object connectionConfig, String systemPrompt, String userPrompt,
                          Double temperature, Integer maxTokens, String thinking);
}
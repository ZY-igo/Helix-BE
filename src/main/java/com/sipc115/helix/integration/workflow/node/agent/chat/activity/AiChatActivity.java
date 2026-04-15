/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat.activity;

import io.temporal.activity.ActivityInterface;

import java.util.Map;

/**
 * AI 对话节点对应的 Temporal Activity 定义。
 * <p>
 * 该接口是工作流执行层与实际 LLM 调用逻辑之间的边界：
 * 一方面由 Temporal 工作流侧发起调用，另一方面由 {@link AiChatActivityImpl}
 * 负责执行具体的模型请求、Agent 工具调用、执行追踪与结果回传。
 * </p>
 * <p>
 * 当前实现支持两种运行模式：
 * </p>
 * <ul>
 *     <li>{@code chat}：普通问答模式，直接基于 systemPrompt 和 userPrompt 调用模型。</li>
 *     <li>{@code agent}：代理模式，模型以 JSON Action 的形式决定是调用工具还是结束执行。</li>
 * </ul>
 * <p>
 * 该 Activity 的返回值不仅包含最终文本响应，还可能携带分支路由信息、结构化输出、
 * Agent 运行时 memory 以及工具调用轨迹，供后续节点或执行追踪系统使用。
 * </p>
 */
@ActivityInterface
public interface AiChatActivity {

    /**
     * 执行一次 AI 对话节点。
     * <p>
     * 该方法是节点实际运行的统一入口。实现类会基于传入的节点配置判断当前应走普通
     * 对话模式还是 Agent 模式，并在执行过程中完成以下工作：
     * </p>
     * <ul>
     *     <li>按 {@code executionId + nodeId + retryCount} 组合执行幂等检查，避免重复产出结果。</li>
     *     <li>创建或复用对应的 LLM 连接客户端。</li>
     *     <li>将工作流变量、节点配置、提示词参数拼装为模型输入。</li>
     *     <li>记录节点执行追踪，并在成功或失败后回写 trace 信息。</li>
     *     <li>在 Agent 模式下，循环驱动模型输出 action，并执行允许的工具调用。</li>
     * </ul>
     *
     * @param executionId 当前工作流执行实例 ID，用于定位本次运行上下文及节点 trace。
     * @param retryCount 当前节点的重试次数，用于区分同一执行实例下的不同尝试，并参与幂等判断。
     * @param nodeId 当前 AI 对话节点 ID，用于执行追踪、节点定位以及结果缓存查询。
     * @param connectionId 已保存连接的主键 ID；当 {@code connectionConfig} 为空时，通常通过它加载连接配置。
     * @param connectionConfig 运行时直接传入的连接配置对象；如果提供，则优先基于该配置创建客户端。
     * @param variables 当前工作流上下文变量集合；在 Agent 模式下可作为可见上下文供工具读取。
     * @param nodeConfig 当前节点的完整配置，例如模式、可见变量范围、工具白名单、最大迭代次数等。
     * @param systemPrompt 系统提示词，用于定义模型的角色、约束和全局行为；在 Agent 模式下会被进一步包装。
     * @param userPrompt 用户提示词，表示本节点要完成的核心任务描述或输入内容。
     * @param temperature 模型采样温度，用于控制输出的发散程度。
     * @param maxTokens 模型单次生成的最大 token 数限制。
     * @param thinking 推理/思考开关或策略配置，按底层 LLM 客户端约定传递。
     * @return 节点执行结果，包含最终响应内容，以及可能的结构化输出、分支信息、memory 和工具调用记录。
     */
    AiChatActivityResult chat(Long executionId, Integer retryCount, String nodeId,
                              Long connectionId, Object connectionConfig, Map<String, Object> variables,
                              Map<String, Object> nodeConfig, String systemPrompt, String userPrompt,
                              Double temperature, Integer maxTokens, String thinking);
}

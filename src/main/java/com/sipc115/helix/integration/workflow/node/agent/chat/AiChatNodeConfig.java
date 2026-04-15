/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat;

import lombok.Data;

import java.util.List;

/**
 * AI 对话节点的配置对象。
 * <p>
 * 该类用于描述一个 {@code AI_TASK} 节点在 DSL / 编译 / 运行三个阶段共享的核心配置。
 * 它本身不包含行为，只承担结构化承载配置项的职责，便于上层表单、DSL 反序列化、
 * 编译器校验和运行期执行器读取。
 * </p>
 * <p>
 * 配置项大体可以分为四类：
 * </p>
 * <ul>
 *     <li>模型连接配置：例如 {@code connectionId}。</li>
 *     <li>提示词配置：例如 {@code systemPrompt}、{@code userPrompt}。</li>
 *     <li>生成参数：例如 {@code temperature}、{@code maxTokens}、{@code thinking}。</li>
 *     <li>Agent 扩展参数：例如 {@code mode}、{@code maxIterations}、工具白名单和上下文变量范围。</li>
 * </ul>
 */
@Data
public class AiChatNodeConfig {

    /**
     * 绑定的 LLM 连接主键。
     * <p>
     * 编译阶段会校验该连接是否存在，且连接类型必须为 {@code LLM}。
     * </p>
     */
    private Long connectionId;

    /**
     * 系统提示词。
     * <p>
     * 用于约束模型的角色、语气和全局行为；在 Agent 模式下还会被运行期包装成更完整的 agent 协议。
     * </p>
     */
    private String systemPrompt;

    /**
     * 用户提示词。
     * <p>
     * 这是节点实际要完成的任务描述或输入内容，也是该节点最核心的业务输入之一。
     * </p>
     */
    private String userPrompt;

    /**
     * 采样温度。
     * <p>
     * 数值越高，输出通常越发散；数值越低，输出通常越稳定。编译阶段会限制在合法范围内。
     * </p>
     */
    private Double temperature;

    /**
     * 单次模型生成允许的最大 token 数。
     */
    private Integer maxTokens;

    /**
     * 推理 / 思考相关配置。
     * <p>
     * 实际语义取决于底层 LLM 客户端的实现约定，例如可能是启用、关闭或指定某种推理模式。
     * </p>
     */
    private String thinking;

    /**
     * 节点运行模式。
     * <p>
     * 当前支持：
     * </p>
     * <ul>
     *     <li>{@code chat}：普通单轮对话。</li>
     *     <li>{@code agent}：多轮 Agent 决策和工具调用模式。</li>
     * </ul>
     */
    private String mode;

    /**
     * Agent 模式下允许的最大迭代轮次。
     * <p>
     * 普通 chat 模式下一般固定为 1；agent 模式下则控制模型“思考 + 调工具”的上限。
     * </p>
     */
    private Integer maxIterations;

    /**
     * Agent transcript 允许保留的最大上下文 token 预算。
     * <p>
     * 这是运行时的软阈值，用于在每轮决策前主动压缩 transcript，降低触发模型上下文上限的概率。
     * </p>
     */
    private Integer maxContextTokens;

    /**
     * transcript 超预算时的压缩策略。
     * <p>
     * 当前支持：
     * </p>
     * <ul>
     *     <li>{@code truncate}：直接裁剪旧消息，只保留最近若干轮。</li>
     *     <li>{@code summarize}：先尝试总结旧消息，失败时再回退到裁剪。</li>
     * </ul>
     */
    private String compactStrategy;

    /**
     * Agent 可见的上下文变量白名单。
     * <p>
     * 如果为空，通常表示默认暴露全部工作流变量；如果非空，则只暴露指定变量。
     * </p>
     */
    private List<String> contextVars;

    /**
     * Agent 允许调用的工具白名单。
     * <p>
     * 如果未显式配置，编译阶段通常会回退到系统支持的默认工具集合。
     * </p>
     */
    private List<String> toolWhitelist;

    /**
     * 当 Agent 达到最大轮次仍未 finish 时，是否直接视为失败。
     * <p>
     * 为 {@code false} 时，运行期会尝试基于已有 transcript 生成一个兜底答案。
     * </p>
     */
    private Boolean failOnMaxIterations;

    /**
     * AI 最终文本输出写回工作流变量时使用的变量名。
     * <p>
     * 例如可以把本次 AI 响应同时放入 {@code aiResponse} 或业务自定义变量中。
     * </p>
     */
    private String outputVar;
}

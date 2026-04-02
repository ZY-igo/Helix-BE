/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step;

import lombok.Data;

/**
 * 运行时策略配置
 * <p>
 * 控制 AI 任务的运行时行为，如最大轮次、超时时间等。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class RuntimePolicyConfig {

    /**
     * 最大总轮次数
     * <p>
     * 任务执行的最大轮次数，防止无限循环
     * <p>
     * 默认值：3
     */
    private Integer maxTotalRounds;

    /**
     * 超时时间（毫秒）
     * <p>
     * 任务执行的超时时间
     * <p>
     * 默认值：60000（1分钟）
     */
    private Long timeout;

    /**
     * 最大模型调用次数
     * <p>
     * 任务执行过程中最多调用 AI 模型的次数
     * <p>
     * 默认值：10
     */
    private Integer maxModelCalls;

    /**
     * 错误处理策略
     * <p>
     * 任务执行出错时的处理策略
     * <p>
     * 可选值：
     * <ul>
     *   <li>FAIL - 失败并终止任务</li>
     *   <li>RETRY - 重试执行</li>
     *   <li>CONTINUE - 继续执行</li>
     * </ul>
     * <p>
     * 默认值：FAIL
     */
    private String onError;
}

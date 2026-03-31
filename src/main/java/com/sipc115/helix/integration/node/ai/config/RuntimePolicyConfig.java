/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.node.ai.config;

import lombok.Data;

/**
 * 运行时策略配置
 */
@Data
public class RuntimePolicyConfig {

    /**
     * 全局最大循环轮次
     */
    private Integer maxTotalRounds;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout;

    /**
     * 最大 AI 模型调用次数
     */
    private Integer maxModelCalls;

    /**
     * 错误处理策略：FAIL, FALLBACK, RETRY
     */
    private String onError;
}

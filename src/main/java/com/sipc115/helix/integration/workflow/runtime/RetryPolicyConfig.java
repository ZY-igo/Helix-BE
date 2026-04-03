/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.runtime;

import lombok.Data;

/**
 * 重试策略配置类
 * <p>
 * 用于配置节点执行失败时的重试策略。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class RetryPolicyConfig {
    /**
     * 最大重试次数
     * <p>
     * 节点执行失败后最多重试的次数。
     */
    private Integer maxAttempts;
    
    /**
     * 初始重试间隔（毫秒）
     * <p>
     * 第一次重试前的等待时间。
     */
    private Long initialInterval;
    
    /**
     * 最大重试间隔（毫秒）
     * <p>
     * 重试间隔的最大值。
     */
    private Long maxInterval;
    
    /**
     * 重试间隔乘数
     * <p>
     * 每次重试后间隔的增长倍数。
     */
    private Double backoffCoefficient;
    
    /**
     * 可重试的异常类型
     * <p>
     * 哪些异常类型应该触发重试。
     */
    private java.util.List<String> retryableExceptions;
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.runtime;

import lombok.Data;
import java.util.List;

/**
 * 验证结果类
 * <p>
 * 表示 AI 验证步骤的执行结果，包含验证状态、得分、反馈和详细信息。
 * <p>
 * 注意：这是运行时数据，不是配置类。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class ValidationResult {

    /**
     * 是否通过验证
     * <p>
     * true 表示验证通过，false 表示验证失败
     */
    private Boolean passed;

    /**
     * 综合得分
     * <p>
     * 范围 0-1，1 表示完全符合要求
     */
    private Double score;

    /**
     * 总体反馈
     * <p>
     * 对验证结果的总体评价和建议
     */
    private String feedback;

    /**
     * 发现的问题列表
     * <p>
     * 验证过程中发现的具体问题
     */
    private List<ValidationIssue> issues;

    /**
     * 每个验证器的详细结果
     * <p>
     * 包含每个验证器的执行结果和得分
     */
    private List<ValidatorDetail> details;

    /**
     * 验证问题类
     * <p>
     * 表示验证过程中发现的具体问题
     */
    @Data
    public static class ValidationIssue {
        /**
         * 问题来源
         * <p>
         * 产生问题的验证器或组件
         */
        private String source;
        
        /**
         * 问题字段
         * <p>
         * 存在问题的字段名称
         */
        private String field;
        
        /**
         * 问题描述
         * <p>
         * 对问题的详细描述
         */
        private String message;
    }

    /**
     * 验证器详细结果类
     * <p>
     * 表示单个验证器的执行结果
     */
    @Data
    public static class ValidatorDetail {
        /**
         * 验证器名称
         * <p>
         * 执行验证的验证器名称
         */
        private String validator;
        
        /**
         * 是否通过
         * <p>
         * true 表示验证通过，false 表示验证失败
         */
        private Boolean passed;
        
        /**
         * 得分
         * <p>
         * 该验证器的评分，范围 0-1
         */
        private Double score;
        
        /**
         * 反馈
         * <p>
         * 该验证器的详细反馈信息
         */
        private String feedback;
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent.runtime;

import lombok.Data;
import java.util.List;

/**
 * 验证结果（运行时数据，非配置）
 */
@Data
public class ValidationResult {

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 综合得分（0-1）
     */
    private Double score;

    /**
     * 总体反馈
     */
    private String feedback;

    /**
     * 发现的问题列表
     */
    private List<ValidationIssue> issues;

    /**
     * 每个验证器的详细结果
     */
    private List<ValidatorDetail> details;

    @Data
    public static class ValidationIssue {
        private String source;
        private String field;
        private String message;
    }

    @Data
    public static class ValidatorDetail {
        private String validator;
        private Boolean passed;
        private Double score;
        private String feedback;
    }
}

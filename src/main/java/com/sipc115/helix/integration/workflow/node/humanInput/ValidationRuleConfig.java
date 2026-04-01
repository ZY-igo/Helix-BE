/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import lombok.Data;

/**
 * 验证规则配置类
 * <p>
 * 用于配置表单字段的验证规则。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class ValidationRuleConfig {
    /**
     * 规则类型
     * <p>
     * 验证规则的类型，如 "required"、"email"、"minLength" 等。
     */
    private String type;
    
    /**
     * 规则参数
     * <p>
     * 验证规则的参数，如最小长度、最大长度等。
     */
    private Object value;
    
    /**
     * 错误提示信息
     * <p>
     * 验证失败时的错误提示信息。
     */
    private String message;
}

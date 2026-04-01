/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.humanInput;

import lombok.Data;

import java.util.List;

/**
 * 表单字段配置类
 * <p>
 * 用于配置表单中的单个字段。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class FormFieldConfig {
    /**
     * 字段名称
     * <p>
     * 字段的唯一标识符。
     */
    private String name;
    
    /**
     * 字段标签
     * <p>
     * 字段的显示名称。
     */
    private String label;
    
    /**
     * 字段类型
     * <p>
     * 字段的类型，如 "text"、"select"、"checkbox" 等。
     */
    private String type;
    
    /**
     * 默认值
     * <p>
     * 字段的默认值。
     */
    private Object defaultValue;
    
    /**
     * 是否必填
     * <p>
     * 字段是否为必填项。
     */
    private Boolean required;
    
    /**
     * 验证规则
     * <p>
     * 字段的验证规则。
     */
    private List<ValidationRuleConfig> validationRules;
    
    /**
     * 选项列表
     * <p>
     * 当字段类型为 "select" 或 "radio" 时的选项列表。
     */
    private List<OptionConfig> options;
    
    /**
     * 字段描述
     * <p>
     * 字段的描述信息。
     */
    private String description;
}

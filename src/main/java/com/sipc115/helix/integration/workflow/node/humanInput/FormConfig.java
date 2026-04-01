/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import lombok.Data;

import java.util.List;

/**
 * 表单配置类
 * <p>
 * 用于配置人工输入节点的表单结构。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class FormConfig {
    /**
     * 表单字段列表
     * <p>
     * 表单中包含的字段定义。
     */
    private List<FormFieldConfig> fields;
    
    /**
     * 提交按钮文本
     * <p>
     * 表单提交按钮的显示文本。
     */
    private String submitText;
    
    /**
     * 取消按钮文本
     * <p>
     * 表单取消按钮的显示文本。
     */
    private String cancelText;
}

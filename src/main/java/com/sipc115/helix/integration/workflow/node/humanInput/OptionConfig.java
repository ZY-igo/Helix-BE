/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import lombok.Data;

/**
 * 选项配置类
 * <p>
 * 用于配置表单字段的选项。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class OptionConfig {
    /**
     * 选项值
     * <p>
     * 选项的实际值。
     */
    private Object value;
    
    /**
     * 选项标签
     * <p>
     * 选项的显示文本。
     */
    private String label;
    
    /**
     * 是否默认选中
     * <p>
     * 选项是否默认被选中。
     */
    private Boolean selected;
}

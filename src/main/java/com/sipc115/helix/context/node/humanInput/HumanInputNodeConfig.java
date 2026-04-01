/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.humanInput;

import lombok.Data;

/**
 * 人工输入节点配置类
 * <p>
 * 用于配置人工输入节点的执行参数和行为。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class HumanInputNodeConfig {
    /**
     * 表单定义
     * <p>
     * 人工输入的表单配置。
     */
    private FormConfig form;
    
    /**
     * 表单标题
     * <p>
     * 人工输入表单的标题。
     */
    private String title;
    
    /**
     * 描述信息
     * <p>
     * 人工输入表单的描述信息。
     */
    private String description;
    
    /**
     * 超时配置（毫秒）
     * <p>
     * 人工输入的最大允许时间。
     */
    private Long timeout;
    
    /**
     * 输出变量名
     * <p>
     * 人工输入结果存储的变量名。
     */
    private String outputVar;
}

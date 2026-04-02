/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Validate;

import lombok.Data;
import java.util.List;

/**
 * 验证器配置
 * <p>
 * 定义验证器的配置信息，用于验证内容是否符合要求。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class ValidatorConfig {

    /**
     * 验证器类型
     * <p>
     * 验证器的类型，如 "format"、"content"、"logic" 等
     */
    private String kind;

    /**
     * 验证提示词
     * <p>
     * 用于指导 AI 进行验证的提示词
     * <p>
     * 示例：
     * <pre>
     * "请验证以下内容是否符合格式要求：\n{{input}}"
     * </pre>
     */
    private String prompt;

    /**
     * Schema 引用
     * <p>
     * 用于验证的 Schema 引用，如 JSON Schema
     */
    private String schemaRef;

    /**
     * 字段列表
     * <p>
     * 要验证的字段列表
     */
    private List<String> fields;
}

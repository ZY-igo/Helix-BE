/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent.config;

import lombok.Data;
import java.util.List;

/**
 * 验证器配置
 */
@Data
public class ValidatorConfig {

    /**
     * 验证器类型：JSON_SCHEMA, REQUIRED_FIELDS, LLM_JUDGE
     */
    private String kind;

    /**
     * Schema 引用（JSON_SCHEMA 必填）
     */
    private String schemaRef;

    /**
     * 必填字段列表（REQUIRED_FIELDS 必填）
     */
    private List<String> fields;

    /**
     * LLM 判断提示词（LLM_JUDGE 必填）
     */
    private String prompt;
}

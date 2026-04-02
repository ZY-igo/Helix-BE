/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Generate;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生成步骤配置
 * <p>
 * 用于配置 AI 生成步骤，定义如何使用 AI 模型生成内容。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GenerateStepConfig extends AiFlowStepConfig {

    /**
     * 提示词模板
     * <p>
     * AI 生成内容的提示词模板，可以包含变量占位符
     * <p>
     * 示例：
     * <pre>
     * "请根据以下问题生成详细回答：{{query}}"
     * </pre>
     */
    private String promptTemplate;

    /**
     * 模型名称（可选）
     * <p>
     * 指定使用的 AI 模型名称，如 "glm-4"、"gpt-4o" 等
     * <p>
     * 默认值："glm-4"
     */
    private String model;

    /**
     * 温度参数（可选）
     * <p>
     * 控制生成文本的随机性，范围 0.0-1.0
     * <p>
     * 默认值：0.7
     */
    private Double temperature;

    /**
     * 最大 Token 数（可选）
     * <p>
     * 控制生成文本的最大长度
     */
    private Integer maxTokens;

    /**
     * 输出格式类型（可选）：null 或 "json"
     * <p>
     * 指定生成内容的格式，设置为 "json" 时会生成 JSON 格式的内容
     */
    private String responseFormatType;

    /**
     * Schema 引用（可选）
     * <p>
     * 当 responseFormatType 为 "json" 时，指定 JSON Schema 的引用
     */
    private String schemaRef;

    /**
     * 输出变量名
     * <p>
     * 生成结果存储的变量名，可在后续步骤中使用
     */
    private String outputVar;
}

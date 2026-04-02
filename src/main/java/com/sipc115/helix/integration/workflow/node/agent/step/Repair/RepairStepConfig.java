/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Repair;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修复步骤配置
 * <p>
 * 用于配置 AI 修复步骤，定义如何根据反馈修复内容。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RepairStepConfig extends AiFlowStepConfig {

    /**
     * 输入变量名
     * <p>
     * 要修复的内容所在的变量名
     */
    private String input;

    /**
     * 反馈信息
     * <p>
     * 用于指导修复的反馈信息
     */
    private String feedback;

    /**
     * 修复提示词模板
     * <p>
     * 用于指导 AI 进行修复的提示词模板
     * <p>
     * 示例：
     * <pre>
     * "请根据以下反馈修复内容：\n反馈：{{feedback}}\n内容：{{input}}"
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
     * 输出变量名
     * <p>
     * 修复结果存储的变量名，可在后续步骤中使用
     */
    private String outputVar;
}

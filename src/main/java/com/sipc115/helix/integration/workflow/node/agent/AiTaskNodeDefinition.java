/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent;

import com.sipc115.helix.integration.workflow.node.agent.config.AiTaskConfig;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * AI 任务节点定义
 * <p>
 * 定义 AI 任务节点（AI_TASK）的元数据和配置类型。
 * AI 任务节点用于执行 AI 微流程，支持生成、验证、修复、条件分支、循环等多种步骤类型。
 * <p>
 * DSL 配置结构：
 * <pre>
 * {
 *   "id": "generate-report",
 *   "type": "AI_TASK",
 *   "config": {
 *     "inputs": {
 *       "topic": "${workflowInput.topic}",
 *       "style": "professional"
 *     },
 *     "vars": {
 *       "maxLength": 1000,
 *       "language": "zh"
 *     },
 *     "flow": [
 *       {
 *         "type": "GENERATE",
 *         "id": "step1",
 *         "prompt": {
 *           "template": "请根据主题 {{topic}} 生成一份报告，风格为 {{style}}"
 *         },
 *         "model": "glm-4",
 *         "temperature": 0.7,
 *         "output": {
 *           "var": "reportContent"
 *         }
 *       },
 *       {
 *         "type": "VALIDATE",
 *         "id": "step2",
 *         "input": "${reportContent}",
 *         "validators": [
 *           {
 *             "kind": "length",
 *             "max": 2000,
 *             "fields": ["content"]
 *           },
 *           {
 *             "kind": "format",
 *             "schema": "report-schema",
 *             "prompt": "验证报告格式是否符合规范"
 *           }
 *         ],
 *         "output": {
 *           "var": "validationResult"
 *         }
 *       },
 *       {
 *         "type": "IF",
 *         "id": "step3",
 *         "condition": "validationResult.passed",
 *         "then": [
 *           {
 *             "type": "RETURN",
 *             "id": "step3-success",
 *             "result": {
 *               "content": "${reportContent}",
 *               "status": "success"
 *             }
 *           }
 *         ],
 *         "else": [
 *           {
 *             "type": "REPAIR",
 *             "id": "step3-repair",
 *             "input": "${reportContent}",
 *             "feedback": "${validationResult.errors}",
 *             "prompt": {
 *               "template": "请根据反馈修改报告：{{feedback}}"
 *             },
 *             "model": "glm-4",
 *             "output": {
 *               "var": "repairedReport"
 *             }
 *           }
 *         ]
 *       },
 *       {
 *         "type": "LOOP_WHILE",
 *         "id": "step4",
 *         "condition": "quality < 0.9",
 *         "maxRounds": 3,
 *         "body": [
 *           {
 *             "type": "GENERATE",
 *             "id": "step4-improve",
 *             "prompt": {
 *               "template": "请改进报告质量"
 *             },
 *             "output": {
 *               "var": "improvedReport"
 *             }
 *           }
 *         ]
 *       },
 *       {
 *         "type": "RETURN",
 *         "id": "final-return",
 *         "result": {
 *           "content": "${reportContent}",
 *           "quality": "${quality}"
 *         }
 *       }
 *     ],
 *     "runtimePolicy": {
 *       "maxRounds": 5,
 *       "timeout": 60000,
 *       "maxModelCalls": 10,
 *       "onError": "FAIL"
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * 配置字段说明：
 * <ul>
 *   <li>inputs: 输入参数映射
 *     <ul>
 *       <li>Key: 变量名，Value: Aviator 表达式或常量值</li>
 *       <li>用于将工作流上下文中的数据映射到 AI 任务中</li>
 *     </ul>
 *   </li>
 *   <li>vars: 中间变量声明
 *     <ul>
 *       <li>Key: 变量名，Value: 初始值</li>
 *       <li>用于在 AI 任务执行过程中存储中间结果</li>
 *     </ul>
 *   </li>
 *   <li>flow: 微流程步骤列表（按顺序执行）
 *     <ul>
 *       <li>GENERATE: AI 生成步骤，调用大模型生成内容</li>
 *       <li>VALIDATE: 验证步骤，验证内容是否符合规范</li>
 *       <li>REPAIR: 修复步骤，根据反馈修复内容</li>
 *       <li>IF: 条件分支，根据条件执行不同分支</li>
 *       <li>LOOP_WHILE: 循环步骤，条件满足时重复执行</li>
 *       <li>RETURN: 返回步骤，结束 AI 任务并返回结果</li>
 *     </ul>
 *   </li>
 *   <li>runtimePolicy: 运行时策略（可选）
 *     <ul>
 *       <li>maxRounds: 最大执行轮数（默认 3）</li>
 *       <li>timeout: 超时时间（毫秒，默认 60000）</li>
 *       <li>maxModelCalls: 最大模型调用次数（默认 10）</li>
 *       <li>onError: 错误处理策略，FAIL 或 CONTINUE（默认 FAIL）</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * 执行行为：
 * <ul>
 *   <li>解析 inputs 表达式，将工作流上下文数据映射到 AI 任务变量</li>
 *   <li>按顺序执行 flow 中的步骤</li>
 *   <li>支持步骤间的数据传递（通过 output.var 和变量引用）</li>
 *   <li>执行 RETURN 步骤后，将结果返回给工作流</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiTaskNodeDefinition implements NodeDefinition<AiTaskConfig> {

    /**
     * 获取节点类型
     *
     * @return AI_TASK 节点类型枚举
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.AI_TASK;
    }

    /**
     * 获取配置类
     *
     * @return AiTaskConfig 配置类
     */
    @Override
    public Class<AiTaskConfig> configClass() {
        return AiTaskConfig.class;
    }
}

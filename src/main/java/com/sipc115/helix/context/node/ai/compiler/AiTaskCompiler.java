/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.ai.compiler;

import com.sipc115.helix.context.node.ai.config.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 任务编译器类
 * <p>
 * 将 DSL 配置编译成可执行的 AI 任务配置对象。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class AiTaskCompiler {

    /**
     * 编译 DSL 配置为 AI 任务配置对象
     * <p>
     * 将输入的 DSL 配置映射转换为 AiTaskConfig 对象。
     * 
     * @param dslConfig DSL 配置映射
     * @return 编译后的 AiTaskConfig 对象
     */
    public AiTaskConfig compile(Map<String, Object> dslConfig) {
        // 解析输入配置
        Map<String, String> inputs = parseInputs(dslConfig);
        
        // 解析变量配置
        Map<String, Object> vars = parseVars(dslConfig);
        
        // 解析流程步骤
        List<AiFlowStepConfig> flow = parseFlow(dslConfig);
        
        // 解析运行时策略
        RuntimePolicyConfig runtimePolicy = parseRuntimePolicy(dslConfig);
        
        // 构建并返回 AI 任务配置
        AiTaskConfig config = new AiTaskConfig();
        config.setInputs(inputs);
        config.setVars(vars);
        config.setFlow(flow);
        config.setRuntimePolicy(runtimePolicy);
        return config;
    }

    /**
     * 解析输入配置
     * 
     * @param dslConfig DSL 配置映射
     * @return 输入配置映射
     */
    private Map<String, String> parseInputs(Map<String, Object> dslConfig) {
        // TODO: 实现输入配置解析逻辑
        return (Map<String, String>) dslConfig.getOrDefault("inputs", Map.of());
    }

    /**
     * 解析变量配置
     * 
     * @param dslConfig DSL 配置映射
     * @return 变量配置映射
     */
    private Map<String, Object> parseVars(Map<String, Object> dslConfig) {
        // TODO: 实现变量配置解析逻辑
        return (Map<String, Object>) dslConfig.getOrDefault("vars", Map.of());
    }

    /**
     * 解析流程步骤
     * 
     * @param dslConfig DSL 配置映射
     * @return 流程步骤配置列表
     */
    private List<AiFlowStepConfig> parseFlow(Map<String, Object> dslConfig) {
        List<AiFlowStepConfig> flow = new ArrayList<>();
        List<Map<String, Object>> steps = (List<Map<String, Object>>) dslConfig.getOrDefault("flow", new ArrayList<>());
        
        for (Map<String, Object> stepConfig : steps) {
            String type = (String) stepConfig.get("type");
            switch (type) {
                case "GENERATE":
                    flow.add(parseGenerateStep(stepConfig));
                    break;
                case "VALIDATE":
                    flow.add(parseValidateStep(stepConfig));
                    break;
                case "REPAIR":
                    flow.add(parseRepairStep(stepConfig));
                    break;
                case "IF":
                    flow.add(parseIfStep(stepConfig));
                    break;
                case "LOOP_WHILE":
                    flow.add(parseLoopWhileStep(stepConfig));
                    break;
                case "RETURN":
                    flow.add(parseReturnStep(stepConfig));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported step type: " + type);
            }
        }
        
        return flow;
    }

    /**
     * 解析生成步骤
     * 
     * @param stepConfig 步骤配置映射
     * @return 生成步骤配置对象
     */
    private GenerateStepConfig parseGenerateStep(Map<String, Object> stepConfig) {
        // TODO: 实现生成步骤解析逻辑
        GenerateStepConfig config = new GenerateStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("GENERATE");
        
        // 解析提示词模板
        Map<String, Object> promptConfig = (Map<String, Object>) stepConfig.get("prompt");
        if (promptConfig != null) {
            config.setPromptTemplate((String) promptConfig.get("template"));
        }
        
        config.setModel((String) stepConfig.getOrDefault("model", "glm-4"));
        config.setTemperature((Double) stepConfig.getOrDefault("temperature", 0.7));
        
        // 解析输出变量
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }
        
        return config;
    }

    /**
     * 解析验证步骤
     * 
     * @param stepConfig 步骤配置映射
     * @return 验证步骤配置对象
     */
    private ValidateStepConfig parseValidateStep(Map<String, Object> stepConfig) {
        // TODO: 实现验证步骤解析逻辑
        ValidateStepConfig config = new ValidateStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("VALIDATE");
        config.setInput((String) stepConfig.get("input"));
        
        // 解析验证器列表
        List<Map<String, Object>> validatorConfigs = (List<Map<String, Object>>) stepConfig.get("validators");
        List<ValidatorConfig> validators = new ArrayList<>();
        
        for (Map<String, Object> validatorConfig : validatorConfigs) {
            ValidatorConfig validator = new ValidatorConfig();
            validator.setKind((String) validatorConfig.get("kind"));
            validator.setPrompt((String) validatorConfig.get("prompt"));
            validator.setSchemaRef((String) validatorConfig.get("schema"));
            
            // 解析字段列表
            if (validatorConfig.get("fields") instanceof String) {
                // TODO: 解析字符串形式的字段列表
            } else if (validatorConfig.get("fields") instanceof List) {
                validator.setFields((List<String>) validatorConfig.get("fields"));
            }
            
            validators.add(validator);
        }
        
        config.setValidators(validators);
        
        // 解析输出变量
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }
        
        return config;
    }

    /**
     * 解析修复步骤
     * 
     * @param stepConfig 步骤配置映射
     * @return 修复步骤配置对象
     */
    private RepairStepConfig parseRepairStep(Map<String, Object> stepConfig) {
        // TODO: 实现修复步骤解析逻辑
        RepairStepConfig config = new RepairStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("REPAIR");
        config.setInput((String) stepConfig.get("input"));
        config.setFeedback((String) stepConfig.get("feedback"));
        
        // 解析提示词模板
        Map<String, Object> promptConfig = (Map<String, Object>) stepConfig.get("prompt");
        if (promptConfig != null) {
            config.setPromptTemplate((String) promptConfig.get("template"));
        }
        
        config.setModel((String) stepConfig.getOrDefault("model", "glm-4"));
        config.setTemperature((Double) stepConfig.getOrDefault("temperature", 0.7));
        
        // 解析输出变量
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }
        
        return config;
    }

    /**
     * 解析条件分支步骤
     * 
     * @param stepConfig 步骤配置映射
     * @return 条件分支步骤配置对象
     */
    private IfStepConfig parseIfStep(Map<String, Object> stepConfig) {
        // TODO: 实现条件分支步骤解析逻辑
        IfStepConfig config = new IfStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("IF");
        config.setCondition((String) stepConfig.get("condition"));
        
        // 解析 then 分支
        List<Map<String, Object>> thenSteps = (List<Map<String, Object>>) stepConfig.getOrDefault("then", new ArrayList<>());
        config.setThenSteps(parseSubFlow(thenSteps));
        
        // 解析 else 分支
        List<Map<String, Object>> elseSteps = (List<Map<String, Object>>) stepConfig.getOrDefault("else", new ArrayList<>());
        config.setElseSteps(parseSubFlow(elseSteps));
        
        return config;
    }

    /**
     * 解析循环步骤
     * 
     * @param stepConfig 步骤配置映射
     * @return 循环步骤配置对象
     */
    private LoopWhileStepConfig parseLoopWhileStep(Map<String, Object> stepConfig) {
        // TODO: 实现循环步骤解析逻辑
        LoopWhileStepConfig config = new LoopWhileStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("LOOP_WHILE");
        config.setCondition((String) stepConfig.get("condition"));
        config.setMaxRounds((Integer) stepConfig.get("maxRounds"));
        
        // 解析循环体
        List<Map<String, Object>> bodySteps = (List<Map<String, Object>>) stepConfig.get("body");
        config.setBody(parseSubFlow(bodySteps));
        
        return config;
    }

    /**
     * 解析返回步骤
     * 
     * @param stepConfig 步骤配置映射
     * @return 返回步骤配置对象
     */
    private ReturnStepConfig parseReturnStep(Map<String, Object> stepConfig) {
        // TODO: 实现返回步骤解析逻辑
        ReturnStepConfig config = new ReturnStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("RETURN");
        config.setResult((Map<String, Object>) stepConfig.get("result"));
        
        return config;
    }

    /**
     * 解析子流程
     * 
     * @param steps 步骤配置列表
     * @return 子流程步骤配置列表
     */
    private List<AiFlowStepConfig> parseSubFlow(List<Map<String, Object>> steps) {
        List<AiFlowStepConfig> subFlow = new ArrayList<>();
        
        for (Map<String, Object> stepConfig : steps) {
            String type = (String) stepConfig.get("type");
            switch (type) {
                case "GENERATE":
                    subFlow.add(parseGenerateStep(stepConfig));
                    break;
                case "VALIDATE":
                    subFlow.add(parseValidateStep(stepConfig));
                    break;
                case "REPAIR":
                    subFlow.add(parseRepairStep(stepConfig));
                    break;
                case "IF":
                    subFlow.add(parseIfStep(stepConfig));
                    break;
                case "LOOP_WHILE":
                    subFlow.add(parseLoopWhileStep(stepConfig));
                    break;
                case "RETURN":
                    subFlow.add(parseReturnStep(stepConfig));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported step type: " + type);
            }
        }
        
        return subFlow;
    }

    /**
     * 解析运行时策略
     * 
     * @param dslConfig DSL 配置映射
     * @return 运行时策略配置对象
     */
    private RuntimePolicyConfig parseRuntimePolicy(Map<String, Object> dslConfig) {
        // TODO: 实现运行时策略解析逻辑
        RuntimePolicyConfig config = new RuntimePolicyConfig();
        Map<String, Object> policyConfig = (Map<String, Object>) dslConfig.getOrDefault("runtimePolicy", Map.of());
        
        config.setMaxTotalRounds((Integer) policyConfig.getOrDefault("maxRounds", 3));
        config.setTimeout((Long) policyConfig.getOrDefault("timeout", 60000L));
        config.setMaxModelCalls((Integer) policyConfig.getOrDefault("maxModelCalls", 10));
        config.setOnError((String) policyConfig.getOrDefault("onError", "FAIL"));
        
        return config;
    }
}

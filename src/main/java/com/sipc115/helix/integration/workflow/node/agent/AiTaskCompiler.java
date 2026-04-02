/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent;

import com.sipc115.helix.expression.ExpressionEngine;
import com.sipc115.helix.integration.workflow.node.agent.config.*;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 任务编译器类
 * <p>
 * 负责将 AI 任务的 DSL 配置编译为可执行的 AiTaskConfig，并进行验证和优化。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiTaskCompiler implements NodeCompiler {

    private static final Logger logger = LoggerFactory.getLogger(AiTaskCompiler.class);

    @Autowired
    private ExpressionEngine expressionEngine;

    @Override
    public DslNodeType supportType() {
        return DslNodeType.AI_TASK;
    }

    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        logger.info("Validating AI task node: {}", source.getId());
        
        Map<String, Object> config = source.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("AI task node must have config: " + source.getId());
        }

        // 验证流程配置
        validateFlow(config, source.getId());
        
        // 验证 LLM 配置
        validateLlmConfig(config, source.getId());
        
        // 验证表达式语法
        validateExpressions(config, context, source.getId());
        
        // 验证运行时策略
        validateRuntimePolicy(config, source.getId());
        
        logger.info("AI task node validation completed: {}", source.getId());
    }

    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        logger.info("Compiling AI task node: {}", source.getId());
        
        AiTaskConfig aiTaskConfig = compile(source.getConfig());

        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setAction("AI_TASK");

        Map<String, Object> compiledConfig = new HashMap<>();
        compiledConfig.put("aiTaskConfig", aiTaskConfig);
        compiledConfig.putAll(source.getConfig());

        node.setConfig(compiledConfig);
        
        logger.info("AI task node compilation completed: {}", source.getId());
        return node;
    }

    /**
     * 编译 AI 任务配置
     * 
     * @param dslConfig DSL 配置
     * @return 编译后的 AiTaskConfig
     */
    public AiTaskConfig compile(Map<String, Object> dslConfig) {
        Map<String, String> inputs = parseInputs(dslConfig);
        Map<String, Object> vars = parseVars(dslConfig);
        List<AiFlowStepConfig> flow = parseFlow(dslConfig);
        RuntimePolicyConfig runtimePolicy = parseRuntimePolicy(dslConfig);
        LlmConfig llmConfig = parseLlmConfig(dslConfig);

        AiTaskConfig config = new AiTaskConfig();
        config.setInputs(inputs);
        config.setVars(vars);
        config.setFlow(flow);
        config.setRuntimePolicy(runtimePolicy);
        config.setLlmConfig(llmConfig);
        return config;
    }

    /**
     * 验证流程配置
     * 
     * @param config DSL 配置
     * @param nodeId 节点 ID
     */
    private void validateFlow(Map<String, Object> config, String nodeId) {
        Object flow = config.get("flow");
        if (flow == null || !(flow instanceof List)) {
            throw new IllegalArgumentException("AI task node must have a flow configuration: " + nodeId);
        }

        List<?> flowList = (List<?>) flow;
        if (flowList.isEmpty()) {
            throw new IllegalArgumentException("AI task flow cannot be empty: " + nodeId);
        }

        // 验证每个步骤
        for (int i = 0; i < flowList.size(); i++) {
            Object stepObj = flowList.get(i);
            if (!(stepObj instanceof Map)) {
                throw new IllegalArgumentException("Flow step " + i + " must be a map: " + nodeId);
            }
            
            @SuppressWarnings("unchecked")
            Map<?, ?> stepMap = (Map<?, ?>) stepObj;
            validateStep(stepMap, i, nodeId);
        }
    }

    /**
     * 验证单个步骤
     * 
     * @param stepMap 步骤配置
     * @param stepIndex 步骤索引
     * @param nodeId 节点 ID
     */
    private void validateStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        String type = (String) stepMap.get("type");
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Step " + stepIndex + " must have a type: " + nodeId);
        }
        
        String id = (String) stepMap.get("id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Step " + stepIndex + " must have an id: " + nodeId);
        }
        
        // 根据步骤类型进行特定验证
        switch (type) {
            case "GENERATE":
                validateGenerateStep(stepMap, stepIndex, nodeId);
                break;
            case "VALIDATE":
                validateValidateStep(stepMap, stepIndex, nodeId);
                break;
            case "REPAIR":
                validateRepairStep(stepMap, stepIndex, nodeId);
                break;
            case "IF":
                validateIfStep(stepMap, stepIndex, nodeId);
                break;
            case "LOOP_WHILE":
                validateLoopWhileStep(stepMap, stepIndex, nodeId);
                break;
            case "RETURN":
                validateReturnStep(stepMap, stepIndex, nodeId);
                break;
            default:
                throw new IllegalArgumentException("Unsupported step type: " + type + " at step " + stepIndex + " in " + nodeId);
        }
    }

    /**
     * 验证生成步骤
     */
    private void validateGenerateStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        Map<?, ?> promptConfig = (Map<?, ?>) stepMap.get("prompt");
        if (promptConfig == null) {
            throw new IllegalArgumentException("Generate step " + stepIndex + " must have prompt configuration: " + nodeId);
        }
        
        String template = (String) promptConfig.get("template");
        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Generate step " + stepIndex + " must have prompt template: " + nodeId);
        }
        
        Map<?, ?> outputConfig = (Map<?, ?>) stepMap.get("output");
        if (outputConfig != null) {
            String var = (String) outputConfig.get("var");
            if (var == null || var.isEmpty()) {
                throw new IllegalArgumentException("Generate step " + stepIndex + " output must have var: " + nodeId);
            }
        }
    }

    /**
     * 验证验证步骤
     */
    private void validateValidateStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        String input = (String) stepMap.get("input");
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Validate step " + stepIndex + " must have input: " + nodeId);
        }
        
        List<?> validators = (List<?>) stepMap.get("validators");
        if (validators == null || validators.isEmpty()) {
            throw new IllegalArgumentException("Validate step " + stepIndex + " must have at least one validator: " + nodeId);
        }
        
        for (int i = 0; i < validators.size(); i++) {
            Object validatorObj = validators.get(i);
            if (!(validatorObj instanceof Map)) {
                throw new IllegalArgumentException("Validator " + i + " in step " + stepIndex + " must be a map: " + nodeId);
            }
            
            Map<?, ?> validatorMap = (Map<?, ?>) validatorObj;
            String kind = (String) validatorMap.get("kind");
            if (kind == null || kind.isEmpty()) {
                throw new IllegalArgumentException("Validator " + i + " in step " + stepIndex + " must have kind: " + nodeId);
            }
        }
    }

    /**
     * 验证修复步骤
     */
    private void validateRepairStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        String input = (String) stepMap.get("input");
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Repair step " + stepIndex + " must have input: " + nodeId);
        }
        
        String feedback = (String) stepMap.get("feedback");
        if (feedback == null || feedback.isEmpty()) {
            throw new IllegalArgumentException("Repair step " + stepIndex + " must have feedback: " + nodeId);
        }
        
        Map<?, ?> promptConfig = (Map<?, ?>) stepMap.get("prompt");
        if (promptConfig != null) {
            String template = (String) promptConfig.get("template");
            if (template == null || template.isEmpty()) {
                throw new IllegalArgumentException("Repair step " + stepIndex + " prompt must have template: " + nodeId);
            }
        }
    }

    /**
     * 验证条件步骤
     */
    private void validateIfStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        String condition = (String) stepMap.get("condition");
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("If step " + stepIndex + " must have condition: " + nodeId);
        }
        
        List<?> thenSteps = (List<?>) stepMap.get("then");
        if (thenSteps == null || thenSteps.isEmpty()) {
            throw new IllegalArgumentException("If step " + stepIndex + " must have then branch: " + nodeId);
        }
    }

    /**
     * 验证循环步骤
     */
    private void validateLoopWhileStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        String condition = (String) stepMap.get("condition");
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("LoopWhile step " + stepIndex + " must have condition: " + nodeId);
        }
        
        List<?> bodySteps = (List<?>) stepMap.get("body");
        if (bodySteps == null || bodySteps.isEmpty()) {
            throw new IllegalArgumentException("LoopWhile step " + stepIndex + " must have body: " + nodeId);
        }
    }

    /**
     * 验证返回步骤
     */
    private void validateReturnStep(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        Object result = stepMap.get("result");
        if (result != null && !(result instanceof Map)) {
            throw new IllegalArgumentException("Return step " + stepIndex + " result must be a map: " + nodeId);
        }
    }

    /**
     * 验证 LLM 配置
     */
    private void validateLlmConfig(Map<String, Object> config, String nodeId) {
        Map<?, ?> llmConfigObj = (Map<?, ?>) config.get("llmConfig");
        if (llmConfigObj != null) {
            String clientType = (String) llmConfigObj.get("clientType");
            if (clientType != null && clientType.isEmpty()) {
                throw new IllegalArgumentException("LLM clientType cannot be empty: " + nodeId);
            }
            
            String model = (String) llmConfigObj.get("model");
            if (model != null && model.isEmpty()) {
                throw new IllegalArgumentException("LLM model cannot be empty: " + nodeId);
            }
            
            Object temperatureObj = llmConfigObj.get("temperature");
            if (temperatureObj instanceof Number) {
                double temperature = ((Number) temperatureObj).doubleValue();
                if (temperature < 0 || temperature > 1) {
                    throw new IllegalArgumentException("LLM temperature must be between 0 and 1: " + nodeId);
                }
            }
        }
    }

    /**
     * 验证运行时策略
     */
    private void validateRuntimePolicy(Map<String, Object> config, String nodeId) {
        Map<?, ?> policyConfig = (Map<?, ?>) config.get("runtimePolicy");
        if (policyConfig != null) {
            Object maxRoundsObj = policyConfig.get("maxRounds");
            if (maxRoundsObj instanceof Number) {
                int maxRounds = ((Number) maxRoundsObj).intValue();
                if (maxRounds < 1) {
                    throw new IllegalArgumentException("Runtime policy maxRounds must be at least 1: " + nodeId);
                }
            }
            
            Object timeoutObj = policyConfig.get("timeout");
            if (timeoutObj instanceof Number) {
                long timeout = ((Number) timeoutObj).longValue();
                if (timeout < 0) {
                    throw new IllegalArgumentException("Runtime policy timeout cannot be negative: " + nodeId);
                }
            }
            
            Object maxModelCallsObj = policyConfig.get("maxModelCalls");
            if (maxModelCallsObj instanceof Number) {
                int maxModelCalls = ((Number) maxModelCallsObj).intValue();
                if (maxModelCalls < 1) {
                    throw new IllegalArgumentException("Runtime policy maxModelCalls must be at least 1: " + nodeId);
                }
            }
        }
    }

    /**
     * 验证表达式语法
     */
    private void validateExpressions(Map<String, Object> config, CompileContext context, String nodeId) {
        // 验证 inputs 中的表达式
        Object inputsObj = config.get("inputs");
        if (inputsObj instanceof Map) {
            ((Map<?, ?>) inputsObj).forEach((key, value) -> {
                if (value instanceof String expr) {
                    // 检查是否是表达式格式
                    if (expr.startsWith("${") && expr.endsWith("}")) {
                        try {
                            // 移除 ${} 并验证表达式
                            String expression = expr.substring(2, expr.length() - 1).trim();
                            expressionEngine.compile(expression);
                            logger.debug("Validated expression: {}", expression);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid expression in input '" + key + "': " + expr + " in " + nodeId, e);
                        }
                    }
                }
            });
        }
        
        // 验证步骤中的表达式
        Object flowObj = config.get("flow");
        if (flowObj instanceof List list) {
            for (int i = 0; i < list.size(); i++) {
                Object stepObj = list.get(i);
                if (stepObj instanceof Map map) {
                    validateStepExpressions(map, i, nodeId);
                }
            }
        }
    }

    /**
     * 验证步骤中的表达式
     */
    private void validateStepExpressions(Map<?, ?> stepMap, int stepIndex, String nodeId) {
        String type = (String) stepMap.get("type");
        
        switch (type) {
            case "IF":
                String condition = (String) stepMap.get("condition");
                if (condition != null) {
                    try {
                        expressionEngine.compile(condition);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid condition expression in step " + stepIndex + ": " + condition + " in " + nodeId, e);
                    }
                }
                break;
                
            case "LOOP_WHILE":
                String loopCondition = (String) stepMap.get("condition");
                if (loopCondition != null) {
                    try {
                        expressionEngine.compile(loopCondition);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid loop condition expression in step " + stepIndex + ": " + loopCondition + " in " + nodeId, e);
                    }
                }
                break;
        }
    }

    /**
     * 解析输入参数
     */
    private Map<String, String> parseInputs(Map<String, Object> dslConfig) {
        Map<String, String> inputs = new HashMap<>();
        Object inputsObj = dslConfig.get("inputs");

        if (inputsObj instanceof Map) {
            ((Map<?, ?>) inputsObj).forEach((key, value) -> {
                if (value instanceof String) {
                    inputs.put(key.toString(), (String) value);
                } else {
                    // 如果是常量，转换为字符串
                    inputs.put(key.toString(), String.valueOf(value));
                }
            });
        }

        return inputs;
    }

    /**
     * 解析变量
     */
    private Map<String, Object> parseVars(Map<String, Object> dslConfig) {
        Object varsObj = dslConfig.get("vars");
        if (varsObj instanceof Map) {
            Map<String, Object> vars = new HashMap<>();
            ((Map<?, ?>) varsObj).forEach((key, value) -> vars.put(key.toString(), value));
            return vars;
        }
        return new HashMap<>();
    }

    /**
     * 解析流程步骤
     */
    private List<AiFlowStepConfig> parseFlow(Map<String, Object> dslConfig) {
        List<AiFlowStepConfig> flow = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>)
            dslConfig.getOrDefault("flow", new ArrayList<>());

        for (Map<String, Object> stepConfig : steps) {
            flow.add(parseStep(stepConfig));
        }

        return flow;
    }

    /**
     * 统一解析单个步骤
     */
    private AiFlowStepConfig parseStep(Map<String, Object> stepConfig) {
        String type = (String) stepConfig.get("type");
        return switch (type) {
            case "GENERATE" -> parseGenerateStep(stepConfig);
            case "VALIDATE" -> parseValidateStep(stepConfig);
            case "REPAIR" -> parseRepairStep(stepConfig);
            case "IF" -> parseIfStep(stepConfig);
            case "LOOP_WHILE" -> parseLoopWhileStep(stepConfig);
            case "RETURN" -> parseReturnStep(stepConfig);
            default -> throw new IllegalArgumentException("Unsupported step type: " + type);
        };
    }

    /**
     * 解析生成步骤
     */
    private GenerateStepConfig parseGenerateStep(Map<String, Object> stepConfig) {
        GenerateStepConfig config = new GenerateStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("GENERATE");

        // 解析提示词配置
        @SuppressWarnings("unchecked")
        Map<String, Object> promptConfig = (Map<String, Object>) stepConfig.get("prompt");
        if (promptConfig != null) {
            config.setPromptTemplate((String) promptConfig.get("template"));
            // 提示词变量暂不支持，因为 GenerateStepConfig 中没有对应的字段
        }

        config.setModel((String) stepConfig.getOrDefault("model", "glm-4"));
        config.setTemperature(getDoubleValue(stepConfig, "temperature", 0.7));

        // 解析输出变量
        @SuppressWarnings("unchecked")
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }

        return config;
    }

    /**
     * 解析验证步骤
     */
    private ValidateStepConfig parseValidateStep(Map<String, Object> stepConfig) {
        ValidateStepConfig config = new ValidateStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("VALIDATE");
        config.setInput((String) stepConfig.get("input"));

        // 解析验证器列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validatorConfigs =
            (List<Map<String, Object>>) stepConfig.getOrDefault("validators", new ArrayList<>());
        List<ValidatorConfig> validators = new ArrayList<>();

        for (Map<String, Object> validatorConfig : validatorConfigs) {
            ValidatorConfig validator = new ValidatorConfig();
            validator.setKind((String) validatorConfig.get("kind"));
            validator.setPrompt((String) validatorConfig.get("prompt"));
            validator.setSchemaRef((String) validatorConfig.get("schema"));

            // 解析字段列表
            Object fieldsObj = validatorConfig.get("fields");
            if (fieldsObj instanceof List) {
                List<String> fields = new ArrayList<>();
                ((List<?>) fieldsObj).forEach(f -> fields.add(String.valueOf(f)));
                validator.setFields(fields);
            }

            validators.add(validator);
        }

        config.setValidators(validators);

        @SuppressWarnings("unchecked")
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }

        return config;
    }

    /**
     * 解析修复步骤
     */
    private RepairStepConfig parseRepairStep(Map<String, Object> stepConfig) {
        RepairStepConfig config = new RepairStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("REPAIR");
        config.setInput((String) stepConfig.get("input"));
        config.setFeedback((String) stepConfig.get("feedback"));

        @SuppressWarnings("unchecked")
        Map<String, Object> promptConfig = (Map<String, Object>) stepConfig.get("prompt");
        if (promptConfig != null) {
            config.setPromptTemplate((String) promptConfig.get("template"));
        }

        config.setModel((String) stepConfig.getOrDefault("model", "glm-4"));
        config.setTemperature(getDoubleValue(stepConfig, "temperature", 0.7));

        @SuppressWarnings("unchecked")
        Map<String, Object> outputConfig = (Map<String, Object>) stepConfig.get("output");
        if (outputConfig != null) {
            config.setOutputVar((String) outputConfig.get("var"));
        }

        return config;
    }

    /**
     * 解析条件步骤
     */
    private IfStepConfig parseIfStep(Map<String, Object> stepConfig) {
        IfStepConfig config = new IfStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("IF");
        config.setCondition((String) stepConfig.get("condition"));

        // 解析 then 分支
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> thenSteps =
            (List<Map<String, Object>>) stepConfig.getOrDefault("then", new ArrayList<>());
        List<AiFlowStepConfig> thenFlow = new ArrayList<>();
        for (Map<String, Object> thenStep : thenSteps) {
            thenFlow.add(parseStep(thenStep));
        }
        config.setThenSteps(thenFlow);

        // 解析 else 分支
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elseSteps =
            (List<Map<String, Object>>) stepConfig.getOrDefault("else", new ArrayList<>());
        List<AiFlowStepConfig> elseFlow = new ArrayList<>();
        for (Map<String, Object> elseStep : elseSteps) {
            elseFlow.add(parseStep(elseStep));
        }
        config.setElseSteps(elseFlow);

        return config;
    }

    /**
     * 解析循环步骤
     */
    private LoopWhileStepConfig parseLoopWhileStep(Map<String, Object> stepConfig) {
        LoopWhileStepConfig config = new LoopWhileStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("LOOP_WHILE");
        config.setCondition((String) stepConfig.get("condition"));
        config.setMaxRounds(getIntValue(stepConfig, "maxRounds", 3));

        // 解析循环体
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bodySteps =
            (List<Map<String, Object>>) stepConfig.getOrDefault("body", new ArrayList<>());
        List<AiFlowStepConfig> bodyFlow = new ArrayList<>();
        for (Map<String, Object> bodyStep : bodySteps) {
            bodyFlow.add(parseStep(bodyStep));
        }
        config.setBody(bodyFlow);

        return config;
    }

    /**
     * 解析返回步骤
     */
    private ReturnStepConfig parseReturnStep(Map<String, Object> stepConfig) {
        ReturnStepConfig config = new ReturnStepConfig();
        config.setId((String) stepConfig.get("id"));
        config.setType("RETURN");

        Object resultObj = stepConfig.get("result");
        if (resultObj instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            ((Map<?, ?>) resultObj).forEach((key, value) -> result.put(key.toString(), value));
            config.setResult(result);
        } else {
            config.setResult(new HashMap<>());
        }

        return config;
    }

    /**
     * 解析子流程
     */
    private List<AiFlowStepConfig> parseSubFlow(List<Map<String, Object>> steps) {
        List<AiFlowStepConfig> subFlow = new ArrayList<>();
        for (Map<String, Object> stepConfig : steps) {
            subFlow.add(parseStep(stepConfig));
        }
        return subFlow;
    }

    /**
     * 解析运行时策略
     */
    private RuntimePolicyConfig parseRuntimePolicy(Map<String, Object> dslConfig) {
        RuntimePolicyConfig config = new RuntimePolicyConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> policyConfig =
            (Map<String, Object>) dslConfig.getOrDefault("runtimePolicy", Map.of());

        config.setMaxTotalRounds(getIntValue(policyConfig, "maxRounds", 3));
        config.setTimeout(getLongValue(policyConfig, "timeout", 60000L));
        config.setMaxModelCalls(getIntValue(policyConfig, "maxModelCalls", 10));
        config.setOnError((String) policyConfig.getOrDefault("onError", "FAIL"));

        return config;
    }
    
    /**
     * 解析 LLM 配置
     */
    private LlmConfig parseLlmConfig(Map<String, Object> dslConfig) {
        LlmConfig config = new LlmConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> llmConfigObj =
            (Map<String, Object>) dslConfig.getOrDefault("llmConfig", Map.of());

        config.setClientType((String) llmConfigObj.getOrDefault("clientType", "zhipu"));
        config.setModel((String) llmConfigObj.getOrDefault("model", "glm-4"));
        config.setTemperature(getDoubleValue(llmConfigObj, "temperature", 0.7));
        
        Object maxTokensObj = llmConfigObj.get("maxTokens");
        if (maxTokensObj instanceof Number) {
            config.setMaxTokens(((Number) maxTokensObj).intValue());
        }
        
        config.setSystemPrompt((String) llmConfigObj.get("systemPrompt"));
        
        Object extraParamsObj = llmConfigObj.get("extraParams");
        if (extraParamsObj instanceof Map) {
            Map<String, Object> extraParams = new HashMap<>();
            ((Map<?, ?>) extraParamsObj).forEach((key, value) -> extraParams.put(key.toString(), value));
            config.setExtraParams(extraParams);
        }

        return config;
    }

    // 辅助方法
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}

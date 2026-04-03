/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent;

import com.sipc115.helix.integration.expression.ExpressionEngine;
import com.sipc115.helix.integration.workflow.node.agent.spi.StepDefinition;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiTaskConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.RuntimePolicyConfig;
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
 * 使用 StepDefinition 模式自动发现和处理不同类型的步骤。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiTaskCompiler implements NodeCompiler {

    private static final Logger logger = LoggerFactory.getLogger(AiTaskCompiler.class);

    @Autowired
    private ExpressionEngine expressionEngine;

    private final Map<String, StepDefinition<? extends AiFlowStepConfig>> stepDefinitions = new HashMap<>();

    /**
     * 构造函数
     * <p>
     * 自动注入所有 StepDefinition 实现，并构建类型到定义的映射。
     *
     * @param stepDefinitions 步骤定义列表
     */
    @Autowired
    public AiTaskCompiler(List<StepDefinition<? extends AiFlowStepConfig>> stepDefinitions) {
        for (StepDefinition<? extends AiFlowStepConfig> definition : stepDefinitions) {
            String type = definition.supportedType();
            this.stepDefinitions.put(type, definition);
            logger.info("Registered StepDefinition for type: {}", type);
        }
    }

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
        
        // 获取对应的 StepDefinition
        StepDefinition<? extends AiFlowStepConfig> definition = stepDefinitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported step type: " + type + " at step " + stepIndex + " in " + nodeId);
        }
        
        // 转换为 Map<String, Object>
        @SuppressWarnings("unchecked")
        Map<String, Object> stepConfig = new HashMap<>();
        for (Map.Entry<?, ?> entry : stepMap.entrySet()) {
            if (entry.getKey() instanceof String) {
                stepConfig.put((String) entry.getKey(), entry.getValue());
            }
        }
        
        // 使用 StepDefinition 进行验证
        definition.validate(stepConfig, stepIndex, nodeId);
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
        
        if ("IF".equals(type) || "LOOP_WHILE".equals(type)) {
            String condition = (String) stepMap.get("condition");
            if (condition != null) {
                try {
                    expressionEngine.compile(condition);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid condition expression in step " + stepIndex + ": " + condition + " in " + nodeId, e);
                }
            }
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
        
        // 获取对应的 StepDefinition
        StepDefinition<? extends AiFlowStepConfig> definition = stepDefinitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported step type: " + type);
        }
        
        // 使用 StepDefinition 进行解析
        return definition.parseConfig(stepConfig);
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

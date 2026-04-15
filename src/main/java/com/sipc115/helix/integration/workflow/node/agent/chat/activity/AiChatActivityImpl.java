/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.llm.LlmAuthClient;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link AiChatActivity} 的默认实现。
 * <p>
 * 这个类是真正执行 AI 对话节点逻辑的地方，负责把工作流层传进来的节点配置、上下文变量、
 * 提示词参数和连接配置，转换为一次具体的 LLM 调用或一轮 Agent 推理过程。
 * </p>
 * <p>
 * 整体职责可以分为几块：
 * </p>
 * <ul>
 *     <li>Activity 入口编排：处理幂等检查、连接创建、异常包装和执行追踪。</li>
 *     <li>模式分发：根据节点配置决定走普通 {@code chat} 模式还是 {@code agent} 模式。</li>
 *     <li>Agent 运行时：维护 transcript、memory、工具调用记录，并驱动模型多轮决策。</li>
 *     <li>结果归档：把最终响应、结构化输出、分支信息等写回 trace，供节点恢复或后续节点使用。</li>
 * </ul>
 * <p>
 * 这里的 Agent 模式不是固定写死的流程，而是把允许使用的工具、可见上下文和结束协议
 * 通过 system prompt 告诉模型，让模型输出标准 JSON action 决定下一步行为。
 * </p>
 */
@Component
public class AiChatActivityImpl implements AiChatActivity {

    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 8_000;

    private static final int DEFAULT_RECENT_MESSAGES = 6;

    /**
     * 当前类的日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(AiChatActivityImpl.class);

    /**
     * Jackson 反序列化时复用的 Map 类型描述，用于把模型返回的 JSON 文本解析为结构化对象。
     */
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /**
     * Agent 模式下默认开放的工具列表。
     * <p>
     * 如果节点配置里没有显式指定工具白名单，就使用这组默认工具。
     * </p>
     */
    private static final List<String> DEFAULT_TOOLS = List.of("list_vars", "get_var", "set_memory", "ask_llm");

    /**
     * 连接客户端注册中心，用于根据连接 ID 或直接配置创建 LLM 客户端。
     */
    private final ConnectionClientRegistry connectionRegistry;

    /**
     * 工作流节点执行追踪服务，用于记录开始、成功、失败以及读取历史成功结果。
     */
    private final WorkflowTraceService traceService;

    /**
     * JSON 序列化 / 反序列化工具，用于构造 transcript、解析 action 和生成 trace 输出。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造 Activity 实现。
     *
     * @param connectionRegistry 连接客户端注册中心
     * @param traceService 工作流追踪服务
     * @param objectMapper Jackson 对象映射器
     */
    public AiChatActivityImpl(ConnectionClientRegistry connectionRegistry,
                              WorkflowTraceService traceService,
                              ObjectMapper objectMapper) {
        this.connectionRegistry = connectionRegistry;
        this.traceService = traceService;
        this.objectMapper = objectMapper;
    }

    /**
     * AI 对话节点的统一执行入口。
     * <p>
     * 执行顺序如下：
     * </p>
     * <ul>
     *     <li>先构造 attemptId，并尝试从 trace 中恢复已经成功的结果，避免重复执行。</li>
     *     <li>如果没有命中缓存，则启动节点追踪记录。</li>
     *     <li>创建 LLM 客户端，并根据节点模式执行普通对话或 Agent 流程。</li>
     *     <li>成功后把关键输出写入 trace；失败后记录错误并抛出运行时异常。</li>
     * </ul>
     * <p>
     * 这里把结果写入 trace 的目的不只是审计，也是为了后续幂等恢复时可以直接重建
     * {@link AiChatActivityResult}，减少重复调用模型带来的成本和不确定性。
     * </p>
     */
    @Override
    public AiChatActivityResult chat(Long executionId, Integer retryCount, String nodeId,
                                     Long connectionId, Object connectionConfig, Map<String, Object> variables,
                                     Map<String, Object> nodeConfig, String systemPrompt, String userPrompt,
                                     Double temperature, Integer maxTokens, String thinking) {
        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        AiChatActivityResult cached = checkAlreadySucceeded(attemptId);
        if (cached != null) {
            return cached;
        }

        Long traceId = startNodeTracking(executionId, nodeId);
        try {
            LlmAuthClient llmClient = createClient(connectionId, connectionConfig);
            AiChatActivityResult result = executeNode(llmClient, nodeConfig, variables, systemPrompt, userPrompt,
                    temperature, maxTokens, thinking);
            Map<String, Object> traceOutput = new LinkedHashMap<>();
            traceOutput.put("response", result.getResponse());
            traceOutput.put("mode", result.getMode());
            traceOutput.put("iterations", result.getIterations());
            traceOutput.put("toolCalls", result.getToolCalls());
            traceOutput.put("memory", result.getMemory());
            traceOutput.put("finalOutput", result.getFinalOutput());
            traceOutput.put("branchKey", result.getBranchKey());
            traceOutput.put("nextNodeId", result.getNextNodeId());
            markNodeSuccess(traceId, traceOutput);
            return result;
        } catch (Exception e) {
            log.error("AI_TASK activity failed. attemptId={}, error={}", attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("AI agent execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * 根据节点配置决定当前执行模式。
     * <p>
     * 当 {@code nodeConfig.mode} 不是 {@code agent} 时，直接调用 LLM 完成一次普通问答；
     * 否则进入多轮 Agent 执行逻辑。
     * </p>
     *
     * @param llmClient 当前节点使用的 LLM 客户端
     * @param nodeConfig 节点配置
     * @param variables 工作流变量
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param temperature 采样温度
     * @param maxTokens 最大输出 token 数
     * @param thinking 推理配置
     * @return 节点最终执行结果
     * @throws Exception 当底层模型调用或 Agent 执行失败时抛出
     */
    private AiChatActivityResult executeNode(LlmAuthClient llmClient, Map<String, Object> nodeConfig,
                                             Map<String, Object> variables, String systemPrompt, String userPrompt,
                                             Double temperature, Integer maxTokens, String thinking) throws Exception {
        String mode = stringValue(nodeConfig.get("mode"), "chat");
        if (!"agent".equalsIgnoreCase(mode)) {
            AiChatActivityResult result = new AiChatActivityResult();
            result.setMode("chat");
            result.setResponse(llmClient.chat(systemPrompt, userPrompt, temperature, maxTokens, thinking));
            result.setIterations(1);
            return result;
        }

        return runAgent(llmClient, nodeConfig, variables, systemPrompt, userPrompt, temperature, maxTokens, thinking);
    }

    /**
     * 运行 Agent 模式的主循环。
     * <p>
     * 该方法把一次节点执行包装成一个“模型决策 + 工具反馈”的循环：
     * </p>
     * <ul>
     *     <li>首先根据节点配置确定可见上下文、可用工具和最大迭代次数。</li>
     *     <li>初始化 transcript，向模型明确 JSON 输出协议和当前任务上下文。</li>
     *     <li>每一轮要求模型返回一个 action：要么调用工具，要么结束执行。</li>
     *     <li>如果是工具调用，则执行工具并把结果作为新的用户消息追加到 transcript。</li>
     *     <li>如果是 finish，则提取最终答案、结构化输出和路由信息后返回。</li>
     * </ul>
     * <p>
     * 这里的 transcript 相当于 Agent 的短期会话记忆，memory 则是显式暴露给工具写入的
     * 可变状态。二者职责不同：transcript 负责维持对话连续性，memory 负责保存结构化中间结果。
     * </p>
     *
     * @return Agent 模式执行结果
     * @throws Exception 当 JSON 解析、工具执行或模型调用发生异常时抛出
     */
    private AiChatActivityResult runAgent(LlmAuthClient llmClient, Map<String, Object> nodeConfig,
                                          Map<String, Object> variables, String systemPrompt, String userPrompt,
                                          Double temperature, Integer maxTokens, String thinking) throws Exception {
        // Agent 最多允许连续决策的轮数，超过后要么失败，要么走兜底总结。
        int maxIterations = intValue(nodeConfig.get("maxIterations"), 6);
        // 是否在达到最大轮次后直接抛错，而不是尝试生成一个可用的 fallback 答案。
        boolean failOnMaxIterations = booleanValue(nodeConfig.get("failOnMaxIterations"), false);
        // transcript 的软性上下文预算，超过后会主动压缩旧消息。
        int maxContextTokens = intValue(nodeConfig.get("maxContextTokens"), DEFAULT_MAX_CONTEXT_TOKENS);
        // transcript 压缩策略，优先支持 summarize，其次 truncate。
        String compactStrategy = stringValue(nodeConfig.get("compactStrategy"), "truncate");
        // 根据节点配置过滤出允许暴露给 Agent 的上下文变量，避免把全部工作流变量都交给模型。
        Map<String, Object> visibleContext = selectVisibleContext(variables, nodeConfig.get("contextVars"));
        // 计算当前 Agent 可使用的工具集合；如果节点没配白名单，则使用默认工具。
        List<String> tools = selectTools(nodeConfig.get("toolWhitelist"));

        // transcript 保存完整的对话历史，是后续每一轮模型决策的上下文基础。
        List<Map<String, Object>> transcript = new ArrayList<>();
        // memory 是 Agent 在执行期间可以显式写入的结构化临时记忆。
        Map<String, Object> memory = new LinkedHashMap<>();
        // 记录每次工具调用的输入输出，最终会写回结果和 trace。
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        // 保存最后一条 assistant 原始消息，便于超出轮次时做 fallback。
        String lastAssistantMessage = null;

        // 第一条 system 消息声明 Agent 协议、输出 JSON 结构和可用工具。
        transcript.add(message("system", buildAgentSystemPrompt(systemPrompt, tools)));
        // 第一条 user 消息提供当前任务和筛选后的工作流上下文。
        transcript.add(message("user", buildAgentStartPrompt(userPrompt, visibleContext)));

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            transcript = compactTranscriptIfNeeded(llmClient, transcript, userPrompt, temperature, maxTokens, thinking,
                    maxContextTokens, compactStrategy);
            // 要求模型基于当前 transcript 输出一条 JSON action，用于决定下一步行为。
            String raw;
            try {
                raw = llmClient.chatJson(transcript, temperature, maxTokens, thinking);
            } catch (Exception e) {
                if (!isContextLimitException(e)) {
                    throw e;
                }
                log.warn("Agent context limit hit. compacting transcript and retrying. iteration={}, error={}",
                        iteration, e.getMessage());
                transcript = forceCompactTranscript(llmClient, transcript, userPrompt, temperature, maxTokens, thinking,
                        compactStrategy);
                raw = llmClient.chatJson(transcript, temperature, maxTokens, thinking);
            }
            lastAssistantMessage = raw;
            // 把模型原始输出记入 transcript，保证后续轮次能看到自己之前的决策。
            transcript.add(message("assistant", raw));

            // 解析模型输出的 JSON 包装对象，期望结构里包含 action 字段。
            Map<String, Object> actionEnvelope = parseJsonMap(raw);
            Map<String, Object> action = asMap(actionEnvelope.get("action"));
            if (action == null) {
                // 如果结构不合法，直接把纠错信息回灌给模型，要求下一轮按协议重试。
                transcript.add(message("user", "Invalid action JSON. Return a valid JSON object."));
                continue;
            }

            String actionType = stringValue(action.get("type"), "");
            if ("finish".equalsIgnoreCase(actionType)) {
                // finish 表示 Agent 认为任务已经完成，此时收集最终答案、分支信息和结构化输出直接返回。
                AiChatActivityResult result = new AiChatActivityResult();
                result.setMode("agent");
                result.setIterations(iteration);
                result.setResponse(stringValue(action.get("finalAnswer"), ""));
                result.setBranchKey(blankToNull(stringValue(action.get("branchKey"), null)));
                result.setNextNodeId(blankToNull(stringValue(action.get("nextNodeId"), null)));
                result.setFinalOutput(copyMap(asMap(action.get("output"))));
                result.setMemory(memory);
                result.setToolCalls(toolCalls);
                return result;
            }

            if (!"tool".equalsIgnoreCase(actionType)) {
                // 非 finish 且非 tool 的 action 不被接受，继续要求模型按规定返回。
                transcript.add(message("user", "Unsupported action.type. Use tool or finish."));
                continue;
            }

            String toolName = stringValue(action.get("tool"), "");
            if (!tools.contains(toolName)) {
                // 即使模型返回了 tool，也必须经过白名单校验，避免调用未授权工具。
                transcript.add(message("user", "Tool not allowed: " + toolName));
                continue;
            }

            // 提取工具输入并执行工具；工具执行结果将作为下一轮决策的观察结果。
            Map<String, Object> toolInput = copyMap(asMap(action.get("input")));
            Map<String, Object> toolResult = executeTool(llmClient, toolName, toolInput, visibleContext, memory,
                    temperature, maxTokens, thinking);

            // 持久化记录这一次工具调用，便于最终返回给上层或写入 trace。
            toolCalls.add(Map.of(
                    "iteration", iteration,
                    "tool", toolName,
                    "input", toolInput,
                    "result", toolResult
            ));
            // 将工具结果伪装成新的 user 消息喂回 transcript，让模型在下一轮继续推理。
            transcript.add(message("user", "TOOL_RESULT " + toJson(toolResult)));
        }

        if (failOnMaxIterations) {
            // 某些场景要求 Agent 必须显式 finish；如果超轮次还未完成，则直接视为失败。
            throw new IllegalStateException("Agent reached maxIterations without finish");
        }

        // 未正常 finish 时，尝试基于已有 transcript 生成一个兜底答案，尽量返回可交付结果。
        AiChatActivityResult fallback = new AiChatActivityResult();
        fallback.setMode("agent");
        fallback.setIterations(maxIterations);
        fallback.setResponse(buildFallbackAnswer(llmClient, transcript, userPrompt, lastAssistantMessage, temperature, maxTokens));
        fallback.setMemory(memory);
        fallback.setToolCalls(toolCalls);
        return fallback;
    }

    /**
     * 根据工具名分派到对应的内置工具实现。
     * <p>
     * 当前工具均为同步执行，且结果统一封装为 Map 返回，便于直接回填到 transcript，
     * 也便于后续序列化到 trace 中。
     * </p>
     *
     * @param llmClient LLM 客户端，某些工具会继续发起子模型调用
     * @param toolName 工具名
     * @param input 工具输入参数
     * @param visibleContext Agent 可见的工作流上下文变量
     * @param memory Agent 运行时 memory
     * @param temperature 采样温度
     * @param maxTokens 最大输出 token 数
     * @param thinking 推理配置
     * @return 工具执行结果
     * @throws Exception 当工具内部依赖的模型调用失败时抛出
     */
    private Map<String, Object> executeTool(LlmAuthClient llmClient, String toolName, Map<String, Object> input,
                                            Map<String, Object> visibleContext, Map<String, Object> memory,
                                            Double temperature, Integer maxTokens, String thinking) throws Exception {
        return switch (toolName) {
            case "list_vars" -> toolListVars(visibleContext, input);
            case "get_var" -> toolGetVar(visibleContext, input);
            case "set_memory" -> toolSetMemory(memory, input);
            case "ask_llm" -> toolAskLlm(llmClient, input, temperature, maxTokens, thinking);
            default -> Map.of("error", "Unsupported tool: " + toolName);
        };
    }

    /**
     * 列出当前 Agent 可见上下文中的变量名。
     * <p>
     * 支持按前缀过滤，并限制返回数量，避免模型一次拿到过多变量名。
     * </p>
     */
    private Map<String, Object> toolListVars(Map<String, Object> visibleContext, Map<String, Object> input) {
        String prefix = stringValue(input.get("prefix"), "");
        int limit = intValue(input.get("limit"), 50);
        List<String> keys = visibleContext.keySet().stream()
                .filter(key -> prefix.isBlank() || key.startsWith(prefix))
                .limit(limit)
                .toList();
        return Map.of("keys", keys, "count", keys.size());
    }

    /**
     * 读取指定变量的值。
     * <p>
     * 无论变量是否存在，都会返回请求的变量名；若不存在则 {@code value} 为 {@code null}。
     * </p>
     */
    private Map<String, Object> toolGetVar(Map<String, Object> visibleContext, Map<String, Object> input) {
        String name = stringValue(input.get("name"), "");
        Object value = visibleContext.get(name);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("value", value);
        return result;
    }

    /**
     * 向 Agent 的运行时 memory 中写入一条键值。
     * <p>
     * 这里的 memory 仅在当前节点这次执行期间有效，并会在最终结果中返回，便于后续节点消费。
     * </p>
     */
    private Map<String, Object> toolSetMemory(Map<String, Object> memory, Map<String, Object> input) {
        String key = stringValue(input.get("key"), "");
        if (key.isBlank()) {
            return Map.of("error", "key is required");
        }
        Object value = input.get("value");
        memory.put(key, value);
        return Map.of("key", key, "value", value, "size", memory.size());
    }

    /**
     * 让 Agent 发起一次子 LLM 调用。
     * <p>
     * 该工具适合把某些子任务交给同一个模型或兼容模型处理，例如生成候选文案、总结资料、
     * 做局部推理等。它本质上是 Agent 能使用的“二次提问”能力。
     * </p>
     */
    private Map<String, Object> toolAskLlm(LlmAuthClient llmClient, Map<String, Object> input,
                                           Double temperature, Integer maxTokens, String thinking) {
        String prompt = stringValue(input.get("prompt"), "");
        if (prompt.isBlank()) {
            return Map.of("error", "prompt is required");
        }
        String subSystemPrompt = stringValue(input.get("systemPrompt"), "You are a specialist assistant helping another agent.");
        String response = llmClient.chat(subSystemPrompt, prompt, temperature, maxTokens, thinking);
        return Map.of("response", response);
    }

    /**
     * 构造 Agent 模式下使用的 system prompt。
     * <p>
     * 在原始业务系统提示词后追加固定协议，明确要求模型只能返回单个 JSON 对象，
     * 并约束 action 的结构和可用工具范围。
     * </p>
     */
    private String buildAgentSystemPrompt(String systemPrompt, List<String> tools) {
        return systemPrompt + "\n\n"
                + "You are operating as a workflow agent.\n"
                + "You must respond with a single JSON object.\n"
                + "Schema:\n"
                + "{\n"
                + "  \"thought\": \"short reasoning\",\n"
                + "  \"action\": {\n"
                + "    \"type\": \"tool\" | \"finish\",\n"
                + "    \"tool\": \"tool name when using tool\",\n"
                + "    \"input\": {\"tool\": \"arguments\"},\n"
                + "    \"finalAnswer\": \"final text when finishing\",\n"
                + "    \"output\": {\"structured\": \"output\"},\n"
                + "    \"branchKey\": \"optional branch key\",\n"
                + "    \"nextNodeId\": \"optional next node id\"\n"
                + "  }\n"
                + "}\n"
                + "Available tools: " + tools + ".\n"
                + "Use tools when you need more context. Finish when you are confident.";
    }

    /**
     * 构造 Agent 第一次收到的用户消息。
     * <p>
     * 这里把任务描述和筛选后的工作流上下文合并发给模型，让模型从第一轮就具备决策所需的信息。
     * </p>
     *
     * @throws JsonProcessingException 当上下文序列化失败时抛出
     */
    private String buildAgentStartPrompt(String userPrompt, Map<String, Object> visibleContext) throws JsonProcessingException {
        return "TASK:\n" + userPrompt + "\n\nWORKFLOW_CONTEXT:\n" + toJson(visibleContext);
    }

    /**
     * 根据节点配置筛选 Agent 可见的上下文变量。
     * <p>
     * 如果没有配置 {@code contextVars}，默认把全部变量暴露给 Agent；
     * 如果配置了白名单，则只返回命中的变量。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> selectVisibleContext(Map<String, Object> variables, Object contextVarsObj) {
        Map<String, Object> visibleContext = new LinkedHashMap<>();
        if (!(contextVarsObj instanceof List<?> contextVars) || contextVars.isEmpty()) {
            visibleContext.putAll(variables);
            return visibleContext;
        }

        for (Object keyObj : contextVars) {
            if (keyObj == null) {
                continue;
            }
            String key = keyObj.toString();
            if (variables.containsKey(key)) {
                visibleContext.put(key, variables.get(key));
            }
        }
        return visibleContext;
    }

    /**
     * 根据节点配置选择允许 Agent 使用的工具列表。
     * <p>
     * 如果传入了非空白名单，就只允许使用配置中的工具；否则回退到默认工具集。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private List<String> selectTools(Object toolWhitelistObj) {
        if (toolWhitelistObj instanceof List<?> toolList && !toolList.isEmpty()) {
            List<String> tools = new ArrayList<>();
            for (Object tool : toolList) {
                if (tool != null) {
                    tools.add(tool.toString());
                }
            }
            return tools;
        }
        return DEFAULT_TOOLS;
    }

    /**
     * 构造一条标准聊天消息对象，供 transcript 使用。
     */
    private Map<String, Object> message(String role, String content) {
        return Map.of("role", role, "content", content);
    }

    /**
     * 创建当前节点所需的 LLM 客户端。
     * <p>
     * 优先使用运行时直接传入的连接配置；如果没有传，则按连接 ID 查询已有连接。
     * </p>
     *
     * @throws Exception 当客户端创建失败时抛出
     */
    private LlmAuthClient createClient(Long connectionId, Object connectionConfig) throws Exception {
        if (connectionConfig != null) {
            return connectionRegistry.getOrCreateClient(null, "LLM", connectionConfig);
        }
        return connectionRegistry.getOrCreateClientByConnection(connectionId);
    }

    /**
     * 检查当前节点是否已经有成功执行结果。
     * <p>
     * 这是一个轻量级幂等恢复机制：如果 trace 中已经存在 SUCCESS 记录，就直接把 trace output
     * 还原成 {@link AiChatActivityResult}，避免在 Activity 重试或重复调度时再次调用模型。
     * </p>
     * <p>
     * 注意这里的 attemptId 仅用于统一传递和日志打印，真正查询时使用的是 executionId 和 nodeId。
     * </p>
     */
    private AiChatActivityResult checkAlreadySucceeded(String attemptId) {
        try {
            String[] parts = attemptId.split("_");
            if (parts.length < 3) {
                return null;
            }
            long executionId = Long.parseLong(parts[0]);
            String nodeId = parts[1];

            var traces = traceService.getNodeTracesByNodeId(executionId, nodeId);
            for (var trace : traces) {
                if (!Objects.equals("SUCCESS", trace.getStatus()) || trace.getOutput() == null) {
                    continue;
                }
                AiChatActivityResult result = new AiChatActivityResult();
                result.setResponse(stringValue(trace.getOutput().get("response"), ""));
                result.setMode(stringValue(trace.getOutput().get("mode"), "chat"));
                result.setIterations(intValue(trace.getOutput().get("iterations"), 1));
                result.setToolCalls(copyList(trace.getOutput().get("toolCalls")));
                result.setMemory(copyMap(asMap(trace.getOutput().get("memory"))));
                result.setFinalOutput(copyMap(asMap(trace.getOutput().get("finalOutput"))));
                result.setBranchKey(blankToNull(stringValue(trace.getOutput().get("branchKey"), null)));
                result.setNextNodeId(blankToNull(stringValue(trace.getOutput().get("nextNodeId"), null)));
                return result;
            }
        } catch (Exception e) {
            log.warn("Skip idempotency check. attemptId={}, error={}", attemptId, e.getMessage());
        }
        return null;
    }

    /**
     * 启动节点追踪记录。
     * <p>
     * 如果 trace 服务不可用，不阻断主流程，只记录告警并返回 {@code null}。
     * 后续成功 / 失败标记方法会自动跳过空 traceId。
     * </p>
     */
    private Long startNodeTracking(Long executionId, String nodeId) {
        try {
            var trace = traceService.startNodeExecution(executionId, nodeId, "AiChat", "NORMAL", 0, null, 0);
            return trace != null ? trace.getId() : null;
        } catch (Exception e) {
            log.warn("Failed to start node trace: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 把节点执行结果标记为成功，并附带输出快照。
     */
    private void markNodeSuccess(Long traceId, Map<String, Object> output) {
        if (traceId == null) {
            return;
        }
        try {
            traceService.markNodeSuccess(traceId, output);
        } catch (Exception e) {
            log.warn("Failed to mark node success: {}", e.getMessage());
        }
    }

    /**
     * 把节点执行结果标记为失败。
     */
    private void markNodeFailed(Long traceId, String errorMessage) {
        if (traceId == null) {
            return;
        }
        try {
            traceService.markNodeFailed(traceId, errorMessage, null);
        } catch (Exception e) {
            log.warn("Failed to mark node failure: {}", e.getMessage());
        }
    }

    /**
     * 在 Agent 超过最大迭代次数但又不要求直接失败时，尝试生成一个兜底答案。
     * <p>
     * 实现方式是把原始任务、完整 transcript 和最后一条 assistant 消息交给模型做一次总结，
     * 生成尽可能可交付的最终答复。如果总结过程也失败，则退化为直接返回最后一条模型消息。
     * </p>
     */
    private String buildFallbackAnswer(LlmAuthClient llmClient, List<Map<String, Object>> transcript, String userPrompt,
                                       String lastAssistantMessage, Double temperature, Integer maxTokens) {
        try {
            List<Map<String, Object>> compactedTranscript = keepRecentMessages(transcript, DEFAULT_RECENT_MESSAGES);
            String prompt = "The agent reached max iterations.\n"
                    + "Original task:\n" + userPrompt + "\n\n"
                    + "Conversation transcript:\n" + toJson(compactedTranscript) + "\n\n"
                    + "Last assistant message:\n" + stringValue(lastAssistantMessage, "") + "\n\n"
                    + "Produce the best final answer for the user.";
            return llmClient.chat("You finalize unfinished agent work.", prompt, temperature, maxTokens, "disabled");
        } catch (Exception e) {
            return stringValue(lastAssistantMessage, "");
        }
    }

    /**
     * 把模型输出解析成 Map。
     * <p>
     * 在解析前会先尝试提取真正的 JSON 对象，以兼容模型把 JSON 包在 Markdown 代码块里的情况。
     * </p>
     *
     * @throws JsonProcessingException 当 JSON 格式不合法时抛出
     */
    private Map<String, Object> parseJsonMap(String raw) throws JsonProcessingException {
        return objectMapper.readValue(extractJsonObject(raw), MAP_TYPE);
    }

    /**
     * 从模型原始输出中提取 JSON 对象文本。
     * <p>
     * 主要用于处理形如 ```json ... ``` 的包裹格式，尽量提升 Agent 输出解析的容错性。
     * </p>
     */
    private String extractJsonObject(String raw) {
        String candidate = stringValue(raw, "").trim();
        if (candidate.startsWith("```")) {
            int firstBrace = candidate.indexOf('{');
            int lastBrace = candidate.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                candidate = candidate.substring(firstBrace, lastBrace + 1);
            }
        }
        return candidate;
    }

    /**
     * 把对象序列化为 JSON 字符串。
     *
     * @throws JsonProcessingException 当序列化失败时抛出
     */
    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    /**
     * 在每轮调用前按预算压缩 transcript，尽量避免请求直接因为上下文过长而失败。
     */
    private List<Map<String, Object>> compactTranscriptIfNeeded(LlmAuthClient llmClient,
                                                                List<Map<String, Object>> transcript,
                                                                String userPrompt,
                                                                Double temperature,
                                                                Integer maxTokens,
                                                                String thinking,
                                                                int maxContextTokens,
                                                                String compactStrategy) {
        if (estimateTokens(transcript) <= maxContextTokens) {
            return transcript;
        }
        return compactTranscript(llmClient, transcript, userPrompt, temperature, maxTokens, thinking, compactStrategy);
    }

    /**
     * 当已经触发上下文超限异常时，强制压缩到更小窗口后再重试。
     */
    private List<Map<String, Object>> forceCompactTranscript(LlmAuthClient llmClient,
                                                             List<Map<String, Object>> transcript,
                                                             String userPrompt,
                                                             Double temperature,
                                                             Integer maxTokens,
                                                             String thinking,
                                                             String compactStrategy) {
        List<Map<String, Object>> compacted = compactTranscript(llmClient, transcript, userPrompt, temperature, maxTokens,
                thinking, compactStrategy);
        if (compacted.size() >= transcript.size()) {
            compacted = keepRecentMessages(transcript, Math.max(2, DEFAULT_RECENT_MESSAGES - 2));
        }
        return compacted;
    }

    /**
     * 根据策略压缩 transcript。summarize 失败时会回退到 truncate。
     */
    private List<Map<String, Object>> compactTranscript(LlmAuthClient llmClient,
                                                        List<Map<String, Object>> transcript,
                                                        String userPrompt,
                                                        Double temperature,
                                                        Integer maxTokens,
                                                        String thinking,
                                                        String compactStrategy) {
        if ("summarize".equalsIgnoreCase(compactStrategy)) {
            try {
                return summarizeTranscript(llmClient, transcript, userPrompt, temperature, maxTokens, thinking);
            } catch (Exception e) {
                log.warn("Failed to summarize transcript, falling back to truncation: {}", e.getMessage());
            }
        }
        return keepRecentMessages(transcript, DEFAULT_RECENT_MESSAGES);
    }

    /**
     * 保留固定前缀消息和最近若干条交互，直接丢弃更早的中间轮次。
     */
    private List<Map<String, Object>> keepRecentMessages(List<Map<String, Object>> transcript, int recentMessages) {
        if (transcript.size() <= 2 + recentMessages) {
            return new ArrayList<>(transcript);
        }

        List<Map<String, Object>> compacted = new ArrayList<>();
        compacted.add(transcript.get(0));
        compacted.add(transcript.get(1));
        compacted.add(message("system", "Earlier transcript entries were truncated to stay within the model context limit."));
        int fromIndex = Math.max(2, transcript.size() - recentMessages);
        compacted.addAll(transcript.subList(fromIndex, transcript.size()));
        return compacted;
    }

    /**
     * 把较早的轮次总结成一条系统摘要，同时保留最近若干条原始消息。
     */
    private List<Map<String, Object>> summarizeTranscript(LlmAuthClient llmClient,
                                                          List<Map<String, Object>> transcript,
                                                          String userPrompt,
                                                          Double temperature,
                                                          Integer maxTokens,
                                                          String thinking) throws JsonProcessingException {
        if (transcript.size() <= 2 + DEFAULT_RECENT_MESSAGES) {
            return new ArrayList<>(transcript);
        }

        int splitIndex = Math.max(2, transcript.size() - DEFAULT_RECENT_MESSAGES);
        List<Map<String, Object>> olderMessages = new ArrayList<>(transcript.subList(2, splitIndex));
        List<Map<String, Object>> recentMessages = new ArrayList<>(transcript.subList(splitIndex, transcript.size()));
        String summaryPrompt = "Summarize the earlier agent progress for continued reasoning.\n"
                + "Original task:\n" + userPrompt + "\n\n"
                + "Keep only facts, decisions, tool observations, unresolved questions, and memory-relevant details.\n"
                + "Do not invent content.\n\n"
                + "Earlier transcript:\n" + toJson(olderMessages);
        String summary = llmClient.chat("You compress agent conversation context.", summaryPrompt, temperature,
                Math.min(maxTokens, 512), thinking);

        List<Map<String, Object>> compacted = new ArrayList<>();
        compacted.add(transcript.get(0));
        compacted.add(transcript.get(1));
        compacted.add(message("system", "Conversation summary of earlier iterations:\n" + summary));
        compacted.addAll(recentMessages);
        return compacted;
    }

    /**
     * 轻量估算 token 数，用于提前触发 transcript 压缩。
     */
    private int estimateTokens(List<Map<String, Object>> transcript) {
        int characters = 0;
        for (Map<String, Object> item : transcript) {
            characters += stringValue(item.get("role"), "").length();
            characters += stringValue(item.get("content"), "").length();
        }
        return Math.max(1, characters / 4);
    }

    /**
     * 基于常见报错关键词判断是否属于上下文或 token 超限。
     */
    private boolean isContextLimitException(Exception e) {
        String message = stringValue(e.getMessage(), "").toLowerCase();
        return message.contains("token limit")
                || message.contains("context_length")
                || message.contains("context length")
                || message.contains("maximum context length")
                || message.contains("too many tokens")
                || message.contains("prompt is too long");
    }

    /**
     * 尝试把任意对象转换为新的 Map 副本。
     * <p>
     * 返回副本而不是原始引用，是为了避免后续调用方无意修改源对象。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return null;
    }

    /**
     * 尝试把对象复制为由 Map 组成的列表。
     * <p>
     * 常用于从 trace output 中恢复工具调用记录，避免直接复用外部返回的数据结构。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> copyList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add(new LinkedHashMap<>((Map<String, Object>) map));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    /**
     * 复制 Map；空值时返回一个新的空 Map，避免调用方额外判空。
     */
    private Map<String, Object> copyMap(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }

    /**
     * 把任意对象安全转换为字符串。
     * <p>
     * 当值为 {@code null} 时返回默认值。
     * </p>
     */
    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    /**
     * 把任意对象转换为整数。
     * <p>
     * 支持直接处理 {@link Number}，也支持字符串数字；空值时返回默认值。
     * </p>
     */
    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 把任意对象转换为布尔值。
     * <p>
     * 支持布尔对象本身，也支持字符串形式；空值时返回默认值。
     * </p>
     */
    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * 把空白字符串归一化为 {@code null}，便于后续判断“是否真的有值”。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

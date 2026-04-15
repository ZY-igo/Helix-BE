package com.sipc115.helix.integration.workflow.node.agent.chat;

import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeSpec;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.CompileContext;
import com.sipc115.helix.integration.workflow.compiler.NodeCompiler;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话节点的编译器。
 * <p>
 * 该类负责把 DSL 层定义的 {@code AI_TASK} 节点从“原始配置”整理为“可执行配置”。
 * 主要职责包括：
 * </p>
 * <ul>
 *     <li>校验节点配置是否合法，例如连接是否存在、模式是否支持、参数范围是否正确。</li>
 *     <li>归一化输入配置，例如补默认值、规范字符串列表、统一布尔和数值字段。</li>
 *     <li>把连接中的关键信息提前写入节点配置，减少运行期重复查询的负担。</li>
 * </ul>
 * <p>
 * 编译器阶段不真正调用模型，它的目标是尽量在执行前暴露配置错误，避免错误在运行时才爆出。
 * </p>
 */
@Component
public class AiChatNodeCompiler implements NodeCompiler {

    /**
     * 当前节点支持的工具列表。
     * <p>
     * 编译阶段会用它校验 {@code toolWhitelist} 中是否出现未支持的工具名。
     * </p>
     */
    private static final List<String> SUPPORTED_TOOLS = List.of("list_vars", "get_var", "set_memory", "ask_llm");

    /**
     * 连接仓库，用于校验 DSL 中声明的连接是否存在以及连接类型是否正确。
     */
    private final IntegrationConnectionRepository connectionRepository;

    /**
     * 构造编译器。
     *
     * @param connectionRepository 连接仓库
     */
    public AiChatNodeCompiler(IntegrationConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    /**
     * 声明当前编译器支持的节点类型。
     */
    @Override
    public DslNodeType supportType() {
        return DslNodeType.AI_TASK;
    }

    /**
     * 校验并归一化 AI 节点配置。
     * <p>
     * 这一步会直接修改 {@code source.getConfig()} 中的内容，把原始 DSL 输入补全成运行期更容易消费的格式，
     * 例如写入默认值、标准化 mode、追加 outputVar 和 thinking 配置等。
     * </p>
     *
     * @param source 原始 DSL 节点定义
     * @param context 编译上下文，当前实现中未显式使用，但保留统一接口
     */
    @Override
    public void validate(DslNodeSpec source, CompileContext context) {
        if (source == null) {
            throw new IllegalArgumentException("Node spec cannot be null");
        }

        Map<String, Object> config = source.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("AI_TASK node config is required: " + source.getId());
        }

        // 连接是 AI 节点运行的基础依赖，必须存在且类型必须为 LLM。
        Long connectionId = asLong(config.get("connectionId"), "connectionId is required: " + source.getId());
        IntegrationConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));
        if (!"LLM".equals(connection.getType())) {
            throw new IllegalArgumentException("AI_TASK connection must be LLM: " + connection.getType());
        }

        // userPrompt 是节点的核心任务输入，不允许缺失或空白。
        String userPrompt = asRequiredString(config.get("userPrompt"), "userPrompt is required: " + source.getId());
        if (userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt cannot be blank: " + source.getId());
        }

        // mode 统一归一化为小写，后续运行期和分支逻辑都依赖它。
        String mode = stringValue(config.get("mode"), "chat").trim().toLowerCase();
        if (!"chat".equals(mode) && !"agent".equals(mode)) {
            throw new IllegalArgumentException("mode must be chat or agent: " + source.getId());
        }

        // chat 模式默认只跑一轮，agent 模式默认给 6 轮决策空间。
        Integer maxIterations = intValue(config.get("maxIterations"), "agent".equals(mode) ? 6 : 1);
        if (maxIterations < 1 || maxIterations > 20) {
            throw new IllegalArgumentException("maxIterations must be between 1 and 20: " + source.getId());
        }

        Integer maxContextTokens = intValue(config.get("maxContextTokens"), 8_000);
        if (maxContextTokens <= 0) {
            throw new IllegalArgumentException("maxContextTokens must be positive: " + source.getId());
        }

        String compactStrategy = stringValue(config.get("compactStrategy"), "truncate").trim().toLowerCase();
        if (!"truncate".equals(compactStrategy) && !"summarize".equals(compactStrategy)) {
            throw new IllegalArgumentException("compactStrategy must be truncate or summarize: " + source.getId());
        }

        // 生成参数范围在编译阶段前置校验，避免运行期才发现配置越界。
        Double temperature = doubleValue(config.get("temperature"), 0.7d);
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("temperature must be between 0 and 2: " + source.getId());
        }

        Integer maxTokens = intValue(config.get("maxTokens"), 4096);
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive: " + source.getId());
        }

        // contextVars 可以为空，但如果传了必须是字符串数组。
        normalizeStringList(config, "contextVars", false);
        // toolWhitelist 为空时默认回退到全量支持工具，并逐项校验工具是否受支持。
        List<String> toolWhitelist = normalizeStringList(config, "toolWhitelist", true);
        for (String tool : toolWhitelist) {
            if (!SUPPORTED_TOOLS.contains(tool)) {
                throw new IllegalArgumentException("Unsupported tool '" + tool + "' in node: " + source.getId());
            }
        }

        // 把运行期需要的标准化配置写回 config，后续执行器可以直接读取。
        config.put("mode", mode);
        config.put("maxIterations", maxIterations);
        config.put("temperature", temperature);
        config.put("maxTokens", maxTokens);
        config.put("thinking", stringValue(config.get("thinking"), "disabled"));
        config.put("outputVar", stringValue(config.get("outputVar"), "aiResponse"));
        config.put("failOnMaxIterations", booleanValue(config.get("failOnMaxIterations"), false));
        config.put("maxContextTokens", maxContextTokens);
        config.put("compactStrategy", compactStrategy);

        // 为减少运行期再次查连接，直接把连接配置快照和模型名称写入节点配置。
        if (connection.getConfig() != null) {
            config.put(WorkflowConstants.CONNECTION_CONFIG_KEY, connection.getConfig());
        }
        config.put("model", extractModel(connection.getConfig()));
    }

    /**
     * 把已经校验和归一化过的 DSL 节点编译为运行期节点对象。
     * <p>
     * 这里做的是浅层编译：保留节点 ID、类型和一份配置副本，供执行期直接消费。
     * </p>
     */
    @Override
    public CompiledNode compile(DslNodeSpec source, CompileContext context) {
        CompiledNode node = new CompiledNode();
        node.setId(source.getId());
        node.setType(source.getType());
        node.setConfig(new LinkedHashMap<>(source.getConfig()));
        return node;
    }

    /**
     * 将对象解析为 Long；空值时抛出带上下文的异常。
     */
    private Long asLong(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 读取必填字符串；空对象直接抛错。
     */
    private String asRequiredString(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value.toString();
    }

    /**
     * 安全读取字符串，空值时返回默认值。
     */
    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    /**
     * 安全读取整数，支持数字对象和数字字符串。
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
     * 安全读取浮点数，支持数字对象和数字字符串。
     */
    private Double doubleValue(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * 安全读取布尔值，支持布尔对象和字符串形式。
     */
    private Boolean booleanValue(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * 把配置中的数组字段归一化为字符串列表。
     * <p>
     * 会过滤掉空值和空白字符串；如果允许使用默认值且结果为空，则回退到系统默认工具集。
     * 最终结果会直接写回原始 config。
     * </p>
     *
     * @param config 节点配置
     * @param key 要归一化的字段名
     * @param useDefault 结果为空时是否回退到默认值
     * @return 归一化后的字符串列表
     */
    @SuppressWarnings("unchecked")
    private List<String> normalizeStringList(Map<String, Object> config, String key, boolean useDefault) {
        Object rawValue = config.get(key);
        List<String> values = new ArrayList<>();
        if (rawValue instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item == null || item.toString().isBlank()) {
                    continue;
                }
                values.add(item.toString().trim());
            }
        } else if (rawValue != null) {
            throw new IllegalArgumentException(key + " must be an array");
        }

        if (values.isEmpty() && useDefault) {
            values.addAll(SUPPORTED_TOOLS);
        }

        config.put(key, values);
        return values;
    }

    /**
     * 从连接配置中提取模型名称。
     * <p>
     * 该字段当前主要用于输出结果展示和调试，不参与实际模型选择逻辑。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private String extractModel(Object connectionConfig) {
        if (connectionConfig instanceof Map<?, ?> config) {
            Object model = ((Map<String, Object>) config).get("model");
            return model == null ? "unknown" : model.toString();
        }
        return "unknown";
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.compiler;

import com.sipc115.helix.domain.workflow.*;
import com.sipc115.helix.integration.expression.ExpressionEngine;
import com.sipc115.helix.integration.workflow.spi.DslCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 默认 DSL 编译器实现
 * <p>
 * 负责将工作流 DSL 编译为执行计划，包含节点编译和转换处理。
 * 实现了 DslCompiler 接口，提供 DSL 到执行计划的转换功能。
 * <p>
 * 完整实现了 DSL 校验、条件表达式编译、复杂图编译等功能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class DefaultDslCompiler implements DslCompiler {
    /**
     * 日志记录器
     * <p>
     * 用于记录编译过程中的日志信息
     */
    private static final Logger logger = LoggerFactory.getLogger(DefaultDslCompiler.class);
    
    /**
     * 表达式引擎
     * <p>
     * 用于编译和执行条件表达式。
     * 通过接口注入，支持不同的表达式引擎实现（如 Aviator、SpEL 等）。
     */
    @Autowired
    private ExpressionEngine expressionEngine;
    
    /**
     * 节点编译器注册中心
     * <p>
     * 用于管理和获取不同类型的节点编译器，实现编译器与节点类型的解耦。
     */
    @Autowired
    private NodeCompilerRegistry nodeCompilerRegistry;


    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 编译工作流 DSL
     * <p>
     * 将工作流 DSL 转换为执行计划，包含工作流 ID、版本、入口节点、编译后的节点和转换关系。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 编译后的执行计划
     * @throws IllegalArgumentException 当 DSL 中缺少 START 节点时抛出
     */
    @Override
    public ExecutionPlan compile(WorkflowDsl dsl) {
        logger.info("Starting compilation of workflow DSL: {}", dsl.getWorkflowId());
        
        try {
            // 1. 执行完整 DSL 校验
            logger.debug("Validating DSL for workflow: {}", dsl.getWorkflowId());
            validateDsl(dsl);
            logger.debug("DSL validation completed successfully");

            // 2. 构建编译上下文
            CompileContext context = buildCompileContext(dsl);
            
            // 4. 创建执行计划对象
            ExecutionPlan plan = new ExecutionPlan();
            
            // 5. 设置工作流 ID 和版本
            plan.setWorkflowId(dsl.getWorkflowId());
            plan.setWorkflowVersion(dsl.getVersion());
            
            // 6. 查找并设置入口节点 ID
            logger.debug("Finding entry node for workflow: {}", dsl.getWorkflowId());
            plan.setEntryNodeId(findEntryNodeId(dsl));
            logger.debug("Entry node found: {}", plan.getEntryNodeId());
            
            // 7. 编译节点并转换为映射（使用注册中心）
            logger.debug("Compiling nodes for workflow: {}", dsl.getWorkflowId());
            plan.setNodes(dsl.getNodes().stream()
                    .map(node -> compileNodeWithRegistry(node, context))
                    .collect(Collectors.toMap(CompiledNode::getId, Function.identity())));
            logger.debug("Compiled {} nodes", plan.getNodes().size());
            
            // 8. 转换边为转换对象
            logger.debug("Compiling edges for workflow: {}", dsl.getWorkflowId());
            plan.setTransitions(dsl.getEdges().stream().map(this::compileEdge)
                    .collect(Collectors.toList()));
            logger.debug("Compiled {} edges", plan.getTransitions().size());

            // 9. 计算节点的入度和前后继关系（用于Join/Barrier调度）
            computePredecessorsAndSuccessors(dsl, plan);
            logger.debug("Computed predecessors and successors for workflow");

            // 10. 计算变量依赖图（用于精准内存清理）
            computeVariableDependencies(dsl, plan);
            logger.debug("Computed variable dependencies for workflow");

            // 11. 设置调度信息（如果有）
            plan.setSchedule(dsl.getSchedule());

            logger.info("Compilation completed successfully for workflow: {}", dsl.getWorkflowId());
            return plan;
        } catch (Exception e) {
            logger.error("Compilation failed for workflow: {}", dsl.getWorkflowId(), e);
            throw new RuntimeException("Failed to compile workflow DSL: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建编译上下文
     * <p>
     * 构建包含编译过程中所需共享依赖的上下文对象。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 编译上下文
     */
    private CompileContext buildCompileContext(WorkflowDsl dsl) {
        // 构建节点索引
        Map<String, DslNodeSpec> nodeIndex = dsl.getNodes().stream()
                .collect(Collectors.toMap(DslNodeSpec::getId, Function.identity()));
        
        // 收集已声明的变量
        Set<String> declaredVariables = collectDefinedVariables(dsl);
        
        return new CompileContext(
                expressionEngine,
                dsl.getMetadata(),
                nodeIndex,
                declaredVariables
        );
    }
    
    /**
     * 使用注册中心编译节点
     * <p>
     * 根据节点类型从注册中心获取对应的编译器，执行节点编译。
     * 
     * @param source DSL 节点规范
     * @param context 编译上下文
     * @return 编译后的节点
     */
    private CompiledNode compileNodeWithRegistry(DslNodeSpec source, CompileContext context) {
        NodeCompiler compiler = nodeCompilerRegistry.getRequiredCompiler(source.getType());
        compiler.validate(source, context);
        CompiledNode compiledNode = compiler.compile(source, context);
        attachActivityInvocationConfig(source, compiledNode);
        return compiledNode;
    }

    private void attachActivityInvocationConfig(DslNodeSpec source, CompiledNode compiledNode) {
        if (compiledNode == null) {
            return;
        }

        Map<String, Object> config = compiledNode.getConfig() != null
                ? new HashMap<>(compiledNode.getConfig())
                : new HashMap<>();

        Map<String, Object> activityConfig = buildActivityConfigFromPolicy(source.getPolicy());
        if (!activityConfig.isEmpty()) {
            Object existingActivityConfig = config.get("activityConfig");
            if (existingActivityConfig instanceof Map<?, ?> existingMap) {
                Map<String, Object> mergedActivityConfig = new HashMap<>();
                existingMap.forEach((key, value) -> mergedActivityConfig.put(String.valueOf(key), value));
                mergedActivityConfig.putAll(activityConfig);
                config.put("activityConfig", mergedActivityConfig);
            } else {
                config.put("activityConfig", activityConfig);
            }
        }

        compiledNode.setConfig(config);
    }

    private Map<String, Object> buildActivityConfigFromPolicy(NodePolicyConfig policy) {
        if (policy == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> activityConfig = new HashMap<>();

        NodePolicyConfig.TimeoutConfig timeoutConfig = policy.getTimeout();
        if (timeoutConfig != null && timeoutConfig.getStartToCloseTimeout() != null) {
            activityConfig.put("timeout", timeoutConfig.getStartToCloseTimeout().toString());
        }

        NodePolicyConfig.RetryPolicy retryPolicy = policy.getRetryPolicy();
        if (retryPolicy != null) {
            Map<String, Object> retryConfig = new HashMap<>();
            if (retryPolicy.getInitialInterval() != null) {
                retryConfig.put("initialInterval", retryPolicy.getInitialInterval().toString());
            }
            if (retryPolicy.getMaxInterval() != null) {
                retryConfig.put("maxInterval", retryPolicy.getMaxInterval().toString());
            }
            if (retryPolicy.getMaxAttempts() != null) {
                retryConfig.put("maxAttempts", retryPolicy.getMaxAttempts());
            }
            if (!retryConfig.isEmpty()) {
                activityConfig.put("retry", retryConfig);
            }
        }

        return activityConfig;
    }

    /**
     * 验证 DSL 的完整性和合法性
     * <p>
     * 执行以下验证：
     * 1. 检查 START 节点的唯一性
     * 2. 检查 END 节点的存在性
     * 3. 检查节点 ID 的唯一性
     * 4. 检查边的合法性（源节点和目标节点必须存在）
     * 5. 检查是否存在环
     * 6. 检查变量引用的合法性
     * 
     * @param dsl 工作流 DSL 对象
     * @throws IllegalArgumentException 当 DSL 验证失败时抛出
     */
    private void validateDsl(WorkflowDsl dsl) {
        // 检查 START 节点的唯一性
        long startNodeCount = dsl.getNodes().stream()
                .filter(node -> node.getType() == DslNodeType.START)
                .count();
        if (startNodeCount == 0) {
            throw new IllegalArgumentException("DSL must contain exactly one START node");
        }
        if (startNodeCount > 1) {
            throw new IllegalArgumentException("DSL must contain exactly one START node");
        }
        
        // 检查 END 节点的存在性
        boolean hasEndNode = dsl.getNodes().stream()
                .anyMatch(node -> node.getType() == DslNodeType.END);
        if (!hasEndNode) {
            throw new IllegalArgumentException("DSL must contain at least one END node");
        }
        
        // 检查节点 ID 的唯一性
        Set<String> nodeIds = new HashSet<>();
        for (DslNodeSpec node : dsl.getNodes()) {
            if (!nodeIds.add(node.getId())) {
                throw new IllegalArgumentException("Node ID must be unique: " + node.getId());
            }
        }
        
        // 检查边的合法性
        for (DslEdgeSpec edge : dsl.getEdges()) {
            boolean fromExists = dsl.getNodes().stream().anyMatch(node -> node.getId().equals(edge.getFrom()));
            boolean toExists = dsl.getNodes().stream().anyMatch(node -> node.getId().equals(edge.getTo()));
            if (!fromExists) {
                throw new IllegalArgumentException("Edge source node does not exist: " + edge.getFrom());
            }
            if (!toExists) {
                throw new IllegalArgumentException("Edge target node does not exist: " + edge.getTo());
            }
        }
        
        // 检查是否存在环
        if (hasCycle(dsl)) {
            throw new IllegalArgumentException("DSL contains cycle");
        }
        
        // 检查变量引用的合法性（简化实现，实际需要更复杂的逻辑）
        validateVariableReferences(dsl);
    }

    /**
     * 检查 DSL 是否存在环
     * <p>
     * 使用深度优先搜索算法检查工作流图是否存在环。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 如果存在环返回 true，否则返回 false
     */
    private boolean hasCycle(WorkflowDsl dsl) {
        // 构建邻接表
        Map<String, List<String>> adjacencyList = new HashMap<>();
        for (DslEdgeSpec edge : dsl.getEdges()) {
            adjacencyList.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge.getTo());
        }
        
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        // 对每个节点进行深度优先搜索
        for (DslNodeSpec node : dsl.getNodes()) {
            if (!visited.contains(node.getId())) {
                if (hasCycleUtil(node.getId(), adjacencyList, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 深度优先搜索检查环的辅助方法
     * 
     * @param nodeId 当前节点 ID
     * @param adjacencyList 邻接表
     * @param visited 已访问节点集合
     * @param recursionStack 递归栈
     * @return 如果存在环返回 true，否则返回 false
     */
    private boolean hasCycleUtil(String nodeId, Map<String, List<String>> adjacencyList, Set<String> visited, Set<String> recursionStack) {
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        List<String> neighbors = adjacencyList.getOrDefault(nodeId, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                if (hasCycleUtil(neighbor, adjacencyList, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                return true;
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * 验证变量引用的合法性
     * <p>
     * 检查 DSL 中变量的定义和使用是否合法，包括变量是否已定义、类型是否匹配等。
     * 
     * @param dsl 工作流 DSL 对象
     * @throws IllegalArgumentException 当变量引用不合法时抛出
     */
    private void validateVariableReferences(WorkflowDsl dsl) {
        // 1. 收集所有已定义的变量
        Set<String> definedVariables = collectDefinedVariables(dsl);
        
        // 2. 检查所有表达式中的变量引用
        checkVariableReferencesInEdges(dsl, definedVariables);
        checkVariableReferencesInNodes(dsl, definedVariables);
    }
    
    /**
     * 收集所有已定义的变量
     * <p>
     * 从工作流 DSL 中收集所有已定义的变量，包括工作流输入、节点输出等。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 已定义变量的集合
     */
    private Set<String> collectDefinedVariables(WorkflowDsl dsl) {
        Set<String> definedVariables = new HashSet<>();

        // Workflow-level variables are now accessed through WorkflowMetadata
        // Currently no workflow-level variables are defined in WorkflowMetadata
        // This can be extended later if needed

        // 收集节点级别的变量定义
        for (DslNodeSpec node : dsl.getNodes()) {
            Map<String, Object> config = node.getConfig();
            if (config != null) {
                // 检查节点是否定义了输出变量
                Object output = config.get("output");
                if (output instanceof Map) {
                    Object var = ((Map<?, ?>) output).get("var");
                    if (var != null) {
                        definedVariables.add(var.toString());
                    }
                }
                
                // 检查节点是否定义了内部变量（如 AI_TASK 节点）
                Object vars = config.get("vars");
                if (vars instanceof Map) {
                    ((Map<?, ?>) vars).keySet().forEach(key -> definedVariables.add(key.toString()));
                }
            }
        }
        
        return definedVariables;
    }
    
    /**
     * 检查边中条件表达式的变量引用
     * <p>
     * 检查边的条件表达式中使用的变量是否已定义。
     * 
     * @param dsl 工作流 DSL 对象
     * @param definedVariables 已定义变量的集合
     * @throws IllegalArgumentException 当变量引用不合法时抛出
     */
    private void checkVariableReferencesInEdges(WorkflowDsl dsl, Set<String> definedVariables) {
        for (DslEdgeSpec edge : dsl.getEdges()) {
            String conditionKey = edge.getConditionKey();
            if (conditionKey != null && !conditionKey.isEmpty()) {
                // 提取表达式中的变量引用
                Set<String> referencedVariables = extractVariablesFromExpression(conditionKey);
                // 检查变量是否已定义
                for (String var : referencedVariables) {
                    if (!definedVariables.contains(var)) {
                        throw new IllegalArgumentException("Variable not defined: " + var + " in condition for edge from " + edge.getFrom() + " to " + edge.getTo());
                    }
                }
            }
        }
    }
    
    /**
     * 检查节点中表达式的变量引用
     * <p>
     * 检查节点配置中表达式使用的变量是否已定义。
     * 
     * @param dsl 工作流 DSL 对象
     * @param definedVariables 已定义变量的集合
     * @throws IllegalArgumentException 当变量引用不合法时抛出
     */
    private void checkVariableReferencesInNodes(WorkflowDsl dsl, Set<String> definedVariables) {
        for (DslNodeSpec node : dsl.getNodes()) {
            Map<String, Object> config = node.getConfig();
            if (config != null) {
                // 检查 CONDITION 节点的条件表达式
                if (node.getType() == DslNodeType.CONDITION) {
                    Object condition = config.get("condition");
                    if (condition != null && condition instanceof String) {
                        Set<String> referencedVariables = extractVariablesFromExpression((String) condition);
                        for (String var : referencedVariables) {
                            if (!definedVariables.contains(var)) {
                                throw new IllegalArgumentException("Variable not defined: " + var + " in condition for IF node " + node.getId());
                            }
                        }
                    }
                }
                
                // 检查其他类型节点的变量引用
                // TODO: 扩展其他节点类型的变量引用检查
            }
        }
    }
    
    /**
     * 从表达式中提取变量引用
     * <p>
     * 简单实现，提取表达式中可能的变量引用。实际应用中可能需要更复杂的解析。
     * 
     * @param expression 表达式字符串
     * @return 提取的变量引用集合
     */
    private Set<String> extractVariablesFromExpression(String expression) {
        Set<String> variables = new HashSet<>();

        // 使用预编译的 Pattern 常量
        java.util.regex.Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            String varExpr = matcher.group(1);
            // 提取变量名（简单处理，实际可能需要更复杂的解析）
            if (varExpr.contains(".")) {
                // 处理对象属性引用，如 ${vars.draft}
                String[] parts = varExpr.split("\\.");
                if (parts.length > 0) {
                    variables.add(parts[0]);
                }
            } else {
                // 处理简单变量引用，如 ${variable}
                variables.add(varExpr);
            }
        }
        
        return variables;
    }



    /**
     * 查找入口节点 ID
     * <p>
     * 从 DSL 中查找类型为 START 的节点，作为工作流的入口节点。
     * 
     * @param dsl 工作流 DSL 对象
     * @return 入口节点的 ID
     * @throws IllegalArgumentException 当 DSL 中缺少 START 节点时抛出
     */
    private String findEntryNodeId(WorkflowDsl dsl) {
        return dsl.getNodes().stream()
                .filter(node -> node.getType() == DslNodeType.START)
                .findFirst()
                .map(DslNodeSpec::getId)
                .orElseThrow(() -> new IllegalArgumentException("DSL must contain START node"));
    }

    /**
     * 编译节点
     * <p>
     * 将 DSL 节点规范转换为编译后的节点，设置节点 ID、类型、配置和操作。
     * 对不同类型的节点进行特殊处理，确保每个节点类型都能正确编译。
     * 
     * @param source DSL 节点规范
     * @return 编译后的节点
     * @throws IllegalArgumentException 当节点配置不合法时抛出
     */


    /**
     * 编译边
     * <p>
     * 将 DSL 边规范转换为转换对象，设置源节点、目标节点和条件键。
     * 对条件边进行特殊处理，确保条件表达式的正确性。
     * 
     * @param edge DSL 边规范
     * @return 转换对象
     * @throws IllegalArgumentException 当边配置不合法时抛出
     */
    private Transition compileEdge(DslEdgeSpec edge) {
        Transition transition = new Transition();
        transition.setFrom(edge.getFrom());
        transition.setTo(edge.getTo());
        transition.setConditionKey(edge.getConditionKey());
        
        // 检查边的合法性
        validateEdge(edge);
        
        // 对条件边进行特殊处理
        if (edge.getConditionKey() != null && !edge.getConditionKey().isEmpty()) {
            processConditionEdge(transition, edge);
        }
        
        return transition;
    }
    
    /**
     * 验证边的合法性
     * <p>
     * 检查边的源节点和目标节点是否合法，以及边的配置是否正确。
     * 
     * @param edge DSL 边规范
     * @throws IllegalArgumentException 当边配置不合法时抛出
     */
    private void validateEdge(DslEdgeSpec edge) {
        // 检查源节点和目标节点是否为空
        if (edge.getFrom() == null || edge.getFrom().isEmpty()) {
            throw new IllegalArgumentException("Edge must have a source node");
        }
        if (edge.getTo() == null || edge.getTo().isEmpty()) {
            throw new IllegalArgumentException("Edge must have a target node");
        }
        
        // 检查源节点和目标节点是否相同
        if (edge.getFrom().equals(edge.getTo())) {
            throw new IllegalArgumentException("Edge source and target nodes cannot be the same: " + edge.getFrom());
        }
    }
    
    /**
     * 处理条件边
     * <p>
     * 对条件边进行特殊处理，确保条件表达式的正确性。
     * 
     * @param transition 转换对象
     * @param edge DSL 边规范
     */
    private void processConditionEdge(Transition transition, DslEdgeSpec edge) {
        // 条件表达式已经在 compileExpressions 方法中验证和归一化
        // 这里可以添加额外的处理逻辑，如：
        // 1. 为条件边添加优先级
        // 2. 优化条件表达式的执行
        // 3. 处理条件边的特殊属性

        // 示例：可以在这里添加条件边的特殊处理逻辑
        // 例如，检查条件表达式的复杂度，或进行性能优化
    }

    /**
     * 计算节点的入度和出度关系
     * <p>
     * 根据工作流的边（edges）计算每个节点的前驱（predecessors）和后继（successors）。
     * 这是实现 Join/Barrier 机制的基础。
     *
     * <h3>算法流程：</h3>
     * <pre>
     * 1. 初始化：为所有节点创建空的 pre/succ 集合
     * 2. 遍历所有边：
     *    - 对于边 A → B，将 A 添加到 B 的 predecessors
     *    - 对于边 A → B，将 B 添加到 A 的 successors
     * </pre>
     *
     * <h3>示例：</h3>
     * <pre>
     * 边定义:
     * edges = [
     *   {from: "A", to: "B"},
     *   {from: "A", to: "C"},
     *   {from: "B", to: "D"},
     *   {from: "C", to: "D"}
     * ]
     *
     * 执行结果:
     * predecessors = {
     *   "A": {},      // 入口节点无前驱
     *   "B": {"A"},
     *   "C": {"A"},
     *   "D": {"B", "C"}  // D 有两个前驱
     * }
     *
     * successors = {
     *   "A": {"B", "C"},
     *   "B": {"D"},
     *   "C": {"D"},
     *   "D": {}       // 结束节点无后继
     * }
     * </pre>
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>DslOrchestratorWorkflowImpl.canExecuteNode() - 判断节点是否可执行</li>
     *   <li>DslOrchestratorWorkflowImpl.markNodeCompleted() - 通知下游节点</li>
     *   <li>检测环形依赖</li>
     * </ul>
     *
     * @param dsl 工作流 DSL（包含边信息）
     * @param plan 执行计划（包含节点信息，用于初始化集合）
     */
    private void computePredecessorsAndSuccessors(WorkflowDsl dsl, ExecutionPlan plan) {
        Map<String, Set<String>> predecessors = new TreeMap<>();
        Map<String, Set<String>> successors = new TreeMap<>();

        for (String nodeId : plan.getNodes().keySet()) {
            predecessors.put(nodeId, new TreeSet<>());
            successors.put(nodeId, new TreeSet<>());
        }

        for (DslEdgeSpec edge : dsl.getEdges()) {
            String from = edge.getFrom();
            String to = edge.getTo();

            if (predecessors.containsKey(to)) {
                predecessors.get(to).add(from);
            }
            if (successors.containsKey(from)) {
                successors.get(from).add(to);
            }
        }

        plan.setPredecessors(predecessors);
        plan.setSuccessors(successors);
    }

    /**
     * 计算变量依赖图（编译期构建）
     * <p>
     * 分析每个节点配置中的表达式引用，构建变量依赖关系图。
     * 用于运行时精准内存清理：只有当一个变量的引用计数降为 0 时，才从上下文中删除。
     *
     * <h3>算法流程：</h3>
     * <pre>
     * 1. 遍历所有节点
     * 2. 检查节点配置中的所有字符串值
     * 3. 提取 ${xxx.yyy} 格式的变量引用
     * 4. 提取节点ID作为依赖源（如 ${A.output} → A）
     * 5. 统计每个节点被多少下游节点引用（引用计数）
     * </pre>
     *
     * <h3>示例：</h3>
     * <pre>
     * 节点配置：
     * - node-A: config = {}
     * - node-B: config = {text: "${A.output}"}     // B 引用了 A
     * - node-C: config = {msg: "${A.output}"}     // C 也引用了 A
     * - node-D: config = {data: "${B.result}"}    // D 引用了 B
     *
     * variableDependencies = {
     *     "node-B": {"node-A": 1},           // B 依赖 A
     *     "node-C": {"node-A": 1},           // C 依赖 A
     *     "node-D": {"node-B": 1}             // D 依赖 B
     * }
     *
     * 运行时引用计数（从后往前推）：
     * - A 被 B 和 C 引用 → refCount = 2
     * - B 被 D 引用 → refCount = 1
     * - C 无下游引用 → refCount = 0（执行完可清理）
     * - D 无下游引用 → refCount = 0（执行完可清理）
     * </pre>
     *
     * @param dsl 工作流 DSL
     * @param plan 执行计划（用于获取编译后的节点配置）
     */
    private void computeVariableDependencies(WorkflowDsl dsl, ExecutionPlan plan) {
        Map<String, Map<String, Integer>> variableDependencies = new TreeMap<>();

        // 遍历所有节点，构建依赖图
        for (Map.Entry<String, CompiledNode> entry : plan.getNodes().entrySet()) {
            String nodeId = entry.getKey();
            CompiledNode node = entry.getValue();
            Map<String, Object> config = node.getConfig();

            if (config == null || config.isEmpty()) {
                continue;
            }

            // 分析节点配置中的变量引用
            Map<String, Integer> dependencies = new TreeMap<>();
            analyzeConfigForReferences(nodeId, config, dependencies, plan.getNodes().keySet());

            if (!dependencies.isEmpty()) {
                variableDependencies.put(nodeId, dependencies);
            }
        }

        plan.setVariableDependencies(variableDependencies);
    }

    /**
     * 分析节点配置中的变量引用（递归实现）
     * <p>
     * 该方法会遍历配置对象的所有层级，提取出所有符合 ${xxx.yyy} 格式的表达式，
     * 并统计当前节点对上游节点的依赖次数。
     *
     * @param nodeId        当前正在分析的节点ID（主要用于调试或错误定位）
     * @param config        节点的配置对象（可能包含嵌套的 Map 或 List）
     * @param dependencies  【输出参数】用于累加依赖关系的 Map，Key=上游节点ID, Value=引用次数
     * @param validNodeIds  工作流中所有合法节点ID的集合，用于过滤掉非节点引用的全局变量
     */
    private void analyzeConfigForReferences(String nodeId, Map<String, Object> config,
                                            Map<String, Integer> dependencies, Set<String> validNodeIds) {
        // 1. 遍历当前层级的所有配置项
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();

            // --- 情况 A：值是字符串，检查是否包含变量表达式 ---
            if (value instanceof String) {
                String strValue = (String) value;
                // 使用预编译的正则匹配 ${...} 模式
                Matcher matcher = VARIABLE_PATTERN.matcher(strValue);
                while (matcher.find()) {
                    String varExpr = matcher.group(1); // 获取花括号内的内容，如 "A.output"

                    // 只有包含 "." 的才被认为是节点间引用（如 A.output），排除简单变量
                    if (varExpr.contains(".")) {
                        // 提取点号前面的部分作为上游节点ID（即 "A"）
                        String referencedNodeId = varExpr.split("\\.")[0];

                        // 校验：确保这个 ID 确实是工作流里的一个节点，而不是全局常量
                        if (validNodeIds.contains(referencedNodeId)) {
                            // 累加引用计数：如果 B 两次引用了 A，那么 A 的计数就是 2
                            dependencies.merge(referencedNodeId, 1, Integer::sum);
                        }
                    }
                }

            // --- 情况 B：值是嵌套的 Map，递归深入分析 ---
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                // 递归调用：进入下一层 Map 继续寻找表达式
                analyzeConfigForReferences(nodeId, nestedMap, dependencies, validNodeIds);

            // --- 情况 C：值是 List 数组，遍历数组元素分析 ---
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    // 如果数组元素是 Map，递归分析
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) item;
                        analyzeConfigForReferences(nodeId, nestedMap, dependencies, validNodeIds);

                    // 如果数组元素是字符串，执行与情况 A 相同的匹配逻辑
                    } else if (item instanceof String) {
                        String strValue = (String) item;
                        Matcher matcher = VARIABLE_PATTERN.matcher(strValue);
                        while (matcher.find()) {
                            String varExpr = matcher.group(1);
                            if (varExpr.contains(".")) {
                                String referencedNodeId = varExpr.split("\\.")[0];
                                if (validNodeIds.contains(referencedNodeId)) {
                                    dependencies.merge(referencedNodeId, 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

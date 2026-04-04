/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.logic.loop;

import com.sipc115.helix.common.constant.NodeRoleConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import io.temporal.workflow.ChildWorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环节点执行器
 * <p>
 * 负责执行循环节点，循环节点会创建一个 Temporal 子工作流来执行内部的子工作流。
 * 支持两种退出方式：
 * <ul>
 *   <li>达到最大循环次数（maxRounds）</li>
 *   <li>满足退出条件（exitCondition）</li>
 * </ul>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>从节点配置中获取最大循环次数和退出条件</li>
 *   <li>创建 Temporal 子工作流（LoopSubWorkflow）来执行循环逻辑</li>
 *   <li>收集循环执行结果（迭代次数、最后结果、退出原因）</li>
 *   <li>将结果通过 NodeExecutionResult 返回，供工作流引擎合并到上下文</li>
 * </ol>
 *
 * <h3>输出字段说明：</h3>
 * <ul>
 *   <li>iterations - 实际执行的迭代次数</li>
 *   <li>lastResult - 最后一次迭代的结果</li>
 *   <li>exitReason - 退出原因（MAX_ROUNDS / EXIT_CONDITION）</li>
 *   <li>success - 是否成功完成循环</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see LoopSubWorkflow
 * @see LoopResult
 */
@Component
public class LoopNodeExecutor implements WorkflowNodeExecutor {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(LoopNodeExecutor.class);

    /**
     * 工作流追踪服务
     * <p>
     * 静态注入，用于记录节点执行的追踪信息。
     * 使用 setter 注入方式，因为在 Temporal 工作流上下文中无法直接使用 Spring 依赖注入。
     */
    private static WorkflowTraceService traceService;

    /**
     * 设置追踪服务
     * <p>
     * 通过 Spring 自动注入 WorkflowTraceService 实例。
     * 由于 Temporal 工作流的特殊性，需要使用静态注入方式。
     *
     * @param traceService 工作流追踪服务实例
     */
    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        LoopNodeExecutor.traceService = traceService;
    }

    /**
     * 判断是否支持指定节点类型
     * <p>
     * 只有 LOOP 类型的节点才由本执行器处理。
     *
     * @param type 节点类型名称
     * @return 如果类型为 LOOP 则返回 true
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.LOOP.name().equals(type);
    }

    /**
     * 执行循环节点
     * <p>
     * 创建并执行一个 Temporal 子工作流来实现循环逻辑。
     * 子工作流会按照配置的最大循环次数执行，或在满足退出条件时提前退出。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>启动节点追踪记录</li>
     *   <li>从节点配置中提取循环参数（maxRounds、exitCondition）</li>
     *   <li>准备子工作流输入数据（包含变量快照和退出条件）</li>
     *   <li>创建并启动 Temporal 子工作流</li>
     *   <li>收集子工作流返回的循环结果</li>
     *   <li>构建标准输出并标记节点成功</li>
     * </ol>
     *
     * @param node 编译后的节点定义，包含节点类型和配置
     * @param context 执行上下文，包含工作流变量和执行状态
     * @param bridge 工作流运行时桥接，提供 Temporal 等底层能力
     * @return 节点执行结果，包含循环执行的统计信息
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        // 步骤1：启动节点追踪
        NodeExecutionTraceEntity trace = null;
        if (traceService != null && context.getExecutionId() != null) {
            try {
                trace = traceService.startNodeExecution(
                    context.getExecutionId(),
                    node.getId(),
                    node.getType().name(),
                    NodeRoleConstants.NORMAL,
                    context.getExecutionOrder(),
                    context.getVariables()
                );
                // 将追踪ID保存到上下文，供后续节点使用
                context.setCurrentNodeTraceId(trace.getId());
            } catch (Exception e) {
                log.warn("启动节点追踪失败: {}", e.getMessage());
            }
        }

        log.info("开始执行循环节点: {}", node.getId());

        // 步骤2：提取循环配置参数
        Map<String, Object> config = node.getConfig();
        // 最大循环次数，默认为10次
        Integer maxRounds = getIntValue(config, "maxRounds", 10);

        // 步骤3：准备子工作流输入数据
        // 将当前上下文变量传递给子工作流
        Map<String, Object> loopInput = new HashMap<>(context.getVariables());
        // 添加编译后的退出条件表达式
        CompiledExpression compiledExitCondition = (CompiledExpression) config.get("compiledExitCondition");
        loopInput.put("compiledExitCondition", compiledExitCondition);
        // 添加原始退出条件字符串（用于日志和调试）
        Object exitConditionObj = config.get("exitCondition");
        if (exitConditionObj instanceof String) {
            loopInput.put("exitCondition", exitConditionObj);
        }

        // 步骤4：创建子工作流选项
        // 使用唯一的工作流ID避免冲突
        String taskQueue = (String) config.getOrDefault("taskQueue", "helix-task-queue");
        String workflowId = "loop-" + node.getId() + "-" + System.currentTimeMillis();

        ChildWorkflowOptions childWorkflowOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(taskQueue)
                .build();

        // 步骤5：创建并启动子工作流
        LoopSubWorkflow loopWorkflow = io.temporal.workflow.Workflow.newChildWorkflowStub(
                LoopSubWorkflow.class,
                childWorkflowOptions
        );

        // 执行子工作流，获取循环结果
        LoopResult loopResult = loopWorkflow.execute(maxRounds, loopInput);

        // 步骤6：构建标准输出
        // 统一使用 result.setOutput() 方式返回结果
        // 供工作流引擎合并到上下文变量中
        Map<String, Object> output = new HashMap<>();
        output.put("iterations", loopResult.getIterations());
        output.put("lastResult", loopResult.getLastResult());
        output.put("exitReason", loopResult.getExitReason());
        output.put("success", loopResult.isSuccess());

        NodeExecutionResult executionResult = new NodeExecutionResult();
        executionResult.setStatus(ExecutionStatus.COMPLETED);
        executionResult.setOutput(output);

        // 步骤7：标记节点执行成功
        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), output);
            } catch (Exception e) {
                log.warn("标记节点成功失败: {}", e.getMessage());
            }
        }

        return executionResult;
    }

    /**
     * 从配置中获取整数值
     * <p>
     * 安全地从配置Map中获取整数值，支持多种类型转换。
     *
     * @param config 节点配置Map
     * @param key 配置键名
     * @param defaultValue 默认值
     * @return 配置的整数值，如果获取失败则返回默认值
     */
    private Integer getIntValue(Map<String, Object> config, String key, Integer defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}

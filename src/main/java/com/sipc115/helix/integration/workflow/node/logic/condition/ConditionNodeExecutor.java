/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.logic.condition;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.expression.CompiledExpression;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 条件节点执行器
 * <p>
 * 负责执行条件节点，根据表达式的求值结果确定下一步执行的分支路径。
 * 支持两种条件类型：
 * <ul>
 *   <li>编译后的表达式（compiledExpression）- 高性能，直接执行</li>
 *   <li>默认分支（defaultBranch）- 求值失败时的备选值</li>
 * </ul>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>从节点配置中获取编译后的表达式</li>
 *   <li>使用当前上下文变量执行表达式求值</li>
 *   <li>根据求值结果确定分支键（branchKey）</li>
 *   <li>返回包含 branchKey 的执行结果</li>
 * </ol>
 *
 * <h3>分支键说明：</h3>
 * <ul>
 *   <li>true - 条件为真，走真分支</li>
 *   <li>false - 条件为假，走假分支</li>
 * </ul>
 *
 * <h3>与边选择器的配合：</h3>
 * <p>
 * 条件节点不直接指定下一节点，而是通过 branchKey 返回条件结果。
 * 工作流引擎会根据 branchKey 调用 TransitionResolver.nextNode() 
 * 来查找对应的出边并确定下一节点。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see ExecutionContext
 * @see TransitionResolver
 */
@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ConditionNodeExecutor.class);

    /**
     * 工作流追踪服务
     * <p>
     * 静态注入，用于记录节点执行的追踪信息。
     */
    private static WorkflowTraceService traceService;

    /**
     * 设置追踪服务
     *
     * @param traceService 工作流追踪服务实例
     */
    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        ConditionNodeExecutor.traceService = traceService;
    }

    /**
     * 判断是否支持指定节点类型
     * <p>
     * 只有 CONDITION 类型的节点才由本执行器处理。
     *
     * @param type 节点类型名称
     * @return 如果类型为 CONDITION 则返回 true
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.CONDITION.name().equals(type);
    }

    /**
     * 执行条件节点
     * <p>
     * 根据表达式的求值结果确定分支路径。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>启动节点追踪记录</li>
     *   <li>从节点配置中获取编译后的表达式</li>
     *   <li>使用上下文变量执行表达式求值</li>
     *   <li>根据求值结果设置 branchKey</li>
     *   <li>返回包含 branchKey 的执行结果</li>
     * </ol>
     *
     * <h3>求值失败处理：</h3>
     * <ul>
     *   <li>如果表达式执行抛出异常，使用 defaultBranch 作为备选</li>
     *   <li>如果配置中没有 defaultBranch，默认为 "true"</li>
     * </ul>
     *
     * @param node 编译后的节点定义，包含节点类型和配置
     * @param context 执行上下文，包含工作流变量和执行状态
     * @param bridge 工作流运行时桥接（此节点不使用）</li>
     * @return 节点执行结果，包含 branchKey 信息
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
                    "NORMAL",
                    context.getExecutionOrder(),
                    context.getVariables()
                );
                context.setCurrentNodeTraceId(trace.getId());
            } catch (Exception e) {
                log.warn("启动节点追踪失败: {}", e.getMessage());
            }
        }

        String branchKey;

        // 步骤2：获取条件表达式配置
        Map<String, Object> config = node.getConfig();
        Object compiledExprObj = config.get("compiledExpression");

        // 步骤3：执行表达式求值
        if (compiledExprObj instanceof CompiledExpression) {
            try {
                CompiledExpression compiledExpr = (CompiledExpression) compiledExprObj;
                // 使用当前上下文变量执行表达式
                Boolean evalResult = (Boolean) compiledExpr.execute(context.getVariables());
                // 将布尔结果转换为分支键
                branchKey = evalResult ? "true" : "false";
                log.debug("条件表达式求值结果: nodeId={}, result={}, branchKey={}",
                    node.getId(), evalResult, branchKey);
            } catch (Exception e) {
                // 求值失败，使用默认分支
                log.warn("条件表达式求值失败，使用默认分支: nodeId={}, error={}",
                    node.getId(), e.getMessage());
                branchKey = String.valueOf(config.getOrDefault("defaultBranch", "true"));
            }
        } else {
            // 配置中没有表达式，使用默认分支
            log.warn("条件节点缺少编译表达式，使用默认分支: nodeId={}", node.getId());
            branchKey = String.valueOf(config.getOrDefault("defaultBranch", "true"));
        }

        // 步骤4：构建执行结果
        // 设置 branchKey 让工作流引擎根据它查找对应的出边
        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(branchKey);

        // 步骤5：标记节点执行成功
        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), result.getOutput());
            } catch (Exception e) {
                log.warn("标记节点成功失败: {}", e.getMessage());
            }
        }

        log.info("条件节点执行完成: nodeId={}, branchKey={}", node.getId(), branchKey);
        return result;
    }
}

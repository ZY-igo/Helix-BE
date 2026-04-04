/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.humanInput;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 人工输入节点执行器
 * <p>
 * 负责执行人工输入节点，暂停工作流执行等待用户人工输入。
 * 工作流会阻塞在当前节点，直到用户提交信号或超过超时时间。
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>节点开始执行时，工作流暂停并等待人工信号</li>
 *   <li>用户通过外部系统（如前端、API）提交人工输入</li>
 *   <li>信号被添加到缓冲队列，工作流恢复执行</li>
 *   <li>节点获取信号载荷并继续执行</li>
 * </ol>
 *
 * <h3>超时机制：</h3>
 * <ul>
 *   <li>支持通过节点配置指定超时时间</li>
 *   <li>超时格式支持：24h、30m、60s 或数字秒</li>
 *   <li>默认超时时间为 24 小时</li>
 *   <li>超时时返回 TIMED_OUT 状态，不会永久阻塞</li>
 * </ul>
 *
 * <h3>输出说明：</h3>
 * <ul>
 *   <li>正常返回时，output 包含用户提交的 payload 数据</li>
 *   <li>超时时，output 包含 timeout=true 和相关错误信息</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see HumanSignalPayload
 * @see WorkflowRuntimeBridge#awaitHumanSignal(String, Duration)
 */
@Component
public class HumanInputNodeExecutor implements WorkflowNodeExecutor {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(HumanInputNodeExecutor.class);

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
        HumanInputNodeExecutor.traceService = traceService;
    }

    /**
     * 判断是否支持指定节点类型
     * <p>
     * 只有 HUMAN_INPUT 类型的节点才由本执行器处理。
     *
     * @param type 节点类型名称
     * @return 如果类型为 HUMAN_INPUT 则返回 true
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.HUMAN_INPUT.name().equals(type);
    }

    /**
     * 执行人工输入节点
     * <p>
     * 暂停工作流执行，等待用户人工输入或超时。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>启动节点追踪记录</li>
     *   <li>解析节点配置中的超时时间</li>
     *   <li>调用 bridge.awaitHumanSignal() 等待信号</li>
     *   <li>根据结果设置执行状态和输出</li>
     *   <li>标记节点执行成功</li>
     * </ol>
     *
     * <h3>返回值说明：</h3>
     * <ul>
     *   <li>收到信号：status=COMPLETED，output=用户提交的payload</li>
     *   <li>超时：status=TIMED_OUT，output包含timeout=true</li>
     * </ul>
     *
     * @param node 编译后的节点定义，包含节点类型和配置
     * @param context 执行上下文，包含工作流变量和执行状态
     * @param bridge 工作流运行时桥接，提供等待信号的能力
     * @return 节点执行结果
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

        // 步骤2：解析超时配置
        Map<String, Object> config = node.getConfig();
        Duration timeout = parseTimeout(config);

        log.info("等待人工输入，节点: {}, 超时时间: {}", node.getId(), timeout);

        // 步骤3：等待人工信号
        // 这里会阻塞工作流执行，直到收到信号或超时
        HumanSignalPayload payload = bridge.awaitHumanSignal(node.getId(), timeout);

        NodeExecutionResult result = new NodeExecutionResult();

        // 步骤4：根据结果设置执行状态和输出
        if (payload == null) {
            // 超时情况
            log.warn("人工输入超时，节点: {}", node.getId());
            result.setStatus(ExecutionStatus.TIMED_OUT);
            result.setOutput(Map.of(
                "timeout", true,
                "nodeId", node.getId(),
                "message", "人工输入超时"
            ));
        } else {
            // 正常收到信号
            log.info("收到人工输入，节点: {}", node.getId());
            result.setStatus(ExecutionStatus.COMPLETED);
            result.setOutput(payload.getPayload());
        }

        // 步骤5：标记节点执行成功
        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), result.getOutput());
            } catch (Exception e) {
                log.warn("标记节点成功失败: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * 解析超时配置
     * <p>
     * 从节点配置中解析超时时间，支持多种格式。
     *
     * <h3>支持的格式：</h3>
     * <ul>
     *   <li>数字秒：60, 3600</li>
     *   <li>小时：24h, 1h</li>
     *   <li>分钟：30m, 60m</li>
     *   <li>秒：60s, 120s</li>
     * </ul>
     *
     * <p>默认超时时间为 24 小时。
     *
     * @param config 节点配置Map
     * @return 解析后的超时时间Duration
     */
    private Duration parseTimeout(Map<String, Object> config) {
        Object timeoutValue = config.get("timeout");
        if (timeoutValue == null) {
            return Duration.ofHours(24);
        }

        // 支持数字类型（秒）
        if (timeoutValue instanceof Number) {
            long seconds = ((Number) timeoutValue).longValue();
            return Duration.ofSeconds(seconds);
        }

        // 支持字符串类型（带单位或不带）
        if (timeoutValue instanceof String) {
            String str = ((String) timeoutValue).trim().toLowerCase();
            if (str.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(str.substring(0, str.length() - 1)));
            } else if (str.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(str.substring(0, str.length() - 1)));
            } else if (str.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(str.substring(0, str.length() - 1)));
            } else {
                return Duration.ofSeconds(Long.parseLong(str));
            }
        }

        return Duration.ofHours(24);
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.engine;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import com.sipc115.helix.integration.workflow.runtime.DslOrchestratorWorkflowImpl;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * DSL 运行时工作流实现类
 * <p>
 * 基于 Temporal 框架实现的工作流运行时，负责执行 DSL 定义的工作流。
 * 提供工作流的启动、信号处理和状态查询功能。
 */
public class DslRuntimeWorkflowImpl implements DslRuntimeWorkflow {

    private static final Logger logger = LoggerFactory.getLogger(DslRuntimeWorkflowImpl.class);

    // 解释器实例（不是 Spring Bean）
    private final DslOrchestratorWorkflowImpl interpreter = new DslOrchestratorWorkflowImpl();

    public DslRuntimeWorkflowImpl() {
        // Temporal 无参构造函数
    }

    @Override
    public void run(ExecutionPlan plan, Map<String, Object> input) {
        logger.info("Starting workflow execution. workflowId={}, version={}",
                   plan.getWorkflowId(), plan.getWorkflowVersion());

        // 使用 Temporal 的 CancellationScope 来处理工作流取消
        CancellationScope cancellationScope = Workflow.newCancellationScope(() -> {
            try {
                // 委托给解释器执行
                interpreter.run(plan, input);
                logger.info("Workflow execution completed successfully.");
            } catch (Exception e) {
                logger.error("Workflow execution failed: {}", e.getMessage(), e);
                throw e;
            }
        });

        // 启动取消作用域并等待完成
        cancellationScope.run();
    }

    @Override
    public void provideHumanInput(HumanSignalPayload payload) {
        logger.info("Received human input signal for node: {}", payload.getNodeId());
        interpreter.provideHumanInput(payload);
    }

    @Override
    public WorkflowStateView currentState() {
        return interpreter.currentState();
    }
}

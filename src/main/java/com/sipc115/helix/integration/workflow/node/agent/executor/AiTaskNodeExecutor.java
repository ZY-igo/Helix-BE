/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiTaskConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.workflow.AiTaskWorkflow;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 任务节点执行器
 * <p>
 * 使用 Temporal Workflow 执行 AI 子流程
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiTaskNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiTaskNodeExecutor.class);

    private final WorkflowServiceStubs serviceStubs;
    private final WorkflowClient workflowClient;

    public AiTaskNodeExecutor() {
        // 初始化 Temporal 客户端
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget("localhost:7233") // 默认 Temporal 服务器地址
                .build();
        this.serviceStubs = WorkflowServiceStubs.newServiceStubs(options);
        this.workflowClient = WorkflowClient.newInstance(serviceStubs);
    }

    @Override
    public boolean supports(String type) {
        return "AI_TASK".equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        log.info("Executing AI task node: {}", node.getId());

        try {
            // 1. 从配置中获取 AI 任务配置
            AiTaskConfig aiTaskConfig = (AiTaskConfig) node.getConfig().get("aiTaskConfig");
            if (aiTaskConfig == null) {
                throw new IllegalArgumentException("AI task config not found");
            }

            // 2. 构建初始状态
            AiTaskState initialState = AiTaskState.builder()
                    .input(context.getVariables())
                    .vars(new HashMap<>(aiTaskConfig.getVars()))
                    .llmConfig(aiTaskConfig.getLlmConfig())
                    .build();

            // 3. 启动 Temporal Workflow
            WorkflowOptions workflowOptions = WorkflowOptions.newBuilder()
                    .setTaskQueue("ai-task-queue")
                    .setWorkflowId("ai-task-" + node.getId() + "-" + System.currentTimeMillis())
                    .build();

            AiTaskWorkflow workflow = workflowClient.newWorkflowStub(AiTaskWorkflow.class, workflowOptions);
            AiTaskState finalState = workflow.execute(aiTaskConfig, initialState);

            // 4. 处理执行结果
            Map<String, Object> output = finalState.getOutput();
            if (output != null) {
                context.getVariables().putAll(output);
                log.info("AI task node completed with output: {}", output);
            } else {
                log.info("AI task node completed without output");
            }

            return NodeExecutionResult.completed();

        } catch (Exception e) {
            log.error("Failed to execute AI task node: {}", node.getId(), e);
            NodeExecutionResult result = NodeExecutionResult.completed();
            result.setStatus(com.sipc115.helix.domain.workflow.ExecutionStatus.FAILED);
            return result;
        } finally {
            // 关闭 Temporal 客户端（可选，在实际应用中可能会保持长连接）
            // serviceStubs.shutdown();
        }
    }
}

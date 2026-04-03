/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionRequest;
import com.sipc115.helix.integration.workflow.engine.DslRuntimeWorkflow;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WorkflowExecutionApplicationService {

    public static final String TASK_QUEUE = "helix-task-queue";

    private final ExecutionPlanRepository executionPlanRepository;
    private final WorkflowClient workflowClient;
    private final WorkflowPersistenceService persistenceService;
    private final WorkflowTraceService traceService;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionApplicationService.class);

    public WorkflowExecutionApplicationService(
            ExecutionPlanRepository executionPlanRepository,
            WorkflowClient workflowClient,
            WorkflowPersistenceService persistenceService,
            WorkflowTraceService traceService) {
        this.executionPlanRepository = executionPlanRepository;
        this.workflowClient = workflowClient;
        this.persistenceService = persistenceService;
        this.traceService = traceService;
    }

    public String start(WorkflowExecutionRequest request) {
        ExecutionPlan plan = loadExecutionPlan(request.getWorkflowId(), request.getWorkflowVersion())
            .orElseThrow(() -> new IllegalArgumentException(
                "Execution plan not found: workflowId=" + request.getWorkflowId() +
                ", version=" + request.getWorkflowVersion()));

        String workflowId = request.getWorkflowId() + "-v" + request.getWorkflowVersion() + "-" + System.currentTimeMillis();

        WorkflowExecutionEntity execution = traceService.startExecution(
            request.getWorkflowId(),
            request.getWorkflowVersion(),
            request.getInput(),
            null
        );

        Map<String, Object> inputWithExecutionId = new HashMap<>();
        if (request.getInput() != null) {
            inputWithExecutionId.putAll(request.getInput());
        }
        inputWithExecutionId.put("_executionId", execution.getId());

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(workflowId)
            .build();

        DslRuntimeWorkflow workflow = workflowClient.newWorkflowStub(DslRuntimeWorkflow.class, options);
        WorkflowClient.start(workflow::run, plan, inputWithExecutionId);
        return workflowId;
    }

    private java.util.Optional<ExecutionPlan> loadExecutionPlan(String workflowId, String version) {
        java.util.Optional<ExecutionPlan> dbPlan = persistenceService.findExecutionPlan(workflowId, version);
        if (dbPlan.isPresent()) {
            logger.info("从数据库加载执行计划。workflowId={}, version={}", workflowId, version);
            return dbPlan;
        }

        logger.warn("数据库未找到，回退到内存仓库。workflowId={}, version={}", workflowId, version);
        return executionPlanRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    public void cancel(String workflowId) {
        WorkflowStub.fromTyped(workflowClient.newWorkflowStub(DslRuntimeWorkflow.class, workflowId)).cancel();
    }
}

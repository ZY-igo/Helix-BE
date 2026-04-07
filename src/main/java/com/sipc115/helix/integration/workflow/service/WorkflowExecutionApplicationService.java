/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.ScheduleSpec;
import com.sipc115.helix.domain.workflow.WorkflowDsl;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionRequest;
import com.sipc115.helix.domain.workflow.WorkflowStartResponse;
import com.sipc115.helix.integration.workflow.engine.DslRuntimeWorkflow;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleIntervalSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkflowExecutionApplicationService {

    public static final String TASK_QUEUE = "helix-task-queue";

    private final WorkflowClient workflowClient;
    private final ScheduleClient scheduleClient;
    private final WorkflowPersistenceService persistenceService;
    private final WorkflowTraceService traceService;
    private final WorkflowDefinitionApplicationService definitionService;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionApplicationService.class);

    public WorkflowExecutionApplicationService(
            WorkflowClient workflowClient,
            ScheduleClient scheduleClient,
            WorkflowPersistenceService persistenceService,
            WorkflowTraceService traceService,
            WorkflowDefinitionApplicationService definitionService) {
        this.workflowClient = workflowClient;
        this.scheduleClient = scheduleClient;
        this.persistenceService = persistenceService;
        this.traceService = traceService;
        this.definitionService = definitionService;
    }

    /**
     * 启动工作流执行
     * <p>
     * 自动查找或编译执行计划，然后启动工作流。
     * 1. 优先查找已发布的执行计划
     * 2. 如果没有已发布的执行计划，查找最新版本并自动编译保存
     * 3. 如果没有任何版本，抛出异常
     */
    public WorkflowStartResponse start(WorkflowExecutionRequest request) {
        ExecutionPlan plan = findOrCompileExecutionPlan(request.getWorkflowId());

        String temporalWorkflowId = request.getWorkflowId() + "-v" + plan.getWorkflowVersion() + "-" + System.currentTimeMillis();

        WorkflowExecutionEntity execution = traceService.startExecution(
            request.getWorkflowId(),
            plan.getWorkflowVersion(),
            request.getInput(),
            null
        );

        Map<String, Object> inputWithExecutionId = new HashMap<>();
        if (request.getInput() != null) {
            inputWithExecutionId.putAll(request.getInput());
        }
        inputWithExecutionId.put(WorkflowConstants.EXECUTION_ID_KEY, execution.getId());

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(temporalWorkflowId)
            .build();

        DslRuntimeWorkflow workflow = workflowClient.newWorkflowStub(DslRuntimeWorkflow.class, options);
        WorkflowClient.start(workflow::run, plan, inputWithExecutionId);

        WorkflowStartResponse response = new WorkflowStartResponse();
        response.setExecutionId(execution.getId());
        response.setTemporalWorkflowId(temporalWorkflowId);
        response.setStatus("STARTED");
        response.setMessage("工作流已启动");
        return response;
    }

    /**
     * 运行工作流调度
     * <p>
     * 自动查找或编译执行计划，然后创建 Temporal Schedule。
     * 1. 优先查找已发布的执行计划
     * 2. 如果没有已发布的执行计划，查找最新版本并自动编译保存
     * 3. 如果没有任何版本，抛出异常
     * scheduleId 格式: {workflowId}
     */
    public String runWorkflow(String workflowId) {
        ExecutionPlan plan = findOrCompileExecutionPlan(workflowId);

        ScheduleSpec dslSchedule = plan.getSchedule();
        if (dslSchedule == null || !Boolean.TRUE.equals(dslSchedule.getEnabled())) {
            throw new IllegalStateException("Workflow does not have an enabled schedule. workflowId=" + workflowId);
        }

        String version = plan.getWorkflowVersion();
        String temporalWorkflowId = workflowId + "-scheduled-" + version + "-" + System.currentTimeMillis();

        WorkflowOptions workflowOptions = WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(temporalWorkflowId)
            .build();

        ScheduleActionStartWorkflow action =
            ScheduleActionStartWorkflow.newBuilder()
                .setWorkflowType(DslRuntimeWorkflow.class)
                .setArguments(plan, new HashMap<>())
                .setOptions(workflowOptions)
                .build();

        io.temporal.client.schedules.ScheduleSpec.Builder specBuilder =
            io.temporal.client.schedules.ScheduleSpec.newBuilder();

        if (dslSchedule.getCron() != null && !dslSchedule.getCron().isEmpty()) {
            specBuilder.setIntervals(Collections.singletonList(
                new ScheduleIntervalSpec(Duration.ofMinutes(1))));
            if (dslSchedule.getTimezone() != null && !dslSchedule.getTimezone().isEmpty()) {
                specBuilder.setTimeZoneName(dslSchedule.getTimezone());
            }
        } else if (dslSchedule.getIntervalMs() != null && dslSchedule.getIntervalMs() > 0) {
            specBuilder.setIntervals(Collections.singletonList(
                new ScheduleIntervalSpec(Duration.ofMillis(dslSchedule.getIntervalMs()))));
        } else {
            throw new IllegalArgumentException("Schedule must have either cron or intervalMs configured");
        }

        Schedule schedule = Schedule.newBuilder()
            .setAction(action)
            .setSpec(specBuilder.build())
            .build();

        scheduleClient.createSchedule(workflowId, schedule, null);

        try {
            persistenceService.updateDslState(
                workflowId,
                version,
                com.sipc115.helix.domain.workflow.WorkflowState.RUNNING.name(),
                null
            );
        } catch (Exception e) {
            logger.warn("更新运行状态失败，继续流程。workflowId={}, version={}", workflowId, version);
        }

        logger.info("调度创建成功: scheduleId={}, cron={}, intervalMs={}",
                workflowId, dslSchedule.getCron(), dslSchedule.getIntervalMs());
        return workflowId;
    }

    /**
     * 查找或编译执行计划
     * <p>
     * 1. 优先返回已发布的执行计划
     * 2. 如果没有已发布的执行计划，查找最新版本的 DSL 并自动编译保存
     * 3. 如果没有任何版本，抛出异常
     */
    private ExecutionPlan findOrCompileExecutionPlan(String workflowId) {
        Optional<ExecutionPlan> publishedPlan = persistenceService.findLatestPublishedExecutionPlan(workflowId);
        if (publishedPlan.isPresent()) {
            logger.info("使用已发布的执行计划。workflowId={}, version={}",
                    workflowId, publishedPlan.get().getWorkflowVersion());
            return publishedPlan.get();
        }

        logger.info("未找到已发布的执行计划，尝试查找最新版本并自动编译。workflowId={}", workflowId);

        Optional<WorkflowDsl> latestDsl = persistenceService.findLatestDsl(workflowId);
        if (latestDsl.isEmpty()) {
            throw new IllegalArgumentException("No workflow found for workflowId: " + workflowId);
        }

        WorkflowDsl dsl = latestDsl.get();
        logger.info("找到最新 DSL 版本，正在编译。workflowId={}, version={}",
                workflowId, dsl.getVersion());

        ExecutionPlan compiledPlan = definitionService.saveAndCompile(dsl);
        logger.info("自动编译完成。workflowId={}, version={}, planId={}",
                workflowId, dsl.getVersion(), compiledPlan.getPlanId());

        return compiledPlan;
    }

    /**
     * 暂停调度
     */
    public void pauseSchedule(String scheduleId) {
        scheduleClient.getHandle(scheduleId).pause();
        logger.info("调度已暂停: scheduleId={}", scheduleId);
    }

    /**
     * 恢复调度
     */
    public void resumeSchedule(String scheduleId) {
        scheduleClient.getHandle(scheduleId).unpause();
        logger.info("调度已恢复: scheduleId={}", scheduleId);
    }

    /**
     * 删除调度
     */
    public void deleteSchedule(String scheduleId) {
        scheduleClient.getHandle(scheduleId).delete();

        try {
            String runningVersion = persistenceService.findRunningVersion(scheduleId);
            if (runningVersion != null) {
                persistenceService.updateDslState(
                    scheduleId,
                    runningVersion,
                    com.sipc115.helix.domain.workflow.WorkflowState.STOPPED.name(),
                    null
                );
            }
        } catch (Exception e) {
            logger.warn("更新停用状态失败，继续流程。scheduleId={}", scheduleId);
        }

        logger.info("调度已删除: scheduleId={}", scheduleId);
    }

    public void cancel(String workflowId) {
        WorkflowStub.fromTyped(workflowClient.newWorkflowStub(DslRuntimeWorkflow.class, workflowId)).cancel();
    }

    /**
     * 向工作流发送人工输入信号
     * <p>
     * 用于唤醒处于等待人工输入状态的工作流实例。
     *
     * @param workflowId Temporal 工作流 ID
     * @param payload 包含节点 ID 和用户输入数据的载荷
     */
    public void sendHumanSignal(String workflowId, com.sipc115.helix.domain.workflow.HumanSignalPayload payload) {
        logger.info("发送人工输入信号: workflowId={}, nodeId={}", workflowId, payload.getNodeId());

        DslRuntimeWorkflow workflow = workflowClient.newWorkflowStub(DslRuntimeWorkflow.class, workflowId);
        workflow.provideHumanInput(payload);

        logger.info("人工输入信号已发送: workflowId={}, nodeId={}", workflowId, payload.getNodeId());
    }
}
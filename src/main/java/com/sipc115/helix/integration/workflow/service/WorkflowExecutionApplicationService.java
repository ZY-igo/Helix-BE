/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.service;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.domain.workflow.WorkflowExecutionRequest;
import com.sipc115.helix.domain.workflow.WorkflowStateView;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import com.sipc115.helix.integration.workflow.runtime.DslOrchestratorWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 工作流执行应用服务类
 * <p>
 * 该服务类是 Helix 系统中负责工作流执行的核心组件，基于 Temporal 工作流引擎实现。
 * 主要功能包括：
 * 1. 启动新的工作流实例
 * 2. 向运行中的工作流发送人工输入信号
 * 3. 查询工作流的当前状态
 * 4. 取消运行中的工作流
 * <p>
 * 该服务通过 Spring 注解 {@code @Service} 标记，作为应用层服务被其他组件调用。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class WorkflowExecutionApplicationService {
    /**
     * Temporal 任务队列名称
     * <p>
     * 用于分配工作流任务给对应的 Worker，确保工作流能够被正确执行。
     * 所有处理该类型工作流的 Worker 都需要监听此任务队列。
     */
    public static final String TASK_QUEUE = "dsl-workflow-task-queue";

    /**
     * 执行计划仓库
     * <p>
     * 用于存储和检索工作流执行计划，根据工作流 ID 和版本号查询对应的执行计划。
     */
    private final ExecutionPlanRepository executionPlanRepository;

    /**
     * 日志记录器
     * <p>
     * 用于记录应用程序运行时的日志信息，方便调试和排查问题。
     */
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionApplicationService.class);

    /**
     * Temporal 工作流客户端
     * <p>
     * 用于与 Temporal 服务进行交互，创建工作流存根、启动工作流、发送信号等操作。
     */
    private final WorkflowClient workflowClient;

    // ⭐ 新增：注入持久化服务（解耦的核心）
    private final WorkflowPersistenceService persistenceService;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取执行计划仓库和工作流客户端实例。
     *
     * @param executionPlanRepository 执行计划仓库实例
     * @param workflowClient 工作流客户端实例
     * @param persistenceService 工作流持久化服务
     */
    public WorkflowExecutionApplicationService(ExecutionPlanRepository executionPlanRepository,
                                               WorkflowClient workflowClient,
                                               WorkflowPersistenceService persistenceService) {
        this.executionPlanRepository = executionPlanRepository;
        this.workflowClient = workflowClient;
        this.persistenceService = persistenceService;
    }

    /**
     * 启动一个新的工作流实例
     * <p>
     * 根据提供的工作流执行请求，查找对应的执行计划并启动工作流。
     * 生成唯一的工作流实例 ID，确保每个实例的唯一性。
     *
     * @param request 工作流执行请求对象，包含工作流 ID、版本号和输入参数
     * @return 生成的工作流实例 ID，格式为：{workflowId}-v{version}-{timestamp}
     * @throws IllegalArgumentException 当找不到对应的执行计划时抛出异常
     */
    public String start(WorkflowExecutionRequest request) {
        // ⭐ 优先从数据库读取执行计划（新方式）
        ExecutionPlan plan = loadExecutionPlan(request.getWorkflowId(), request.getWorkflowVersion())
            .orElseThrow(() -> new IllegalArgumentException(
                "Execution plan not found: workflowId=" + request.getWorkflowId() +
                ", version=" + request.getWorkflowVersion()));

        // 生成唯一的工作流实例 ID
        // 格式为：{workflowId}-v{version}-{timestamp}，使用时间戳确保唯一性
        String workflowId = request.getWorkflowId() + "-v" + request.getWorkflowVersion() + "-" + System.currentTimeMillis();

        // 配置工作流选项
        // 设置任务队列为 TASK_QUEUE，工作流 ID 为生成的唯一 ID
        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(workflowId)
            .build();

        // 创建工作流存根
        // 使用工作流客户端创建 DslOrchestratorWorkflow 类型的工作流存根
        DslOrchestratorWorkflowImpl workflow = workflowClient.newWorkflowStub(DslOrchestratorWorkflowImpl.class, options);

        // 异步启动工作流
        // 传入执行计划和输入参数，工作流将在后台异步执行
        WorkflowClient.start(workflow::run, plan, request.getInput());
        return workflowId;
    }

    /**
     * 加载执行计划
     * <p>
     * 优先从数据库加载，如果不存在则回退到内存仓库。
     *
     * @param workflowId 工作流 ID
     * @param version 版本号
     * @return 执行计划的 Optional 对象
     */
    private java.util.Optional<ExecutionPlan> loadExecutionPlan(String workflowId, Integer version) {
        // ① 尝试从数据库读取（新方式）
        java.util.Optional<ExecutionPlan> dbPlan = persistenceService.findExecutionPlan(workflowId, version);
        if (dbPlan.isPresent()) {
            logger.info("✅ 从数据库加载执行计划。workflowId={}, version={}", workflowId, version);
            return dbPlan;
        }

        // ② 回退到内存仓库（向后兼容）
        logger.warn("⚠️ 数据库未找到，回退到内存仓库。workflowId={}, version={}", workflowId, version);
        return executionPlanRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    /**
     * 取消运行中的工作流
     * <p>
     * 取消指定的工作流实例，会触发工作流的取消处理逻辑，
     * 包括清理资源和执行必要的回滚操作。
     *
     * @param workflowId 工作流实例 ID，用于标识要取消的工作流实例
     */
    public void cancel(String workflowId) {
        // 取消工作流
        // 1. 首先创建 DslOrchestratorWorkflow 类型的工作流存根
        // 2. 将其转换为通用的 WorkflowStub
        // 3. 调用 cancel 方法取消工作流
        WorkflowStub.fromTyped(workflowClient.newWorkflowStub(DslOrchestratorWorkflowImpl.class, workflowId)).cancel();
    }
}

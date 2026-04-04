/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.config;

import com.sipc115.helix.domain.workflow.ExecutionPlan;
import com.sipc115.helix.integration.workflow.engine.DslRuntimeWorkflowImpl;
import com.sipc115.helix.integration.workflow.repository.InMemoryExecutionPlanRepository;
import com.sipc115.helix.integration.workflow.spi.ExecutionPlanRepository;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporal Worker 管理器
 * <p>
 * 负责 Temporal Worker 的生命周期管理，包括：
 * <ul>
 *   <li>Worker 的初始化和启动</li>
 *   <li>Workflow 的动态注册和更新</li>
 *   <li>Worker 的优雅关闭</li>
 * </ul>
 *
 * <p>核心概念：
 * <ul>
 *   <li>Worker：Temporal 中的工作线程，负责执行 Workflow 和 Activity</li>
 *   <li>Task Queue：任务队列，Worker 从这里获取任务</li>
 *   <li>Workflow：工作流定义，由 Worker 执行</li>
 *   <li>Activity：具体的工作单元，由 Worker 执行</li>
 * </ul>
 *
 * <p>使用方式：
 * <pre>
 * // 1. Spring 容器启动时自动初始化 Worker
 * // 2. 发布新版本工作流时调用 registerWorkflow()
 * // 3. 下线工作流时调用 unregisterWorkflow()
 * // 4. 应用关闭时自动调用 shutdown() 优雅关闭
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class TemporalWorkerManager {

    /**
     * 日志记录器
     * <p>
     * 用于记录 Worker 管理相关的日志信息，包括初始化、注册、更新和关闭等操作。
     */
    private static final Logger log = LoggerFactory.getLogger(TemporalWorkerManager.class);

    /**
     * 默认任务队列名称
     * <p>
     * 所有 Helix 工作流都使用这个统一的任务队列。
     * 任务队列是 Temporal 中的核心概念，用于分发任务给 Worker。
     *
     * <p>设计考虑：
     * 使用单一任务队列可以简化 Worker 管理，
     * 所有 Workflow 和 Activity 都在同一个队列中执行。
     */
    private static final String DEFAULT_TASK_QUEUE = "helix-task-queue";

    /**
     * Temporal 工作流客户端
     * <p>
     * 用于与 Temporal 服务端通信，创建 Workflow 存根等。
     * 由 Spring 通过 @Autowired 自动注入。
     */
    private final WorkflowClient workflowClient;

    /**
     * 所有 Activity 实例列表
     * <p>
     * Spring 自动注入所有实现了 Activity 接口的 Bean。
     * 这些 Activity 会在 Worker 初始化时注册到 Temporal。
     */
    private final List<Object> allActivities;

    /**
     * 执行计划仓库
     * <p>
     * 用于存储和管理已注册的工作流执行计划。
     * 当 Temporal 启动工作流实例时，会从这里获取对应的 ExecutionPlan。
     * 采用依赖注入获取 InMemoryExecutionPlanRepository 实例。
     */
    private final ExecutionPlanRepository executionPlanRepository;

    /**
     * Worker 工厂
     * <p>
     * 用于创建和管理 Worker 实例。
     * 在 init() 方法中初始化，在 shutdown() 方法中关闭。
     */
    private WorkerFactory workerFactory;

    /**
     * 默认 Worker 实例
     * <p>
     * 应用启动时创建的默认 Worker，负责执行所有 Helix 工作流。
     */
    private Worker defaultWorker;

    /**
     * 按工作流 ID 和版本存储的 Worker 映射
     * <p>
     * 用于支持多版本工作流共存。
     * Key 格式为 "workflowId:version"，Value 为对应的 Worker 实例。
     *
     * <p>使用 ConcurrentHashMap 保证线程安全，
     * 支持在运行时动态注册和注销工作流。
     */
    private final Map<String, Worker> workflowWorkers = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取 WorkflowClient、ExecutionPlanRepository 和所有 Activity 实例。
     *
     * @param workflowClient Temporal 工作流客户端
     * @param executionPlanRepository 执行计划仓库
     * @param allActivities 所有 Activity 实例列表
     */
    @Autowired
    public TemporalWorkerManager(
            WorkflowClient workflowClient,
            ExecutionPlanRepository executionPlanRepository,
            List<Object> allActivities) {
        this.workflowClient = workflowClient;
        this.executionPlanRepository = executionPlanRepository;
        this.allActivities = allActivities;
    }

    /**
     * 初始化 Worker
     * <p>
     * 在 Spring 容器启动完成后自动调用（@PostConstruct）。
     * 执行以下操作：
     * <ol>
     *   <li>创建 WorkerFactory 实例</li>
     *   <li>创建默认 Worker 并绑定到任务队列</li>
     *   <li>注册 DslRuntimeWorkflowImpl 工作流实现</li>
     *   <li>注册所有 Activity 实例</li>
     *   <li>启动 WorkerFactory，开始处理任务</li>
     * </ol>
     *
     * <p>重要说明：
     * Temporal 的工作流定义是不可变的（Immutable）。
     * 这意味着：
     * <ul>
     *   <li>工作流代码不能修改，只能创建新版本</li>
     *   <li>运行中的实例会继续使用启动时的版本</li>
     *   <li>新启动的实例可以使用新版本</li>
     * </ul>
     *
     * <p>架构设计说明：
     * Helix 使用单一通用工作流实现（DslRuntimeWorkflowImpl）来执行所有 DSL 工作流。
     * 工作流的差异（节点、转换、调度规则等）存储在 ExecutionPlan 中，
     * 由工作流实现类在运行时解释执行。这种设计避免了为每个工作流版本创建独立的 Worker。
     *
     * @see DslRuntimeWorkflowImpl
     */
    @PostConstruct
    public void init() {
        // 步骤1：创建 WorkerFactory
        // WorkerFactory 是创建 Worker 的工厂类，负责管理所有 Worker 实例
        this.workerFactory = WorkerFactory.newInstance(workflowClient);

        // 步骤2：创建默认 Worker
        // 每个 Worker 必须绑定到一个任务队列，从该队列中获取任务
        this.defaultWorker = workerFactory.newWorker(DEFAULT_TASK_QUEUE);

        // 步骤3：注册工作流实现
        // DslRuntimeWorkflowImpl 是 Helix 自定义的工作流实现类
        // 它负责解释执行编译后的 ExecutionPlan
        // 由于使用通用工作流实现，工作流的具体定义（ExecutionPlan）存储在仓库中
        this.defaultWorker.registerWorkflowImplementationTypes(
            DslRuntimeWorkflowImpl.class
        );

        // 步骤4：注册所有 Activity 实现
        // Spring 自动收集所有实现了 Activity 接口的 Bean
        // 这些 Activity 会被 Worker 用来执行具体的工作单元
        this.defaultWorker.registerActivitiesImplementations(
            allActivities.toArray(new Object[0])
        );

        // 步骤5：启动 WorkerFactory
        // 启动后 Worker 开始从任务队列中获取并执行任务
        // 这是一个阻塞操作，应用启动后 Worker 会持续运行
        this.workerFactory.start();

        log.info("TemporalWorkerManager 初始化完成，已启动默认 Worker，任务队列: {}",
                DEFAULT_TASK_QUEUE);
    }

    /**
     * 关闭 Worker
     * <p>
     * 在 Spring 容器关闭时自动调用（@PreDestroy）。
     * 执行以下操作：
     * <ol>
     *   <li>平滑关闭 WorkerFactory，停止接收新任务</li>
     *   <li>等待正在执行的任务完成</li>
     *   <li>释放资源</li>
     * </ol>
     *
     * <p>重要说明：
     * shutdown() 是一个优雅关闭（Graceful Shutdown）过程。
     * 它会等待正在执行的工作流实例完成，但不会再接收新的任务。
     * 默认等待时间为 10 秒。
     *
     * <p>建议：
     * 在生产环境中，确保在关闭应用前给 Worker 足够的时间完成当前任务。
     * 可以通过配置 Temporal 服务的 shutdown 超时时间来实现。
     */
    @PreDestroy
    public void shutdown() {
        if (workerFactory != null) {
            // shutdown() 会等待已启动的工作流实例完成
            // 可以通过参数指定超时时间
            workerFactory.shutdown();
            log.info("TemporalWorkerManager 已关闭，正在等待工作流实例完成...");
        }
    }

    /**
     * 注册工作流
     * <p>
     * 当发布新版本工作流时调用此方法进行注册。
     * Temporal 的工作流定义是不可变的，所以每次修改都相当于注册新版本。
     *
     * <p>注册流程：
     * <ol>
     *   <li>构建工作流唯一标识键（workflowId:version）</li>
     *   <li>验证 ExecutionPlan 的完整性和正确性</li>
     *   <li>将 ExecutionPlan 保存到仓库（供运行时使用）</li>
     *   <li>更新内存中的工作流映射</li>
     * </ol>
     *
     * <p>架构说明：
     * Helix 使用通用工作流实现（DslRuntimeWorkflowImpl），因此：
     * <ul>
     *   <li>不需要为每个工作流版本创建独立的 Worker</li>
     *   <li>只需要将 ExecutionPlan 保存到仓库即可</li>
     *   <li>工作流启动时从仓库获取 ExecutionPlan 并传递给通用实现</li>
     * </ul>
     *
     * <p>版本管理策略：
     * <ul>
     *   <li>发布新版本时，旧版本继续运行已有实例</li>
     *   <li>新实例自动使用新版本的工作流定义</li>
     *   <li>通过 DEPRECATED 状态标记不再使用的工作流版本</li>
     * </ul>
     *
     * @param plan 执行计划，包含工作流的完整定义
     * @see ExecutionPlan
     */
    public void registerWorkflow(ExecutionPlan plan) {
        // 步骤1：构建唯一标识键，格式为 "workflowId:version"
        // 例如：daily-report:v1.0.0
        String workflowKey = buildWorkflowKey(plan.getWorkflowId(), plan.getWorkflowVersion());

        log.info("正在注册工作流: {}", workflowKey);

        // 步骤2：验证 ExecutionPlan 的完整性
        validateExecutionPlan(plan, workflowKey);

        // 步骤3：保存到执行计划仓库
        // 这是核心步骤：工作流启动时会从这里获取 ExecutionPlan
        executionPlanRepository.save(plan);

        // 步骤4：更新内存中的工作流映射（用于追踪和管理）
        workflowWorkers.put(workflowKey, defaultWorker);

        log.info("工作流注册完成: {}, 节点数量: {}, 入口节点: {}",
                workflowKey,
                plan.getNodes() != null ? plan.getNodes().size() : 0,
                plan.getEntryNodeId());
    }

    /**
     * 注销工作流
     * <p>
     * 当工作流版本下线或废弃时调用此方法。
     *
     * <p>注销说明：
     * <ul>
     *   <li>正在运行的工作流实例不会受到影响</li>
     *   <li>新创建的工作流实例将无法使用此版本（因为仓库中已删除）</li>
     *   <li>建议使用 DEPRECATED 状态而非直接注销</li>
     * </ul>
     *
     * <p>实现说明：
     * 由于 Helix 使用通用工作流实现，注销操作主要是从仓库中删除 ExecutionPlan。
     * Temporal Worker 本身继续运行，因为它们是通用的。
     *
     * @param workflowId 工作流 ID
     * @param version 工作流版本号
     */
    public void unregisterWorkflow(String workflowId, String version) {
        String workflowKey = buildWorkflowKey(workflowId, version);

        log.info("正在注销工作流: {}", workflowKey);

        // 步骤1：从执行计划仓库中删除
        // 这会导致新启动的工作流实例无法找到该版本
        executionPlanRepository.deleteByWorkflowIdAndVersion(workflowId, version);

        // 步骤2：从内存映射中移除
        workflowWorkers.remove(workflowKey);

        log.info("工作流注销完成: {}", workflowKey);
    }

    /**
     * 更新工作流
     * <p>
     * 当工作流版本需要更新时调用此方法。
     * 实际上，由于 Temporal 的不可变性，工作流更新相当于注册新版本。
     *
     * <p>更新策略：
     * <ul>
     *   <li>旧版本保持 DEPRECATED 状态，运行中的实例继续执行</li>
     *   <li>新版本注册为 PUBLISHED 状态</li>
     *   <li>Temporal 自动确保实例使用启动时的版本</li>
     * </ul>
     *
     * <p>实现说明：
     * 直接调用 registerWorkflow() 方法即可完成更新。
     * ExecutionPlanRepository.save() 会自动覆盖相同 workflowId:version 的记录。
     *
     * @param newPlan 新的执行计划
     */
    public void updateWorkflow(ExecutionPlan newPlan) {
        String workflowKey = buildWorkflowKey(
                newPlan.getWorkflowId(),
                newPlan.getWorkflowVersion()
        );

        log.info("正在更新工作流注册: {}", workflowKey);

        // 直接调用 registerWorkflow 即可
        // Repository.save() 会自动覆盖旧版本
        registerWorkflow(newPlan);
    }

    /**
     * 获取已注册的工作流
     * <p>
     * 根据工作流 ID 和版本获取已注册的 ExecutionPlan。
     *
     * @param workflowId 工作流 ID
     * @param version 工作流版本
     * @return ExecutionPlan，如果不存在则返回 null
     */
    public ExecutionPlan getRegisteredWorkflow(String workflowId, String version) {
        String workflowKey = buildWorkflowKey(workflowId, version);
        return executionPlanRepository.findByWorkflowIdAndVersion(workflowId, version).orElse(null);
    }

    /**
     * 获取所有已注册的工作流
     * <p>
     * 返回所有已注册的工作流执行计划列表。
     *
     * @return 已注册的工作流列表
     */
    public List<ExecutionPlan> getAllRegisteredWorkflows() {
        return executionPlanRepository.findAll();
    }

    /**
     * 检查工作流是否已注册
     *
     * @param workflowId 工作流 ID
     * @param version 工作流版本
     * @return 是否已注册
     */
    public boolean isWorkflowRegistered(String workflowId, String version) {
        String workflowKey = buildWorkflowKey(workflowId, version);
        return workflowWorkers.containsKey(workflowKey);
    }

    /**
     * 验证执行计划的完整性和正确性
     * <p>
     * 在注册工作流之前验证 ExecutionPlan 的必要字段和结构。
     *
     * @param plan 执行计划
     * @param workflowKey 工作流唯一标识键
     * @throws IllegalArgumentException 如果验证失败
     */
    private void validateExecutionPlan(ExecutionPlan plan, String workflowKey) {
        // 验证基本信息
        if (plan == null) {
            throw new IllegalArgumentException("ExecutionPlan 不能为空: " + workflowKey);
        }

        if (plan.getWorkflowId() == null || plan.getWorkflowId().isEmpty()) {
            throw new IllegalArgumentException("WorkflowId 不能为空: " + workflowKey);
        }

        if (plan.getWorkflowVersion() == null || plan.getWorkflowVersion().isEmpty()) {
            throw new IllegalArgumentException("WorkflowVersion 不能为空: " + workflowKey);
        }

        // 验证节点定义
        if (plan.getNodes() == null || plan.getNodes().isEmpty()) {
            throw new IllegalArgumentException("节点列表不能为空: " + workflowKey);
        }

        // 验证入口节点
        if (plan.getEntryNodeId() == null || plan.getEntryNodeId().isEmpty()) {
            throw new IllegalArgumentException("入口节点 ID 不能为空: " + workflowKey);
        }

        if (!plan.getNodes().containsKey(plan.getEntryNodeId())) {
            throw new IllegalArgumentException("入口节点不存在: " + plan.getEntryNodeId() + ", " + workflowKey);
        }

        // 验证所有引用的节点都存在
        if (plan.getTransitions() != null) {
            for (var transition : plan.getTransitions()) {
                if (transition.getFrom() != null && !plan.getNodes().containsKey(transition.getFrom())) {
                    throw new IllegalArgumentException("转换引用的源节点不存在: " + transition.getFrom());
                }
                if (transition.getTo() != null && !plan.getNodes().containsKey(transition.getTo())) {
                    throw new IllegalArgumentException("转换引用的目标节点不存在: " + transition.getTo());
                }
            }
        }

        log.debug("ExecutionPlan 验证通过: {}", workflowKey);
    }

    /**
     * 构建工作流唯一标识键
     * <p>
     * 使用 workflowId:version 格式构建唯一键。
     * 这个键用于在 workflowWorkers 映射中标识不同版本的工作流。
     *
     * <p>键格式示例：
     * <ul>
     *   <li>daily-report:v1.0.0</li>
     *   <li>daily-report:v2.0-beta</li>
     *   <li>user-onboarding:v1.0.1</li>
     * </ul>
     *
     * @param workflowId 工作流 ID
     * @param version 工作流版本
     * @return 格式化的唯一键
     */
    private String buildWorkflowKey(String workflowId, String version) {
        return workflowId + ":" + version;
    }
}

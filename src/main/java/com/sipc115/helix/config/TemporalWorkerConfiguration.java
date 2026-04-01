/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.config;

import com.sipc115.helix.integration.workflow.engine.DslRuntimeWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Temporal Worker 配置类
 * <p>
 * 配置并启动 Temporal Worker，自动注册所有 Workflow 和 Activity。
 * <p>
 * 特性：
 * <ul>
 *   <li>自动注册 DslRuntimeWorkflowImpl 工作流</li>
 *   <li>自动注入并注册所有 Activity Bean</li>
 *   <li>使用 @PostConstruct 在 Spring 容器启动后自动启动 Worker</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Configuration
public class TemporalWorkerConfiguration {

    /**
     * 任务队列名称
     */
    private static final String TASK_QUEUE = "helix-task-queue";

    /**
     * WorkflowClient，用于创建 WorkerFactory
     */
    @Autowired
    private WorkflowClient workflowClient;

    /**
     * 所有 Activity Bean 列表
     * <p>
     * Spring 会自动注入所有标记为 @Component 且实现了 Activity 接口的 Bean。
     * 这样可以自动注册所有 Activity，无需手动逐个添加。
     */
    @Autowired
    private List<Object> allActivities;

    /**
     * 创建并启动 WorkerFactory
     * <p>
     * 使用 @PostConstruct 注解确保在 Spring 容器初始化完成后自动启动 Worker。
     * <p>
     * 工作流程：
     * <ol>
     *   <li>创建 WorkerFactory 实例</li>
     *   <li>创建 Worker 并指定任务队列</li>
     *   <li>注册 DslRuntimeWorkflowImpl 工作流实现</li>
     *   <li>自动注册所有 Activity Bean</li>
     *   <li>启动 WorkerFactory</li>
     * </ol>
     */
    @PostConstruct
    public void initWorker() {
        // 创建 WorkerFactory
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);

        // 创建 Worker，指定任务队列
        Worker worker = factory.newWorker(TASK_QUEUE);

        // ⭐ 注册 Workflow 实现
        worker.registerWorkflowImplementationTypes(
            DslRuntimeWorkflowImpl.class
        );

        // ⭐ 自动注册所有 Activity
        // 将 List 转换为数组并注册，无需手动指定每个 Activity
        worker.registerActivitiesImplementations(
            allActivities.toArray(new Object[0])
        );

        // 启动 WorkerFactory
        factory.start();
    }

    /**
     * 创建 WorkflowClient Bean
     * <p>
     * 用于与 Temporal 服务通信的客户端。
     *
     * @param serviceStubs WorkflowServiceStubs 实例
     * @return WorkflowClient 实例
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    /**
     * 创建 WorkflowServiceStubs Bean
     * <p>
     * 用于连接 Temporal 服务的存根。
     *
     * @return WorkflowServiceStubs 实例
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }
}

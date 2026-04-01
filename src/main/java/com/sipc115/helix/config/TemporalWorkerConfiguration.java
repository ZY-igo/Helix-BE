/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.config;

import com.sipc115.helix.integration.workflow.service.WorkflowExecutionApplicationService;
import com.sipc115.helix.integration.workflow.bridge.temporal.activity.ActivityTaskRouterActivityImpl;
import com.sipc115.helix.integration.workflow.runtime.DslOrchestratorWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal 工作流 Worker 配置类
 * <p>
 * 负责配置和初始化 Temporal 工作流引擎的核心组件，包括：
 * 1. WorkflowServiceStubs - 与 Temporal 服务通信的客户端存根
 * 2. WorkflowClient - 用于启动和管理工作流的客户端
 * 3. WorkerFactory - 用于创建和管理 Worker 实例
 * <p>
 * 通过 Spring 注解 {@code @Configuration} 标记，作为配置类被 Spring 容器管理。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Configuration
public class TemporalWorkerConfiguration {

    /**
     * 创建 WorkflowServiceStubs 实例
     * <p>
     * 使用本地服务存根，连接到本地运行的 Temporal 服务。
     * 
     * @return WorkflowServiceStubs 实例
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        // 创建本地服务存根，连接到本地运行的 Temporal 服务
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    /**
     * 创建 WorkflowClient 实例
     * <p>
     * 使用提供的 WorkflowServiceStubs 创建 WorkflowClient，用于与 Temporal 服务交互。
     * 
     * @param workflowServiceStubs WorkflowServiceStubs 实例
     * @return WorkflowClient 实例
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        // 使用 WorkflowServiceStubs 创建 WorkflowClient
        return WorkflowClient.newInstance(workflowServiceStubs);
    }

    /**
     * 创建 WorkerFactory 实例
     * <p>
     * 创建并配置 WorkerFactory，注册工作流实现和活动实现，
     * 并设置初始化和销毁方法。
     * 
     * @param workflowClient WorkflowClient 实例
     * @param activityTaskRouterActivity 活动任务路由器实现
     * @return WorkerFactory 实例
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public WorkerFactory workerFactory(WorkflowClient workflowClient,
                                       ActivityTaskRouterActivityImpl activityTaskRouterActivity) {
        // 创建 WorkerFactory 实例
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        
        // 创建 Worker 实例，使用 TASK_QUEUE 作为任务队列
        Worker worker = factory.newWorker(WorkflowExecutionApplicationService.TASK_QUEUE);
        
        // 注册工作流实现类型
        worker.registerWorkflowImplementationTypes(DslOrchestratorWorkflowImpl.class);
        
        // 注册活动实现
        worker.registerActivitiesImplementations(activityTaskRouterActivity);
        
        return factory;
    }
}

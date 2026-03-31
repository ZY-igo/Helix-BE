package com.sipc115.helix.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal 配置类
 * <p>
 * 初始化工作流客户端和服务端连接
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Configuration
public class TemporalConfig {

    /**
     * Temporal 服务端连接地址
     */
    @Value("${temporal.connection.target:192.168.115.23:7233}")
    private String temporalTarget;

    /**
     * Temporal 命名空间
     */
    @Value("${temporal.namespace:default}")
    private String namespace;

    /**
     * 创建 WorkflowServiceStubs Bean
     * <p>
     * 连接到 Temporal 服务端
     * </p>
     * 
     * @return WorkflowServiceStubs 实例
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalTarget)
                .build();
        return WorkflowServiceStubs.newInstance(options);
    }

    /**
     * 创建 WorkflowClient Bean
     * <p>
     * 用于启动和查询工作流
     * </p>
     * 
     * @param workflowServiceStubs 工作流服务存根
     * @return WorkflowClient 实例
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .build();
        return WorkflowClient.newInstance(workflowServiceStubs, options);
    }
}

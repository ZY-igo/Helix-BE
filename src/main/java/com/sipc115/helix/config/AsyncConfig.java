package com.sipc115.helix.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步配置类
 * <p>
 * 配置线程池执行器，用于处理异步任务
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Configuration
public class AsyncConfig {

    /**
     * 创建报告任务执行器
     * 
     * @return 线程池执行器
     */
    @Bean(name = "reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(2);
        // 设置最大线程数
        executor.setMaxPoolSize(4);
        // 设置队列容量
        executor.setQueueCapacity(100);
        // 设置线程名称前缀
        executor.setThreadNamePrefix("report-worker-");
        // 初始化执行器
        executor.initialize();
        return executor;
    }
}

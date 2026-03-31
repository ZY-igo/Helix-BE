package com.sipc115.helix.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 报告任务调度器
 * <p>
 * 负责异步调度报告任务的执行
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Service
public class ReportJobDispatcher {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ReportJobDispatcher.class);

    /**
     * 任务执行服务
     */
    private final TaskExecutionService taskExecutionService;

    /**
     * 构造函数
     * 
     * @param taskExecutionService 任务执行服务
     */
    public ReportJobDispatcher(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * 调度报告任务
     * <p>
     * 异步执行报告任务，支持强制执行和指定触发源
     * </p>
     * 
     * @param taskId 任务ID
     * @param force 是否强制执行
     * @param triggerSource 触发源
     */
    @Async("reportTaskExecutor")
    public void dispatch(String taskId, boolean force, String triggerSource) {
        log.info("[Dispatch] Job accepted. source={}, taskId={}, force={}, next=run task execution",
                triggerSource, taskId, force);
        taskExecutionService.run(taskId, force);
        log.info("[Dispatch] Job finished. source={}, taskId={}", triggerSource, taskId);
    }
}

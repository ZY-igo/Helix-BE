package com.sipc115.helix.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日报调度器
 * <p>
 * 负责按计划触发日报生成任务
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Component
public class DailyReportScheduler {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);

    /**
     * 每日执行的调度任务
     * <p>
     * 根据配置的 cron 表达式定时触发
     * </p>
     */
    @Scheduled(cron = "${app.report.cron}", zone = "${app.report.zone-id}")
    public void runDaily() {
        log.info("[Scheduler] Trigger received, but task-based scheduling is TODO.");
    }
}

package com.sipc115.helix.job;

import com.sipc115.helix.service.ReportJobDispatcher;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);
    private final ReportJobDispatcher reportJobDispatcher;

    public DailyReportScheduler(ReportJobDispatcher reportJobDispatcher) {
        this.reportJobDispatcher = reportJobDispatcher;
    }

    @Scheduled(cron = "${app.report.cron}", zone = "${app.report.zone-id}")
    public void runDaily() {
        LocalDate today = LocalDate.now();
        log.info("Start daily AI report job for {}", today);
        reportJobDispatcher.dispatch(today, false, "scheduler");
    }
}

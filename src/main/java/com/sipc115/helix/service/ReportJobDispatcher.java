package com.sipc115.helix.service;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ReportJobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReportJobDispatcher.class);

    private final DailyReportPipelineService pipelineService;

    public ReportJobDispatcher(DailyReportPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    // Fire-and-forget entry for report generation jobs.
    @Async("reportTaskExecutor")
    public void dispatch(LocalDate date, boolean force, String triggerSource) {
        log.info("[Dispatch] Job accepted. source={}, date={}, force={}, next=run pipeline", triggerSource, date, force);
        pipelineService.run(date, force);
        log.info("[Dispatch] Job finished. source={}, date={}", triggerSource, date);
    }
}

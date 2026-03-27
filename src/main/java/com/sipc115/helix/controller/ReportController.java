package com.sipc115.helix.controller;

import com.sipc115.helix.model.es.AiDailyReportDocument;
import com.sipc115.helix.service.DailyReportPipelineService;
import com.sipc115.helix.service.ReportJobDispatcher;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final DailyReportPipelineService pipelineService;
    private final ReportJobDispatcher reportJobDispatcher;

    public ReportController(DailyReportPipelineService pipelineService, ReportJobDispatcher reportJobDispatcher) {
        this.pipelineService = pipelineService;
        this.reportJobDispatcher = reportJobDispatcher;
    }

    @PostMapping("/run")
    public Map<String, Object> run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean force) {
        LocalDate actualDate = date == null ? LocalDate.now() : date;
        // Non-blocking trigger: return immediately after dispatching an async worker.
        reportJobDispatcher.dispatch(actualDate, force, "api:/api/reports/run");
        log.info("[API] /run accepted. date={}, force={}, next=query status from /api/reports/today", actualDate, force);
        return Map.of(
                "accepted", true,
                "date", actualDate,
                "force", force,
                "message", "Report job dispatched asynchronously. Check logs for progress and /api/reports/today for latest output."
        );
    }

    @GetMapping("/search")
    public List<AiDailyReportDocument> search(@RequestParam String keyword) {
        return pipelineService.search(keyword);
    }

    @GetMapping("/today")
    public Map<String, Object> today() {
        LocalDate now = LocalDate.now();
        return Map.of(
                "date", now,
                "items", pipelineService.queryToday(now)
        );
    }
}

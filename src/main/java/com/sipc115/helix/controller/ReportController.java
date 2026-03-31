/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.controller;

import com.sipc115.helix.domain.es.AiDailyReportDocument;
import com.sipc115.helix.service.DailyReportPipelineService;
import com.sipc115.helix.service.ReportJobDispatcher;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告控制器
 * <p>
 * 提供报告相关的 REST API 接口，包括运行报告任务、搜索报告和获取今日报告等功能。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    /**
     * 每日报告管道服务
     * <p>
     * 用于处理报告的搜索和查询功能。
     */
    private final DailyReportPipelineService pipelineService;
    
    /**
     * 报告任务调度器
     * <p>
     * 用于调度和执行报告任务。
     */
    private final ReportJobDispatcher reportJobDispatcher;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所需的服务实例。
     * 
     * @param pipelineService 每日报告管道服务
     * @param reportJobDispatcher 报告任务调度器
     */
    public ReportController(DailyReportPipelineService pipelineService, ReportJobDispatcher reportJobDispatcher) {
        this.pipelineService = pipelineService;
        this.reportJobDispatcher = reportJobDispatcher;
    }

    /**
     * 运行报告任务
     * <p>
     * 异步调度报告任务的执行，返回任务接受状态。
     * 
     * @param taskId 任务 ID
     * @param force 是否强制执行，忽略执行周期检查
     * @return 任务接受状态
     */
    @PostMapping("/run")
    public Map<String, Object> run(
            @RequestParam String taskId,
            @RequestParam(defaultValue = "false") boolean force) {
        // 调度报告任务
        reportJobDispatcher.dispatch(taskId, force, "api:/api/reports/run");
        log.info("[API] /run accepted. taskId={}, force={}, next=query task status from DB", taskId, force);
        
        // 返回任务接受状态
        return Map.of(
                "accepted", true,
                "taskId", taskId,
                "force", force,
                "message", "Task job dispatched asynchronously. Execution-cycle validation and prompt workflow are pending TODOs."
        );
    }

    /**
     * 搜索报告
     * <p>
     * 根据关键词搜索报告文档。
     * 
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    @GetMapping("/search")
    public List<AiDailyReportDocument> search(@RequestParam String keyword) {
        return pipelineService.search(keyword);
    }

    /**
     * 获取今日报告
     * <p>
     * 获取当前日期的报告信息。
     * 
     * @return 今日报告信息
     */
    @GetMapping("/today")
    public Map<String, Object> today() {
        LocalDate now = LocalDate.now();
        return Map.of(
                "date", now,
                "items", pipelineService.queryToday(now)
        );
    }
}

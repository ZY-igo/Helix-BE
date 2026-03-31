package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.lark.FeishuClient;
import com.sipc115.helix.context.BriefingFormatter;
import com.sipc115.helix.domain.dto.DailyBriefing;
import com.sipc115.helix.domain.entity.DailyReportStatus;
import com.sipc115.helix.domain.es.AiDailyReportDocument;
import com.sipc115.helix.repository.es.AiDailyReportRepository;
import com.sipc115.helix.repository.jpa.DailyReportStatusRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 日报流水线服务
 * <p>
 * 负责日报的生成、格式化、存储和发送整个流程
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Service
public class DailyReportPipelineService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(DailyReportPipelineService.class);

    /**
     * 日报状态仓库
     */
    private final DailyReportStatusRepository statusRepository;
    
    /**
     * AI日报报告仓库
     */
    private final AiDailyReportRepository reportRepository;
    
    /**
     * 智谱简报服务
     */
    private final ZhipuBriefingService zhipuBriefingService;
    
    /**
     * 简报格式化器
     */
    private final BriefingFormatter briefingFormatter;
    
    /**
     * 飞书客户端
     */
    private final FeishuClient feishuClient;
    
    /**
     * Jackson对象映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * 
     * @param statusRepository 日报状态仓库
     * @param reportRepository AI日报报告仓库
     * @param zhipuBriefingService 智谱简报服务
     * @param briefingFormatter 简报格式化器
     * @param feishuClient 飞书客户端
     * @param objectMapper Jackson对象映射器
     */
    public DailyReportPipelineService(
            DailyReportStatusRepository statusRepository,
            AiDailyReportRepository reportRepository,
            ZhipuBriefingService zhipuBriefingService,
            BriefingFormatter briefingFormatter,
            FeishuClient feishuClient,
            ObjectMapper objectMapper) {
        this.statusRepository = statusRepository;
        this.reportRepository = reportRepository;
        this.zhipuBriefingService = zhipuBriefingService;
        this.briefingFormatter = briefingFormatter;
        this.feishuClient = feishuClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 运行日报流水线
     * <p>
     * 生成日报、格式化、存储到Elasticsearch、发送到飞书
     * </p>
     * 
     * @param date 报告日期
     * @param force 是否强制运行
     * @return 日报状态
     */
    @Transactional
    public DailyReportStatus run(LocalDate date, boolean force) {
        log.info("[Pipeline] Start run. date={}, force={}, next=load status row, remaining=5 steps", date, force);
        DailyReportStatus status = statusRepository.findByTaskIdAndReportDate("1", date).orElseGet(DailyReportStatus::new);
        status.setTaskId("1");
        status.setReportDate(date);

        if (!force && status.isSent()) {
            log.info("[Pipeline] Skip run because report already sent. date={}, next=return existing status", date);
            return status;
        }

        try {
            log.info("[Pipeline] Step 1/5 generate briefing from AI, next=format summary/detail");
            DailyBriefing briefing = zhipuBriefingService.generate(date);

            log.info("[Pipeline] Step 2/5 format summary/detail text, next=save ES document");
            String summaryText;
            String detailText;
            if (isNoSourceUpdate(briefing)) {
                summaryText = buildNoUpdateMessage(date);
                detailText = buildNoUpdateMessage(date);
                log.info("[Pipeline] No source update detected. Use fallback message.");
            } else {
                summaryText = briefingFormatter.toSummaryText(briefing);
                detailText = briefingFormatter.toDetailText(briefing);
            }

            log.info("[Pipeline] Step 3/5 save report to Elasticsearch, next=send Feishu");
            AiDailyReportDocument document = new AiDailyReportDocument();
            document.setReportDate(date);
            document.setHeadline(briefing.getHeadline());
            document.setSummaryText(summaryText);
            document.setDetailText(detailText);
            document.setCreatedAt(Instant.now());
            document.setSourceDomains(briefingFormatter.collectSourceDomains(briefing));
            document.setContentJson(objectMapper.writeValueAsString(briefing));
            AiDailyReportDocument saved = reportRepository.save(document);
            log.info("[Pipeline] ES save success. esDocumentId={}", saved.getId());

            log.info("[Pipeline] Step 4/5 publish cloud doc and send messages to Feishu, next=update status table");
            String docUrl = feishuClient.publishCloudDocIfEnabled("AI Daily Report " + date, detailText);
            String summaryMsgId = feishuClient.sendPost(
                    "AI 每日播报精简版 " + date,
                    briefingFormatter.toSummaryRichLines(briefing, 3)
            );
            String detailMsgId = docUrl == null
                    ? feishuClient.sendPost("AI 每日播报详版 " + date, List.of(detailText))
                    : feishuClient.sendPostWithLink(
                            "AI 每日播报详版 " + date,
                            "详细内容已整理为飞书文档，点击下方链接查看。",
                            docUrl,
                            "查看完整文档");
            log.info("[Pipeline] Feishu send success. summaryMessageId={}, detailMessageId={}, docUrl={}",
                    summaryMsgId, detailMsgId, docUrl);

            log.info("[Pipeline] Step 5/5 update report status table, next=return success");
            status.setSent(true);
            status.setEsDocumentId(saved.getId());
            status.setSummaryMessageId(summaryMsgId);
            status.setDetailMessageId(detailMsgId);
            status.setSentAt(LocalDateTime.now());
            status.setErrorMessage(null);
            DailyReportStatus savedStatus = statusRepository.save(status);
            log.info("[Pipeline] Run completed. date={}, sent={}", date, savedStatus.isSent());
            return savedStatus;
        } catch (JsonProcessingException e) {
            status.setSent(false);
            status.setErrorMessage("Serialize briefing failed: " + e.getMessage());
            log.error("[Pipeline] Failed to serialize briefing. date={}", date, e);
            return statusRepository.save(status);
        } catch (Exception e) {
            status.setSent(false);
            status.setErrorMessage(e.getMessage());
            log.error("[Pipeline] Run failed. date={}", date, e);
            return statusRepository.save(status);
        }
    }

    /**
     * 搜索日报
     * 
     * @param keyword 搜索关键词
     * @return 匹配的日报列表
     */
    public List<AiDailyReportDocument> search(String keyword) {
        return reportRepository.findTop5BySummaryTextContainingOrDetailTextContainingOrderByCreatedAtDesc(keyword, keyword);
    }

    /**
     * 查询指定日期的日报
     * 
     * @param date 报告日期
     * @return 匹配的日报列表
     */
    public List<AiDailyReportDocument> queryToday(LocalDate date) {
        return reportRepository.findTop5ByReportDateOrderByCreatedAtDesc(date);
    }

    /**
     * 检查是否没有源更新
     * 
     * @param briefing 简报对象
     * @return 是否没有源更新
     */
    private boolean isNoSourceUpdate(DailyBriefing briefing) {
        return briefing.getHotSignals().isEmpty()
                && briefing.getLatestUpdates().isEmpty()
                && briefing.getClassicInsights().isEmpty();
    }

    /**
     * 构建无更新消息
     * 
     * @param date 报告日期
     * @return 无更新消息
     */
    private String buildNoUpdateMessage(LocalDate date) {
        return "[AI Daily Report] " + date + " no high-value source update today. No new content pushed.";
    }
}

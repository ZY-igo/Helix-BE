package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.config.FeishuClient;
import com.sipc115.helix.context.BriefingFormatter;
import com.sipc115.helix.model.dto.DailyBriefing;
import com.sipc115.helix.model.entity.DailyReportStatus;
import com.sipc115.helix.model.es.AiDailyReportDocument;
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

@Service
public class DailyReportPipelineService {

    private static final Logger log = LoggerFactory.getLogger(DailyReportPipelineService.class);

    private final DailyReportStatusRepository statusRepository;
    private final AiDailyReportRepository reportRepository;
    private final ZhipuBriefingService zhipuBriefingService;
    private final BriefingFormatter briefingFormatter;
    private final FeishuClient feishuClient;
    private final ObjectMapper objectMapper;

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

    @Transactional
    public DailyReportStatus run(LocalDate date, boolean force) {
        log.info("[Pipeline] Start run. date={}, force={}, next=load status row, remaining=5 steps", date, force);
        DailyReportStatus status = statusRepository.findByReportDate(date).orElseGet(DailyReportStatus::new);
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

    public List<AiDailyReportDocument> search(String keyword) {
        return reportRepository.findTop5BySummaryTextContainingOrDetailTextContainingOrderByCreatedAtDesc(keyword, keyword);
    }

    public List<AiDailyReportDocument> queryToday(LocalDate date) {
        return reportRepository.findTop5ByReportDateOrderByCreatedAtDesc(date);
    }

    private boolean isNoSourceUpdate(DailyBriefing briefing) {
        return briefing.getHotSignals().isEmpty()
                && briefing.getLatestUpdates().isEmpty()
                && briefing.getClassicInsights().isEmpty();
    }

    private String buildNoUpdateMessage(LocalDate date) {
        return "[AI Daily Report] " + date + " no high-value source update today. No new content pushed.";
    }
}

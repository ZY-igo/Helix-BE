package com.sipc115.helix.service;

import com.sipc115.helix.model.es.AiDailyReportDocument;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BotCommandService {

    private static final Logger log = LoggerFactory.getLogger(BotCommandService.class);

    private final DailyReportPipelineService pipelineService;
    private final ReportJobDispatcher reportJobDispatcher;

    public BotCommandService(DailyReportPipelineService pipelineService, ReportJobDispatcher reportJobDispatcher) {
        this.pipelineService = pipelineService;
        this.reportJobDispatcher = reportJobDispatcher;
    }

    // Parses supported bot commands and dispatches to report/search capabilities.
    public String execute(String commandText) {
        if (!StringUtils.hasText(commandText)) {
            return helpText();
        }
        String text = commandText.trim();
        log.info("[BotCommand] Execute command={}", text);

        if (text.startsWith("/search")) {
            String keyword = text.replaceFirst("/search", "").trim();
            if (!StringUtils.hasText(keyword)) {
                return "Usage: /search <keyword>";
            }
            List<AiDailyReportDocument> documents = pipelineService.search(keyword);
            log.info("[BotCommand] Search keyword={}, resultCount={}", keyword, documents.size());
            if (documents.isEmpty()) {
                return "No report found for keyword: " + keyword;
            }
            StringBuilder sb = new StringBuilder("Search results:\n");
            for (int i = 0; i < documents.size(); i++) {
                AiDailyReportDocument doc = documents.get(i);
                sb.append(i + 1).append(". ")
                        .append(doc.getReportDate()).append(" ")
                        .append(doc.getHeadline()).append("\n");
            }
            return sb.toString();
        }

        if (text.startsWith("/report today")) {
            log.info("[BotCommand] Trigger report today.");
            reportJobDispatcher.dispatch(LocalDate.now(), false, "feishu:/report today");
            return "Today's report job has been dispatched. It will run in background.";
        }

        if (text.startsWith("/report resend")) {
            log.info("[BotCommand] Trigger report resend.");
            reportJobDispatcher.dispatch(LocalDate.now(), true, "feishu:/report resend");
            return "Today's report resend job has been dispatched. It will run in background.";
        }

        return helpText();
    }

    private String helpText() {
        return """
                Available commands:
                /search <keyword>
                /report today
                /report resend
                """;
    }
}

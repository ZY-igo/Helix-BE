/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service;

import com.sipc115.helix.config.BotProperties;
import com.sipc115.helix.context.BriefingPromptBuilder;
import com.sipc115.helix.integration.llm.AiClient;
import com.sipc115.helix.integration.llm.AiClientFactory;
import com.sipc115.helix.domain.dto.DailyBriefing;
import com.sipc115.helix.repository.es.AiDailyReportRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 智谱简报服务
 * <p>
 * 负责生成每日简报的完整 AI 工作流，包括检索、多轮优化、格式化、去重、质量审计和翻译等步骤。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class ZhipuBriefingService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ZhipuBriefingService.class);

    /**
     * 机器人配置
     * <p>
     * 包含智谱 API 密钥和报告配置等信息。
     */
    private final BotProperties properties;
    
    /**
     * AI 客户端工厂
     * <p>
     * 用于创建和管理不同 AI 提供商的客户端实例。
     */
    private final AiClientFactory aiClientFactory;
    
    /**
     * 简报提示构建器
     * <p>
     * 用于构建各种 AI 提示，如系统提示、检索提示、优化提示等。
     */
    private final BriefingPromptBuilder promptBuilder;
    
    /**
     * AI 每日报告仓库
     * <p>
     * 用于存储和检索历史报告数据，支持去重功能。
     */
    private final AiDailyReportRepository reportRepository;
    
    /**
     * 简报处理器
     * <p>
     * 用于处理简报的解析、去重和质量审计等操作。
     */
    private final BriefingProcessor briefingProcessor;

    /**
     * AI 提供商
     * <p>
     * 从配置中获取，默认为 "zhipu"。
     */
    @Value("${app.ai-provider:zhipu}")
    private String aiProvider;

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所需的服务和配置实例。
     * 
     * @param properties 机器人配置
     * @param aiClientFactory AI 客户端工厂
     * @param promptBuilder 简报提示构建器
     * @param reportRepository AI 每日报告仓库
     * @param briefingProcessor 简报处理器
     */
    public ZhipuBriefingService(
            BotProperties properties,
            AiClientFactory aiClientFactory,
            BriefingPromptBuilder promptBuilder,
            AiDailyReportRepository reportRepository,
            BriefingProcessor briefingProcessor) {
        this.properties = properties;
        this.aiClientFactory = aiClientFactory;
        this.promptBuilder = promptBuilder;
        this.reportRepository = reportRepository;
        this.briefingProcessor = briefingProcessor;
    }

    /**
     * 获取当前配置的 AI 客户端
     * <p>
     * 根据配置的 AI 提供商类型获取对应的客户端实例。
     * 
     * @return AI 客户端实例
     */
    private AiClient getAiClient() {
        log.debug("[ZhipuBriefingService] Using AI provider: {}", aiProvider);
        return aiClientFactory.getClient(aiProvider);
    }

    /**
     * 生成每日简报的完整 AI 工作流
     * <p>
     * 流程：检索 -> 多轮优化 -> 格式化 -> 去重 -> 质量审计 -> 翻译
     * 
     * @param date 报告日期
     * @return 生成的每日简报
     */
    public DailyBriefing generate(LocalDate date) {
        log.info("[AI] step=1/6 start briefing generation, date={}, next=build system prompt", date);
        validateConfig();

        // 构建系统提示
        String systemPrompt = promptBuilder.buildSystemPrompt(
                date,
                properties.getReport().getMaxItemsPerSection()
        );
        log.info("[AI] step=2/6 system prompt ready, next=retrieve candidates");
        log.info("[AI] system prompt built, chars={}", systemPrompt.length());
        log.debug("[AI] system prompt:\n{}", systemPrompt);

        // 第一步：检索初始内容
        String current = getAiClient().chat("retrieve", systemPrompt, promptBuilder.buildRetrievePrompt(date));

        // 多轮优化
        int rounds = properties.getReport().getLoopRounds();
        for (int i = 2; i <= rounds; i++) {
            log.info("[AI] optimize round={}/{}, next=apply optimization prompt", i, rounds);
            current = getAiClient().chat("optimize-" + i, systemPrompt, promptBuilder.buildOptimizePrompt(i, current));
        }

        // 第二步：格式化 JSON
        log.info("[AI] step=3/6 normalize JSON format, next=deduplicate by ES");
        current = getAiClient().chat("formatting", systemPrompt, promptBuilder.buildFormattingPrompt(current));

        // 第三步：基于历史数据去重
        log.info("[AI] step=4/6 check duplicates against ES history");
        current = deduplicateByHistoricalUrls(systemPrompt, current);

        // 第四步：质量审计循环
        log.info("[AI] step=5/7 run quality audit loop");
        int auditLoop = properties.getReport().getAuditLoops();
        for (int i = 0; i < auditLoop; i++) {
            BriefingProcessor.QualityAuditResult audit = runQualityAudit(systemPrompt, current);
            log.info("[AI][audit-{}] score={}, decision={}, next={}", i + 1, audit.score(), audit.decision(),
                    (audit.score() >= 85 || "pass".equalsIgnoreCase(audit.decision())) ? "parse briefing" : "revise and audit again");
            current = audit.briefingJson();
            if (audit.score() >= 85 || "pass".equalsIgnoreCase(audit.decision())) {
                break;
            }
        }

        // 第五步：翻译成中文
        log.info("[AI] step=6/7 translate final briefing JSON to Chinese");
        String translated = getAiClient().chat("translate-zh", systemPrompt, promptBuilder.buildTranslateToChinesePrompt(current));

        // 第六步：解析最终 JSON
        log.info("[AI] step=7/7 parse final JSON to DTO");
        DailyBriefing briefing = briefingProcessor.parseBriefing(translated);
        briefing.setDate(date);
        log.info("[AI] completed, headline={}, remaining=send to formatter and pipeline", briefing.getHeadline());
        return briefing;
    }

    /**
     * 验证配置是否有效
     * <p>
     * 检查智谱 API 密钥是否配置，以及 AI 提供商是否可用。
     * 
     * @throws IllegalStateException 当配置无效时抛出
     */
    private void validateConfig() {
        if (properties.getZhipu().getApiKey() == null || properties.getZhipu().getApiKey().isBlank()) {
            throw new IllegalStateException("app.zhipu.api-key is empty.");
        }

        // 验证工厂中是否有可用的客户端（此时 aiProvider 已经被注入）
        if (!aiClientFactory.hasClient(aiProvider)) {
            throw new IllegalStateException(
                    "Configured AI provider '" + aiProvider + "' is not available. " +
                            "Available providers: " + String.join(", ", aiClientFactory.getAvailableTypes())
            );
        }
    }

    /**
     * 基于历史 URL 去重
     * <p>
     * 检查候选简报中的 URL 是否在历史报告中出现过，去除重复内容。
     * 
     * @param systemPrompt 系统提示
     * @param candidateJson 候选简报的 JSON 字符串
     * @return 去重后的简报 JSON 字符串
     */
    private String deduplicateByHistoricalUrls(String systemPrompt, String candidateJson) {
        DailyBriefing candidate = briefingProcessor.parseBriefing(candidateJson);
        List<String> duplicateUrls = briefingProcessor.findDuplicateUrls(
            candidate,
            url -> reportRepository.existsBySummaryTextContainingOrDetailTextContaining(url, url)
        );

        if (duplicateUrls.isEmpty()) {
            log.info("[AI][dedup] no duplicate URL found in ES, next=audit");
            return candidateJson;
        }

        log.info("[AI][dedup] duplicate URLs detected count={}, next=ask model to remove duplicates", duplicateUrls.size());
        String dedupPrompt = promptBuilder.buildDeduplicatePrompt(candidateJson, duplicateUrls);
        return getAiClient().chat("deduplicate", systemPrompt, dedupPrompt);
    }

    /**
     * 运行质量审计
     * <p>
     * 对候选简报进行质量评估，确保内容质量符合要求。
     * 
     * @param systemPrompt 系统提示
     * @param candidateJson 候选简报的 JSON 字符串
     * @return 质量审计结果
     */
    private BriefingProcessor.QualityAuditResult runQualityAudit(String systemPrompt, String candidateJson) {
        String auditRaw = getAiClient().chat("audit", systemPrompt, promptBuilder.buildQualityAuditPrompt(candidateJson));
        return briefingProcessor.runQualityAudit(auditRaw, candidateJson);
    }

    /**
     * 动态切换 AI 提供商
     * <p>
     * 用于运行时切换不同的 AI 提供商。
     * 
     * @param newProvider 新的 AI 提供商类型
     * @throws IllegalArgumentException 当指定的 AI 提供商不可用时抛出
     */
    public void switchAiProvider(String newProvider) {
        if (!aiClientFactory.hasClient(newProvider)) {
            throw new IllegalArgumentException(
                "Cannot switch to '" + newProvider + "'. Available: " +
                String.join(", ", aiClientFactory.getAvailableTypes())
            );
        }
        this.aiProvider = newProvider;
        log.info("[ZhipuBriefingService] Switched AI provider from {} to {}",
                aiProvider, newProvider);
    }
}

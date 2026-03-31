package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.dto.BriefingItem;
import com.sipc115.helix.domain.dto.DailyBriefing;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 简报处理工具类
 * <p>
 * 负责简报数据的解析、去重、质量审计等逻辑
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Component
public class BriefingProcessor {

    /**
     * Jackson 对象映射器，用于 JSON 解析和序列化
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * 
     * @param objectMapper Jackson 对象映射器
     */
    public BriefingProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析原始文本为 DailyBriefing 对象
     * 
     * @param raw 包含 JSON 的原始文本
     * @return DailyBriefing 对象
     * @throws IllegalStateException 如果解析失败
     */
    public DailyBriefing parseBriefing(String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, DailyBriefing.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse briefing JSON: " + e.getMessage(), e);
        }
    }

    /**
     * 从原始文本中提取 JSON 对象
     * 
     * @param raw 原始文本
     * @return 提取的 JSON 字符串
     * @throws IllegalStateException 如果未找到 JSON 对象
     */
    public String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        throw new IllegalStateException("No JSON object found in model output.");
    }

    /**
     * 查找重复的 URL
     * 
     * @param briefing 简报对象
     * @param urlChecker URL 检查器函数
     * @return 重复的 URL 列表
     */
    public List<String> findDuplicateUrls(DailyBriefing briefing, UrlChecker urlChecker) {
        Set<String> urls = collectAllUrls(briefing);
        List<String> duplicates = new ArrayList<>();

        for (String url : urls) {
            if (urlChecker.exists(url)) {
                duplicates.add(url);
            }
        }
        return duplicates;
    }

    /**
     * 收集简报中的所有 URL
     * 
     * @param briefing 简报对象
     * @return URL 集合
     */
    private Set<String> collectAllUrls(DailyBriefing briefing) {
        Set<String> urls = new LinkedHashSet<>();
        addUrls(urls, briefing.getHotSignals());
        addUrls(urls, briefing.getLatestUpdates());
        addUrls(urls, briefing.getClassicInsights());
        return urls;
    }

    /**
     * 将简报项中的 URL 添加到集合中
     * 
     * @param urls URL 集合
     * @param items 简报项列表
     */
    private void addUrls(Set<String> urls, List<BriefingItem> items) {
        for (BriefingItem item : items) {
            if (StringUtils.hasText(item.getUrl())) {
                urls.add(item.getUrl().trim());
            }
        }
    }

    /**
     * 质量审计结果
     */
    public record QualityAuditResult(int score, String decision, String briefingJson) {
    }

    /**
     * 运行质量审计
     * 
     * @param auditRaw 审计原始响应文本
     * @param candidateJson 候选简报 JSON
     * @return 审计结果
     */
    public QualityAuditResult runQualityAudit(String auditRaw, String candidateJson) {
        try {
            JsonNode node = objectMapper.readTree(extractJson(auditRaw));
            int score = node.path("score").asInt(0);
            String decision = node.path("decision").asText("revise");
            JsonNode briefing = node.path("briefing");

            if (briefing == null || briefing.isMissingNode() || briefing.isNull()) {
                return new QualityAuditResult(score, decision, candidateJson);
            }
            return new QualityAuditResult(score, decision, objectMapper.writeValueAsString(briefing));
        } catch (Exception e) {
            return new QualityAuditResult(0, "revise", candidateJson);
        }
    }

    /**
     * URL 检查器接口
     */
    @FunctionalInterface
    public interface UrlChecker {
        /**
         * 检查 URL 是否存在
         * 
         * @param url URL 地址
         * @return 是否存在
         */
        boolean exists(String url);
    }
}

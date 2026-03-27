package com.sipc115.helix.service;

import com.sipc115.helix.model.dto.BriefingItem;
import com.sipc115.helix.model.dto.DailyBriefing;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BriefingFormatter {

    public String toSummaryText(DailyBriefing briefing) {
        StringBuilder sb = new StringBuilder();
        sb.append("【AI 每日播报】").append(briefing.getDate()).append("\n");
        sb.append("主题：").append(nullSafe(briefing.getHeadline())).append("\n\n");
        sb.append("热点数据（Top 3）\n");
        appendSummaryList(sb, briefing.getHotSignals(), 3);
        return sb.toString();
    }

    public String toDetailText(DailyBriefing briefing) {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI 每日播报详版（").append(briefing.getDate()).append("）\n\n");
        sb.append("## 今日主题\n");
        sb.append(nullSafe(briefing.getHeadline())).append("\n\n");

        appendSection(sb, "热点数据", briefing.getHotSignals());
        appendSection(sb, "最新消息", briefing.getLatestUpdates());
        appendSection(sb, "经典知识", briefing.getClassicInsights());
        return sb.toString();
    }

    public List<String> toSummaryRichLines(DailyBriefing briefing, int maxItems) {
        List<String> lines = new ArrayList<>();
        lines.add("主题：" + nullSafe(briefing.getHeadline()));
        List<BriefingItem> items = briefing.getHotSignals();
        int size = Math.min(items.size(), maxItems);
        for (int i = 0; i < size; i++) {
            BriefingItem item = items.get(i);
            lines.add((i + 1) + ". " + nullSafe(item.getTitle()));
            lines.add("核心内容：" + nullSafe(item.getCoreContent()));
            lines.add("功能作用：" + nullSafe(item.getFunctionImpact()));
        }
        if (size == 0) {
            lines.add("今日暂无高价值更新。");
        }
        return lines;
    }

    public List<String> collectSourceDomains(DailyBriefing briefing) {
        List<String> sources = new ArrayList<>();
        addSource(sources, briefing.getHotSignals());
        addSource(sources, briefing.getLatestUpdates());
        addSource(sources, briefing.getClassicInsights());
        return sources.stream().distinct().toList();
    }

    private void appendSummaryList(StringBuilder sb, List<BriefingItem> items, int max) {
        int size = Math.min(items.size(), max);
        for (int i = 0; i < size; i++) {
            BriefingItem item = items.get(i);
            sb.append(i + 1).append(". ").append(nullSafe(item.getTitle())).append("\n");
            sb.append("   - 核心内容：").append(nullSafe(item.getCoreContent())).append("\n");
            sb.append("   - 功能作用：").append(nullSafe(item.getFunctionImpact())).append("\n");
        }
        if (size == 0) {
            sb.append("1. 今日暂无高价值更新。\n");
        }
    }

    private void appendSection(StringBuilder sb, String title, List<BriefingItem> items) {
        sb.append("## ").append(title).append("\n");
        if (items.isEmpty()) {
            sb.append("- 暂无\n\n");
            return;
        }
        for (BriefingItem item : items) {
            sb.append("- ").append(nullSafe(item.getTitle())).append("\n");
            sb.append("  - 来源：").append(nullSafe(item.getSource())).append("\n");
            sb.append("  - 链接：").append(nullSafe(item.getUrl())).append("\n");
            sb.append("  - 核心内容：").append(nullSafe(item.getCoreContent())).append("\n");
            sb.append("  - 功能作用：").append(nullSafe(item.getFunctionImpact())).append("\n");
        }
        sb.append("\n");
    }

    private void addSource(List<String> sources, List<BriefingItem> items) {
        for (BriefingItem item : items) {
            if (item.getSource() != null && !item.getSource().isBlank()) {
                sources.add(item.getSource().trim());
            }
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

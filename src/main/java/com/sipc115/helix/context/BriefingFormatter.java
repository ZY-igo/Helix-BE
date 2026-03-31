/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context;

import com.sipc115.helix.domain.dto.BriefingItem;
import com.sipc115.helix.domain.dto.DailyBriefing;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 简报格式化器类
 * <p>
 * 负责将每日简报数据格式化为不同形式的文本，包括摘要文本、详细文本、摘要富文本行等。
 * 提供了多种格式化方法，满足不同场景的需求。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class BriefingFormatter {

    /**
     * 生成摘要文本
     * <p>
     * 将每日简报格式化为摘要文本，包含日期、主题和热点数据（前 3 条）。
     * 
     * @param briefing 每日简报对象
     * @return 格式化后的摘要文本
     */
    public String toSummaryText(DailyBriefing briefing) {
        StringBuilder sb = new StringBuilder();
        sb.append("【AI 每日播报】").append(briefing.getDate()).append("\n");
        sb.append("主题：").append(nullSafe(briefing.getHeadline())).append("\n\n");
        sb.append("热点数据（Top 3）\n");
        appendSummaryList(sb, briefing.getHotSignals(), 3);
        return sb.toString();
    }

    /**
     * 生成详细文本
     * <p>
     * 将每日简报格式化为详细文本，包含日期、主题、热点数据、最新消息和经典知识等完整信息。
     * 
     * @param briefing 每日简报对象
     * @return 格式化后的详细文本
     */
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

    /**
     * 生成摘要富文本行
     * <p>
     * 将每日简报格式化为摘要富文本行列表，包含主题和热点数据（最多 maxItems 条）。
     * 
     * @param briefing 每日简报对象
     * @param maxItems 最大条目数
     * @return 格式化后的摘要富文本行列表
     */
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

    /**
     * 收集来源域名
     * <p>
     * 从每日简报的各个部分收集来源域名，并去重返回。
     * 
     * @param briefing 每日简报对象
     * @return 去重后的来源域名列表
     */
    public List<String> collectSourceDomains(DailyBriefing briefing) {
        List<String> sources = new ArrayList<>();
        addSource(sources, briefing.getHotSignals());
        addSource(sources, briefing.getLatestUpdates());
        addSource(sources, briefing.getClassicInsights());
        return sources.stream().distinct().toList();
    }

    /**
     * 追加摘要列表
     * <p>
     * 将简报项目列表追加到字符串构建器中，最多显示 max 个项目。
     * 
     * @param sb 字符串构建器
     * @param items 简报项目列表
     * @param max 最大项目数
     */
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

    /**
     * 追加章节
     * <p>
     * 将指定标题和简报项目列表追加到字符串构建器中，形成一个完整的章节。
     * 
     * @param sb 字符串构建器
     * @param title 章节标题
     * @param items 简报项目列表
     */
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

    /**
     * 添加来源
     * <p>
     * 从简报项目列表中提取来源信息，添加到来源列表中。
     * 
     * @param sources 来源列表
     * @param items 简报项目列表
     */
    private void addSource(List<String> sources, List<BriefingItem> items) {
        for (BriefingItem item : items) {
            if (item.getSource() != null && !item.getSource().isBlank()) {
                sources.add(item.getSource().trim());
            }
        }
    }

    /**
     * 安全处理字符串
     * <p>
     * 处理可能为 null 的字符串，确保返回非 null 值。
     * 
     * @param value 输入字符串
     * @return 处理后的字符串，为 null 时返回空字符串
     */
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

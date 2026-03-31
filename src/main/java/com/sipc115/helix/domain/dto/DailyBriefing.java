/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日简报类
 * <p>
 * 用于表示每日简报的完整信息，包含日期、主题、热点数据、最新消息和经典知识等部分。
 * 使用 Jackson 注解忽略未知属性，提高序列化和反序列化的灵活性。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyBriefing {

    /**
     * 日期
     * <p>
     * 简报的日期。
     */
    private LocalDate date;
    
    /**
     * 主题
     * <p>
     * 简报的主题，概括当日的主要内容。
     */
    private String headline;
    
    /**
     * 热点数据
     * <p>
     * 当日的热点数据列表。
     * 默认为空 ArrayList，确保始终可用。
     */
    private List<BriefingItem> hotSignals = new ArrayList<>();
    
    /**
     * 最新消息
     * <p>
     * 当日的最新消息列表。
     * 默认为空 ArrayList，确保始终可用。
     */
    private List<BriefingItem> latestUpdates = new ArrayList<>();
    
    /**
     * 经典知识
     * <p>
     * 当日的经典知识列表。
     * 默认为空 ArrayList，确保始终可用。
     */
    private List<BriefingItem> classicInsights = new ArrayList<>();

    /**
     * 获取日期
     * 
     * @return 日期
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * 设置日期
     * 
     * @param date 日期
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * 获取主题
     * 
     * @return 主题
     */
    public String getHeadline() {
        return headline;
    }

    /**
     * 设置主题
     * 
     * @param headline 主题
     */
    public void setHeadline(String headline) {
        this.headline = headline;
    }

    /**
     * 获取热点数据
     * 
     * @return 热点数据列表
     */
    public List<BriefingItem> getHotSignals() {
        return hotSignals;
    }

    /**
     * 设置热点数据
     * 
     * @param hotSignals 热点数据列表
     */
    public void setHotSignals(List<BriefingItem> hotSignals) {
        this.hotSignals = hotSignals;
    }

    /**
     * 获取最新消息
     * 
     * @return 最新消息列表
     */
    public List<BriefingItem> getLatestUpdates() {
        return latestUpdates;
    }

    /**
     * 设置最新消息
     * 
     * @param latestUpdates 最新消息列表
     */
    public void setLatestUpdates(List<BriefingItem> latestUpdates) {
        this.latestUpdates = latestUpdates;
    }

    /**
     * 获取经典知识
     * 
     * @return 经典知识列表
     */
    public List<BriefingItem> getClassicInsights() {
        return classicInsights;
    }

    /**
     * 设置经典知识
     * 
     * @param classicInsights 经典知识列表
     */
    public void setClassicInsights(List<BriefingItem> classicInsights) {
        this.classicInsights = classicInsights;
    }
}

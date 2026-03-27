package com.sipc115.helix.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyBriefing {

    private LocalDate date;
    private String headline;
    private List<BriefingItem> hotSignals = new ArrayList<>();
    private List<BriefingItem> latestUpdates = new ArrayList<>();
    private List<BriefingItem> classicInsights = new ArrayList<>();

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public List<BriefingItem> getHotSignals() {
        return hotSignals;
    }

    public void setHotSignals(List<BriefingItem> hotSignals) {
        this.hotSignals = hotSignals;
    }

    public List<BriefingItem> getLatestUpdates() {
        return latestUpdates;
    }

    public void setLatestUpdates(List<BriefingItem> latestUpdates) {
        this.latestUpdates = latestUpdates;
    }

    public List<BriefingItem> getClassicInsights() {
        return classicInsights;
    }

    public void setClassicInsights(List<BriefingItem> classicInsights) {
        this.classicInsights = classicInsights;
    }
}

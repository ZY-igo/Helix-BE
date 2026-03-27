package com.sipc115.helix.model.es;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "ai_daily_report")
public class AiDailyReportDocument {

    @Id
    private String id;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate reportDate;

    @Field(type = FieldType.Text)
    private String headline;

    @Field(type = FieldType.Text)
    private String summaryText;

    @Field(type = FieldType.Text)
    private String detailText;

    @Field(type = FieldType.Text)
    private String contentJson;

    @Field(type = FieldType.Keyword)
    private List<String> sourceDomains = new ArrayList<>();

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getDetailText() {
        return detailText;
    }

    public void setDetailText(String detailText) {
        this.detailText = detailText;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public List<String> getSourceDomains() {
        return sourceDomains;
    }

    public void setSourceDomains(List<String> sourceDomains) {
        this.sourceDomains = sourceDomains;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

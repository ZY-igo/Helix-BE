package com.sipc115.helix.domain.es;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * AI日报报告文档
 * <p>
 * 用于存储在Elasticsearch中的AI日报报告文档
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Document(indexName = "ai_daily_report")
public class AiDailyReportDocument {

    /**
     * 文档ID
     */
    @Id
    private String id;

    /**
     * 报告日期
     */
    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate reportDate;

    /**
     * 标题
     */
    @Field(type = FieldType.Text)
    private String headline;

    /**
     * 摘要文本
     */
    @Field(type = FieldType.Text)
    private String summaryText;

    /**
     * 详细文本
     */
    @Field(type = FieldType.Text)
    private String detailText;

    /**
     * 内容JSON
     */
    @Field(type = FieldType.Text)
    private String contentJson;

    /**
     * 源域名列表
     */
    @Field(type = FieldType.Keyword)
    private List<String> sourceDomains = new ArrayList<>();

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    /**
     * 获取文档ID
     * 
     * @return 文档ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置文档ID
     * 
     * @param id 文档ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取报告日期
     * 
     * @return 报告日期
     */
    public LocalDate getReportDate() {
        return reportDate;
    }

    /**
     * 设置报告日期
     * 
     * @param reportDate 报告日期
     */
    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    /**
     * 获取标题
     * 
     * @return 标题
     */
    public String getHeadline() {
        return headline;
    }

    /**
     * 设置标题
     * 
     * @param headline 标题
     */
    public void setHeadline(String headline) {
        this.headline = headline;
    }

    /**
     * 获取摘要文本
     * 
     * @return 摘要文本
     */
    public String getSummaryText() {
        return summaryText;
    }

    /**
     * 设置摘要文本
     * 
     * @param summaryText 摘要文本
     */
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    /**
     * 获取详细文本
     * 
     * @return 详细文本
     */
    public String getDetailText() {
        return detailText;
    }

    /**
     * 设置详细文本
     * 
     * @param detailText 详细文本
     */
    public void setDetailText(String detailText) {
        this.detailText = detailText;
    }

    /**
     * 获取内容JSON
     * 
     * @return 内容JSON
     */
    public String getContentJson() {
        return contentJson;
    }

    /**
     * 设置内容JSON
     * 
     * @param contentJson 内容JSON
     */
    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    /**
     * 获取源域名列表
     * 
     * @return 源域名列表
     */
    public List<String> getSourceDomains() {
        return sourceDomains;
    }

    /**
     * 设置源域名列表
     * 
     * @param sourceDomains 源域名列表
     */
    public void setSourceDomains(List<String> sourceDomains) {
        this.sourceDomains = sourceDomains;
    }

    /**
     * 获取创建时间
     * 
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间
     * 
     * @param createdAt 创建时间
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.sipc115.helix.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_report_status")
public class DailyReportStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(nullable = false)
    private boolean sent;

    @Column(name = "es_document_id")
    private String esDocumentId;

    @Column(name = "summary_message_id")
    private String summaryMessageId;

    @Column(name = "detail_message_id")
    private String detailMessageId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 1200)
    private String errorMessage;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getEsDocumentId() {
        return esDocumentId;
    }

    public void setEsDocumentId(String esDocumentId) {
        this.esDocumentId = esDocumentId;
    }

    public String getSummaryMessageId() {
        return summaryMessageId;
    }

    public void setSummaryMessageId(String summaryMessageId) {
        this.summaryMessageId = summaryMessageId;
    }

    public String getDetailMessageId() {
        return detailMessageId;
    }

    public void setDetailMessageId(String detailMessageId) {
        this.detailMessageId = detailMessageId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

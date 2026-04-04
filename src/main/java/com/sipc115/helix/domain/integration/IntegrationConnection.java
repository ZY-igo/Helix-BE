/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.integration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "integration_connection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(length = 32)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "encrypted_config")
    private String encryptedConfig;

    @Column(length = 32)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "last_test_at")
    private Instant lastTestAt;

    @Column(name = "last_test_result", length = 32)
    private String lastTestResult;

    @Column
    private String description;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public static class Status {
        public static final String ACTIVE = "ACTIVE";
        public static final String INACTIVE = "INACTIVE";
        public static final String ERROR = "ERROR";
    }

    public static class Type {
        public static final String FEISHU = "FEISHU";
        public static final String MYSQL = "MYSQL";
        public static final String LLM = "LLM";
        public static final String REDIS = "REDIS";
    }

    public static class Category {
        public static final String IM = "IM";
        public static final String DATABASE = "DATABASE";
        public static final String AI = "AI";
        public static final String CACHE = "CACHE";
    }
}
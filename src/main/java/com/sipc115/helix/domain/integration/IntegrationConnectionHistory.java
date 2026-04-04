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
@Table(name = "integration_connection_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationConnectionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "encrypted_config")
    private String encryptedConfig;

    @Column(length = 32)
    private String status;

    @Column(name = "changed_by", length = 64)
    private String changedBy;

    @Column(name = "changed_at")
    private Instant changedAt;

    @Column(name = "change_type", length = 32)
    private String changeType;

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }

    public static class ChangeType {
        public static final String CREATE = "CREATE";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String TEST = "TEST";
    }
}
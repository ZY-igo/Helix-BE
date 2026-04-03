/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Entity
@Table(name = "ai_step_execution", indexes = {
    @Index(name = "idx_node_trace_round", columnList = "nodeTraceId, round"),
    @Index(name = "idx_ai_step_execution_id", columnList = "executionId")
})
public class AiStepExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long nodeTraceId;

    @Column(nullable = false)
    private Long executionId;

    @Column(nullable = false, length = 128)
    private String stepId;

    @Column(nullable = false, length = 64)
    private String stepType;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false, length = 32)
    private String status;

    private Instant startedAt;

    private Instant endedAt;

    private Long durationMs;

    @Column(length = 64)
    private String modelName;

    @Column
    private Integer tokenUsage;

    @Column(columnDefinition = "TEXT")
    private String inputContent;

    @Column(columnDefinition = "TEXT")
    private String outputContent;

    @Column(columnDefinition = "TEXT")
    private Map<String, Object> varsSnapshot;

    @Column(length = 2048)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}

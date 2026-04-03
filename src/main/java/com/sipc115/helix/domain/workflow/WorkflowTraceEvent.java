/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.domain.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTraceEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EVENT_WORKFLOW_START = "WORKFLOW_START";
    public static final String EVENT_WORKFLOW_COMPLETE = "WORKFLOW_COMPLETE";
    public static final String EVENT_NODE_START = "NODE_START";
    public static final String EVENT_NODE_COMPLETE = "NODE_COMPLETE";
    public static final String EVENT_AI_STEP = "AI_STEP";

    private String eventType;

    private Long executionId;

    private String nodeId;

    private Long nodeTraceId;

    private String status;

    private Integer executionOrder;

    private Integer round;

    private String stepType;

    private Map<String, Object> inputData;

    private Map<String, Object> outputData;

    private Instant timestamp;

    private Map<String, Object> metadata;
}

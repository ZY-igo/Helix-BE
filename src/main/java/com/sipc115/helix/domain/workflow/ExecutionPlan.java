package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExecutionPlan implements Serializable {

    private String workflowId;

    private String workflowVersion;

    private String entryNodeId;

    private Map<String, CompiledNode> nodes = new HashMap<>();

    private List<Transition> transitions;

    private String planId;

    private PlanMetadata metadata;

    private ScheduleSpec schedule;
}

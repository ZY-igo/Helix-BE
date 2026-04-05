package com.sipc115.helix.domain.workflow;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

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

    private Map<String, Set<String>> predecessors = new TreeMap<>();

    private Map<String, Set<String>> successors = new TreeMap<>();

    private Map<String, Set<String>> completedPredecessors = new TreeMap<>();
}

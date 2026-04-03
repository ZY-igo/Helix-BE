/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.AiStepExecutionEntity;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import com.sipc115.helix.domain.workflow.WorkflowTraceEvent;
import com.sipc115.helix.integration.mq.WorkflowTraceMQService;
import com.sipc115.helix.repository.jpa.AiStepExecutionRepository;
import com.sipc115.helix.repository.jpa.NodeExecutionTraceRepository;
import com.sipc115.helix.repository.jpa.WorkflowExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowTraceService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTraceService.class);

    private final WorkflowExecutionRepository executionRepo;
    private final NodeExecutionTraceRepository nodeTraceRepo;
    private final AiStepExecutionRepository aiStepRepo;
    private final WorkflowTraceMQService mqService;
    private final ObjectMapper objectMapper;

    public WorkflowTraceService(
            WorkflowExecutionRepository executionRepo,
            NodeExecutionTraceRepository nodeTraceRepo,
            AiStepExecutionRepository aiStepRepo,
            WorkflowTraceMQService mqService,
            ObjectMapper objectMapper) {
        this.executionRepo = executionRepo;
        this.nodeTraceRepo = nodeTraceRepo;
        this.aiStepRepo = aiStepRepo;
        this.mqService = mqService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowExecutionEntity startExecution(String workflowId, String version,
                                                   Map<String, Object> input, String triggeredBy) {
        WorkflowExecutionEntity entity = new WorkflowExecutionEntity();
        entity.setWorkflowId(workflowId);
        entity.setVersion(version);
        entity.setStatus("RUNNING");
        entity.setStartedAt(Instant.now());
        entity.setInput(input);
        entity.setTriggeredBy(triggeredBy);

        WorkflowExecutionEntity saved = executionRepo.save(entity);
        log.info("Workflow execution started: executionId={}, workflowId={}",
            saved.getId(), workflowId);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_START)
            .executionId(saved.getId())
            .status("RUNNING")
            .inputData(input)
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);

        return saved;
    }

    @Transactional
    public void markExecutionSuccess(Long executionId, Map<String, Object> output) {
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        entity.setStatus("SUCCESS");
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        executionRepo.save(entity);
        log.info("Workflow execution completed successfully: executionId={}", executionId);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_COMPLETE)
            .executionId(executionId)
            .status("SUCCESS")
            .outputData(output)
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);
    }

    @Transactional
    public void markExecutionFailed(Long executionId, String errorMessage) {
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        entity.setStatus("FAILED");
        entity.setEndedAt(Instant.now());
        entity.setErrorMessage(errorMessage);
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        executionRepo.save(entity);
        log.error("Workflow execution failed: executionId={}, error={}", executionId, errorMessage);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_COMPLETE)
            .executionId(executionId)
            .status("FAILED")
            .metadata(Map.of("errorMessage", errorMessage))
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);
    }

    @Transactional
    public NodeExecutionTraceEntity startNodeExecution(Long executionId, String nodeId,
                                                        String nodeType, String nodeRole,
                                                        Integer executionOrder,
                                                        Map<String, Object> input) {
        NodeExecutionTraceEntity entity = new NodeExecutionTraceEntity();
        entity.setExecutionId(executionId);
        entity.setNodeId(nodeId);
        entity.setNodeType(nodeType);
        entity.setNodeRole(nodeRole);
        entity.setExecutionOrder(executionOrder);
        entity.setStatus("RUNNING");
        entity.setStartedAt(Instant.now());
        entity.setInput(input);
        entity.setRetryCount(0);

        NodeExecutionTraceEntity saved = nodeTraceRepo.save(entity);
        log.debug("Node execution started: traceId={}, nodeId={}", saved.getId(), nodeId);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_START)
            .executionId(executionId)
            .nodeId(nodeId)
            .nodeTraceId(saved.getId())
            .status("RUNNING")
            .executionOrder(executionOrder)
            .stepType(nodeRole)
            .inputData(input)
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);

        return saved;
    }

    @Transactional
    public void markNodeSuccess(Long traceId, Map<String, Object> output) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus("SUCCESS");
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        nodeTraceRepo.save(entity);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_COMPLETE)
            .executionId(entity.getExecutionId())
            .nodeId(entity.getNodeId())
            .nodeTraceId(traceId)
            .status("SUCCESS")
            .executionOrder(entity.getExecutionOrder())
            .outputData(output)
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);
    }

    @Transactional
    public void markNodeFailed(Long traceId, String errorMessage, String errorStack) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus("FAILED");
        entity.setEndedAt(Instant.now());
        entity.setErrorMessage(errorMessage);
        entity.setErrorStack(errorStack);
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        nodeTraceRepo.save(entity);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_COMPLETE)
            .executionId(entity.getExecutionId())
            .nodeId(entity.getNodeId())
            .nodeTraceId(traceId)
            .status("FAILED")
            .executionOrder(entity.getExecutionOrder())
            .metadata(Map.of(
                "errorMessage", errorMessage,
                "errorStack", errorStack != null ? errorStack : ""
            ))
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);
    }

    @Transactional
    public void markNodeSkipped(Long traceId, String reason) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus("SKIPPED");
        entity.setErrorMessage(reason);

        nodeTraceRepo.save(entity);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_COMPLETE)
            .executionId(entity.getExecutionId())
            .nodeId(entity.getNodeId())
            .nodeTraceId(traceId)
            .status("SKIPPED")
            .executionOrder(entity.getExecutionOrder())
            .metadata(Map.of("reason", reason))
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);
    }

    @Transactional
    public AiStepExecutionEntity recordAiStep(Long nodeTraceId, Long executionId, String stepId, String stepType,
                                              int round, String status, long durationMs,
                                              String modelName, String inputContent,
                                              String outputContent, Map<String, Object> varsSnapshot) {
        Instant now = Instant.now();
        AiStepExecutionEntity entity = new AiStepExecutionEntity();
        entity.setNodeTraceId(nodeTraceId);
        entity.setExecutionId(executionId);
        entity.setStepId(stepId);
        entity.setStepType(stepType);
        entity.setRound(round);
        entity.setStatus(status);
        entity.setStartedAt(now);
        entity.setEndedAt(now.plusMillis(durationMs));
        entity.setDurationMs(durationMs);
        entity.setModelName(modelName);
        entity.setInputContent(inputContent);
        entity.setOutputContent(outputContent);
        entity.setVarsSnapshot(varsSnapshot);

        AiStepExecutionEntity saved = aiStepRepo.save(entity);
        log.trace("AI step recorded: stepId={}, round={}", stepId, round);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_AI_STEP)
            .executionId(executionId)
            .nodeTraceId(nodeTraceId)
            .status(status)
            .round(round)
            .stepType(stepType)
            .inputData(varsSnapshot)
            .outputData(Map.of(
                "modelName", modelName != null ? modelName : "",
                "durationMs", durationMs,
                "inputContent", inputContent != null ? inputContent : "",
                "outputContent", outputContent != null ? outputContent : ""
            ))
            .timestamp(now)
            .build();
        mqService.sendAiStepEvent(event);

        return saved;
    }

    public List<WorkflowExecutionEntity> getExecutions(String workflowId, int limit) {
        return executionRepo.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .limit(limit)
                .toList();
    }

    public List<NodeExecutionTraceEntity> getNodeTraces(Long executionId) {
        return nodeTraceRepo.findByExecutionIdOrderByExecutionOrder(executionId);
    }

    public List<AiStepExecutionEntity> getAiSteps(Long nodeTraceId) {
        return aiStepRepo.findByNodeTraceIdOrderByRound(nodeTraceId);
    }

    public List<AiStepExecutionEntity> getAiStepsByExecutionId(Long executionId) {
        return aiStepRepo.findByExecutionIdOrderByCreatedAt(executionId);
    }

    public ExecutionStats getStats(String workflowId) {
        List<WorkflowExecutionEntity> executions = executionRepo.findByWorkflowId(workflowId);

        ExecutionStats stats = new ExecutionStats();
        stats.setTotal(executions.size());
        stats.setSuccess(executions.stream()
                .filter(e -> "SUCCESS".equals(e.getStatus()))
                .count());
        stats.setFailed(executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .count());

        stats.setAvgDurationMs(executions.stream()
                .filter(e -> e.getTotalDurationMs() != null)
                .mapToLong(WorkflowExecutionEntity::getTotalDurationMs)
                .average()
                .orElse(0.0));

        return stats;
    }

    private long calculateDuration(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).toMillis();
    }

    @lombok.Data
    public static class ExecutionStats {
        private long total;
        private long success;
        private long failed;
        private double avgDurationMs;
    }
}

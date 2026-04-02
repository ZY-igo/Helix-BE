/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.AiStepExecutionEntity;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
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

/**
 * 工作流追踪服务
 * <p>
 * 负责记录和查询工作流的执行轨迹
 */
@Service
public class WorkflowTraceService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTraceService.class);

    private final WorkflowExecutionRepository executionRepo;
    private final NodeExecutionTraceRepository nodeTraceRepo;
    private final AiStepExecutionRepository aiStepRepo;
    private final ObjectMapper objectMapper;

    public WorkflowTraceService(
            WorkflowExecutionRepository executionRepo,
            NodeExecutionTraceRepository nodeTraceRepo,
            AiStepExecutionRepository aiStepRepo,
            ObjectMapper objectMapper) {
        this.executionRepo = executionRepo;
        this.nodeTraceRepo = nodeTraceRepo;
        this.aiStepRepo = aiStepRepo;
        this.objectMapper = objectMapper;
    }

    // ==================== 工作流执行追踪 ====================

    /**
     * 创建工作流执行记录
     */
    @Transactional
    public WorkflowExecutionEntity startExecution(String workflowId, Integer version,
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

        return saved;
    }

    /**
     * 标记工作流执行成功
     */
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
    }

    /**
     * 标记工作流执行失败
     */
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
    }

    // ==================== 节点执行追踪 ====================

    /**
     * 记录节点开始执行
     */
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

        return saved;
    }

    /**
     * 记录节点执行成功
     */
    @Transactional
    public void markNodeSuccess(Long traceId, Map<String, Object> output) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus("SUCCESS");
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        nodeTraceRepo.save(entity);
    }

    /**
     * 记录节点执行失败
     */
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
    }

    /**
     * 记录节点被跳过
     */
    @Transactional
    public void markNodeSkipped(Long traceId, String reason) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus("SKIPPED");
        entity.setErrorMessage(reason);

        nodeTraceRepo.save(entity);
    }

    // ==================== AI 步骤追踪 ====================

    /**
     * 记录 AI 步骤执行
     */
    @Transactional
    public AiStepExecutionEntity recordAiStep(Long nodeTraceId, String stepId, String stepType,
                                              int round, String status, long durationMs,
                                              String modelName, String inputContent,
                                              String outputContent, Map<String, Object> varsSnapshot) {
        AiStepExecutionEntity entity = new AiStepExecutionEntity();
        entity.setNodeTraceId(nodeTraceId);
        entity.setStepId(stepId);
        entity.setStepType(stepType);
        entity.setRound(round);
        entity.setStatus(status);
        entity.setDurationMs(durationMs);
        entity.setModelName(modelName);
        entity.setInputContent(inputContent);
        entity.setOutputContent(outputContent);
        entity.setVarsSnapshot(varsSnapshot);

        AiStepExecutionEntity saved = aiStepRepo.save(entity);
        log.trace("AI step recorded: stepId={}, round={}", stepId, round);

        return saved;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取工作流的所有执行记录
     */
    public List<WorkflowExecutionEntity> getExecutions(String workflowId, int limit) {
        return executionRepo.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * 获取某次执行的所有节点轨迹
     */
    public List<NodeExecutionTraceEntity> getNodeTraces(Long executionId) {
        return nodeTraceRepo.findByExecutionIdOrderByExecutionOrder(executionId);
    }

    /**
     * 获取某个节点的所有 AI 步骤轨迹
     */
    public List<AiStepExecutionEntity> getAiSteps(Long nodeTraceId) {
        return aiStepRepo.findByNodeTraceIdOrderByRound(nodeTraceId);
    }

    /**
     * 获取执行统计信息
     */
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

        // 计算平均耗时
        stats.setAvgDurationMs(executions.stream()
                .filter(e -> e.getTotalDurationMs() != null)
                .mapToLong(WorkflowExecutionEntity::getTotalDurationMs)
                .average()
                .orElse(0.0));

        return stats;
    }

    // ==================== 工具方法 ====================

    private long calculateDuration(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).toMillis();
    }

    /**
     * 执行统计 DTO
     */
    @lombok.Data
    public static class ExecutionStats {
        private long total;
        private long success;
        private long failed;
        private double avgDurationMs;
    }
}

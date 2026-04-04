/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.trace;

import com.sipc115.helix.common.constant.ExecutionStatusConstants;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.domain.workflow.WorkflowExecutionEntity;
import com.sipc115.helix.domain.workflow.WorkflowTraceEvent;
import com.sipc115.helix.integration.mq.WorkflowTraceMQService;
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
 * 核心服务类，负责记录和查询工作流的执行轨迹。
 *
 * <h3>追踪模型：</h3>
 * <ul>
 *   <li>L1 工作流级 - 记录整个工作流的执行状态</li>
 *   <li>L2 节点级 - 记录每个节点的执行情况</li>
 * </ul>
 *
 * <h3>存储策略：</h3>
 * <ul>
 *   <li>所有数据首先同步存储到 PostgreSQL 保证可靠性</li>
 *   <li>工作流事件同步发送到 RocketMQ，确保不丢失</li>
 *   <li>节点事件异步发送，平衡性能和可靠性</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Service
public class WorkflowTraceService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTraceService.class);

    private final WorkflowExecutionRepository executionRepo;
    private final NodeExecutionTraceRepository nodeTraceRepo;
    private final WorkflowTraceMQService mqService;

    public WorkflowTraceService(
            WorkflowExecutionRepository executionRepo,
            NodeExecutionTraceRepository nodeTraceRepo,
            WorkflowTraceMQService mqService) {
        this.executionRepo = executionRepo;
        this.nodeTraceRepo = nodeTraceRepo;
        this.mqService = mqService;
    }

    /**
     * 记录工作流执行开始
     */
    @Transactional
    public WorkflowExecutionEntity startExecution(String workflowId, String version,
                                                   Map<String, Object> input, String triggeredBy) {
        WorkflowExecutionEntity entity = new WorkflowExecutionEntity();
        entity.setWorkflowId(workflowId);
        entity.setVersion(version);
        entity.setStatus(ExecutionStatusConstants.WORKFLOW_RUNNING);
        entity.setStartedAt(Instant.now());
        entity.setInput(input);
        entity.setTriggeredBy(triggeredBy);

        WorkflowExecutionEntity saved = executionRepo.save(entity);
        log.info("工作流执行开始: executionId={}, workflowId={}", saved.getId(), workflowId);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_START)
            .executionId(saved.getId())
            .status(ExecutionStatusConstants.WORKFLOW_RUNNING)
            .inputData(input)
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);

        return saved;
    }

    /**
     * 标记工作流执行成功
     */
    @Transactional
    public void markExecutionSuccess(Long executionId, Map<String, Object> output) {
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        entity.setStatus(ExecutionStatusConstants.WORKFLOW_SUCCESS);
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        executionRepo.save(entity);
        log.info("工作流执行成功: executionId={}", executionId);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_COMPLETE)
            .executionId(executionId)
            .status(ExecutionStatusConstants.WORKFLOW_SUCCESS)
            .outputData(output)
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);
    }

    /**
     * 标记工作流执行失败
     */
    @Transactional
    public void markExecutionFailed(Long executionId, String errorMessage) {
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        entity.setStatus(ExecutionStatusConstants.WORKFLOW_FAILED);
        entity.setEndedAt(Instant.now());
        entity.setErrorMessage(errorMessage);
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        executionRepo.save(entity);
        log.error("工作流执行失败: executionId={}, error={}", executionId, errorMessage);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_COMPLETE)
            .executionId(executionId)
            .status(ExecutionStatusConstants.WORKFLOW_FAILED)
            .metadata(Map.of("errorMessage", errorMessage))
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);
    }

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
        entity.setStatus(ExecutionStatusConstants.NODE_RUNNING);
        entity.setStartedAt(Instant.now());
        entity.setInput(input);
        entity.setRetryCount(0);

        NodeExecutionTraceEntity saved = nodeTraceRepo.save(entity);
        log.debug("节点执行开始: traceId={}, nodeId={}", saved.getId(), nodeId);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_START)
            .executionId(executionId)
            .nodeId(nodeId)
            .nodeTraceId(saved.getId())
            .status(ExecutionStatusConstants.NODE_RUNNING)
            .executionOrder(executionOrder)
            .inputData(input)
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);

        return saved;
    }

    /**
     * 记录节点执行成功
     */
    @Transactional
    public void markNodeSuccess(Long traceId, Map<String, Object> output) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus(ExecutionStatusConstants.NODE_SUCCESS);
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        nodeTraceRepo.save(entity);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_COMPLETE)
            .executionId(entity.getExecutionId())
            .nodeId(entity.getNodeId())
            .nodeTraceId(traceId)
            .status(ExecutionStatusConstants.NODE_SUCCESS)
            .executionOrder(entity.getExecutionOrder())
            .outputData(output)
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);
    }

    /**
     * 记录节点执行失败
     */
    @Transactional
    public void markNodeFailed(Long traceId, String errorMessage, String errorStack) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus(ExecutionStatusConstants.NODE_FAILED);
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
            .status(ExecutionStatusConstants.NODE_FAILED)
            .executionOrder(entity.getExecutionOrder())
            .metadata(Map.of(
                "errorMessage", errorMessage,
                "errorStack", errorStack != null ? errorStack : ""
            ))
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);
    }

    /**
     * 记录节点被跳过
     */
    @Transactional
    public void markNodeSkipped(Long traceId, String reason) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus(ExecutionStatusConstants.NODE_SKIPPED);
        entity.setErrorMessage(reason);

        nodeTraceRepo.save(entity);

        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_NODE_COMPLETE)
            .executionId(entity.getExecutionId())
            .nodeId(entity.getNodeId())
            .nodeTraceId(traceId)
            .status(ExecutionStatusConstants.NODE_SKIPPED)
            .executionOrder(entity.getExecutionOrder())
            .metadata(Map.of("reason", reason))
            .timestamp(Instant.now())
            .build();
        mqService.sendNodeEvent(event);
    }

    /**
     * 查询工作流的所有执行记录
     */
    public List<WorkflowExecutionEntity> getExecutions(String workflowId, int limit) {
        return executionRepo.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * 查询某次执行的所有节点追踪记录
     */
    public List<NodeExecutionTraceEntity> getNodeTraces(Long executionId) {
        return nodeTraceRepo.findByExecutionIdOrderByExecutionOrder(executionId);
    }

    /**
     * 获取工作流执行统计信息
     */
    public ExecutionStats getStats(String workflowId) {
        List<WorkflowExecutionEntity> executions = executionRepo.findByWorkflowId(workflowId);

        ExecutionStats stats = new ExecutionStats();
        stats.setTotal(executions.size());
        stats.setSuccess(executions.stream()
                .filter(e -> ExecutionStatusConstants.WORKFLOW_SUCCESS.equals(e.getStatus()))
                .count());
        stats.setFailed(executions.stream()
                .filter(e -> ExecutionStatusConstants.WORKFLOW_FAILED.equals(e.getStatus()))
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

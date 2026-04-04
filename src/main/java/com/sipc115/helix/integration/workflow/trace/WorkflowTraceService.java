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

/**
 * 工作流追踪服务
 * <p>
 * 核心服务类，负责记录和查询工作流的执行轨迹。
 * 采用三层追踪模型：
 * <ul>
 *   <li>L1 工作流级 - 记录整个工作流的执行状态</li>
 *   <li>L2 节点级 - 记录每个节点的执行情况</li>
 *   <li>L3 AI步骤级 - 记录AI任务内部的微流程步骤</li>
 * </ul>
 *
 * <h3>存储策略：</h3>
 * <ul>
 *   <li>所有数据首先同步存储到 PostgreSQL 保证可靠性</li>
 *   <li>工作流事件同步发送到 RocketMQ，确保不丢失</li>
 *   <li>节点事件异步发送，平衡性能和可靠性</li>
 *   <li>AI步骤事件批量缓冲发送，提高吞吐量</li>
 * </ul>
 *
 * <h3>事务管理：</h3>
 * <p>
 * 所有写操作都使用 @Transactional 注解确保原子性。
 * MQ发送发生在事务提交后，由 Spring 的 TransactionSynchronization 管理。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowTraceMQService
 * @see WorkflowExecutionEntity
 * @see NodeExecutionTraceEntity
 * @see AiStepExecutionEntity
 */
@Service
public class WorkflowTraceService {

    // ==================== 日志记录器 ====================

    /**
     * 日志记录器
     * <p>
     * 用于记录追踪服务的关键操作和错误信息。
     */
    private static final Logger log = LoggerFactory.getLogger(WorkflowTraceService.class);

    // ==================== 依赖组件 ====================

    /**
     * 工作流执行记录仓储
     * <p>
     * 用于持久化和查询 WorkflowExecutionEntity。
     */
    private final WorkflowExecutionRepository executionRepo;

    /**
     * 节点执行追踪仓储
     * <p>
     * 用于持久化和查询 NodeExecutionTraceEntity。
     */
    private final NodeExecutionTraceRepository nodeTraceRepo;

    /**
     * AI步骤执行记录仓储
     * <p>
     * 用于持久化和查询 AiStepExecutionEntity。
     */
    private final AiStepExecutionRepository aiStepRepo;

    /**
     * MQ发送服务
     * <p>
     * 负责将追踪事件发送到 RocketMQ。
     * 采用三级发送策略：同步、异步、批量。
     */
    private final WorkflowTraceMQService mqService;

    /**
     * JSON对象映射器
     * <p>
     * 用于序列化和反序列化JSON数据。
     */
    private final ObjectMapper objectMapper;

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     * <p>
     * 通过依赖注入获取所有必需的组件实例。
     *
     * @param executionRepo 工作流执行记录仓储
     * @param nodeTraceRepo 节点执行追踪仓储
     * @param aiStepRepo AI步骤执行记录仓储
     * @param mqService MQ发送服务
     * @param objectMapper JSON对象映射器
     */
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

    // ==================== L1: 工作流级追踪 ====================

    /**
     * 记录工作流执行开始
     * <p>
     * 当工作流实例开始执行时调用，创建工作流执行记录并发送启动事件。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>创建 WorkflowExecutionEntity 并设置初始状态为 RUNNING</li>
     *   <li>保存到数据库</li>
     *   <li>发送 WORKFLOW_START 事件到 MQ（同步）</li>
     * </ol>
     *
     * <h3>事务说明：</h3>
     * <p>
     * 如果 MQ 发送失败，会抛出 RuntimeException 导致事务回滚，
     * 确保 DB 和 MQ 的数据一致性。
     *
     * @param workflowId 工作流ID
     * @param version 工作流版本
     * @param input 输入参数
     * @param triggeredBy 触发者
     * @return 保存后的工作流执行实体，包含生成的主键ID
     * @throws RuntimeException 如果MQ发送失败
     */
    @Transactional
    public WorkflowExecutionEntity startExecution(String workflowId, String version,
                                                   Map<String, Object> input, String triggeredBy) {
        // 创建工作流执行记录实体
        WorkflowExecutionEntity entity = new WorkflowExecutionEntity();
        entity.setWorkflowId(workflowId);
        entity.setVersion(version);
        entity.setStatus("RUNNING");
        entity.setStartedAt(Instant.now());
        entity.setInput(input);
        entity.setTriggeredBy(triggeredBy);

        // 持久化到数据库
        WorkflowExecutionEntity saved = executionRepo.save(entity);
        log.info("Workflow execution started: executionId={}, workflowId={}",
            saved.getId(), workflowId);

        // 构建并发送工作流启动事件（同步发送）
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

    /**
     * 标记工作流执行成功
     * <p>
     * 当工作流正常完成时调用，更新执行记录状态并发送完成事件。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>查询并更新 WorkflowExecutionEntity 状态为 SUCCESS</li>
     *   <li>计算并设置总执行耗时</li>
     *   <li>保存到数据库</li>
     *   <li>发送 WORKFLOW_COMPLETE 事件到 MQ（同步）</li>
     * </ol>
     *
     * @param executionId 工作流执行ID
     * @param output 工作流输出结果
     * @throws IllegalArgumentException 如果找不到对应的执行记录
     */
    @Transactional
    public void markExecutionSuccess(Long executionId, Map<String, Object> output) {
        // 查询并更新执行记录
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        entity.setStatus("SUCCESS");
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        executionRepo.save(entity);
        log.info("Workflow execution completed successfully: executionId={}", executionId);

        // 发送工作流完成事件
        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_COMPLETE)
            .executionId(executionId)
            .status("SUCCESS")
            .outputData(output)
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);
    }

    /**
     * 标记工作流执行失败
     * <p>
     * 当工作流执行异常终止时调用，记录错误信息并发送失败事件。
     *
     * @param executionId 工作流执行ID
     * @param errorMessage 错误信息摘要
     * @throws IllegalArgumentException 如果找不到对应的执行记录
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

        // 发送失败事件，错误信息放在 metadata 中
        WorkflowTraceEvent event = WorkflowTraceEvent.builder()
            .eventType(WorkflowTraceEvent.EVENT_WORKFLOW_COMPLETE)
            .executionId(executionId)
            .status("FAILED")
            .metadata(Map.of("errorMessage", errorMessage))
            .timestamp(Instant.now())
            .build();
        mqService.sendWorkflowEvent(event);
    }

    // ==================== L2: 节点级追踪 ====================

    /**
     * 记录节点开始执行
     * <p>
     * 当工作流中的节点开始执行时调用，创建节点执行追踪记录。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>创建 NodeExecutionTraceEntity 并设置初始状态为 RUNNING</li>
     *   <li>保存到数据库</li>
     *   <li>发送 NODE_START 事件到 MQ（异步）</li>
     * </ol>
     *
     * <h3>返回说明：</h3>
     * <p>
     * 返回保存后的实体，其中包含生成的主键ID。
     * 调用方需要使用此ID在节点执行完成时更新记录。
     *
     * @param executionId 工作流执行ID
     * @param nodeId 节点ID
     * @param nodeType 节点类型
     * @param nodeRole 节点角色（START/NORMAL/END）
     * @param executionOrder 执行顺序号
     * @param input 节点输入参数
     * @return 保存后的节点执行追踪实体
     */
    @Transactional
    public NodeExecutionTraceEntity startNodeExecution(Long executionId, String nodeId,
                                                        String nodeType, String nodeRole,
                                                        Integer executionOrder,
                                                        Map<String, Object> input) {
        // 创建节点执行追踪记录
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

        // 持久化到数据库
        NodeExecutionTraceEntity saved = nodeTraceRepo.save(entity);
        log.debug("Node execution started: traceId={}, nodeId={}", saved.getId(), nodeId);

        // 发送节点开始事件（异步发送）
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

    /**
     * 记录节点执行成功
     * <p>
     * 当节点正常执行完成时调用，更新追踪记录状态并计算执行耗时。
     *
     * @param traceId 节点执行追踪ID（来自 startNodeExecution 的返回值）
     * @param output 节点输出结果
     * @throws IllegalArgumentException 如果找不到对应的追踪记录
     */
    @Transactional
    public void markNodeSuccess(Long traceId, Map<String, Object> output) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        // 更新执行结果
        entity.setStatus("SUCCESS");
        entity.setEndedAt(Instant.now());
        entity.setOutput(output);
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        nodeTraceRepo.save(entity);

        // 发送节点完成事件
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

    /**
     * 记录节点执行失败
     * <p>
     * 当节点执行过程中发生异常时调用，记录错误信息和堆栈。
     *
     * @param traceId 节点执行追踪ID
     * @param errorMessage 错误信息摘要
     * @param errorStack 完整错误堆栈
     * @throws IllegalArgumentException 如果找不到对应的追踪记录
     */
    @Transactional
    public void markNodeFailed(Long traceId, String errorMessage, String errorStack) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        // 更新失败状态和错误信息
        entity.setStatus("FAILED");
        entity.setEndedAt(Instant.now());
        entity.setErrorMessage(errorMessage);
        entity.setErrorStack(errorStack);
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        nodeTraceRepo.save(entity);

        // 发送节点失败事件，错误详情放在 metadata 中
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

    /**
     * 记录节点被跳过
     * <p>
     * 当节点由于条件判断或其他逻辑被跳过时调用。
     *
     * @param traceId 节点执行追踪ID
     * @param reason 跳过原因
     * @throws IllegalArgumentException 如果找不到对应的追踪记录
     */
    @Transactional
    public void markNodeSkipped(Long traceId, String reason) {
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        entity.setStatus("SKIPPED");
        entity.setErrorMessage(reason);

        nodeTraceRepo.save(entity);

        // 发送节点跳过事件，跳过原因放在 metadata 中
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

    // ==================== L3: AI步骤级追踪 ====================

    /**
     * 记录AI步骤执行
     * <p>
     * 当AI任务节点内部的微流程步骤执行时调用。
     * 用于追踪多轮对话、生成验证等AI执行细节。
     *
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>设置开始时间和结束时间（基于durationMs计算）</li>
     *   <li>保存 AiStepExecutionEntity 到数据库</li>
     *   <li>将事件添加到MQ缓冲池（达到阈值后自动批量发送）</li>
     * </ol>
     *
     * <h3>性能优化：</h3>
     * <p>
     * AI步骤事件采用缓冲发送模式，减少网络往返。
     * 建议在AI节点执行结束时调用 flushAiStepBuffer() 确保所有事件被发送。
     *
     * @param nodeTraceId 关联的节点执行追踪ID
     * @param executionId 工作流执行ID（用于跨节点查询）
     * @param stepId 步骤ID
     * @param stepType 步骤类型（GENERATE/VALIDATE/CHAT等）
     * @param round 轮次号（多轮对话时递增）
     * @param status 执行状态（RUNNING/SUCCESS/FAILED）
     * @param durationMs 执行耗时（毫秒）
     * @param modelName 使用的模型名称
     * @param inputContent 输入内容（Prompt）
     * @param outputContent 输出内容（AI回复）
     * @param varsSnapshot 变量状态快照
     * @return 保存后的AI步骤执行记录
     */
    @Transactional
    public AiStepExecutionEntity recordAiStep(Long nodeTraceId, Long executionId, String stepId, String stepType,
                                              int round, String status, long durationMs,
                                              String modelName, String inputContent,
                                              String outputContent, Map<String, Object> varsSnapshot) {
        Instant now = Instant.now();

        // 创建并填充AI步骤执行记录
        AiStepExecutionEntity entity = new AiStepExecutionEntity();
        entity.setNodeTraceId(nodeTraceId);
        entity.setExecutionId(executionId);
        entity.setStepId(stepId);
        entity.setStepType(stepType);
        entity.setRound(round);
        entity.setStatus(status);
        entity.setStartedAt(now);
        // 根据durationMs计算结束时间
        entity.setEndedAt(now.plusMillis(durationMs));
        entity.setDurationMs(durationMs);
        entity.setModelName(modelName);
        entity.setInputContent(inputContent);
        entity.setOutputContent(outputContent);
        entity.setVarsSnapshot(varsSnapshot);

        // 持久化到数据库
        AiStepExecutionEntity saved = aiStepRepo.save(entity);
        log.trace("AI step recorded: stepId={}, round={}", stepId, round);

        // 构建并发送AI步骤事件（添加到缓冲池）
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
        // 缓冲发送，攒够阈值后自动批量发送
        mqService.sendAiStepEvent(event);

        return saved;
    }

    // ==================== 查询方法 ====================

    /**
     * 查询工作流的所有执行记录
     * <p>
     * 按创建时间倒序返回指定工作流的所有执行记录。
     *
     * @param workflowId 工作流ID
     * @param limit 返回记录数量限制
     * @return 执行记录列表
     */
    public List<WorkflowExecutionEntity> getExecutions(String workflowId, int limit) {
        return executionRepo.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * 查询某次执行的所有节点追踪记录
     * <p>
     * 按执行顺序号排序返回。
     *
     * @param executionId 工作流执行ID
     * @return 节点追踪记录列表
     */
    public List<NodeExecutionTraceEntity> getNodeTraces(Long executionId) {
        return nodeTraceRepo.findByExecutionIdOrderByExecutionOrder(executionId);
    }

    /**
     * 查询某节点的所有AI步骤记录
     * <p>
     * 按轮次号排序返回。
     *
     * @param nodeTraceId 节点执行追踪ID
     * @return AI步骤记录列表
     */
    public List<AiStepExecutionEntity> getAiSteps(Long nodeTraceId) {
        return aiStepRepo.findByNodeTraceIdOrderByRound(nodeTraceId);
    }

    /**
     * 查询某次工作流执行的所有AI步骤
     * <p>
     * 直接通过executionId查询，无需通过节点间接查询。
     * 用于一次性获取某次执行中的所有AI交互记录。
     *
     * @param executionId 工作流执行ID
     * @return AI步骤记录列表（按创建时间排序）
     */
    public List<AiStepExecutionEntity> getAiStepsByExecutionId(Long executionId) {
        return aiStepRepo.findByExecutionIdOrderByCreatedAt(executionId);
    }

    /**
     * 获取工作流执行统计信息
     * <p>
     * 统计指定工作流的执行次数、成功次数、失败次数和平均执行耗时。
     *
     * @param workflowId 工作流ID
     * @return 统计信息对象
     */
    public ExecutionStats getStats(String workflowId) {
        List<WorkflowExecutionEntity> executions = executionRepo.findByWorkflowId(workflowId);

        // 构建统计结果
        ExecutionStats stats = new ExecutionStats();
        stats.setTotal(executions.size());
        stats.setSuccess(executions.stream()
                .filter(e -> "SUCCESS".equals(e.getStatus()))
                .count());
        stats.setFailed(executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .count());

        // 计算平均执行耗时
        stats.setAvgDurationMs(executions.stream()
                .filter(e -> e.getTotalDurationMs() != null)
                .mapToLong(WorkflowExecutionEntity::getTotalDurationMs)
                .average()
                .orElse(0.0));

        return stats;
    }

    // ==================== 工具方法 ====================

    /**
     * 计算时间间隔
     * <p>
     * 计算两个时间点之间的毫秒差值。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 毫秒差值，如果任一参数为null则返回0
     */
    private long calculateDuration(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0L;
        }
        return java.time.Duration.between(start, end).toMillis();
    }

    // ==================== 内部类 ====================

    /**
     * 执行统计信息
     * <p>
     * 用于封装工作流执行统计结果。
     */
    @lombok.Data
    public static class ExecutionStats {
        /**
         * 总执行次数
         */
        private long total;

        /**
         * 成功次数
         */
        private long success;

        /**
         * 失败次数
         */
        private long failed;

        /**
         * 平均执行耗时（毫秒）
         */
        private double avgDurationMs;
    }
}

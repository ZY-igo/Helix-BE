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
 * 提供工作流级别和节点级别的执行追踪能力，是 Helix 工作流引擎可观测性的核心组件。
 *
 * <h3>追踪模型（两层架构）：</h3>
 * <ul>
 *   <li>L1 工作流级追踪 - 记录整个工作流执行的生命周期</li>
 *   <li>L2 节点级追踪 - 记录每个节点的输入、输出、状态和耗时</li>
 * </ul>
 *
 * <h3>存储策略：</h3>
 * <ul>
 *   <li>PostgreSQL 同步存储 - 保证追踪数据的可靠性</li>
 *   <li>RocketMQ 事件发送 - 实现追踪数据的实时流式处理</li>
 *   <li>工作流事件同步发送 - 确保关键事件不丢失</li>
 *   <li>节点事件异步发送 - 平衡性能和可靠性</li>
 * </ul>
 *
 * <h3>核心功能：</h3>
 * <ul>
 *   <li>工作流执行生命周期记录（开始、成功、失败）</li>
 *   <li>节点执行追踪（开始、成功、失败、跳过）</li>
 *   <li>重试次数追踪和幂等键生成</li>
 *   <li>执行耗时计算</li>
 *   <li>执行统计信息查询</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 1. 开始工作流执行追踪
 * WorkflowExecutionEntity execution = traceService.startExecution(
 *     "daily-report", "v1.0.0", input, "SCHEDULE"
 * );
 *
 * // 2. 开始节点执行追踪
 * NodeExecutionTraceEntity nodeTrace = traceService.startNodeExecution(
 *     execution.getId(), "sendNotify", "FEISHU_SEND_TEXT",
 *     "NORMAL", 1, nodeInput
 * );
 *
 * // 3. 标记节点执行成功
 * traceService.markNodeSuccess(nodeTrace.getId(), output);
 *
 * // 4. 标记工作流执行成功
 * traceService.markExecutionSuccess(execution.getId(), workflowOutput);
 * }
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowExecutionEntity
 * @see NodeExecutionTraceEntity
 * @see WorkflowTraceEvent
 */
@Service
public class WorkflowTraceService {

    /**
     * 日志记录器
     * <p>
     * 用于记录追踪服务的关键操作和调试信息。
     * 使用静态方式声明，因为 Spring 依赖注入在 Temporal Workflow 上下文中可能不可用。
     */
    private static final Logger log = LoggerFactory.getLogger(WorkflowTraceService.class);

    /**
     * 工作流执行仓储
     * <p>
     * 负责 WorkflowExecutionEntity 的持久化操作。
     * 用于保存和查询工作流级别的追踪数据。
     */
    private final WorkflowExecutionRepository executionRepo;

    /**
     * 节点执行追踪仓储
     * <p>
     * 负责 NodeExecutionTraceEntity 的持久化操作。
     * 用于保存和查询节点级别的追踪数据。
     */
    private final NodeExecutionTraceRepository nodeTraceRepo;

    /**
     * 工作流追踪消息队列服务
     * <p>
     * 负责将追踪事件发送到 RocketMQ，实现追踪数据的实时流式处理。
     * 支持工作流事件和节点事件的异步发送。
     */
    private final WorkflowTraceMQService mqService;

    /**
     * 构造函数
     * <p>
     * 通过构造器注入的方式初始化依赖。
     *
     * @param executionRepo 工作流执行仓储
     * @param nodeTraceRepo 节点执行追踪仓储
     * @param mqService 消息队列服务
     */
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
     * <p>
     * 当工作流开始执行时调用，创建工作流执行记录并发送开始事件。
     *
     * <h3>执行的操作：</h3>
     * <ol>
     *   <li>创建 WorkflowExecutionEntity 并设置初始状态为 RUNNING</li>
     *   <li>保存到 PostgreSQL 数据库</li>
     *   <li>发送工作流开始事件到 RocketMQ</li>
     * </ol>
     *
     * <h3>触发时机：</h3>
     * <ul>
     *   <li>工作流首次启动时</li>
     *   <li>工作流重新执行时（会创建新的 execution 记录）</li>
     * </ul>
     *
     * @param workflowId 工作流 ID（来自 DSL 定义）
     * @param version 工作流版本号
     * @param input 工作流输入参数
     * @param triggeredBy 触发来源（MANUAL/SCHEDULE/API/WEBHOOK 等）
     * @return 保存后的工作流执行实体（包含自动生成的主键 ID）
     */
    @Transactional
    public WorkflowExecutionEntity startExecution(String workflowId, String version,
                                                   Map<String, Object> input, String triggeredBy) {
        // 创建工作流执行实体
        WorkflowExecutionEntity entity = new WorkflowExecutionEntity();
        entity.setWorkflowId(workflowId);
        entity.setVersion(version);

        // 设置初始状态为运行中
        entity.setStatus(ExecutionStatusConstants.WORKFLOW_RUNNING);

        // 记录开始时间
        entity.setStartedAt(Instant.now());

        // 设置输入参数
        entity.setInput(input);

        // 设置触发来源
        entity.setTriggeredBy(triggeredBy);

        // 保存到数据库
        WorkflowExecutionEntity saved = executionRepo.save(entity);
        log.info("工作流执行开始: executionId={}, workflowId={}", saved.getId(), workflowId);

        // 构建并发送工作流开始事件到 MQ
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
     * <p>
     * 当工作流正常完成时调用，更新执行记录并发送成功事件。
     *
     * <h3>执行的操作：</h3>
     * <ol>
     *   <li>查找并更新 WorkflowExecutionEntity 状态为 SUCCESS</li>
     *   <li>设置结束时间和总耗时</li>
     *   <li>保存输出结果</li>
     *   <li>发送工作流完成事件到 RocketMQ</li>
     * </ol>
     *
     * <h3>与 markExecutionFailed 的区别：</h3>
     * <ul>
     *   <li>此方法标记成功状态，输出为业务数据</li>
     *   <li>markExecutionFailed 标记失败状态，errorMessage 存储错误信息</li>
     * </ul>
     *
     * @param executionId 工作流执行记录 ID
     * @param output 工作流执行完成后的输出结果
     * @throws IllegalArgumentException 如果找不到对应的执行记录
     */
    @Transactional
    public void markExecutionSuccess(Long executionId, Map<String, Object> output) {
        // 查找工作流执行记录
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        // 更新状态为成功
        entity.setStatus(ExecutionStatusConstants.WORKFLOW_SUCCESS);

        // 记录结束时间
        entity.setEndedAt(Instant.now());

        // 保存输出结果
        entity.setOutput(output);

        // 计算并保存总耗时
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        // 保存更新
        executionRepo.save(entity);
        log.info("工作流执行成功: executionId={}", executionId);

        // 发送工作流完成事件
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
     * <p>
     * 当工作流执行失败时调用，记录错误信息并发送失败事件。
     *
     * <h3>执行的操作：</h3>
     * <ol>
     *   <li>查找并更新 WorkflowExecutionEntity 状态为 FAILED</li>
     *   <li>设置结束时间和错误信息</li>
     *   <li>计算并保存总耗时</li>
     *   <li>发送工作流失败事件到 RocketMQ</li>
     * </ol>
     *
     * <h3>触发时机：</h3>
     * <ul>
     *   <li>工作流执行过程中抛出未捕获的异常</li>
     *   <li>工作流达到最大迭代次数</li>
     *   <li>外部取消工作流执行</li>
     * </ul>
     *
     * @param executionId 工作流执行记录 ID
     * @param errorMessage 错误信息摘要
     * @throws IllegalArgumentException 如果找不到对应的执行记录
     */
    @Transactional
    public void markExecutionFailed(Long executionId, String errorMessage) {
        // 查找工作流执行记录
        WorkflowExecutionEntity entity = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        // 更新状态为失败
        entity.setStatus(ExecutionStatusConstants.WORKFLOW_FAILED);

        // 记录结束时间
        entity.setEndedAt(Instant.now());

        // 保存错误信息
        entity.setErrorMessage(errorMessage);

        // 计算并保存总耗时
        entity.setTotalDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        // 保存更新
        executionRepo.save(entity);
        log.error("工作流执行失败: executionId={}, error={}", executionId, errorMessage);

        // 发送工作流失败事件
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
     * 记录节点开始执行（简化版本）
     * <p>
     * 用于首次执行节点的追踪记录创建。
     * 重试次数默认为 0。
     *
     * @param executionId 工作流执行 ID
     * @param nodeId 节点 ID
     * @param nodeType 节点类型
     * @param nodeRole 节点角色
     * @param executionOrder 执行顺序号
     * @param input 节点输入参数
     * @return 创建的节点追踪实体
     * @see #startNodeExecution(Long, String, String, String, Integer, Map, int)
     */
    @Transactional
    public NodeExecutionTraceEntity startNodeExecution(Long executionId, String nodeId,
                                                        String nodeType, String nodeRole,
                                                        Integer executionOrder,
                                                        Map<String, Object> input) {
        // 调用完整版本的重载方法，retryCount 默认为 0
        return startNodeExecution(executionId, nodeId, nodeType, nodeRole, executionOrder, input, 0);
    }

    /**
     * 记录节点开始执行（完整版本）
     * <p>
     * 当节点开始执行时调用，创建节点追踪记录并发送开始事件。
     *
     * <h3>执行的操作：</h3>
     * <ol>
     *   <li>生成幂等键 attemptId</li>
     *   <li>创建 NodeExecutionTraceEntity 并设置初始状态为 RUNNING</li>
     *   <li>保存到 PostgreSQL 数据库</li>
     *   <li>发送节点开始事件到 RocketMQ</li>
     * </ol>
     *
     * <h3>幂等键生成规则：</h3>
     * <pre>
     * attemptId = {executionId}_{nodeId}_{retryCount}
     *
     * 示例：
     * - 12345_sendNotify_0  （首次执行）
     * - 12345_sendNotify_1  （第一次重试）
     * </pre>
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>节点首次执行</li>
     *   <li>节点重试执行</li>
     *   <li>手动重新执行节点</li>
     * </ul>
     *
     * @param executionId 工作流执行 ID
     * @param nodeId 节点 ID
     * @param nodeType 节点类型（如 FEISHU_SEND_TEXT）
     * @param nodeRole 节点角色（NORMAL/START/END）
     * @param executionOrder 执行顺序号
     * @param input 节点输入参数
     * @param retryCount 重试次数（0 表示首次执行）
     * @return 创建的节点追踪实体
     */
    @Transactional
    public NodeExecutionTraceEntity startNodeExecution(Long executionId, String nodeId,
                                                        String nodeType, String nodeRole,
                                                        Integer executionOrder,
                                                        Map<String, Object> input,
                                                        int retryCount) {
        // 生成幂等键，用于防止 Activity 重复执行
        String attemptId = generateAttemptId(executionId, nodeId, retryCount);

        // 创建节点追踪实体
        NodeExecutionTraceEntity entity = new NodeExecutionTraceEntity();
        entity.setExecutionId(executionId);
        entity.setNodeId(nodeId);
        entity.setNodeType(nodeType);
        entity.setNodeRole(nodeRole);
        entity.setExecutionOrder(executionOrder);

        // 设置初始状态为运行中
        entity.setStatus(ExecutionStatusConstants.NODE_RUNNING);

        // 记录开始时间
        entity.setStartedAt(Instant.now());

        // 设置输入参数
        entity.setInput(input);

        // 设置重试次数
        entity.setRetryCount(retryCount);

        // 设置幂等键
        entity.setAttemptId(attemptId);

        // 保存到数据库
        NodeExecutionTraceEntity saved = nodeTraceRepo.save(entity);
        log.debug("节点执行开始: traceId={}, nodeId={}, attemptId={}", saved.getId(), nodeId, attemptId);

        // 构建并发送节点开始事件到 MQ
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
     * 生成幂等键
     * <p>
     * 根据执行 ID、节点 ID 和重试次数生成唯一的幂等键。
     * 幂等键用于防止 Activity 的重复执行。
     *
     * <h3>格式定义：</h3>
     * <pre>
     * {executionId}_{nodeId}_{retryCount}
     *
     * 示例：
     * - 12345_sendNotify_0
     * - 12345_fetchData_1
     * - 67890_loopStart_0
     * </pre>
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>作为 Temporal Activity 的幂等键</li>
     *   <li>防止消息队列重复消费</li>
     *   <li>支持节点的重新执行</li>
     * </ul>
     *
     * @param executionId 工作流执行 ID
     * @param nodeId 节点 ID
     * @param retryCount 重试次数
     * @return 格式化的幂等键字符串
     */
    public String generateAttemptId(Long executionId, String nodeId, int retryCount) {
        return String.format("%d_%s_%d", executionId, nodeId, retryCount);
    }

    /**
     * 记录节点执行成功
     * <p>
     * 当节点成功完成时调用，更新追踪记录并发送成功事件。
     *
     * <h3>执行的操作：</h3>
     * <ol>
     *   <li>查找并更新节点追踪记录状态为 SUCCESS</li>
     *   <li>设置结束时间和输出结果</li>
     *   <li>计算并保存执行耗时</li>
     *   <li>发送节点完成事件到 RocketMQ</li>
     * </ol>
     *
     * <h3>耗时计算：</h3>
     * <pre>
     * durationMs = endedAt - startedAt
     * </pre>
     *
     * @param traceId 节点追踪记录 ID
     * @param output 节点执行完成后的输出结果
     * @throws IllegalArgumentException 如果找不到对应的追踪记录
     */
    @Transactional
    public void markNodeSuccess(Long traceId, Map<String, Object> output) {
        // 查找节点追踪记录
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        // 更新状态为成功
        entity.setStatus(ExecutionStatusConstants.NODE_SUCCESS);

        // 记录结束时间
        entity.setEndedAt(Instant.now());

        // 保存输出结果
        entity.setOutput(output);

        // 计算并保存执行耗时
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        // 保存更新
        nodeTraceRepo.save(entity);

        // 发送节点完成事件
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
     * <p>
     * 当节点执行失败时调用，记录错误信息并发送失败事件。
     *
     * <h3>执行的操作：</h3>
     * <ol>
     *   <li>查找并更新节点追踪记录状态为 FAILED</li>
     *   <li>设置结束时间和错误信息</li>
     *   <li>计算并保存执行耗时</li>
     *   <li>发送节点失败事件到 RocketMQ</li>
     * </ol>
     *
     * <h3>与 markNodeSuccess 的区别：</h3>
     * <ul>
     *   <li>此方法标记失败状态，保存错误信息</li>
     *   <li>markNodeSuccess 标记成功状态，保存输出结果</li>
     * </ul>
     *
     * <h3>重试提示：</h3>
     * <ul>
     *   <li>节点失败后，工作流引擎会根据重试策略决定是否重试</li>
     *   <li>重试时会创建新的追踪记录（retryCount + 1）</li>
     *   <li>旧的追踪记录保持 FAILED 状态</li>
     * </ul>
     *
     * @param traceId 节点追踪记录 ID
     * @param errorMessage 错误信息摘要
     * @param errorStack 完整错误堆栈（可为 null）
     * @throws IllegalArgumentException 如果找不到对应的追踪记录
     */
    @Transactional
    public void markNodeFailed(Long traceId, String errorMessage, String errorStack) {
        // 查找节点追踪记录
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        // 更新状态为失败
        entity.setStatus(ExecutionStatusConstants.NODE_FAILED);

        // 记录结束时间
        entity.setEndedAt(Instant.now());

        // 保存错误信息
        entity.setErrorMessage(errorMessage);
        entity.setErrorStack(errorStack);

        // 计算并保存执行耗时
        entity.setDurationMs(calculateDuration(entity.getStartedAt(), entity.getEndedAt()));

        // 保存更新
        nodeTraceRepo.save(entity);

        // 发送节点失败事件
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
     * <p>
     * 当节点因为条件分支或其他原因被跳过时调用。
     *
     * <h3>触发场景：</h3>
     * <ul>
     *   <li>条件节点走向了其他分支，当前分支被跳过</li>
     *   <li>并行分支中，部分分支被跳过</li>
     *   <li>循环节点中，未执行的迭代被跳过</li>
     * </ul>
     *
     * <h3>注意：</h3>
     * <ul>
     *   <li>被跳过的节点不会执行，也不会有耗时</li>
     *   <li>被跳过的节点不会阻塞依赖它的下游节点</li>
     * </ul>
     *
     * @param traceId 节点追踪记录 ID
     * @param reason 跳过的原因
     * @throws IllegalArgumentException 如果找不到对应的追踪记录
     */
    @Transactional
    public void markNodeSkipped(Long traceId, String reason) {
        // 查找节点追踪记录
        NodeExecutionTraceEntity entity = nodeTraceRepo.findById(traceId)
                .orElseThrow(() -> new IllegalArgumentException("Node trace not found: " + traceId));

        // 更新状态为跳过
        entity.setStatus(ExecutionStatusConstants.NODE_SKIPPED);

        // 保存跳过原因
        entity.setErrorMessage(reason);

        // 保存更新
        nodeTraceRepo.save(entity);

        // 发送节点跳过事件
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
     * <p>
     * 获取指定工作流的所有历史执行记录，按创建时间倒序排列。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>工作流执行历史查看</li>
     *   <li>工作流统计和分析</li>
     *   <li>问题排查和复现</li>
     * </ul>
     *
     * @param workflowId 工作流 ID
     * @param limit 返回记录数量限制
     * @return 工作流执行记录列表
     */
    public List<WorkflowExecutionEntity> getExecutions(String workflowId, int limit) {
        // 查询并限制返回数量
        return executionRepo.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * 根据执行ID获取执行状态视图
     * <p>
     * 用于 API 查询工作流的当前执行状态。
     *
     * <h3>返回的视图信息：</h3>
     * <ul>
     *   <li>workflowId - 工作流 ID</li>
     *   <li>status - 当前状态</li>
     *   <li>variables - 变量上下文（来自输出）</li>
     * </ul>
     *
     * @param executionId 工作流执行记录 ID
     * @return 工作流状态视图，如果不存在返回 null
     */
    public com.sipc115.helix.domain.workflow.WorkflowStateView getExecutionStateView(Long executionId) {
        return executionRepo.findById(executionId)
                .map(entity -> {
                    // 创建状态视图
                    com.sipc115.helix.domain.workflow.WorkflowStateView view =
                            new com.sipc115.helix.domain.workflow.WorkflowStateView();
                    view.setWorkflowId(entity.getWorkflowId());
                    view.setStatus(com.sipc115.helix.domain.workflow.ExecutionStatus.valueOf(entity.getStatus()));
                    view.setVariables(entity.getOutput() != null ? entity.getOutput() : new java.util.HashMap<>());
                    return view;
                })
                .orElse(null);
    }

    /**
     * 查询某次执行的所有节点追踪记录
     * <p>
     * 获取指定工作流执行的所有节点追踪记录，按执行顺序排列。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>工作流执行详情查看</li>
     *   <li>节点执行顺序分析</li>
     *   <li>问题排查和调试</li>
     * </ul>
     *
     * @param executionId 工作流执行记录 ID
     * @return 节点追踪记录列表
     */
    public List<NodeExecutionTraceEntity> getNodeTraces(Long executionId) {
        return nodeTraceRepo.findByExecutionIdOrderByExecutionOrder(executionId);
    }

    /**
     * 查询指定节点的成功追踪记录
     * <p>
     * 获取指定工作流执行中某个节点的所有追踪记录。
     * 用于幂等性检查时直接查询特定节点。
     *
     * <h3>性能优化：</h3>
     * <p>
     * 此方法使用数据库索引直接查询，比遍历所有节点更高效。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>Activity 幂等性检查</li>
     *   <li>节点重跑历史查询</li>
     *   <li>特定节点执行历史分析</li>
     * </ul>
     *
     * @param executionId 工作流执行记录 ID
     * @param nodeId 节点 ID
     * @return 该节点的追踪记录列表（按执行顺序排列）
     */
    public List<NodeExecutionTraceEntity> getNodeTracesByNodeId(Long executionId, String nodeId) {
        return nodeTraceRepo.findByExecutionIdAndNodeId(executionId, nodeId);
    }

    /**
     * 获取工作流执行统计信息
     * <p>
     * 统计指定工作流的执行情况，包括总数、成功数、失败数和平均耗时。
     *
     * <h3>返回的统计信息：</h3>
     * <ul>
     *   <li>total - 总执行次数</li>
     *   <li>success - 成功次数</li>
     *   <li>failed - 失败次数</li>
     *   <li>avgDurationMs - 平均执行耗时（毫秒）</li>
     * </ul>
     *
     * @param workflowId 工作流 ID
     * @return 执行统计信息
     */
    public ExecutionStats getStats(String workflowId) {
        // 查询该工作流的所有执行记录
        List<WorkflowExecutionEntity> executions = executionRepo.findByWorkflowId(workflowId);

        // 创建统计对象
        ExecutionStats stats = new ExecutionStats();

        // 统计总执行次数
        stats.setTotal(executions.size());

        // 统计成功次数
        stats.setSuccess(executions.stream()
                .filter(e -> ExecutionStatusConstants.WORKFLOW_SUCCESS.equals(e.getStatus()))
                .count());

        // 统计失败次数
        stats.setFailed(executions.stream()
                .filter(e -> ExecutionStatusConstants.WORKFLOW_FAILED.equals(e.getStatus()))
                .count());

        // 计算平均执行耗时（只统计有耗时记录的）
        stats.setAvgDurationMs(executions.stream()
                .filter(e -> e.getTotalDurationMs() != null)
                .mapToLong(WorkflowExecutionEntity::getTotalDurationMs)
                .average()
                .orElse(0.0));

        return stats;
    }

    /**
     * 计算执行耗时
     * <p>
     * 计算从开始时间到结束时间的毫秒数。
     *
     * <h3>边界情况处理：</h3>
     * <ul>
     *   <li>如果 start 或 end 为 null，返回 0</li>
     *   <li>使用 Duration.between 计算时间差</li>
     * </ul>
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 毫秒数，如果时间无效返回 0
     */
    private long calculateDuration(Instant start, Instant end) {
        // 处理空值情况
        if (start == null || end == null) {
            return 0L;
        }
        // 计算时间差并转换为毫秒
        return java.time.Duration.between(start, end).toMillis();
    }

    /**
     * 工作流执行统计信息
     * <p>
     * 封装工作流执行的统计数据，用于监控和分析。
     *
     * <h3>用途：</h3>
     * <ul>
     *   <li>工作流健康度监控</li>
     *   <li>性能趋势分析</li>
     *   <li>SLA 指标统计</li>
     * </ul>
     */
    @lombok.Data
    public static class ExecutionStats {
        /**
         * 总执行次数
         */
        private long total;

        /**
         * 成功执行次数
         */
        private long success;

        /**
         * 失败执行次数
         */
        private long failed;

        /**
         * 平均执行耗时（毫秒）
         * <p>
         * 只统计有耗时记录的样本。
         * 如果所有执行都没有耗时记录，返回 0.0。
         */
        private double avgDurationMs;
    }
}
/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.workflow.WorkflowTraceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流追踪消息队列服务
 * <p>
 * 负责将工作流执行追踪事件发送到 RocketMQ，实现追踪数据的异步处理和削峰填谷。
 * 采用三级发送策略：
 * <ul>
 *   <li>L1 工作流事件：同步发送，确保关键事件不丢失</li>
 *   <li>L2 节点事件：异步发送，平衡性能和可靠性</li>
 *   <li>L3 AI步骤事件：批量缓冲发送，攒够指定数量后批量发送，提高吞吐量</li>
 * </ul>
 *
 * <h3>Topic 和 Tag 配置：</h3>
 * <ul>
 *   <li>Topic: workflow-trace-topic</li>
 *   <li>Tag(workflow): workflow - 工作流级别事件</li>
 *   <li>Tag(node): node - 节点级别事件</li>
 *   <li>Tag(ai_step): ai_step - AI步骤级别事件</li>
 * </ul>
 *
 * <h3>消息格式：</h3>
 * <ul>
 *   <li>工作流/节点事件：单个 JSON 对象</li>
 *   <li>AI步骤事件：JSON 数组格式，便于批量解析</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowTraceEvent
 */
@Slf4j
@Service
public class WorkflowTraceMQService {

    // ==================== MQ 配置常量 ====================

    /**
     * RocketMQ Topic 名称
     * <p>
     * 所有工作流追踪事件都发送到此 Topic。
     * 需要提前在 RocketMQ 服务端创建此 Topic。
     */
    private static final String TOPIC_TRACE = "workflow-trace-topic";

    /**
     * 工作流事件 Tag
     * <p>
     * 用于标识工作流级别的事件，如工作流启动、完成。
     */
    private static final String TAG_WORKFLOW = "workflow";

    /**
     * 节点事件 Tag
     * <p>
     * 用于标识节点级别的事件，如节点开始执行、完成。
     */
    private static final String TAG_NODE = "node";

    /**
     * AI步骤事件 Tag
     * <p>
     * 用于标识AI任务内部步骤的事件，如多轮对话的每次交互。
     */
    private static final String TAG_AI_STEP = "ai_step";

    /**
     * AI步骤缓冲池大小
     * <p>
     * 当缓冲池中的 AI 步骤事件达到此数量时，触发批量发送。
     * 默认为 10 条，可以根据实际吞吐量需求调整。
     */
    private static final int AI_STEP_BUFFER_SIZE = 10;

    // ==================== 依赖组件 ====================

    /**
     * RocketMQ 消息模板
     * <p>
     * 用于发送消息到 RocketMQ，支持同步、异步、延迟等多种发送模式。
     * 通过 Spring 自动注入获取实例。
     */
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * JSON 对象映射器
     * <p>
     * 用于将 WorkflowTraceEvent 对象序列化为 JSON 字符串。
     * 采用构造函数注入，便于单元测试时替换为 Mock 对象。
     */
    private final ObjectMapper objectMapper;

    /**
     * AI步骤事件缓冲池
     * <p>
     * 线程安全的 ArrayList，用于临时存储 AI 步骤事件。
     * 使用 synchronized 代码块保证线程安全。
     * 当达到缓冲池大小时，触发批量发送。
     */
    private final List<WorkflowTraceEvent> aiStepBuffer = new ArrayList<>();

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param objectMapper JSON 对象映射器，用于序列化事件对象
     */
    public WorkflowTraceMQService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== 公共方法 ====================

    /**
     * 发送工作流事件（同步）
     * <p>
     * 用于发送工作流级别的追踪事件，如工作流启动和完成。
     * 采用同步发送方式，确保消息成功发送到 RocketMQ 后才返回。
     * 如果发送失败，将抛出 RuntimeException 触发事务回滚。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>工作流启动时 - 记录工作流开始执行的输入参数</li>
     *   <li>工作流完成时 - 记录工作流执行结果或错误信息</li>
     * </ul>
     *
     * @param event 工作流追踪事件对象，不能为 null
     * @throws RuntimeException 如果消息发送失败，将终止当前事务
     */
    public void sendWorkflowEvent(WorkflowTraceEvent event) {
        try {
            // 将事件对象序列化为 JSON 字符串
            String messageBody = objectMapper.writeValueAsString(event);

            // 同步发送消息到指定的 Topic 和 Tag
            // syncSend 会阻塞直到收到 Broker 的确认响应
            rocketMQTemplate.syncSend(TOPIC_TRACE + ":" + TAG_WORKFLOW, messageBody);

            log.debug("Workflow event sent to MQ: type={}, executionId={}",
                event.getEventType(), event.getExecutionId());
        } catch (Exception e) {
            // 记录错误日志并抛出运行时异常
            // 上层服务会捕获此异常并触发事务回滚
            log.error("Failed to send workflow event to MQ: {}", e.getMessage(), e);
            throw new RuntimeException("MQ send failed for workflow event", e);
        }
    }

    /**
     * 发送节点事件（异步）
     * <p>
     * 用于发送节点级别的追踪事件，如节点开始执行和完成。
     * 采用异步发送方式，发送后立即返回，不阻塞主流程。
     * 即使发送失败也只是记录日志，不影响工作流的正常执行。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>节点开始执行时 - 记录节点类型、输入参数</li>
     *   <li>节点执行完成时 - 记录执行结果、输出参数</li>
     *   <li>节点执行失败时 - 记录错误信息和堆栈</li>
     * </ul>
     *
     * @param event 节点追踪事件对象，不能为 null
     */
    public void sendNodeEvent(WorkflowTraceEvent event) {
        try {
            // 序列化为 JSON 字符串
            String messageBody = objectMapper.writeValueAsString(event);

            // 异步发送消息，通过 SendCallback 回调处理发送结果
            // asyncSend 不会阻塞，发送完成后回调 onSuccess 或 onException
            rocketMQTemplate.asyncSend(TOPIC_TRACE + ":" + TAG_NODE, messageBody, new SendCallback() {
                /**
                 * 消息发送成功回调
                 * <p>
                 * 仅记录调试级别的日志，不影响主流程。
                 *
                 * @param sendResult 发送结果，包含消息ID、发送时间等信息
                 */
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("Node event sent to MQ: type={}, nodeId={}",
                        event.getEventType(), event.getNodeId());
                }

                /**
                 * 消息发送异常回调
                 * <p>
                 * 记录错误日志，但不会抛出异常影响主流程。
                 * 这是异步发送的预期行为，丢消息不会导致工作流失败。
                 *
                 * @param e 发送异常信息
                 */
                @Override
                public void onException(Throwable e) {
                    log.error("Failed to send node event to MQ: nodeId={}, error={}",
                        event.getNodeId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            // 捕获序列化等异常，同样只是记录日志
            log.error("Failed to send node event to MQ: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送 AI 步骤事件（缓冲模式）
     * <p>
     * 用于发送 AI 任务内部的微流程步骤事件。
     * 不直接发送消息，而是将事件添加到本地缓冲池。
     * 当缓冲池达到指定大小（{@link #AI_STEP_BUFFER_SIZE}）时，自动触发批量发送。
     *
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>AI 多轮对话的每次交互</li>
     *   <li>AI 生成、验证、修复等步骤</li>
     * </ul>
     *
     * <h3>性能优化：</h3>
     * <p>
     * 通过批量发送减少网络往返次数，提高消息吞吐量。
     * 适合 AI 步骤这种高频、小体积的消息场景。
     *
     * @param event AI步骤追踪事件对象，不能为 null
     * @see #flushAiStepBuffer() 手动触发缓冲池刷新
     */
    public void sendAiStepEvent(WorkflowTraceEvent event) {
        // 使用同步代码块保证缓冲池的线程安全
        // 多个线程可能同时调用此方法（AI步骤可能并发执行）
        synchronized (aiStepBuffer) {
            // 将事件添加到缓冲池
            aiStepBuffer.add(event);

            // 检查是否达到批量发送的阈值
            if (aiStepBuffer.size() >= AI_STEP_BUFFER_SIZE) {
                flushAiStepBuffer();
            }
        }
    }

    /**
     * 刷新 AI 步骤缓冲池
     * <p>
     * 将缓冲池中的所有 AI 步骤事件批量发送到 RocketMQ。
     * 发送完成后清空缓冲池。
     *
     * <h3>触发时机：</h3>
     * <ul>
     *   <li>缓冲池达到阈值大小时自动触发</li>
     *   <li>AI 节点执行完成时由上层服务手动调用</li>
     *   <li>工作流结束前确保所有事件都被发送</li>
     * </ul>
     *
     * <h3>消息格式：</h3>
     * <p>
     * 所有事件序列化为 JSON 数组格式：
     * <pre>
     * [event1, event2, event3, ...]
     * </pre>
     * Consumer 可以直接解析为 JSON 数组批量处理。
     */
    public void flushAiStepBuffer() {
        // 从缓冲池中取出所有待发送的事件
        List<WorkflowTraceEvent> eventsToSend;

        synchronized (aiStepBuffer) {
            // 如果缓冲池为空，直接返回不发送任何消息
            if (aiStepBuffer.isEmpty()) {
                return;
            }

            // 复制一份事件列表用于发送
            // 原列表需要在同步块内清空以释放内存
            eventsToSend = new ArrayList<>(aiStepBuffer);
            aiStepBuffer.clear();
        }

        try {
            // 将事件列表序列化为 JSON 数组字符串
            // 例如: [{"eventType":"AI_STEP",...},{"eventType":"AI_STEP",...}]
            String messageBody = objectMapper.writeValueAsString(eventsToSend);

            // 异步批量发送，传递事件数量用于日志记录
            final int eventCount = eventsToSend.size();
            rocketMQTemplate.asyncSend(TOPIC_TRACE + ":" + TAG_AI_STEP, messageBody, new SendCallback() {
                /**
                 * 批量发送成功回调
                 *
                 * @param sendResult 发送结果
                 */
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("AI step batch sent to MQ: count={}", eventCount);
                }

                /**
                 * 批量发送异常回调
                 * <p>
                 * 记录错误日志，事件已经丢失。
                 * 但由于 DB 中已有持久化记录，可以通过 DB 恢复或重新处理。
                 *
                 * @param e 发送异常
                 */
                @Override
                public void onException(Throwable e) {
                    log.error("Failed to send AI step batch to MQ: count={}, error={}",
                        eventCount, e.getMessage());
                }
            });
        } catch (Exception e) {
            // 捕获序列化异常，记录错误日志
            // 消息已从缓冲池取出但未发送成功，数据丢失
            log.error("Failed to flush AI step buffer: {}", e.getMessage(), e);
        }
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.common.constant.MqConstants;
import com.sipc115.helix.domain.workflow.WorkflowTraceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.stereotype.Service;

/**
 * 工作流追踪消息队列服务
 * <p>
 * 负责将工作流执行追踪事件发送到 RocketMQ，实现追踪数据的异步处理和削峰填谷。
 * 采用两级发送策略：
 * <ul>
 *   <li>L1 工作流事件：同步发送，确保关键事件不丢失</li>
 *   <li>L2 节点事件：异步发送，平衡性能和可靠性</li>
 * </ul>
 *
 * <h3>Topic 和 Tag 配置：</h3>
 * <ul>
 *   <li>Topic: workflow-trace-topic</li>
 *   <li>Tag(workflow): workflow - 工作流级别事件</li>
 *   <li>Tag(node): node - 节点级别事件</li>
 * </ul>
 *
 * <h3>消息格式：</h3>
 * <ul>
 *   <li>工作流/节点事件：单个 JSON 对象</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowTraceEvent
 * @see RocketMQProducer
 */
@Slf4j
@Service
public class WorkflowTraceMQService {

    private final RocketMQProducer rocketMQProducer;
    private final ObjectMapper objectMapper;

    public WorkflowTraceMQService(RocketMQProducer rocketMQProducer, ObjectMapper objectMapper) {
        this.rocketMQProducer = rocketMQProducer;
        this.objectMapper = objectMapper;
    }

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
            String messageBody = objectMapper.writeValueAsString(event);

            SendResult result = rocketMQProducer.sendMessageSync(MqConstants.TOPIC_TRACE, MqConstants.TAG_WORKFLOW, messageBody);

            if (result.getSendStatus() == SendStatus.SEND_OK) {
                log.debug("Workflow event sent to MQ: type={}, executionId={}, msgId={}",
                    event.getEventType(), event.getExecutionId(), result.getMsgId());
            } else {
                log.error("Workflow event send failed: type={}, executionId={}, status={}",
                    event.getEventType(), event.getExecutionId(), result.getSendStatus());
                throw new RuntimeException("MQ send failed with status: " + result.getSendStatus());
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
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
            String messageBody = objectMapper.writeValueAsString(event);

            rocketMQProducer.sendMessage(MqConstants.TOPIC_TRACE, MqConstants.TAG_NODE, messageBody, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    if (result.getSendStatus() == SendStatus.SEND_OK) {
                        log.debug("Node event sent to MQ: type={}, nodeId={}, msgId={}",
                            event.getEventType(), event.getNodeId(), result.getMsgId());
                    } else {
                        log.warn("Node event send status abnormal: type={}, nodeId={}, status={}",
                            event.getEventType(), event.getNodeId(), result.getSendStatus());
                    }
                }

                @Override
                public void onException(Throwable e) {
                    log.error("Failed to send node event to MQ: nodeId={}, error={}",
                        event.getNodeId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to send node event to MQ: {}", e.getMessage(), e);
        }
    }
}
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTraceMQService {

    private static final String TOPIC_TRACE = "workflow-trace-topic";
    private static final String TAG_WORKFLOW = "workflow";
    private static final String TAG_NODE = "node";
    private static final String TAG_AI_STEP = "ai_step";

    private static final int AI_STEP_BUFFER_SIZE = 10;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    private final List<WorkflowTraceEvent> aiStepBuffer = new ArrayList<>();

    public void sendWorkflowEvent(WorkflowTraceEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            rocketMQTemplate.syncSend(TOPIC_TRACE + ":" + TAG_WORKFLOW, messageBody);
            log.debug("Workflow event sent to MQ: type={}, executionId={}",
                event.getEventType(), event.getExecutionId());
        } catch (Exception e) {
            log.error("Failed to send workflow event to MQ: {}", e.getMessage(), e);
            throw new RuntimeException("MQ send failed for workflow event", e);
        }
    }

    public void sendNodeEvent(WorkflowTraceEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            rocketMQTemplate.asyncSend(TOPIC_TRACE + ":" + TAG_NODE, messageBody, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("Node event sent to MQ: type={}, nodeId={}",
                        event.getEventType(), event.getNodeId());
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

    public void sendAiStepEvent(WorkflowTraceEvent event) {
        synchronized (aiStepBuffer) {
            aiStepBuffer.add(event);
            if (aiStepBuffer.size() >= AI_STEP_BUFFER_SIZE) {
                flushAiStepBuffer();
            }
        }
    }

    public void flushAiStepBuffer() {
        List<WorkflowTraceEvent> eventsToSend;
        synchronized (aiStepBuffer) {
            if (aiStepBuffer.isEmpty()) {
                return;
            }
            eventsToSend = new ArrayList<>(aiStepBuffer);
            aiStepBuffer.clear();
        }

        try {
            String messageBody = objectMapper.writeValueAsString(eventsToSend);
            rocketMQTemplate.asyncSend(TOPIC_TRACE + ":" + TAG_AI_STEP, messageBody, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("AI step batch sent to MQ: count={}", eventsToSend.size());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("Failed to send AI step batch to MQ: count={}, error={}",
                        eventsToSend.size(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Failed to flush AI step buffer: {}", e.getMessage(), e);
        }
    }
}

/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.bridge.temporal;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.HumanSignalPayload;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.bridge.temporal.activity.ActivityTaskRouterActivity;
import com.sipc115.helix.domain.workflow.ActivityTaskRequest;
import com.sipc115.helix.integration.lark.FeishuClient;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Temporal 工作流运行时桥接实现
 */
public class TemporalWorkflowRuntimeBridge implements WorkflowRuntimeBridge {
    
    private static final Logger logger = LoggerFactory.getLogger(TemporalWorkflowRuntimeBridge.class);

    private final List<HumanSignalPayload> bufferedSignals;
    
    private final FeishuClient feishuClient;

    public TemporalWorkflowRuntimeBridge(List<HumanSignalPayload> bufferedSignals, FeishuClient feishuClient) {
        this.bufferedSignals = bufferedSignals;
        this.feishuClient = feishuClient;
    }

    @Override
    public HumanSignalPayload awaitHumanSignal(String expectedNodeId) {
        Workflow.await(() -> bufferedSignals.stream().anyMatch(signal -> expectedNodeId.equals(signal.getNodeId())));

        for (HumanSignalPayload payload : bufferedSignals) {
            if (expectedNodeId.equals(payload.getNodeId())) {
                bufferedSignals.remove(payload);
                return payload;
            }
        }
        throw new IllegalStateException("Signal arrived but matching payload not found");
    }
    
    @Override
    public boolean sendFeishuText(String chatId, String text) {
        try {
            logger.info("Sending Feishu text message. chatId={}, textLength={}", chatId, text != null ? text.length() : 0);
            
            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendText(text);
            } else {
                messageId = feishuClient.sendTextToChat(chatId, text);
            }
            
            logger.info("Feishu text message sent successfully. messageId={}", messageId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send Feishu text message", e);
            return false;
        }
    }
    
    @Override
    public boolean sendFeishuPost(String chatId, String title, List<String> lines) {
        try {
            logger.info("Sending Feishu post message. chatId={}, title={}", chatId, title);
            
            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendPost(title, lines);
            } else {
                messageId = feishuClient.sendPostToChat(chatId, title, lines);
            }
            
            logger.info("Feishu post message sent successfully. messageId={}", messageId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send Feishu post message", e);
            return false;
        }
    }
    
    @Override
    public boolean sendFeishuPostWithLink(String chatId, String title, String text, String url, String linkText) {
        try {
            logger.info("Sending Feishu post with link. chatId={}, url={}", chatId, url);
            
            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendPostWithLink(title, text, url, linkText);
            } else {
                messageId = feishuClient.sendPostWithLinkToChat(chatId, title, text, url, linkText);
            }
            
            logger.info("Feishu post with link sent successfully. messageId={}", messageId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send Feishu post with link", e);
            return false;
        }
    }
    
    @Override
    public String publishFeishuCloudDoc(String title, String content) {
        try {
            logger.info("Publishing Feishu cloud doc. title={}", title);
            
            String docUrl = feishuClient.publishCloudDocIfEnabled(title, content);
            
            if (docUrl != null) {
                logger.info("Feishu cloud doc published successfully. docUrl={}", docUrl);
            } else {
                logger.warn("Feishu cloud doc is disabled, skipped publishing");
            }
            
            return docUrl;
            
        } catch (Exception e) {
            logger.error("Failed to publish Feishu cloud doc", e);
            return null;
        }
    }
}
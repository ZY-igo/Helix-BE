/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat.activity;

import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.llm.LlmAuthClient;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 聊天 Activity 实现
 * <p>
 * 实现了 {@link AiChatActivity} 接口，
 * 负责在 Activity Worker 上执行 LLM API 调用。
 *
 * <h3>幂等性保护机制：</h3>
 * <p>
 * 为了防止 Temporal 重试导致重复调用 LLM API（通常费用较高），
 * 此 Activity 实现了幂等性保护：执行前检查该节点是否已成功，
 * 如果已成功则直接返回缓存的响应内容。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see AiChatActivity
 */
@Component
public class AiChatActivityImpl implements AiChatActivity {

    private static final Logger log = LoggerFactory.getLogger(AiChatActivityImpl.class);

    private static ConnectionClientRegistry connectionRegistry;
    private static WorkflowTraceService traceService;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        AiChatActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        AiChatActivityImpl.traceService = traceService;
    }

    @Override
    public String chat(Long executionId, Integer retryCount, String nodeId,
                      Long connectionId, String systemPrompt, String userPrompt,
                      Double temperature, Integer maxTokens, String thinking) {
        log.info("AiChatActivity: 开始调用 LLM, executionId={}, nodeId={}, retryCount={}, connectionId={}",
            executionId, nodeId, retryCount, connectionId);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedResponse = checkAlreadySucceeded(attemptId);
        if (cachedResponse != null) {
            log.info("AiChatActivity: 检测到重复执行，返回缓存结果, attemptId={}, responseLength={}",
                attemptId, cachedResponse.length());
            return cachedResponse;
        }

        Long traceId = startNodeTracking(executionId, nodeId);

        try {
            LlmAuthClient llmClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            log.debug("获取 LLM 客户端成功, connectionId={}", connectionId);

            String response = llmClient.chat(systemPrompt, userPrompt, temperature, maxTokens, thinking);
            log.info("AiChatActivity: LLM 调用成功, attemptId={}, responseLength={}",
                attemptId, response != null ? response.length() : 0);

            Map<String, Object> successOutput = new HashMap<>();
            successOutput.put("response", response);
            markNodeSuccess(traceId, successOutput);
            return response;

        } catch (Exception e) {
            log.error("AiChatActivity: LLM 调用失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("AI 对话调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String chatWithConfig(Long executionId, Integer retryCount, String nodeId,
                                Object connectionConfig, String systemPrompt, String userPrompt,
                                Double temperature, Integer maxTokens, String thinking) {
        log.info("AiChatActivity: 开始调用 LLM(带配置), executionId={}, nodeId={}, retryCount={}",
            executionId, nodeId, retryCount);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedResponse = checkAlreadySucceeded(attemptId);
        if (cachedResponse != null) {
            log.info("AiChatActivity: 检测到重复执行，返回缓存结果, attemptId={}, responseLength={}",
                attemptId, cachedResponse.length());
            return cachedResponse;
        }

        Long traceId = startNodeTracking(executionId, nodeId);

        try {
            LlmAuthClient llmClient = connectionRegistry.getOrCreateClient(
                null,
                "LLM",
                connectionConfig
            );
            log.debug("获取 LLM 客户端成功");

            String response = llmClient.chat(systemPrompt, userPrompt, temperature, maxTokens, thinking);
            log.info("AiChatActivity: LLM 调用成功, attemptId={}, responseLength={}",
                attemptId, response != null ? response.length() : 0);

            Map<String, Object> successOutput = new HashMap<>();
            successOutput.put("response", response);
            markNodeSuccess(traceId, successOutput);
            return response;

        } catch (Exception e) {
            log.error("AiChatActivity: LLM 调用失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("AI 对话调用失败: " + e.getMessage(), e);
        }
    }

    private String checkAlreadySucceeded(String attemptId) {
        if (traceService == null) {
            log.debug("WorkflowTraceService 未注入，跳过幂等性检查");
            return null;
        }

        try {
            String[] parts = attemptId.split("_");
            if (parts.length < 3) {
                log.warn("无效的 attemptId 格式: {}", attemptId);
                return null;
            }

            long executionId = Long.parseLong(parts[0]);
            String nodeId = parts[1];

            var traces = traceService.getNodeTracesByNodeId(executionId, nodeId);

            for (var trace : traces) {
                if ("SUCCESS".equals(trace.getStatus()) && trace.getOutput() != null) {
                    Object response = trace.getOutput().get("response");
                    if (response != null) {
                        log.info("检测到节点已成功执行过, executionId={}, nodeId={}, cachedResponseLength={}",
                            executionId, nodeId, response.toString().length());
                        return response.toString();
                    }
                }
            }

            log.debug("节点未执行过或未成功: attemptId={}", attemptId);
            return null;

        } catch (Exception e) {
            log.warn("检查节点成功状态异常，跳过幂等检查: attemptId={}, error={}",
                attemptId, e.getMessage());
            return null;
        }
    }

    private Long startNodeTracking(Long executionId, String nodeId) {
        if (traceService == null) {
            return null;
        }
        try {
            var trace = traceService.startNodeExecution(
                executionId, nodeId, "AiChat",
                "NORMAL", 0, null, 0
            );
            return trace != null ? trace.getId() : null;
        } catch (Exception e) {
            log.warn("启动节点追踪失败: {}", e.getMessage());
            return null;
        }
    }

    private void markNodeSuccess(Long traceId, Map<String, Object> output) {
        if (traceService == null || traceId == null) {
            return;
        }
        try {
            traceService.markNodeSuccess(traceId, output);
        } catch (Exception e) {
            log.warn("标记节点成功失败: {}", e.getMessage());
        }
    }

    private void markNodeFailed(Long traceId, String errorMessage) {
        if (traceService == null || traceId == null) {
            return;
        }
        try {
            traceService.markNodeFailed(traceId, errorMessage, null);
        } catch (Exception e) {
            log.warn("标记节点失败失败: {}", e.getMessage());
        }
    }
}
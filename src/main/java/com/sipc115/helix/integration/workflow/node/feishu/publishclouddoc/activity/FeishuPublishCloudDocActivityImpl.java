/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 飞书发布云文档 Activity 实现
 * <p>
 * 实现了 {@link FeishuPublishCloudDocActivity} 接口，
 * 负责在 Activity Worker 上执行飞书云文档发布 API 调用。
 *
 * <h3>幂等性保护机制：</h3>
 * <p>
 * 为了防止 Temporal 重试导致重复创建文档，此 Activity 实现了幂等性保护：
 * 执行前检查该节点是否已成功，如果已成功则直接返回缓存的文档 URL。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuPublishCloudDocActivity
 */
@Component
public class FeishuPublishCloudDocActivityImpl implements FeishuPublishCloudDocActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuPublishCloudDocActivityImpl.class);

    private static ConnectionClientRegistry connectionRegistry;
    private static WorkflowTraceService traceService;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuPublishCloudDocActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuPublishCloudDocActivityImpl.traceService = traceService;
    }

    @Override
    public String publishCloudDoc(Long executionId, Integer retryCount, String nodeId,
                                  Long connectionId, String title, String content) {
        log.info("FeishuPublishCloudDocActivity: 开始发布云文档, executionId={}, nodeId={}, retryCount={}, title={}",
            executionId, nodeId, retryCount, title);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedUrl = checkAlreadySucceeded(attemptId);
        if (cachedUrl != null) {
            log.info("FeishuPublishCloudDocActivity: 检测到重复执行，返回缓存结果, attemptId={}, url={}",
                attemptId, cachedUrl);
            return cachedUrl;
        }

        try {
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功, connectionId={}", connectionId);

            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            String documentId = handler.createDocument(token, title, null);
            String url = "https://feishu.cn/docx/" + documentId;
            log.info("FeishuPublishCloudDocActivity: 云文档创建成功, attemptId={}, documentId={}, url={}",
                attemptId, documentId, url);

            return url;

        } catch (Exception e) {
            log.error("FeishuPublishCloudDocActivity: 云文档发布失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            throw new RuntimeException("飞书发布云文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String publishCloudDocWithConfig(Long executionId, Integer retryCount, String nodeId,
                                            Object connectionConfig, String title, String content) {
        log.info("FeishuPublishCloudDocActivity: 开始发布云文档(带配置), executionId={}, nodeId={}, retryCount={}, title={}",
            executionId, nodeId, retryCount, title);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedUrl = checkAlreadySucceeded(attemptId);
        if (cachedUrl != null) {
            log.info("FeishuPublishCloudDocActivity: 检测到重复执行，返回缓存结果, attemptId={}, url={}",
                attemptId, cachedUrl);
            return cachedUrl;
        }

        try {
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClient(
                null,
                "FEISHU",
                connectionConfig
            );

            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功");

            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            String documentId = handler.createDocument(token, title, null);
            String url = "https://feishu.cn/docx/" + documentId;
            log.info("FeishuPublishCloudDocActivity: 云文档创建成功, attemptId={}, documentId={}, url={}",
                attemptId, documentId, url);

            return url;

        } catch (Exception e) {
            log.error("FeishuPublishCloudDocActivity: 云文档发布失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            throw new RuntimeException("飞书发布云文档失败: " + e.getMessage(), e);
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
                    Object url = trace.getOutput().get("documentUrl");
                    if (url != null) {
                        log.info("检测到节点已成功执行过, executionId={}, nodeId={}, cachedUrl={}",
                            executionId, nodeId, url);
                        return url.toString();
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
}
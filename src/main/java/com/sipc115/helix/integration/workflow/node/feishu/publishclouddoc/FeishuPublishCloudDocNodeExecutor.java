/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc;

import com.sipc115.helix.common.constant.BranchKeyConstants;
import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc.activity.FeishuPublishCloudDocActivity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书发布云文档节点执行器
 * <p>
 * 负责在飞书云空间中创建并发布云文档。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_PUBLISH_CLOUD_DOC",
 *   "config": {
 *     "connectionId": 123,
 *     "title": "每日报告",
 *     "content": "报告内容..."
 *   }
 * }
 * </pre>
 *
 * <h3>架构说明（使用 Temporal Activity）：</h3>
 * <p>
 * 此执行器将 API 调用委托给 Temporal Activity 执行，
 * 保证 Workflow 的确定性。
 *
 * <h3>幂等性保护：</h3>
 * <p>
 * 通过传递 executionId、nodeId、retryCount 给 Activity，
 * Activity 可以实现幂等性检查，避免 Temporal 重试导致重复创建文档。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowNodeExecutor
 * @see FeishuPublishCloudDocActivity
 */
@Component
public class FeishuPublishCloudDocNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(FeishuPublishCloudDocNodeExecutor.class);

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_PUBLISH_CLOUD_DOC.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        Map<String, Object> config = node.getConfig();
        Long connectionId = getLongValue(config, "connectionId");
        if (connectionId == null) {
            throw new IllegalArgumentException("节点配置错误: connectionId 不能为空，节点ID: " + node.getId());
        }
        String title = getStringValue(config, "title");
        String content = getStringValue(config, "content");

        Map<String, Object> output = new HashMap<>();
        output.put("action", "publishCloudDoc");
        output.put("connectionId", connectionId);
        output.put("title", title);

        boolean success = false;
        try {
            ActivityInvocationSpec activitySpec = ActivityInvocationSpec.fromNodeConfig(config);
            FeishuPublishCloudDocActivity activity = bridge.activities().getActivity(FeishuPublishCloudDocActivity.class, activitySpec);

            Long executionId = context.getExecutionId();
            String nodeId = node.getId();

            String docUrl;
            Object cachedConfig = config.get(WorkflowConstants.CONNECTION_CONFIG_KEY);
            if (cachedConfig != null) {
                docUrl = activity.publishCloudDocWithConfig(
                    executionId, 0, nodeId,
                    cachedConfig, title, content);
            } else {
                docUrl = activity.publishCloudDoc(
                    executionId, 0, nodeId,
                    connectionId, title, content);
            }

            success = docUrl != null && !docUrl.isEmpty();
            output.put("success", success);
            output.put("documentUrl", docUrl);
            output.put("timestamp", System.currentTimeMillis());

            log.info("发布云文档完成: connectionId={}, title={}, success={}", connectionId, title, success);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            throw new RuntimeException("飞书发布云文档失败: " + e.getMessage(), e);
        }

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(success ? BranchKeyConstants.SUCCESS : BranchKeyConstants.FAILURE);
        result.setOutput(output);
        return result;
    }

    private String getStringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
